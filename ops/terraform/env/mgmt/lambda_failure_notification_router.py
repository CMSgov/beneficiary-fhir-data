import boto3
from datetime import datetime, timedelta, timezone
import json
import os
import logging

sns_client = boto3.client("sns")
cw_client = boto3.client("cloudwatch")
lambda_client = boto3.client("lambda")
ssm_client = boto3.client("ssm")
cloudwatch_client = boto3.client("cloudwatch")

DEFAULT_SNS_TOPIC_ARN = os.environ["DEFAULT_SNS_TOPIC_ARN"]
VICTOR_OPS = os.environ["VICTOR_OPS"]
BFD_INTERNAL_TOPIC_ARN = os.environ["BFD_INTERNAL"]
BFD_ALERTS_TOPIC_ARN = os.environ["BFD_ALERTS"]
BFD_WARNINGS_TOPIC_ARN = os.environ["BFD_WARNINGS"]
BFD_NOTICES_TOPIC_ARN = os.environ["BFD_NOTICES"]

DO_NOT_NOTIFY_FOR_PARAM = os.environ["DO_NOT_NOTIFY_FOR_PARAM"] 
SEND_ALERT_TO_PARAM = os.environ["SEND_ALERT_TO_PARAM"] 

TOPIC_TO_ROUTE_TO = {
    "DEFAULT_SNS_TOPIC": DEFAULT_SNS_TOPIC_ARN,
    "VICTOR_OPS": VICTOR_OPS,
    "BFD_INTERNAL": BFD_INTERNAL_TOPIC_ARN,
    "BFD_ALERTS": BFD_ALERTS_TOPIC_ARN,
    "BFD_WARNINGS": BFD_WARNINGS_TOPIC_ARN,
    "BFD_NOTICES": BFD_NOTICES_TOPIC_ARN,
}

logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()

def get_failed_lambdas(lookback_minutes=2):
    lambda_client = boto3.client("lambda")
    cloudwatch = boto3.client("cloudwatch")

    # Time window
    end_time = datetime.now(timezone.utc)
    start_time = end_time - timedelta(minutes=lookback_minutes)

    # List all Lambda functions
    function_names = []
    paginator = lambda_client.get_paginator("list_functions")
    for page in paginator.paginate():
        for fn in page["Functions"]:
            function_names.append(fn["FunctionName"])

    if not function_names:
        logger.info("No Lambda functions found.")
        return []

    # Prepare MetricDataQueries
    queries = []
    for idx, fn_name in enumerate(function_names):
        queries.append({
            "Id": f"errors_{idx}",
            "MetricStat": {
                "Metric": {
                    "Namespace": "AWS/Lambda",
                    "MetricName": "Errors",
                    "Dimensions": [{"Name": "FunctionName", "Value": fn_name}],
                },
                "Period": 60,
                "Stat": "Sum",
            },
            "Label": fn_name,
            "ReturnData": True,
        })

    failed_functions = set()

    for i in range(0, len(queries), 100):
        batch = queries[i : i + 100]
        response = cloudwatch.get_metric_data(
            MetricDataQueries=batch,
            StartTime=start_time,
            EndTime=end_time,
            ScanBy="TimestampDescending",
        )
        for result in response["MetricDataResults"]:
            if any(value > 0 for value in result["Values"]):
                failed_functions.add(result["Label"])

    return failed_functions

def get_do_not_notify_list():
    try:
        response = ssm_client.get_parameter(Name=DO_NOT_NOTIFY_FOR_PARAM)
        logger.info(f"Do not notify list retrieved successfully: {str(response["Parameter"]["Value"])}")
        value = response["Parameter"]["Value"]
        return value.split(",") if value else []
    except Exception as e:
        logger.error(f"Error retrieving or parsing SSM parameter {DO_NOT_NOTIFY_FOR_PARAM}: {e}")
        return []

def get_send_alert_to():
    try:
        response = ssm_client.get_parameter(Name=SEND_ALERT_TO_PARAM)
        logger.info(f"Send alert to list retrieved successfully: {str(response["Parameter"]["Value"])}")
        value = response["Parameter"]["Value"]
        return json.loads(value) if value else {}
    except Exception as e:
        logger.error(f"Error retrieving or parsing SSM parameter {SEND_ALERT_TO_PARAM}: {e}")
        return []

def handler(event, context):
    try:
        alarm_name = event["detail"]["alarmName"]

        failed_lambda_functions = get_failed_lambdas(lookback_minutes=2)
        logger.info("Failed Lambdas: " + str(failed_lambda_functions))

        do_not_notify_for = get_do_not_notify_list()
        send_alert_for = get_send_alert_to()

        failed_lambda_functions_to_notify = failed_lambda_functions.difference(
            set(do_not_notify_for)
        )

        logger.info("Failed Lambdas to notify: " + str(failed_lambda_functions_to_notify))

        for lambda_function_name in failed_lambda_functions_to_notify:
            # Determine SNS topic ARN to use
            topic_arn = TOPIC_TO_ROUTE_TO.get(
                send_alert_for.get(lambda_function_name, "DEFAULT_SNS_TOPIC")
            )
            logger.info(
                f"Sending notification to {topic_arn} for function {lambda_function_name}"
            )
            message = json.dumps(
                {
                    "AlarmDescription": f"A Lambda failure occured for function {lambda_function_name}",
                    "AlarmName": f"{alarm_name}",
                    "NewStateReason": f"Lambda function '{lambda_function_name}' triggered alarm '{alarm_name}'.",
                    "Trigger": {
                        "MetricName": "Errors",
                    },
                }
            )
            sns_client.publish(
                TopicArn=topic_arn,
                Subject=f"Lambda Failure: {lambda_function_name}",
                Message=message,
            )
            logger.info(f"Notification sent to {topic_arn} for function {lambda_function_name}")
    except Exception as e:
        logger.error(f"{context.function_name}-Error failed to send SNS notification: {e}")

    return "Processed successfully"
