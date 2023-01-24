import json
import os
from enum import Enum

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")
ALARM_ACTION_ARN = os.environ.get("ALARM_ACTION_ARN", "")
OK_ACTION_ARN = os.environ.get("OK_ACTION_ARN", "")
METRIC_NAMESPACE = os.environ.get("METRIC_NAMESPACE", f"bfd-{ENV}/bfd-server/CWAgent")
METRIC_NAME = os.environ.get("METRIC_NAME", "disk_used_percent")

DEFAULT_DIMENSIONS = [
    {"Name": "path", "Value": "/"},
    {"Name": "fstype", "Value": "xfs"},
    {"Name": "device", "Value": "nvme0n1p1"},
]

boto_config = Config(region_name=REGION)
cw_client = boto3.client("cloudwatch", config=boto_config)


class AutoScalingAction(str, Enum):
    """"""

    INSTANCE_CREATE = "EC2 Instance Launch Successful"
    INSTANCE_TERMINATE = "EC2 Instance Terminate Successful"


def handler(event, context):
    # TODO: Remove mocked-out data
    ENV = "prod"
    METRIC_NAMESPACE = f"bfd-{ENV}/bfd-server/CWAgent"
    ALARM_ACTION_ARN = "EXAMPLE"
    OK_ACTION_ARN = "EXAMPLE"

    if not ENV:
        print("ENV was not defined, exiting...")
        return

    if not ALARM_ACTION_ARN:
        print("ALARM_ACTION_ARN was not defined, exiting...")
        return

    if not OK_ACTION_ARN:
        print("OK_ACTION_ARN was not defined, exiting...")
        return

    # TODO: Remove sample event
    event = json.loads(
        """
{
  "version": "0",
  "id": "3e3c153a-8339-4e30-8c35-687ebef853fe",
  "detail-type": "EC2 Instance Launch Successful",
  "source": "aws.autoscaling",
  "account": "123456789012",
  "time": "2015-11-11T21:31:47Z",
  "region": "us-east-1",
  "resources": ["arn:aws:autoscaling:us-east-1:123456789012:autoScalingGroup:eb56d16b-bbf0-401d-b893-d5978ed4a025:autoScalingGroupName/sampleLuanchSucASG", "arn:aws:ec2:us-east-1:123456789012:instance/i-b188560f"],
  "detail": {
    "StatusCode": "InProgress",
    "AutoScalingGroupName": "example",
    "ActivityId": "9cabb81f-42de-417d-8aa7-ce16bf026590",
    "Details": {
      "Availability Zone": "us-east-1b",
      "Subnet ID": "subnet-95bfcebe"
    },
    "RequestId": "9cabb81f-42de-417d-8aa7-ce16bf026590",
    "EndTime": "2015-11-11T21:31:47.208Z",
    "EC2InstanceId": "i-EXAMPLE",
    "StartTime": "2015-11-11T21:31:13.671Z",
    "Cause": "At 2015-11-11T21:31:10Z a user request created an AutoScalingGroup changing the desired capacity from 0 to 1.  At 2015-11-11T21:31:11Z an instance was started in response to a difference between desired and actual capacity, increasing the capacity from 0 to 1."
  }
}"""
    )

    try:
        auto_scaling_action = AutoScalingAction(event["detail-type"])
    except KeyError:
        print(
            'Event does not contain property "detail-type", Lambda was invoked with incorrect event'
        )
        return
    except ValueError:
        print(f'Invalid "detail-type" was specified: {event["detail-type"]}')
        return

    try:
        instance_id: str = event["detail"]["EC2InstanceId"]
    except KeyError as ex:
        print(
            "No EC2 instance ID was specified by auto-scaling event. Event is missing keys:"
            f" {str(ex)}"
        )
        return

    try:
        asg_name: str = event["detail"]["AutoScalingGroupName"]
    except KeyError as ex:
        print(
            "No auto-scaling group name was specified by auto-scaling event. Event is missing"
            f" keys: {str(ex)}"
        )
        return

    alarm_name = f"bfd-server-{ENV}-alert-disk-usage-percent-{instance_id}"

    if auto_scaling_action == AutoScalingAction.INSTANCE_CREATE:
        print(f"Instance {instance_id} is being created, creating associated disk usage alarm")

        metric_dimensions = DEFAULT_DIMENSIONS + [
            {"Name": "InstanceId", "Value": instance_id},
            {"Name": "AutoScalingGroupName", "Value": asg_name},
        ]

        cw_client.put_metric_alarm(
            AlarmName=alarm_name,
            AlarmDescription=(
                f"Disk usage percent for BFD Server instance {instance_id} in"
                f" {ENV} environment exceeded 95% in the past minute"
            ),
            ComparisonOperator="GreaterThanThreshold",
            EvaluationPeriods=1,
            Namespace=METRIC_NAMESPACE,
            MetricName=METRIC_NAME,
            Dimensions=metric_dimensions,
            Statistic="Maximum",
            Period=60,
            Threshold=95.0,
            Unit="Percent",
            # ActionsEnabled=True,
            # AlarmActions=[ALARM_ACTION_ARN],
            # OKActions=[OK_ACTION_ARN],
        )
    elif auto_scaling_action == AutoScalingAction.INSTANCE_TERMINATE:
        print(f"Instance {instance_id} is being terminated, removing associated alarm")

        cw_client.delete_alarms(AlarmNames=[alarm_name])


handler(None, None)
