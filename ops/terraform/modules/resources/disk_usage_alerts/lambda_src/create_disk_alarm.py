import os
from enum import Enum

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")
ALARM_ACTION_ARN = os.environ.get("ALARM_ACTION_ARN", "")
OK_ACTION_ARN = os.environ.get("OK_ACTION_ARN", "")

boto_config = Config(region_name=REGION)
cw_client = boto3.client("cloudwatch", config=boto_config)


class AutoScalingAction(str, Enum):
    """"""

    UNKNOWN = ""
    INSTANCE_CREATE = "create"
    INSTANCE_TERMINATE = ""


def handler(event, context):
    ENV = "prod"
    ALARM_ACTION_ARN = "EXAMPLE"
    OK_ACTION_ARN = "EXAMPLE"
    if not ENV:
        print("ENV was not defined, exiting...")
        return

    if not ALARM_ACTION_ARN:
        print("ALARM_ACTION_ARN was not defined, exiting...")
        return

    # extract instance ID

    instance_id: str = "EXAMPLE"
    alarm_name = f"bfd-server-{ENV}-alert-disk-usage-percent-{instance_id}"
    auto_scaling_action = AutoScalingAction.INSTANCE_TERMINATE

    if auto_scaling_action == AutoScalingAction.INSTANCE_CREATE:
        print(f"Instance {instance_id} is being created, creating associated disk usage alarm")

        # TODO: This might not work since if an instance is being created it will not have
        # metrics associated with it -- may need to hardcode this or do something else
        instance_metrics = cw_client.list_metrics(
            Namespace=f"bfd-{ENV}/bfd-server/CWAgent",
            MetricName="disk_used_percent",
            Dimensions=[
                {"Name": "path", "Value": "/"},
                {"Name": "fstype", "Value": "xfs"},
                {"Name": "InstanceId", "Value": instance_id},
            ],
        )["Metrics"]

        if len(instance_metrics) == 0:
            print(f"No applicable disk usage metric found for {instance_id}, exiting...")
            return

        print(f"Disk usage percent metrics found for {instance_id}")
        disk_usage_metric = instance_metrics.pop()

        cw_client.put_metric_alarm(
            AlarmName=alarm_name,
            AlarmDescription=(
                f"Disk usage percent for BFD Server instance {instance_id} in"
                f" {ENV} environment exceeded 95% in the past minute"
            ),
            ComparisonOperator="GreaterThanThreshold",
            EvaluationPeriods=1,
            Namespace=disk_usage_metric["Namespace"],
            MetricName=disk_usage_metric["MetricName"],
            Dimensions=disk_usage_metric["Dimensions"],
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
    else:
        print("Unknown action requested, exiting...")


handler(None, None)
