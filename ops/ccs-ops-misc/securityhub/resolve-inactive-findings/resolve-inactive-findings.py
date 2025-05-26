#!/usr/bin/env python3
import argparse
import resource
from urllib import response
import boto3
import time
import sys
import re

# We need to throttle requests to AWS to ensure we do not hit rate limits. Each service can have different limits
# depending on the call. And throttle using "leaky bucket", "token bucket", or simple fixed Requests Per Second.
# 
# It's important we do not exceed the limits as they impact account wide limits for the region. For example, if
# we were to hit the limit for Ec2 Describe*, nobody would be able to describe instances in the region. Even the
# console would be impacted.
#
# The following is a dict of services and their limits. The values are the maximum number of requests per second,
# which is roughly half the maximum listed by AWS or 1/4 for Ec2 Describe* calls (just in case).
# EC2: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/throttling.html
THROTTLE_RATES = {
    'security_hub': {
        'get': 5,
        'update': 5,
        'batch_update': 5,
    },
    'ec2': {
        'describe': 25,
    },
    's3': {
        'get': 5,
    },
    'rds': {
        'describe': 25,
    },
    'iam': {
        'list': 5,
    },
    'lambda': {
        'describe': 25,
    },
    'kms': {
        'describe': 25,
    },
}


# How frequently to update the active resource lists (in minutes)
UPDATE_INTERVALS = {
    'AwsEc2Instance': 5,
    'AwsEc2Volume': 5,
    'AwsEc2SecurityGroup': 5,
    'AwsS3Bucket': 5,
    'AwsRdsDbClusterSnapshot': 60,
    'AwsRdsDbInstance': 60,
    'AwsIamUser': 60,
    'AwsIamPolicy': 60,
    'AwsIamAccessKey': 5,
    'AwsAutoScalingAutoScalingGroup': 5,
    'AwsLambdaFunction': 5,
    'AwsSnsTopic': 5,
    'AwsKmsKey': 5,
}

EC2_INSTANCE_STATES = ['pending', 'running', 'stopping', 'stopped', 'shutting-down', 'terminated']
ACTIVE_INSTANCE_STATES = set(EC2_INSTANCE_STATES) - set(['terminated'])
EC2_VOLUME_STATES = ['creating', 'available' , 'in-use' , 'deleting' , 'deleted' , 'error']
ACTIVE_VOLUME_STATES = set(EC2_VOLUME_STATES) - set(['deleting', 'deleted', 'error'])

# When we resolve a finding, we add a note to explain why.
RESOLVED_NOTE = "Finding no longer references active resources."
RESOLVED_BY = "Script"

# This is the filter we use to find findings that need to be resolved. 
# The 'ResourceType' gets set via command line args.
FINDING_FILTERS = {
    'ResourceType': [],
    'WorkflowStatus': [
        {
            'Value': 'NEW',
            'Comparison': 'EQUALS'
        },
        {
            'Value': 'NOTIFIED',
            'Comparison': 'EQUALS'
        },
    ],
    'RecordState': [
        {
            'Value': 'ACTIVE',
            'Comparison': 'EQUALS'
        },
    ]
}

# Resource ID regex patterns
RESOURCE_ID_RE = {
    'AwsEc2Instance': r'^i-[a-zA-Z0-9]+$',
    'AwsS3Bucket': r'^[a-z0-9][a-zA-Z0-9-]{1,61}[a-z0-9]$',
    'AwsEc2Volume': r'^vol-[a-zA-Z0-9]+$',
    'AwsEc2SecurityGroup': r'^sg-[a-zA-Z0-9]+$',
    'AwsRdsDbClusterSnapshot': r'^[a-zA-Z0-9-]+$',
    'AwsRdsDbInstance': r'^[a-zA-Z0-9-]+$',
    'AwsIamUser': r'^[a-zA-Z0-9-]+$',
    'AwsIamPolicy': r'^[a-zA-Z0-9-]+$',
    'AwsIamAccessKey': r'^AWS::IAM::AccessKey:[A-Z0-9]+$',
    'AwsAutoScalingAutoScalingGroup': r'^[a-zA-Z0-9-]+$',
    'AwsLambdaFunction': r'^[a-zA-Z0-9-]+$',
    'AwsSnsTopic': r'^[a-zA-Z0-9-]+$',
    'AwsKmsKey': r'^[a-zA-Z0-9-]+$',
}

# SecurityHub Insights are used to get a count of findings that match our filters. This is much faster than
# paginating through all the findings as there is no .count() method. Insights are dynamically created if
# needed.
INSIGHT_NAME_PREFIX = 'NewFailedFindingsBy'
GROUP_BY = 'AwsAccountId'


# Return a boto3 client based on the resource type or 'hub' for a security hub client
def get_client(region, resource_type):
    if resource_type.startswith('AwsEc2'):
        return boto3.client('ec2', region_name=region)
    elif resource_type.startswith('AwsS3Bucket'):
        return boto3.client('s3', region_name=region)
    elif resource_type.startswith('AwsRds'):
        return boto3.client('rds', region_name=region)
    elif resource_type.startswith('AwsIam'):
        return boto3.client('iam', region_name=region)
    elif resource_type.startswith('AwsAutoScaling'):
        return boto3.client('autoscaling', region_name=region)
    elif resource_type.startswith('AwsLambda'):
        return boto3.client('lambda', region_name=region)
    elif resource_type.startswith('AwsSns'):
        return boto3.client('sns', region_name=region)
    elif resource_type.startswith('AwsKms'):
        return boto3.client('kms', region_name=region)
    elif resource_type == 'hub':
        return boto3.client('securityhub', region_name=region)


# Returns True if the given "id" string looks like an AWS ARN
def is_arn(id):
    return id.startswith('arn:')


# Gets the id from an ARN
def get_id_from_arn(arn):
    # If the arn has a / in it, the id is the last part of the arn
    if '/' in arn:
        return arn.split('/')[-1]
    # Otherwise, the id is the last part of the resource
    return arn.split(':')[-1]


# Gets or creates an insight matching our FINDING_FILTERS and returns the insight object
def get_or_create_insight(region, resource_type):
    # see if any existing insight matches our filters and group by attribute
    client = get_client(region, 'hub')
    insights = client.get_insights()
    insight_arn = None
    for insight in insights['Insights']:
        if insight['Filters'] == FINDING_FILTERS and insight['GroupByAttribute'] == 'AwsAccountId':
            insight_arn = insight['InsightArn']
            break
    # if no matches, create one
    if insight_arn is None:
        name = f"{INSIGHT_NAME_PREFIX}{resource_type}"
        insight_arn = client.create_insight(Name=f"{name}", Filters=FINDING_FILTERS, GroupByAttribute=GROUP_BY).get('InsightArn')
        # give it a few seconds to be created
        time.sleep(10)
    return insight


# Get the count of findings that match our filters
def get_count_from_insight(region, insight):
    client = get_client(region, 'hub')
    results = client.get_insight_results(InsightArn=insight['InsightArn'])
    if len(results['InsightResults']['ResultValues']) > 0:
        return results['InsightResults']['ResultValues'][0]['Count']
    return 0


# Returns a list of active resources by type
def get_active_resources(client, resource_type):
    if resource_type == 'AwsEc2Instance':
        return get_active_instances(client)
    elif resource_type == 'AwsS3Bucket':
        return get_active_s3_buckets(client)
    elif resource_type == 'AwsEc2Volume':
        return get_active_volumes(client)
    elif resource_type == 'AwsEc2SecurityGroup':
        return get_active_security_groups(client)
    elif resource_type == 'AwsRdsDbClusterSnapshot':
        return get_active_rds_snapshots(client)
    elif resource_type == 'AwsRdsDbInstance':
        return get_active_rds_instances(client)
    elif resource_type == 'AwsIamUser':
        return get_active_iam_users(client)
    elif resource_type == 'AwsIamAccessKey':
        return get_active_iam_access_keys(client)
    elif resource_type == 'AwsIamPolicy':
        return get_active_iam_policies(client)
    elif resource_type == 'AwsAutoScalingAutoScalingGroup':
        return get_active_asg_groups(client)
    elif resource_type == 'AwsLambdaFunction':
        return get_active_lambdas(client)
    elif resource_type == 'AwsSnsTopic':
        return get_active_sns_topics(client)
    elif resource_type == 'AwsKmsKey':
        return get_active_kms_keys(client)
    else:
        raise Exception(f"Unknown resource type: {resource_type}")


# Get active kms keys
def get_active_kms_keys(client):
    keys = []
    paginator = client.get_paginator('list_keys')
    for page in paginator.paginate():
        for key in page['Keys']:
            keys.append(key['KeyId'])
    return keys


# Get active sns topics
def get_active_sns_topics(client):
    topics = []
    paginator = client.get_paginator('list_topics')
    for page in paginator.paginate():
        for topic in page['Topics']:
            topics.append(get_id_from_arn(topic['TopicArn']))
    return topics


# Get active IAM users
def get_active_iam_users(client):
    users = client.list_users()
    return [user['UserName'] for user in users['Users']]


# Get active ec2 security groups
def get_active_security_groups(client):
    groups = []
    paginator = client.get_paginator('describe_security_groups')
    for page in paginator.paginate():
        for group in page['SecurityGroups']:
            groups.append(group['GroupId'])
    return groups


# Get active lambda functions
def get_active_lambdas(client):
    lambdas = []
    paginator = client.get_paginator('list_functions')
    for page in paginator.paginate(FunctionVersion='ALL',):
        for lambda_function in page['Functions']:
            lambdas.append(lambda_function['FunctionName'])
    return lambdas


# Get active IAM access keys
def get_active_iam_access_keys(client):
    keys = []
    user_paginator = client.get_paginator('list_users')
    for user_page in user_paginator.paginate():
        for user in user_page['Users']:
            key_paginator = client.get_paginator('list_access_keys')
            for key_page in key_paginator.paginate(UserName=user['UserName']):
                for key in key_page['AccessKeyMetadata']:
                    keys.append(f"AWS::IAM::AccessKey:{key['AccessKeyId']}")        
    return keys


# Get active rds instances
def get_active_rds_instances(client):
    instances = []
    paginator = client.get_paginator('describe_db_instances')
    for page in paginator.paginate():
        for instance in page['DBInstances']:
            instances.append(instance['DBInstanceIdentifier'])
    return instances
    

# Get active ASG groups
def get_active_asg_groups(client):
    groups = []
    response = client.describe_auto_scaling_groups()
    for asg in response['AutoScalingGroups']:
        groups.append(asg['AutoScalingGroupName'])
    return groups


# Get active IAM policies
def get_active_iam_policies(client):
    policies = []
    paginator = client.get_paginator('list_policies')
    for response in paginator.paginate(Scope='Local', PolicyUsageFilter='PermissionsPolicy'):
        for policy in response['Policies']:
            policies.append(policy['PolicyName'])
    return policies


# Get active RDS cluster snapshots
def get_active_rds_snapshots(client):
    snapshots = []
    paginator = client.get_paginator('describe_db_cluster_snapshots')
    for response in paginator.paginate():
        for snapshot in response['DBClusterSnapshots']:
            snapshots.append(snapshot['DBClusterSnapshotIdentifier'])
    return snapshots


# Returns a list of active ec2 volumes
def get_active_volumes(client):
    # TODO: add pagination
    active_volumes = []
    response = client.describe_volumes(
        Filters=[{'Name': 'status', 'Values': list(ACTIVE_VOLUME_STATES)}]
    )
    for volume in response['Volumes']:
        active_volumes.append(volume.get('VolumeId'))
    
    return active_volumes


# Returns a list of active s3 bucket names.
def get_active_s3_buckets(client):
    buckets = []
    response = client.list_buckets()
    for bucket in response['Buckets']:
        buckets.append(bucket['Name'])
    return buckets


# Returns a list of currently active ec2 instances (any instance not in a 'terminated' state)
def get_active_instances(client):
    active_instances = []
    response = client.describe_instances(
        Filters=[{'Name': 'instance-state-name', 'Values': list(ACTIVE_INSTANCE_STATES)}])
    for reservation in response['Reservations']:
        for instance in reservation['Instances']:
            active_instances.append(instance.get('InstanceId'))
    return active_instances


# Validate a resource id against the current filter types regex pattern
def validate_resource_id(resource_id, resource_type):
    resource_re = re.compile(RESOURCE_ID_RE[resource_type])
    return resource_re.match(resource_id)


# Resolve findings that no longer reference active resources
def resolve_findings(region, resource_type, dry_run):
    resource_client = get_client(region, resource_type)
    throttle_rate = THROTTLE_RATES['security_hub']['batch_update']
    update_interval = UPDATE_INTERVALS[str(resource_type)] * 60
    last_update = 0
    resolved_finding_count = 0
    
    # page through and batch findings to resolve
    print('Processing findings...')
    hub = get_client(region, 'hub')
    paginator = hub.get_paginator('get_findings')
    page_iterator = paginator.paginate(Filters=FINDING_FILTERS, MaxResults=100)
    
    # throttle requests (leaky bucket)
    request_bucket = THROTTLE_RATES['security_hub']['batch_update']
    t = time.time()
    
    # page through findings 100 at a time
    for page in page_iterator:
        batch = []
        # for each finding
        for finding in page['Findings']:
            num_resources = len(finding['Resources'])
            
            # update the list of active resources if needed
            if (time.time() - last_update) > update_interval:
                active_resources = get_active_resources(resource_client, resource_type)
                last_update = time.time()

            # for each resource in the finding
            for resource in finding['Resources']:
                if is_arn(resource['Id']):
                    id = get_id_from_arn(resource['Id'])
                else:
                    id = resource['Id']
                
                # validate the resource id and check if it's in the active resource list
                if resource['Type'] != resource_type:
                    continue
                
                if not validate_resource_id(id, resource_type):
                    print(f"Skipping invalid resource id: {id}")
                    continue

                if id not in active_resources:
                    if dry_run:
                        print(f"{id} not active (first 3 for comparison: {active_resources[0:3]}")
                    # decrement num_resources if it's not
                    num_resources -= 1

            # if there are no remaining active resources, add the resource to the batch to get resolved
            if num_resources == 0:
                batch.append({'Id': finding['Id'], 'ProductArn': finding['ProductArn']})

        # be sure we throttle requests (leaky bucket)
        if (time.time() - t) > 1:
            request_bucket = throttle_rate
        if request_bucket == 0:
            print('Rate limit reached, backing off...')
            time.sleep(1.5)

        # resolve findings in the batch
        num_findings_in_batch = len(batch)
        if num_findings_in_batch > 0:
            print(f"Resolving {num_findings_in_batch} inactive findings...")
            
            if dry_run:
                resolved_finding_count += num_findings_in_batch
                continue

            result = hub.batch_update_findings(
                FindingIdentifiers=batch,
                Note={
                    'Text': RESOLVED_NOTE,
                    'UpdatedBy': RESOLVED_BY
                },
                Workflow={
                    'Status': 'RESOLVED'
                },
            )
            request_bucket -= 1
            t = time.time()
            
            resolved_finding_count += num_findings_in_batch # - len(result['ProcessedFindings'])
            if len(result['ProcessedFindings']) != num_findings_in_batch:
                print(f"Failed to resolve {num_findings_in_batch - len(result['ProcessedFindings'])} findings.")
                
            
    return resolved_finding_count


# process
def process(region, resource_type, dry_run, yes):
    print(f"Processing {resource_type} findings in {region} with:")
    print(" -> WorkflowStatus: NEW or NOTIFIED")
    print(" -> RecordState: ACTIVE")

    # continue?
    if not yes:
        if input("This may take some time.. continue? (y/n): ").lower() != 'y':
            return

    # Get a count of new findings matching the filter using seucrity hub insights
    print(f'Getting {resource_type} related findings...')
    insight = get_or_create_insight(region, resource_type)
    count = get_count_from_insight(region, insight)
    print(f'There are {count} findings matching the search criteria.')
    if count == 0:
        print('Nothing to do.')
        return

    # resolve findings
    num_resolved = resolve_findings(region, resource_type, dry_run)

    print(f"Done.\n")
    print(
        f"We resolved {num_resolved} out of {count} {resource_type} findings matching the search criteria.")
    print("It may take a few minutes for SecuriyHub to catch up. You may also need to refresh console to see the updates.")
    if dry_run:
        print("*** This was a dry run, no findings were actually resolved. ***")


def main():
    # parse args
    parser = argparse.ArgumentParser(description='Resolve inactive Security Hub findings.')
    parser.add_argument('--dry-run', action='store_true', help='Do not update findings, just print what would be done.')
    parser.add_argument('--region', default='us-east-1', help='AWS region to use.')
    parser.add_argument('--yes', action='store_true', help='Skip confirmation prompts.', default=False)
    resource_group = parser.add_mutually_exclusive_group(required=True)
    resource_group.add_argument('--ec2-instances', action='store_const', const='AwsEc2Instance', help='Resolve findings referencing non-existent EC2 instances')
    resource_group.add_argument('--ec2-volumes', action='store_const', const='AwsEc2Volume', help='Resolve findings referencing non-existent EC2 volumes')
    resource_group.add_argument('--ec2-security-groups', action='store_const', const='AwsEc2SecurityGroup',help='Resolve findings referencing non-existent EC2 security groups')
    resource_group.add_argument('--s3-buckets', action='store_const', const='AwsS3Bucket', help='Resolve findings referencing non-existent S3 buckets')
    resource_group.add_argument('--rds-cluster-snapshots', action='store_const', const='AwsRdsDbClusterSnapshot', help='Resolve findings referencing non-existent RDS cluster snapshots')
    resource_group.add_argument('--rds-db-instances', action='store_const', const='AwsRdsDbInstance', help='Resolve findings referencing non-existent RDS DB instances')
    resource_group.add_argument('--iam-users', action='store_const', const='AwsIamUser', help='Resolve findings referencing non-existent IAM users')
    resource_group.add_argument('--iam-access-keys', action='store_const', const='AwsIamAccessKey', help='Resolve findings referencing non-existent IAM access keys')
    resource_group.add_argument('--iam-policies', action='store_const', const='AwsIamPolicy', help='Resolve findings referencing non-existent IAM policies')
    resource_group.add_argument('--autoscaling-groups', action='store_const', const='AwsAutoScalingAutoScalingGroup', help='Resolve findings referencing non-existent ASG groups')
    resource_group.add_argument('--lambda-functions', action='store_const', const='AwsLambdaFunction', help='Resolve findings referencing non-existent Lambda functions')
    resource_group.add_argument('--sns-topics', action='store_const', const='AwsSnsTopic', help='Resolve findings referencing non-existent SNS topics')
    resource_group.add_argument('--kms-keys', action='store_const', const='AwsKmsKey', help='Resolve findings referencing non-existent KMS keys')
    resource_group.add_argument('--all', action='store_const', const='all', help='Resolve findings referencing non-existent resources for all supported resource types')
    args = parser.parse_args()
    
    print("This script resolves Security Hub findings that no longer reference active resources.")
    print(f"Findings will be marked resolved by '{RESOLVED_BY}' with the following note: '{RESOLVED_NOTE}'\n")
    
    # set the resource type filter
    resource_type = \
        args.ec2_instances or \
        args.s3_buckets or \
        args.ec2_volumes or \
        args.ec2_security_groups or \
        args.rds_cluster_snapshots or \
        args.iam_users or \
        args.iam_access_keys or \
        args.iam_policies or \
        args.autoscaling_groups or \
        args.rds_db_instances or \
        args.lambda_functions or \
        args.sns_topics or \
        args.kms_keys or \
        args.all
    if resource_type != 'all':
        process(args.region, resource_type, args.dry_run, args.yes)
    else:
        # iterate over the keys defined in RESOURCE_ID_RE
        for resource_type in RESOURCE_ID_RE.keys():
            print(f"============================================================")
            FINDING_FILTERS['ResourceType'] = [{'Comparison': 'EQUALS', 'Value': resource_type}]
            process(args.region, resource_type, args.dry_run, args.yes)
            print("")
    print(f"Done.\n")


if __name__ == '__main__':
    main()
