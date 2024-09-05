import json
import os
from enum import StrEnum

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")
ALARMS_PREFIX = os.environ.get("ALARMS_PREFIX", "")
ALARM_THRESHOLD = float(os.environ.get("ALARM_THRESHOLD", "95.0"))
ALARM_PERIOD = int(os.environ.get("ALARM_PERIOD", "60"))
ALARM_ACTION_ARN = os.environ.get("ALARM_ACTION_ARN", "") # The ALARM action is optional
OK_ACTION_ARN = os.environ.get("OK_ACTION_ARN", "") # The OK action is optional
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
autoscaling_client = boto3.client("autoscaling", config=boto_config)


class AutoScalingEvent(StrEnum):
    """Represents the possible AWS AutoScaling Notifcations events that this Lambda will react to"""

    INSTANCE_LAUNCH = "autoscaling:EC2_INSTANCE_LAUNCH"
    """Represents when an EC2 instance is launched in an AutoScaling Group"""
    INSTANCE_TERMINATE = "autoscaling:EC2_INSTANCE_TERMINATE"
    """Represents when an EC2 instance is terminated in an AutoScaling Group"""


def handler(event, context):
    if not all(
        [
            REGION,
            ENV,
            ALARMS_PREFIX,
            ALARM_THRESHOLD,
            ALARM_PERIOD,
            METRIC_NAMESPACE,
            METRIC_NAME,
        ]
    ):
        print("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record = event["Records"][0]
    except IndexError:
        print("Invalid SNS notification, no records found")
        return

    try:
        sns_message = record["Sns"]["Message"]
    except KeyError as exc:
        print(f"No message found in SNS notification: {exc}")
        return

    try:
        asg_notification = json.loads(sns_message)
    except json.JSONDecodeError:
        print("SNS message body was not valid JSON")
        return

    try:
        auto_scaling_action = AutoScalingEvent(asg_notification["Event"])
    except KeyError:
        print(
            'Notification does not contain property "Event", Lambda was invoked with'
            " incorrect event"
        )
        return
    except ValueError:
        print(f'Invalid "Event" was specified: {asg_notification["Event"]}')
        return

    print(f"Valid AutoScaling Event {auto_scaling_action} received")

    try:
        asg_name: str = asg_notification["AutoScalingGroupName"]
    except KeyError as ex:
        print(
            "No auto-scaling group name was specified by auto-scaling event. Event is"
            f" missing keys: {str(ex)}"
        )
        return

    asg_details = autoscaling_client.describe_auto_scaling_groups(AutoScalingGroupNames=[asg_name])

    try:
        asg_instances = [x["InstanceId"] for x in asg_details["AutoScalingGroups"][0]["Instances"]]
    except KeyError as ex:
        print(f"AutoScaling Group details are missing from ASG {asg_name}, see: {str(ex)}")
        return

    try:
        existing_alarms = [
            x["AlarmName"]
            for x in cw_client.describe_alarms(AlarmNamePrefix=ALARMS_PREFIX)["MetricAlarms"]
        ]
    except KeyError as ex:
        print(f"Unable to discover existing metric alarms: {str(ex)}")
        return

    alarms_to_ids = {f"{ALARMS_PREFIX}-{instance_id}": instance_id for instance_id in asg_instances}
    alarms_to_create = list(set(alarms_to_ids.keys()) - set(existing_alarms))
    # Remaining existing alarms, after subtracting those that _should_ exist, are alarms for
    # instances that no longer exist in the current ASG
    alarms_to_delete = list(set(existing_alarms) - set(alarms_to_ids.keys()))

    if alarms_to_create:
        # Take only instances that have no existing alarm -- this dictionary maps the instance's ID
        # to its alarm name
        ids_to_new_alarms = {
            alarms_to_ids[alarm_name]: alarm_name for alarm_name in alarms_to_create
        }

        # Create new alarms for all instances in ASG
        for instance_id, alarm_name in ids_to_new_alarms.items():
            print(f"Alarm {alarm_name} does not exist already, creating it...")

            metric_dimensions = DEFAULT_DIMENSIONS + [
                {"Name": "InstanceId", "Value": instance_id},
                {"Name": "AutoScalingGroupName", "Value": asg_name},
            ]

            cw_client.put_metric_alarm(
                AlarmName=alarm_name,
                AlarmDescription=(
                    f"Disk usage percent for BFD Server instance {instance_id} in"
                    f" {ENV} environment exceeded {ALARM_THRESHOLD}% in the past"
                    f" {ALARM_PERIOD} seconds"
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
                AlarmActions=[ALARM_ACTION_ARN] if ALARM_ACTION_ARN else [],
                OKActions=[OK_ACTION_ARN] if OK_ACTION_ARN else [],
            )

            print(f"Alarm {alarm_name} successfully created")
    else:
        print(f"Alarms for all instances in ASG {asg_name} already exist")

    if alarms_to_delete:
        print(
            "The following alarms exist but their corresponding EC2 instances are no"
            f" longer InService in the {asg_name} ASG {asg_name} so they will be"
            f" deleted: {alarms_to_delete}"
        )

        cw_client.delete_alarms(AlarmNames=alarms_to_delete)

        print(f"The following alarms were successfully deleted: {alarms_to_delete}")
    else:
        print(f"No stale disk usage alarms found, skipping deletion")
