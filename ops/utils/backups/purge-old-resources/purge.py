#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import datetime
import logging
import os
import re
import time

import boto3
from botocore.exceptions import ClientError

# Globals (may be overridden via environment variables)
DRY_RUN = False
DEBUG = False

## EC2 EBS Retention
# Retain any snapshots associated with an active EC2 volume and purge all others after n days. The filter
# will match DLM lifecycle created snapshots.
EC2_MIN_RETENTION_DAYS = 1
EC2_DAILY_EBS_SNAPSHOT_FILTER = [
    {'Name': 'description','Values': ['Created for policy*']},
    {'Name': 'status','Values': ['completed']},
]
#

## Platinum AMI Retention
# Retain all platinum AMIs for n days. The filter should match all of our Platinum AMIs.
PLATINUM_AMI_MIN_RETENTION_DAYS = 365
PLATINUM_AMI_FILTER = [
    {'Name': 'tag:Name','Values': ['bfd*-platinum-*']},
    {'Name': 'state','Values': ['available']},
]
#

## Unassociated APP AMI's (fhir, pipeline, etc)
# Retain the last two application ami's just in case (this leaves the current set and two previous)
# Don't delete any app amis that are less than n days old (setting this to 1 day to be safe)
APP_AMI_RETENTION_NUM = 2  # retain last n app amis
APP_AMI_RETENTION_DAYS = 1 # older than n days
APP_AMI_FILTERS = [
    {'Name': 'name','Values': ['bfd-fhir-*']},
    {'Name': 'state','Values': ['available']},
],[
    {'Name': 'name','Values': ['bfd-etl-*']},
    {'Name': 'state','Values': ['available']},
],[
    {'Name': 'name','Values': ['bfd-amzn2-jdk*-fhir-*']},
    {'Name': 'state','Values': ['available']},
],[
    {'Name': 'name','Values': ['bfd-amzn2-jdk*-etl-*']},
    {'Name': 'state','Values': ['available']},
],[
    {'Name': 'name','Values': ['bfd-amzn2-jdk*-db-migrator-*']},
    {'Name': 'state','Values': ['available']},
],[
    {'Name': 'name','Values': ['server-load-*']},
    {'Name': 'state','Values': ['available']},
]

## RDS Retention
RDS_CLUSTER_IDS=[
    "bfd-prod-aurora-cluster",
    "bfd-prod-sbx-aurora-cluster",
    "bfd-test-aurora-cluster",
]
RDS_DAILY_RETENTION_NUM = 3          # retain last 3 dailes (we can't delete automatic snapshots so we just log them)
RDS_CPM_WEEKLY_RETENTION_NUM = 5     # retain last 5 weeklies (cpm-policy-590-.*)
RDS_CPM_MONTHLY_RETENTION_NUM = 2    # retain last 2 monthlies (cpm-policy-591-.*)
RDS_REPLICA_RETENTION_NUM = 1        # retain last 1 replicas (same policy as weekly
RDS_FILTERS_RE={
    'dailies': "^rds:bfd-.*-aurora-cluster-.*$",
    'weeklies': "^cpm-policy-590-.*$",
    'monthlies': "^cpm-policy-591-.*$",
    'replicas': "^cpm-policy-590-.*$",
}

## AWS API Throttling
# Care must be taken to not hit AWS's rate limiter as rate limits and throttling is account wide. If we just go crazy,
# we could impact other services (like autoscaling). We are using a simple leaky bucket implementation to manage our
# API calls using a fraction of AWS's posted limits. See the following for more info:
#  https://aws.amazon.com/blogs/mt/managing-monitoring-api-throttling-in-workloads/
#  https://docs.aws.amazon.com/sdkref/latest/guide/feature-retry-behavior.html
#  https://docs.aws.amazon.com/AWSEC2/latest/APIReference/throttling.html
#
# Note: "NOMU" and "MUT" means No Mutate (Describe*, Get*, List*) and Mutate (Delete*, etc.).
EC2_NOMU_RATE_BUCKET = 25 # of 100 max
EC2_NOMU_LEAK_RATE = 5 # of 20 max
EC2_MUT_RATE_BUCKET = 50 # of 200 max
EC2_MUT_LEAK_RATE = 1 # of 5 max

# I'm not sure what the RDS limits are but they are likely the same as EC2. Either way, I'm being way more conservative
# here just in case.
RDS_NOMU_RATE_BUCKET = 5
RDS_NOMU_LEAK_RATE = 1
RDS_MUT_RATE_BUCKET = 5
RDS_MUT_LEAK_RATE = 1
#

## Logger `python3 purge.py 2>&1 | tee purge.log` to capture all output.
logger = logging.getLogger(__name__)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler = logging.StreamHandler()
handler.setFormatter(formatter)
logger.addHandler(handler)
#

def get_bool_env_var(var_name, default=False):
    """Retrieve environment variable and convert to boolean."""
    true_values = {'true', '1', 'yes'}
    return os.environ.get(var_name, str(default)).lower() in true_values

class LeakyBucket:
    """
    A simple leaky bucket implementation. The bucket has a capacity and a leak rate that matches AWS's API throttling
    mechanism. The bucket will leak out water at the given leak rate per second. The bucket will fill up to its capacity
    at the given rate per second. If the bucket is full, it will not fill up any more until some water leaks out.
    """
    def __init__(self, capacity, leak_rate):
        self.capacity = capacity
        self.leak_rate = leak_rate
        self.current_water = 0
        self.last_time = time.time()

    def fill(self, amount=1):
        self._leak_out()
        if self.current_water + amount <= self.capacity:
            self.current_water += amount
            return True
        return False

    def _leak_out(self):
        now = time.time()
        leaked_amount = (now - self.last_time) * self.leak_rate
        self.current_water -= leaked_amount
        if self.current_water < 0:
            self.current_water = 0
        self.last_time = now


def get_rds_cluster_snapshots(cluster_id, region='us-east-1'):
    """
    Returns a list of available (completed) RDS cluster snapshots for the given cluster id.
    """
    client = boto3.client('rds', region_name=region)
    bucket = LeakyBucket(RDS_NOMU_RATE_BUCKET, RDS_NOMU_LEAK_RATE)
    logger.info(f"Getting RDS cluster snapshots for {cluster_id} in {region}")
    logger.debug(f"RDS NOMUT LeakyBucket: CAP {bucket.capacity} RATE {bucket.leak_rate}")
    snapshots = []
    paginator = client.get_paginator('describe_db_cluster_snapshots')
    for page in paginator.paginate(DBClusterIdentifier=cluster_id):
        if not bucket.fill() or page['ResponseMetadata']['HTTPStatusCode'] == 503:
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)
        snapshots.extend(page['DBClusterSnapshots'])
    logger.debug(f"Found {len(snapshots)} RDS cluster snapshots in {region} for {cluster_id}")

    # only return completed snapshots
    snapshots = [snapshot for snapshot in snapshots if snapshot['Status'] == 'available']
    return snapshots


def delete_rds_cluster_snapshots(region='us-east-1'):
    """
    Deletes RDS cluster snapshots according to the following retention policies:
    - RDS_DAILY_RETENTION_NUM: retain the last n daily snapshots (log only- see note below)
    - RDS_CPM_WEEKLY_RETENTION_NUM: retain the last n weekly snapshots
    - RDS_CPM_MONTHLY_RETENTION_NUM: retain the last n monthly snapshots

    Note: We cannot delete automated snapshots as they are managed by AWS so we just log them here. AWS deletes any auto
    mated snapshot past the configured retention period. I had already written the code to delete them before I learned
    this, so I'm leaving it in and just logging any automated snapshots we would like to see deleted according to the
    RDS_DAILY_RETENTION_NUM policy.
    """
    client = boto3.client('rds', region_name=region)
    bucket = LeakyBucket(RDS_MUT_RATE_BUCKET, RDS_MUT_LEAK_RATE)
    dailies = re.compile(RDS_FILTERS_RE['dailies'])
    weeklies = re.compile(RDS_FILTERS_RE['weeklies'])
    monthlies = re.compile(RDS_FILTERS_RE['monthlies'])

    # map of cluster ids with their list of snapshots sorted by creation time desc
    snapshots = {}
    for cluster in RDS_CLUSTER_IDS:
        snapshots[cluster] = get_rds_cluster_snapshots(cluster, region)
        snapshots[cluster].sort(key=lambda x: x['SnapshotCreateTime'], reverse=True)

    # gather snapshots
    logger.debug("Gathering snapshots")
    for cluster in RDS_CLUSTER_IDS:
        daily_snapshots = []
        weekly_snapshots = []
        monthly_snapshots = []
        for snapshot in snapshots[cluster]:
            if dailies.match(snapshot['DBClusterSnapshotIdentifier']):
                daily_snapshots.append(snapshot)
            elif weeklies.match(snapshot['DBClusterSnapshotIdentifier']):
                weekly_snapshots.append(snapshot)
            elif monthlies.match(snapshot['DBClusterSnapshotIdentifier']):
                monthly_snapshots.append(snapshot)

        num_dailies = len(daily_snapshots)
        num_weeklies = len(weekly_snapshots)
        num_monthlies = len(monthly_snapshots)
        logger.info(f"Found {num_dailies}/{RDS_DAILY_RETENTION_NUM} daily {cluster} snapshots in {region}")
        logger.info(f"Found {num_weeklies}/{RDS_CPM_WEEKLY_RETENTION_NUM} weekly {cluster} snapshots in {region}")
        logger.info(f"Found {num_monthlies}/{RDS_CPM_MONTHLY_RETENTION_NUM} monthly {cluster} snapshots in {region}")

        # retain the last n daily snapshots
        if num_dailies == 0:
            logger.warning(f"{cluster} has zero daily snapshots in {region}... skipping")
            continue
        if len(daily_snapshots) > RDS_DAILY_RETENTION_NUM:
            for snapshot in daily_snapshots[RDS_DAILY_RETENTION_NUM:]:
                if not bucket.fill():
                    logger.warning("Throttle bucket full or throttled, backing off...")
                    time.sleep(1)
                logger.info(f"*** {cluster} daily snapshot in {region} cannot be deleted because it"
                            "is an AWS Managed Automatic Snapshot ***")

        # retain the last n weekly snapshots
        if num_weeklies == 0:
            logger.warning(f"{cluster} has zero weekly snapshots in {region}... skipping")
            continue
        if num_weeklies > RDS_CPM_WEEKLY_RETENTION_NUM:
            logger.info(f"Found {num_weeklies} {cluster} weekly snapshots in {region} to delete!")
            for snapshot in weekly_snapshots[RDS_CPM_WEEKLY_RETENTION_NUM:]:
                if not bucket.fill():
                    logger.warning("Throttle bucket full or throttled, backing off...")
                    time.sleep(1)
                if DRY_RUN:
                    logger.info(f"Would delete {snapshot['DBClusterIdentifier']} weekly snapshot "
                                f"{snapshot['DBClusterSnapshotIdentifier']}")
                else:
                    try:
                        client.delete_db_cluster_snapshot(
                            DBClusterSnapshotIdentifier=snapshot['DBClusterSnapshotIdentifier']
                        )
                        logger.info(f"Deleted {snapshot['DBClusterIdentifier']} weekly snapshot "
                                    f"{snapshot['DBClusterSnapshotIdentifier']}")
                    except Exception as e:
                        logger.error(f"Error deleting snapshot {snapshot['DBClusterSnapshotIdentifier']}: {e}")

        # retain the last n monthly snapshots
        if num_monthlies == 0:
            logger.warning(f"{cluster} has zero monthly snapshots in {region}... skipping")
            continue
        if num_monthlies > RDS_CPM_MONTHLY_RETENTION_NUM:
            logger.info(f"Found {num_monthlies} {cluster} monthly snapshots in {region} to delete!")
            for snapshot in monthly_snapshots[RDS_CPM_MONTHLY_RETENTION_NUM:]:
                if not bucket.fill():
                    logger.warning("Throttle bucket full or throttled, backing off...")
                    time.sleep(1)
                if DRY_RUN:
                    logger.info(f"Would delete {snapshot['DBClusterIdentifier']} monthly snapshot "
                                f"{snapshot['DBClusterSnapshotIdentifier']}")
                else:
                    try:
                        client.delete_db_cluster_snapshot(
                            DBClusterSnapshotIdentifier=snapshot['DBClusterSnapshotIdentifier']
                        )
                        logger.info(f"Deleted {snapshot['DBClusterIdentifier']} monthly snapshot "
                                    f"{snapshot['DBClusterSnapshotIdentifier']}")
                    except Exception as e:
                        logger.error(
                            f"Error deleting {cluster} snapshot {snapshot['DBClusterSnapshotIdentifier']}: {e}"
                        )


def delete_rds_cluster_replicas(region='us-west-2'):
    client = boto3.client('rds', region_name=region)
    bucket = LeakyBucket(RDS_MUT_RATE_BUCKET, RDS_MUT_LEAK_RATE)
    replicas_re = re.compile(RDS_FILTERS_RE['replicas'])
    logger.debug(f"Processing RDS cluster replicas in {region}")

    # get all snapshots in the region
    snapshots = []
    paginator = client.get_paginator('describe_db_cluster_snapshots')
    for page in paginator.paginate():
        if not bucket.fill() or page['ResponseMetadata']['HTTPStatusCode'] == 503:
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)
        snapshots.extend(page['DBClusterSnapshots'])

    # filter out non-available snapshots
    snapshots = [snapshot for snapshot in snapshots if snapshot['Status'] == 'available']
    logger.debug(f"Found {len(snapshots)} RDS cluster snapshots in {region}")

    # build a map of cluster ids with their list of snapshots sorted by creation time desc
    # doing this because there are no actual clusters in the failover region, just replicated snapshots
    clusters = {}
    for cluster in RDS_CLUSTER_IDS:
        clusters[cluster] = [snapshot for snapshot in snapshots if snapshot['DBClusterIdentifier'] == cluster]
        clusters[cluster].sort(key=lambda x: x['SnapshotCreateTime'], reverse=True)

    # for each cluster, retain the last n replicas
    for cluster in RDS_CLUSTER_IDS:
        replicas = []
        for snapshot in clusters[cluster]:
            if replicas_re.match(snapshot['DBClusterSnapshotIdentifier']):
                replicas.append(snapshot)

        # filter out non-manual snapshots just in case
        replicas = [snapshot for snapshot in replicas if snapshot['SnapshotType'] == 'manual']

        # retain the last n replicas
        num_replicas = len(replicas)
        if num_replicas == 0:
            logger.warning(f"{cluster} has zero replicated snapshots in {region} found... skipping")
            continue

        if num_replicas > RDS_REPLICA_RETENTION_NUM:
            logger.info(f"Found {num_replicas} {cluster} replicated snapshots in {region} to delete!")
            for snapshot in replicas[RDS_REPLICA_RETENTION_NUM:]:
                if not bucket.fill():
                    logger.warning("Throttle bucket full or throttled, backing off...")
                    time.sleep(1)
                if DRY_RUN:
                    logger.info(f"Would delete {cluster} {region} replicated snapshot "
                                f"{snapshot['DBClusterSnapshotIdentifier']}")
                else:
                    try:
                        client.delete_db_cluster_snapshot(
                            DBClusterSnapshotIdentifier=snapshot['DBClusterSnapshotIdentifier']
                        )
                        logger.info(f"Deleted {cluster} {region} replicated snapshot "
                                    f"{snapshot['DBClusterSnapshotIdentifier']}")
                    except Exception as e:
                        logger.error(f"Error deleting {cluster} {region} replicated snapshot"
                                     f"{snapshot['DBClusterSnapshotIdentifier']}: {e}")


def get_amis_older_than(days, filter, region='us-east-1'):
    """
    Returns a list of available "Platinum" AMIs that are older than n days.
    """
    if filter is None:
        raise ValueError("filter is required")
    client = boto3.client('ec2', region_name=region)
    bucket = LeakyBucket(EC2_NOMU_RATE_BUCKET, EC2_NOMU_LEAK_RATE)
    logger.debug(f"EC2 NOMUT LeakyBucket: CAP {bucket.capacity}, RATE {bucket.leak_rate}")

    # get all AMIs matching the filter
    amis = []
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)
    ami_paginator = client.get_paginator('describe_images')
    for page in ami_paginator.paginate(Owners=['self'], Filters=filter):
        # backoff if our bucket is full or if we get throttled
        if not bucket.fill() or page['ResponseMetadata']['HTTPStatusCode'] == 503:
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)
        amis.extend(page['Images'])
        logger.debug(f"Found {len(amis)} AMIs in {region} matching filter")

    # return the ones older than n days
    cutoff_amis = [
        ami for ami in amis if datetime.datetime.fromisoformat(ami['CreationDate'].replace('Z', '+00:00')) < cutoff
    ]
    logger.debug(f"Found {len(cutoff_amis)} matching AMIs in {region} older than {days} days")

    return cutoff_amis


def delete_platinum_amis_older_than(days=PLATINUM_AMI_MIN_RETENTION_DAYS, region='us-east-1'):
    """
    Unregisters old Platinum AMIs and deletes any associated snapshots.
    """
    logger.debug(f"Processing Platinum AMIs in {region}")
    client = boto3.client('ec2', region_name=region)
    bucket = LeakyBucket(EC2_MUT_RATE_BUCKET, EC2_MUT_LEAK_RATE)
    logger.debug(f"EC2 MUT LeakyBucket: CAP {bucket.capacity}, RATE {bucket.leak_rate}")

    # get all platinum amis older than n days
    old_platinum_amis = get_amis_older_than(days, PLATINUM_AMI_FILTER, region)
    if len(old_platinum_amis) == 0:
        logger.info(f"No Platinum AMIs older than {days} days in {region} to delete.. skipping")
        return
    num_old = len(old_platinum_amis)

    # abort if no old amis found
    if num_old == 0:
        logger.info(f"No Platinum AMIs older than {days} days in {region} to delete.. skipping")
        return

    # for each ami, deregister and delete associated snapshots
    logger.info(f"Found {num_old} Platinum AMIs in {region} to delete!")
    for ami in old_platinum_amis:
        if not bucket.fill():
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)

        if DRY_RUN:
            logger.info(f"Would deregister Platinum AMI: {ami['ImageId']}")
            for block_device in ami['BlockDeviceMappings']:
                if 'Ebs' in block_device:
                    logger.info(f"Would delete {ami['ImageId']} snapshot {block_device['Ebs']['SnapshotId']}")
            continue
        else:
            try:
                client.deregister_image(ImageId=ami['ImageId'])
                logger.info(f"Deregistered {region} Platinum AMI {ami['ImageId']}")
                for block_device in ami['BlockDeviceMappings']:
                    try:
                        if 'Ebs' in block_device:
                            client.delete_snapshot(SnapshotId=block_device['Ebs']['SnapshotId'])
                            logger.info(
                                f"Deleted {ami['ImageId']} associated snapshot {block_device['Ebs']['SnapshotId']}"
                            )
                    except Exception as e:
                        logger.error(f"Error deleting {ami['ImageId']} associated snapshot "
                                     f"{block_device['Ebs']['SnapshotId']}: {e}")
            except ClientError as e:
                logger.error(f"Failed to deregister Platinum AMI {ami['ImageId']}")
                logger.error(e)


def delete_unassocaited_app_amis_older_than(days=APP_AMI_RETENTION_DAYS, region='us-east-1'):
    """
    Deletes all unassociated APP AMIs older than n days.
    - Finds all AMIs matching the APP_AMI_FILTERS
    - Finds all AMIs associated with EC2 instances
    - Deletes all unassociated AMIs older than n days along with any associated snapshots
    """
    logger.debug(f"Processing unassociated APP AMIs in {region}")
    client = boto3.client('ec2', region_name=region)
    bucket = LeakyBucket(EC2_MUT_RATE_BUCKET, EC2_MUT_LEAK_RATE)

    # Find associated AMIs
    associated_amis=[]
    instance_paginator = client.get_paginator('describe_instances')
    for page in instance_paginator.paginate():
        if not bucket.fill() or page['ResponseMetadata']['HTTPStatusCode'] == 503:
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)
        for reservation in page['Reservations']:
            for instance in reservation['Instances']:
                associated_amis.append(instance['ImageId'])
    logger.debug(f"Found {len(associated_amis)} associated AMIs in {region}")

    # get unassociated app amis older than n days for each filter
    unassociated_amis = []
    for filter in APP_AMI_FILTERS:
        amis = get_amis_older_than(days, filter, region)
        amis = [ami for ami in amis if ami['ImageId'] not in associated_amis]
        unassociated_amis.extend(amis)

    num_unassociated_amis = len(unassociated_amis)
    if num_unassociated_amis == 0:
        logger.info(f"No unassociated APP AMIs older than {days} days in {region} to delete.. skipping")
        return

    logger.info(f"Found {num_unassociated_amis} unassociated APP AMIs in {region} to delete!")
    for ami in unassociated_amis:
        if not bucket.fill():
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)

        if DRY_RUN:
            logger.info(f"Would deregister {region} APP AMI {ami['ImageId']}")
            for block_device in ami['BlockDeviceMappings']:
                if 'Ebs' in block_device:
                    logger.info(f"Would delete {ami['ImageId']}'s associated snapshot "
                                "{block_device['Ebs']['SnapshotId']}")
            continue

        # DO IT
        try:
            client.deregister_image(ImageId=ami['ImageId'])
            logger.info(f"Deregistered {region} APP AMI {ami['ImageId']}")
            for block_device in ami['BlockDeviceMappings']:
                try:
                    if 'Ebs' in block_device:
                        client.delete_snapshot(SnapshotId=block_device['Ebs']['SnapshotId'])
                        logger.info(
                            f"Deleted {ami['ImageId']}'s associated snapshot {block_device['Ebs']['SnapshotId']}"
                        )
                except Exception as e:
                    logger.error(f"Error deleting {ami['ImageId']}'s associated snapshot "
                                 f"{block_device['Ebs']['SnapshotId']}: {e}")
        except ClientError as e:
            logger.error(f"Failed to deregister APP AMI {ami['ImageId']}")
            logger.error(e)


def get_unassociated_ebs_snapshots(region='us-east-1'):
    """
    Returns a list of EC2 EBS Daily snapshots that are not associated with an active volume. Ignores snapshots that are
    currently in progress (eg pending, completed, error, etc.) filtered on EC2_DAILY_EBS_SNAPSHOT_FILTER.
    """
    logger.debug(f"Finding unassociated ec2 ebs snapshots in {region}")
    client = boto3.client('ec2', region_name=region)
    bucket = LeakyBucket(EC2_NOMU_RATE_BUCKET, EC2_NOMU_LEAK_RATE)
    logger.debug(f"EC2 NOMUT LeakyBucket: CAP {bucket.capacity}, RATE {bucket.leak_rate}")

    # get all snapshots
    logger.debug(f"Finding EBS snapshots in {region}...")
    snapshots = []
    snapshot_paginator = client.get_paginator('describe_snapshots')
    for page in snapshot_paginator.paginate(OwnerIds=['self'], Filters=EC2_DAILY_EBS_SNAPSHOT_FILTER):
        if not bucket.fill() or page['ResponseMetadata']['HTTPStatusCode'] == 503:
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)
        snapshots.extend(page['Snapshots'])
    num_snapshots = len(snapshots)

    # abort if no snapshots found
    if num_snapshots == 0:
        logger.warning(f"No EBS snapshots in {region} found... skipping")
        return []

    # get active volumes
    existing_volumes = []
    logger.debug(f"Finding active EC2 volumes in {region}")
    volume_paginator = client.get_paginator('describe_volumes')
    for page in volume_paginator.paginate():
        if not bucket.fill() or page['ResponseMetadata']['HTTPStatusCode'] == 503:
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)
        existing_volumes.extend(page['Volumes'])
    num_volumes = len(existing_volumes)

    # abort if no volumes found
    if num_volumes == 0:
        logger.warning(f"No active EC2 volumes in {region} found... skipping")
        return

    # filter out snapshots associated with active volumes
    logger.debug("Finding unassociated EBS snapshots...")
    volume_ids = {volume['VolumeId'] for volume in existing_volumes}
    unassociated_snapshots = [snapshot for snapshot in snapshots if snapshot['VolumeId'] not in volume_ids]

    # log the counts and return the unassociated snapshots
    logger.debug(f"Found {len(volume_ids)} existing EC2 volumes in {region}")
    logger.debug(f"Found {len(snapshots)} total EBS snapshots in {region}")
    logger.debug(f"Found {len(unassociated_snapshots)} unassociated EBS snapshots in {region}")

    return unassociated_snapshots


def delete_unassociated_ebs_snapshots(days=7, region='us-east-1'):
    """
    Deletes all unassociated EC2 EBS snapshots older than n days.
    """
    logger.info(f"Processing unassociated EBS snapshots in {region}")
    client = boto3.client('ec2', region_name=region)
    bucket = LeakyBucket(EC2_MUT_RATE_BUCKET, EC2_MUT_LEAK_RATE)
    logger.debug(f"EC2 MUT LeakyBucket: CAP {bucket.capacity}, RATE {bucket.leak_rate}")
    old_snapshots = get_unassociated_ebs_snapshots(region)

    # delete all snapshots older than n days
    cutoff = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)
    old_snapshots = [
        snapshot for snapshot in old_snapshots if snapshot['StartTime'].replace(tzinfo=datetime.timezone.utc) < cutoff
    ]
    num_unassociated = len(old_snapshots)
    logger.debug(f"Found {num_unassociated} unassociated EBS snapshots in {region} older than {days} days")
    if len(old_snapshots) == 0:
        logger.info(f"No unassociated EBS snapshots older than {days} days in {region} found.. skipping")
        return

    # DO IT
    for snapshot in old_snapshots:
        if not bucket.fill():
            logger.warning("Throttle bucket full or throttled, backing off...")
            time.sleep(1)

        if DRY_RUN:
            logger.info(f"Would delete unassociated EBS snapshot {snapshot['SnapshotId']} in {region}")
        else:
            try:
                client.delete_snapshot(SnapshotId=snapshot['SnapshotId'])
                logger.info(f"Deleted unassociated {region} EBS snapshot {snapshot['SnapshotId']}")
            except ClientError as e:
                logger.error(f"Failed to delete {region} unassociated EBS snapshot {snapshot['SnapshotId']}")
                logger.error(e)


# entry point
def main():
    global DEBUG
    DEBUG = get_bool_env_var('DEBUG', default=False)
    if DEBUG:
        logger.setLevel(logging.DEBUG)
        logger.info("LOG_LEVEL=DEBUG")
    else:
        logger.setLevel(logging.INFO)

    global DRY_RUN
    DRY_RUN = get_bool_env_var('DRY_RUN', default=False)
    if DRY_RUN:
        logger.info("Running in dry run mode.. no deletions will be made")

    # purge
    delete_rds_cluster_snapshots(region='us-east-1')
    delete_rds_cluster_replicas(region='us-west-2')
    delete_unassocaited_app_amis_older_than(days=APP_AMI_RETENTION_DAYS, region='us-east-1')
    delete_platinum_amis_older_than(days=PLATINUM_AMI_MIN_RETENTION_DAYS, region='us-east-1')
    delete_unassociated_ebs_snapshots(days=EC2_MIN_RETENTION_DAYS, region='us-east-1')
    logger.info("Done")


if __name__ == '__main__':
    main()
