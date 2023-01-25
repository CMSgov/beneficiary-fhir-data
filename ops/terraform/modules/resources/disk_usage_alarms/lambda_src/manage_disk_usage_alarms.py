import os
from enum import Enum

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")
ALARMS_PREFIX = os.environ.get("ALARMS_PREFIX", "")
ALARM_THRESHOLD = float(os.environ.get("ALARM_THRESHOLD", "95.0"))
ALARM_PERIOD = int(os.environ.get("ALARM_PERIOD", "60"))
ALARM_ACTION_ARN = os.environ.get("ALARM_ACTION_ARN", "")
OK_ACTION_ARN = os.environ.get("OK_ACTION_ARN", "")
METRIC_NAMESPACE = os.environ.get("METRIC_NAMESPACE", "")
METRIC_NAME = os.environ.get("METRIC_NAME", "")

DEFAULT_DIMENSIONS = [
    {"Name": "path", "Value": "/"},
    {"Name": "fstype", "Value": "xfs"},
    {"Name": "device", "Value": "nvme0n1p1"},
]

boto_config = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
cw_client = boto3.client("cloudwatch", config=boto_config)


class AutoScalingAction(str, Enum):
    """Represents the possible AWS EventBridge AutoScaling actions that this Lambda will react to"""

    INSTANCE_LAUNCH = "EC2 Instance Launch Successful"
    """Represents when an EC2 instance is launched in an AutoScaling Group"""
    INSTANCE_TERMINATE = "EC2 Instance Terminate Successful"
    """Represents when an EC2 instance is terminated in an AutoScaling Group"""


def handler(event, context):
    if not all(
        [
            REGION,
            ENV,
            ALARMS_PREFIX,
            ALARM_THRESHOLD,
            ALARM_PERIOD,
            ALARM_ACTION_ARN,
            OK_ACTION_ARN,
            METRIC_NAMESPACE,
            METRIC_NAME,
        ]
    ):
        print("Not all necessary environment variables were defined, exiting...")
        return

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

    alarm_name = f"{ALARMS_PREFIX}-{instance_id}"

    if auto_scaling_action == AutoScalingAction.INSTANCE_LAUNCH:
        print(f"Instance {instance_id} is being created, creating associated disk usage alarm")

        metric_dimensions = DEFAULT_DIMENSIONS + [
            {"Name": "InstanceId", "Value": instance_id},
            {"Name": "AutoScalingGroupName", "Value": asg_name},
        ]

        cw_client.put_metric_alarm(
            AlarmName=alarm_name,
            AlarmDescription=(
                f"Disk usage percent for BFD Server instance {instance_id} in"
                f" {ENV} environment exceeded {ALARM_THRESHOLD}% in the past {ALARM_PERIOD} seconds"
            ),
            ComparisonOperator="GreaterThanThreshold",
            EvaluationPeriods=1,
            Namespace=METRIC_NAMESPACE,
            MetricName=METRIC_NAME,
            Dimensions=metric_dimensions,
            Statistic="Maximum",
            Period=ALARM_PERIOD,
            Threshold=ALARM_THRESHOLD,
            Unit="Percent",
            TreatMissingData="notBreaching",
            ActionsEnabled=True,
            AlarmActions=[ALARM_ACTION_ARN],
            OKActions=[OK_ACTION_ARN],
        )
        
        print(f"Alarm {alarm_name} successfully created")
    elif auto_scaling_action == AutoScalingAction.INSTANCE_TERMINATE:
        print(f"Instance {instance_id} is being terminated, removing associated alarm")

        cw_client.delete_alarms(AlarmNames=[alarm_name])

        print(f"Alarm {alarm_name} successfully deleted")
