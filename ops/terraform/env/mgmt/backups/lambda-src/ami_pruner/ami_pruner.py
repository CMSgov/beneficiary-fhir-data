import datetime
import functools
import logging
import os

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError

AWS_PRUNE_REGION = os.environ.get('AWS_PRUNE_REGION', 'us-east-1')
DRY_RUN = os.environ.get('DRY_RUN', 'False').lower() in ['true', '1', 'yes']
DEBUG = os.environ.get('DEBUG', 'False').lower() in ['true', '1', 'yes']
LOG_LEVEL = int(os.environ.get('LOG_LEVEL', 20)) # 0=NOTSET, 10=DEBUG, 20=INFO, 30=WARNING, 40=ERROR, 50=CRITICAL

# SSM path to lookup our AMI retention policies
AMI_RETENTION_POLICIES_SSM_PATH = os.environ.get('AMI_RETENTION_POLICIES_SSM_PATH')

# Request filters used when querying AWS.
DEFAULT_AMI_FILTER = [{'Name': 'state', 'Values': ['available']}]
PLATINUM_AMI_FILTER = [
    {'Name': 'tag:Application','Values': ['bfd-platinum']},
]
LAUNCH_TEMPLATE_AMI_FILTERS=[
    [{'Name': 'tag:Function','Values': ['ETL APP SERVER']}],
    [{'Name': 'tag:Function','Values': ['FHIR APP SERVER']}],
]
NON_LAUNCH_TEMPLATE_AMI_FILTERS = [
    [{'Name': 'tag:Function','Values': ['DB MIGRATOR']}],
    [{'Name': 'tag:Function','Values': ['SERVER LOAD']}],
]

# do not touch anything with these tags
KEEP_TAGS = ['do_not_delete', 'do-not-delete', 'DONOTDELETE', 'donotdelete', 'do not delete', 'DO NOT DELETE']

# logger and boto3 clients ('mode: standard' automatically applies exponential backoff and retries)
logging.basicConfig(format='%(asctime)s %(levelname)s %(message)s', level=(logging.DEBUG if DEBUG else logging.INFO))
logger = logging.getLogger(__name__)
logger.level = LOG_LEVEL
config = Config(
    region_name = AWS_PRUNE_REGION,
    retries = {
        'max_attempts': 10,
        'mode': 'standard'
    }
)
ec2_client = boto3.client('ec2', config=config)
ssm_client = boto3.client('ssm', config=config)


@functools.cache
def ami_retention_policies(ssm_path=AMI_RETENTION_POLICIES_SSM_PATH) -> dict:
    """
    Returns a dictionary of retention policies from the provided SSM path and caches the results. Keys are stripped of
    path prefixes.
    """
    policies = {}
    try:
        # get the parameters
        response = ssm_client.get_parameters_by_path(
            Path=ssm_path,
            Recursive=True,
            WithDecryption=True
        )

        # build the dictionary
        for param in response['Parameters']:
            key = param['Name'].split('/')[-1]
            policies[key] = int(param['Value'])
            logger.debug(f"Loaded {key}: {param['Value']} from SSM")

    except ClientError as e:
        logger.error("Failed to load AMI retention policies from SSM: {e}")
        raise e

    return policies

@functools.cache
def active_instances() -> list:
    """
    Returns a list of active (non-terminated) instances in the region and caches the results.
    """
    instances = []
    try:
        paginator = ec2_client.get_paginator('describe_instances')
        for page in paginator.paginate():
            for reservation in page['Reservations']:
                for instance in reservation['Instances']:
                    if instance['State']['Name'] != 'terminated':
                        instances.append(instance)
    except ClientError as e:
        logger.error("Error finding active instances: {e}")
        raise e

    return instances

@functools.cache
def existing_volumes() -> list:
    """
    Returns a list of existing volumes in the region and caches the results.
    """
    volumes = []
    try:
        paginator = ec2_client.get_paginator('describe_volumes')
        for page in paginator.paginate():
            for volume in page['Volumes']:
                volumes.append(volume)
    except ClientError as e:
        logger.error("Error finding existing volumes: {e}")
        raise e

    return volumes

@functools.cache
def launch_templates() -> list:
    """
    Returns a list of launch templates in the region and caches the results.
    """
    templates = []
    try:
        paginator = ec2_client.get_paginator('describe_launch_templates')
        for page in paginator.paginate():
            for template in page['LaunchTemplates']:
                templates.append(template)
    except ClientError as e:
        logger.error("Error finding launch templates: {e}")
        raise e

    return templates

@functools.cache
def launch_template_versions(template_id: str, version: str='$Latest') -> list:
    """
    Returns a list of launch template versions for a template id and caches the results. If 'version' is '$Latest',
    returns the latest version of the template. If 'version' is '$Default', returns the default version of the template.
    If 'version' is an integer, returns the specific version of the template. If 'version' is not provided, returns all
    versions of the template. If template_id is not provided, returns all versions of all templates.
    """
    templates = []
    try:
        paginator = ec2_client.get_paginator('describe_launch_template_versions')
        for page in paginator.paginate(LaunchTemplateId=template_id, Versions=[version] if version else []):
            for template in page['LaunchTemplateVersions']:
                templates.append(template)
    except ClientError as e:
        logger.error("Error finding launch templates: {e}")
        raise e

    return templates

def deregister_ami(ami_id: str, ami_name="") -> bool:
    """
    Deregisters an AMI. Returns True if successful.
    """
    ami_desc = ami_id
    if ami_name != "":
        ami_desc = f"{ami_name} ({ami_id})"

    try:
        ec2_client.deregister_image(ImageId=ami_id, DryRun=DRY_RUN)
        logger.info(f"Deregistered {ami_desc}")
    except ClientError as e:
        if e.response['Error']['Code'] == 'DryRunOperation':
            logger.info(f"Deregistered {ami_desc} (dry run)")
            return True
        else:
            logger.error(f"Failed to deregister {ami_desc}: {e}")
            return False

    return True

def delete_associated_snapshot(ami_id:str, snapshot_id: str) -> bool:
    """
    Deletes an EC2 snapshot. Ignores snapshots tagged with any of the KEEP_TAGS, or those referenced by an existing
    EC2 Volume. Returns True if successful.
    """
    volumes = existing_volumes()
    try:
        snapshot = ec2_client.describe_snapshots(SnapshotIds=[snapshot_id])['Snapshots'][0]
        # skip if tagged with with a KEEP tag
        tags = [tag['Key'] for tag in snapshot['Tags']]
        for tag in KEEP_TAGS:
            if tag in tags:
                logger.info(f"Ignoring {ami_id} {snapshot_id}: tagged with '{tag}'")
                return False
        # skip if referenced by an existing volume
        for volume in volumes:
            if volume['SnapshotId'] == snapshot_id:
                logger.info(f"Ignoring {ami_id} {snapshot_id}: referenced by Volume {volume['VolumeId']}")
                return False
        ec2_client.delete_snapshot(SnapshotId=snapshot_id, DryRun=DRY_RUN)
        logger.info(f"Deleted {ami_id} ec2 snapshot: {snapshot_id}")

    except ClientError as e:
        if e.response['Error']['Code'] == 'DryRunOperation':
            logger.info(f"Deleted {ami_id} ec2 snapshot: {snapshot_id} (dry run)")
            return True
        else:
            logger.error(f"Failed to delete {ami_id} ec2 snapshot: {snapshot_id}: {e}")
            return False

    return True

def find_candidates(filter, default_filter=DEFAULT_AMI_FILTER) -> list:
    """
    Returns a list of candidate AMIs in the region. Images referenced by an active ec2 (non-terminated) instance,
    associated with the most recent version of a launch template, or those tagged with any of the KEEP_TAGS, are
    excluded from the returned list.
    """
    filters = filter+default_filter if filter else default_filter
    logger.debug(f"Finding candidate AMIs with filter: {filters}")
    candidates = []
    try:
        paginator = ec2_client.get_paginator('describe_images')
        for page in paginator.paginate(Owners=['self'], Filters=filters or []):
            for image in page['Images']:
                # skip if no tags
                if 'Tags' not in image:
                    logger.warning(f"Ignoring {image['Name']} due to anomoly (ami has no tags).. please review manually")
                    continue
                tags = [tag['Key'] for tag in image['Tags']]
                # skip if tagged with with a KEEP tag
                for tag in KEEP_TAGS:
                    if tag in tags:
                        logger.info(f"Ignoring ami {image['Name']}: tagged with '{tag}'")
                        continue
                # skip if referenced by an active instance
                for instance in active_instances():
                    if instance['ImageId'] == image['ImageId']:
                        logger.debug(f"Ignoring ami {image['Name']}: in use by instance {instance['InstanceId']}")
                        continue
                candidates.append(image)

    except ClientError as e:
        logger.error(f"Error building pool of candidate AMIs: {e}")
        raise e

    return candidates

def remove_candidates_older_than(candidates, days) -> list:
    """
    Removes images from the provided list of candidates that are older than the provided number of days.
    """
    cutoff_date = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=days)
    candidates = [ami for ami in candidates if datetime.datetime.fromisoformat(ami['CreationDate']) < cutoff_date]

    return candidates

def prune_candidates(candidates) -> (int, int):
    """
    Prunes the provided list of AMIs and associated snapshots. Returns a tuple of the number of AMIs deregistered and
    snapshots deleted.
    """
    deregistered = []
    deleted = []
    if len(candidates) == 0:
        return 0, 0

    for ami in candidates:
        # deregister the ami
        tag_name = [tag['Value'] for tag in ami['Tags'] if tag['Key'] == 'Name']
        tag_name = tag_name[0] if len(tag_name) > 0 else ''
        if deregister_ami(ami['ImageId'], tag_name):
            deregistered.append(ami)
        else:
            continue
        # delete associated snapshots
        for block_device in ami['BlockDeviceMappings']:
            if 'Ebs' in block_device:
                if delete_associated_snapshot(ami['ImageId'], block_device['Ebs']['SnapshotId']):
                    deleted.append(block_device['Ebs']['SnapshotId'])

    return len(deregistered), len(deleted)

def platinum_candidates() -> list:
    """
    Returns a list of platinum amis eligible for pruning.
    """
    days = ami_retention_policies()['platinum_ami_retention_days']
    candidates = find_candidates(PLATINUM_AMI_FILTER)
    candidates = remove_candidates_older_than(candidates, days)

    return candidates

def app_candidates() -> list:
    """
    Returns a list of app amis eligible for pruning.
    """
    candidates = []
    retention_count = ami_retention_policies()['app_ami_retention_count'] + 1

    # build a map of all app amis not referenced by a launch template and group by name (minus trailing suffix)
    app_pool = {}
    for app in NON_LAUNCH_TEMPLATE_AMI_FILTERS:
        amis = find_candidates(app)
        for ami in amis:
            if ami['Name']:
                name = '-'.join(ami['Name'].split('-')[:-1])
                if name not in app_pool:
                    app_pool[name] = []
                app_pool[name].append(ami)
            else:
                logger.warning(f"Ignoring {ami['ImageId']} due to anomoly (ami has no name).. prune manually")
                continue

    # add them to the candidate pool
    for app in app_pool.keys():
        candidates.extend(app_pool[app])
        logger.debug(f"Added {len(app_pool[app])} {app} candidates to the pool")

    # add all launch template amis to the candidate pool
    for app in LAUNCH_TEMPLATE_AMI_FILTERS:
        amis = find_candidates(app)
        candidates.extend(amis)
        logger.debug(f"Added {len(amis)} {app[0]['Values'][0]} candidates to the pool")

    logger.debug(f"Added {len(candidates)} App AMIs to the initial candidate pool")

    # for each non-template pool, remove the most recent n from the candidates
    for app in app_pool.keys():
        logger.info(f"Checking {app} amis...")
        app_pool[app] = sorted(app_pool[app], key=lambda x: x['CreationDate'], reverse=True)
        retention = app_pool[app][:retention_count]
        for image in retention:
            candidates = [ami for ami in candidates if image['ImageId'] != ami['ImageId']]
            logger.debug(f"Ignoring {image['Name']} (in retention)")

    # for each launch template, filter out the most recent n (along with the $Latest and $Default)
    templates = launch_templates()
    for template in templates:
        logger.info(f"Checking launch template {template['LaunchTemplateName']} amis...")

        # $Latest and $Default
        tmpl_id = template['LaunchTemplateId']
        latest_ver = f"{template['LatestVersionNumber']}"
        default_ver = f"{template['DefaultVersionNumber']}"
        latest = launch_template_versions(tmpl_id, latest_ver)[0]['LaunchTemplateData']
        default = launch_template_versions(tmpl_id, default_ver)[0]['LaunchTemplateData']
        logger.info(f"Ignoring {tmpl_id} launch template ami: {latest['ImageId']} (latest version)")
        logger.info(f"Ignoring {tmpl_id} launch tempalte ami: {default['ImageId']} (default version)")
        candidates = [ami for ami in candidates if ami['ImageId'] != latest['ImageId']]
        candidates = [ami for ami in candidates if ami['ImageId'] != default['ImageId']]

        # most recent n
        versions = launch_template_versions(template['LaunchTemplateId'], "")
        versions = sorted(versions, key=lambda x: x['VersionNumber'], reverse=True)[:retention_count]
        for i in range(len(versions)):
            logger.debug(f"Ignoring {versions[i]['LaunchTemplateData']['ImageId']} (still in retention)")
            candidates = [ami for ami in candidates if ami['ImageId'] != versions[i]['LaunchTemplateData']['ImageId']]

    return candidates

def lambda_handler(event, context):
    logger.info("Starting AMI pruning job...")
    total_derigistered = 0
    total_deleted = 0

    # platinum amis
    platinum = platinum_candidates()
    if len(platinum) == 0:
        logger.info("No Platinum AMIs eligible for pruning")
    logger.info(f"Found {len(platinum)} Platinum AMIs eligible for pruning")

    # app amis
    app_amis = app_candidates()
    if len(app_amis) == 0:
        logger.info("No App AMIs eligible for pruning")
    logger.info(f"Found {len(app_amis)} App AMIs eligible for pruning")

    # scream, aim, fire
    logger.info("Pruning Platinum AMIs...")
    num_platinum_derigistered, num_platinum_deleted = prune_candidates(platinum)
    total_derigistered += num_platinum_derigistered
    total_deleted += num_platinum_deleted

    logger.info("Pruning App AMIs...")
    num_app_derigistered, num_app_deleted = prune_candidates(app_amis)
    total_derigistered += num_app_derigistered
    total_deleted += num_app_deleted

    logger.info(f"STATS: Removed {num_platinum_derigistered} App AMIs and {num_platinum_deleted} associated snapshots")
    logger.info(f"STATS: Removed {num_app_derigistered} App AMIs and {num_app_deleted} associated snapshots")
    logger.info(f"STATS: Removed a total of {total_derigistered} AMIs and {total_deleted} associated snapshots")
    logger.info(f"AMI pruning job complete")

if __name__ == '__main__':
    lambda_handler(None, None)
