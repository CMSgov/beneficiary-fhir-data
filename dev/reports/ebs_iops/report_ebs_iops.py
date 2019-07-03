#! /usr/bin/python
################################################################################
## report_ebs_iops.py
##
## Generate CSV with details on EBS volumes' IOPS usage.
##
## Derived from:
## * <https://n2ws.com/blog/ebs-snapshot/ebs-report>
################################################################################
import csv
import os, sys
import datetime
from collections import OrderedDict
import argparse
import boto3

# Defaults, can be modified
AWS_REGIONS = 'us-east-1|us-west-1|us-west-2|eu-west-1|ap-southeast-1|ap-northeast-1|ap-southeast-2|sa-east-1'

def open_file(filepath):
    """
    Opens the output file.
    """
    try:
        f = open(filepath, 'wt')
    except Exception as e:
        f = None
        sys.stderr.write('Could not open file %s. reason: %s\n' % (filepath, e))
        
    return f
        
def ec2_connect(region):
    """
    Connects to EC2, returns a connection object.
    """
    
    try:
        ec2 = boto3.resource('ec2', region_name=region)
    except Exception as e:
        sys.stderr.write('Could not connect to region: %s. Exception: %s\n' % (region, e))
        ec2 = None
        
    return ec2
        
def cloudwatch_connect(region):
    """
    Connects to CloudWatch, returns a connection object.
    """
    
    try:
        cloudwatch = boto3.client('cloudwatch', region_name=region)
    except Exception as e:
        sys.stderr.write('Could not connect to region: %s. Exception: %s\n' % (region, e))
        cloudwatch = None
        
    return cloudwatch
        
def create_ebs_report(regions, filepath):
    """
    Creates the actual report, first into a python data structure
    Then write into a csv file
    """
    # opens file
    f = open_file (filepath)
    if not f:
        return False
    
    region_list = regions.split('|')
    
    volume_dict = {}
    # go over all regions in list
    for region in region_list:
    
        # connects to ec2
        ec2 = ec2_connect(region)
        if not ec2:
            sys.stderr.write('Could not connect to region: %s. Skipping\n' % region)
            continue
   
        cloudwatch = cloudwatch_connect(region)
        if not cloudwatch:
            sys.stderr.write('Could not connect to CloudWatch. Skipping\n')
            return False
     
        # get all volumes and snapshots
        volumes = ec2.volumes.all()
        
        volume_dict[region] = {}
        # goes over volumes and insert relevant data into a python dictionary
        for volume in volumes:
            if volume.tags:
                tags = volume.tags
            else:
                tags = []
            name_tag = next((tag for tag in tags if tag['Key'] == 'Name'), {'Key': 'Name', 'Value': ''})
            name = name_tag['Value']
                
            iops = volume.iops
            if iops == None: iops = 0
            
            if volume.attachments:
                instance_id = volume.attachments[0]['InstanceId']
                device = volume.attachments[0]['Device']
                instance = ec2.Instance(instance_id)
                instance_tags = instance.tags or []
                instance_names = [tag.get('Value') for tag in instance_tags if tag.get('Key') == 'Name']
                instance_name = instance_names[0] if instance_names else None
            else:
                instance_id = 'N/A'
                device = 'N/A'
                instance_name = 'N/A'
            
            if volume.encrypted:
                encrypted = 'yes'
            else:
                encrypted = 'no'
            
            iops_daily = {}
            iops_read_response = cloudwatch.get_metric_statistics(
                Namespace='AWS/EBS',
                MetricName='VolumeReadOps',
                Dimensions=[{'Name': 'VolumeId', 'Value': volume.id}],
                StartTime=datetime.datetime(2017, 1, 1),
                EndTime=datetime.datetime.utcnow(),
                Period=(24*60*60),
                Statistics=['Sum'],
                Unit='Count'
            )
            for iops_read_datapoint in iops_read_response['Datapoints']:
                iops_daily[iops_read_datapoint['Timestamp']] = {'VolumeReadOps': iops_read_datapoint['Sum']}
            iops_write_response = cloudwatch.get_metric_statistics(
                Namespace='AWS/EBS',
                MetricName='VolumeWriteOps',
                Dimensions=[{'Name': 'VolumeId', 'Value': volume.id}],
                StartTime=datetime.datetime(2017, 1, 1),
                EndTime=datetime.datetime.utcnow(),
                Period=(24*60*60),
                Statistics=['Sum'],
                Unit='Count'
            )
            for iops_write_datapoint in iops_write_response['Datapoints']:
                iops_daily.setdefault(iops_write_datapoint['Timestamp'], {})['VolumeWriteOps'] = iops_read_datapoint['Sum']
            iops_daily_sorted = {k: iops_daily[k] for k in sorted(iops_daily)}
 
            volume_dict[region][volume.id] = { 'name': name, 
                                             'size': volume.size,
                                             'zone': volume.availability_zone,
                                             'type': volume.volume_type,
                                             'iops': iops,
                                             'orig_snap': volume.snapshot_id,
                                             'encrypted': encrypted,
                                             'instance_id': instance_id,
                                             'instance_name': instance_name,
                                             'device': device,
                                             'iops_daily': iops_daily_sorted
                                            }

    # Start the CSV file.
    writer = csv.writer(f)
    writer.writerow(['Region', 'volume ID', 'Volume Name', 'Volume Type', 'iops', 'Size (GiB)', \
                     'Created from Snapshot', 'Attached To: Instance ID', 'Attached To: Instance Name', \
                     'Device', 'Encrypted', 'IOPS: Date', 'IOPS: Read', 'IOPS: Write'])
         
    # Write out CSV rows.
    for region in volume_dict.keys():
        for volume_id in volume_dict[region].keys():
            volume = volume_dict[region][volume_id]
            for iops_timestamp, iops_values in volume['iops_daily'].items():
                writer.writerow([region, volume_id, volume['name'], volume['type'], volume['iops'], volume['size'], \
                                 volume['orig_snap'], volume['instance_id'], volume['instance_name'], volume['device'], volume['encrypted'], \
                                 iops_timestamp, iops_values['VolumeReadOps'], iops_values['VolumeWriteOps']])
                              
    f.close()
    return True
    
if __name__ == '__main__':

    # Define command line argument parser
    parser = argparse.ArgumentParser(description='Creates a CSV report about EBS volumes.')
    parser.add_argument('--regions', default = AWS_REGIONS, help='AWS regions to create the report on, can add multiple with | as separator. Default will assume all regions')
    parser.add_argument('--file', required=True, help='Path for output CSV file')
    
    args = parser.parse_args()

    # creates the report
    retval = create_ebs_report(args.regions, args.file)
    if retval:
        sys.exit (0)
    else:
        sys.exit (1)