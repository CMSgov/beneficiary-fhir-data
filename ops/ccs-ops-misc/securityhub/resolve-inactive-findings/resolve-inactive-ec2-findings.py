from pydoc import pager
from unittest import result
import boto3
import time
import sys
import re

# half the max rate limit for security hub requests
THROTTLE_RATES = {
    'security_hub': {
        'get': 3,
        'update': 5,
        'batch_update': 5,
    },
    'ec2': {
        'describe': 5,
    }
}

EC2_INSTANCE_STATES = ['pending', 'running', 'stopping', 'stopped', 'shutting-down', 'terminated']
ACTIVE_INSTANCE_STATES = set(EC2_INSTANCE_STATES) - set(['terminated'])
ACTIVE_INSTANCE_UPDATE_INTERVAL_MINS = 10

RESOLVED_NOTE = "Finding no longer references active resources."
RESOLVED_BY = "Script"
FINDING_FILTERS = {
    'ResourceType': [
        {
            'Value': 'AwsEc2Instance',
            'Comparison': 'EQUALS'
        },
    ],
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

INSIGHT_NAME = 'NewFailedEC2FindingsByAccount'
GROUP_BY = 'AwsAccountId'

# Check if an ID is an ARN
def is_arn(id):
    return id.startswith('arn:')

# Get a list of all active instances (valid instances that are not terminated)
def get_active_instances(client):
    active_instances = []
    response = client.describe_instances(Filters=[{'Name': 'instance-state-name', 'Values': list(ACTIVE_INSTANCE_STATES)}])
    for reservation in response['Reservations']:
        for instance in reservation['Instances']:
            active_instances.append(instance)
            
    return active_instances

# Return a list of instance id's from a list of instances
def get_instance_ids(instances):
    instance_ids = []

    for instance in instances:
        instance_ids.append(instance['InstanceId'])

    return instance_ids

# Get or create an insight matching our FINDING_FILTERS (so we can get a count without having to paginate through a zillion findings)
def get_or_create_insight(client):
    # see if any existing insight matches our filters and group by attribute
    insights = client.get_insights()
    insight_arn = None
    for insight in insights['Insights']:
        if insight['Filters'] == FINDING_FILTERS and insight['GroupByAttribute'] == 'AwsAccountId':
            insight_arn = insight['InsightArn']
            break
    
    # if no matches, create one
    if not insight_arn:
        insight_arn = client.create_insight(Name=INSIGHT_NAME, Filters=FINDING_FILTERS, GroupByAttribute=GROUP_BY).get('InsightArn')
    
    return insight

# Get the count of findings that match our filters
def get_findings_count(client):
    insight = get_or_create_insight(client)
    results = client.get_insight_results(InsightArn=insight['InsightArn'])
    
    return results['InsightResults']['ResultValues'][0]['Count']


# Resolve all findings that are not active instances
def resolve_findings(client, ec2_client):
    last_update = 0
    update_interval = ACTIVE_INSTANCE_UPDATE_INTERVAL_MINS * 60  # convert to seconds
    # ARN format: arn:aws:ec2:us-east-1:123456789012:instance/i-1234567890abcdef0
    ec2_re = re.compile(r'i-(.*)$')
    
    # page through active security hub findings and resolve any that are solely referencing inactive instances
    paginator = client.get_paginator('get_findings')
    page_iterator = paginator.paginate(Filters=FINDING_FILTERS, MaxResults=100)
    request_bucket = THROTTLE_RATES['security_hub']['batch_update']
    t = time.time()
    print('Resolving...')
    for page in page_iterator:
        batch = []
        for finding in page['Findings']:
            # update the list of active instances every n minutes
            if ( time.time() - last_update ) > update_interval:
                active_instances = get_active_instances(ec2_client)
                active_instance_ids = get_instance_ids(active_instances)
                last_update = time.time()

            # decrement num_resources if the resource is not in the active instance list
            num_resources = len(finding['Resources'])
            for resource in finding['Resources']:
                if resource['Type'] == 'AwsEc2Instance':
                    # if the resource id is an ARN, extract the instance id, else assume it's already an instance id
                    if is_arn(resource['Id']):
                        instance_id = resource['Id'].split('/')[-1]
                    else:
                        instance_id = resource['Id']

                    # if it's a valid instance id and not in the list, decrement num_resources
                    if ec2_re.match(instance_id):
                        if instance_id not in active_instance_ids:
                            num_resources -= 1    
            
            # if there are no remaining active resources, add it to the batch
            if num_resources == 0:
                batch.append({'Id': finding['Id'], 'ProductArn': finding['ProductArn']})
    
        # throttle requests (leaky bucket)
        if (time.time() - t) > 1:
            request_bucket = THROTTLE_RATES['security_hub']['batch_update']
        if request_bucket == 0:
            print('Rate limit reached, backing off...')
            time.sleep(1.5)

        # update findings
        num_findings_in_batch = len(batch)
        if num_findings_in_batch > 0:
            print(f"Resolving {num_findings_in_batch} invalid findings...")
            result = client.batch_update_findings(
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
            if len(result['ProcessedFindings']) != num_findings_in_batch:
                print(f"Failed to resolve {num_findings_in_batch - len(result['ProcessedFindings'])} findings.")
                # print(f"Failed to resolve: {result['UnprocessedFindings']}")

def main():
    print("This script will query all findings matching the following search criteria:")
    print(" * ResourceType: AwsEc2Instance")
    print(" * WorkflowStatus: NEW or NOTIFIED")
    print(" * RecordState: ACTIVE")
    print(f"And will resolve any finding that no longer references active resources.\n")
    print(f"Findings will be marked resolved by '{RESOLVED_BY}' with the following note: '{RESOLVED_NOTE}'\n")
    # press enter to continue
    if input("This may take some time.. continue? (y/n): ").lower() != 'y':
        return
 
    print('Querying Security Hub...')
    client = boto3.client('securityhub', region_name='us-east-1')
    
    count = get_findings_count(client)
    print('There are {} findings matching the search criteria.'.format(count))
    if count == 0:
        sys.exit(0)

    ec2_client = boto3.client('ec2', region_name='us-east-1')
    num_resolved = resolve_findings(client, ec2_client)
    
    print(f"Done.\n")
    print(f"We resolved {num_resolved} of the original {count} findings.")
    print("Getting the latest count...")
    count = get_findings_count(client)
    print(f"There are now {count} findings matching the search criteria.")
    print("Note: this is a moving target and may change as new findings are generated or as other resolvers are running.")
    print("You may need to refresh the Security Hub console to see the updates.")
    
if __name__ == '__main__':
    main()
