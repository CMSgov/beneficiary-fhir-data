import json
import logging
import os
import sys
from typing import Any, Mapping
from urllib.error import URLError
from urllib.request import Request, urlopen

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")
WEBHOOK_SSM_PATH = os.environ.get("WEBHOOK_SSM_PATH", "")

boto_config = Config(region_name=REGION)

logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()
try:
    ssm_client = boto3.client("ssm", config=boto_config)  # type: ignore
except Exception:
    logger.error(
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources:",
        exc_info=True,
    )
    sys.exit(0)


def slack_escape_str(str_to_escape: str) -> str:
    """Escapes a string such that Slack can properly display it.
    See https://api.slack.com/reference/surfaces/formatting#escaping

    Args:
        str_to_escape (str): The string to escape

    Returns:
        str: A string escaped such that Slack can properly display it
    """
    return str_to_escape.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def handler(event: Any, context: Any):
    if not ENV:
        logger.error("ENV was not defined, exiting...")
        return

    if not WEBHOOK_SSM_PATH:
        logger.error("WEBHOOK_SSM_PATH was not defined, exiting...")
        return

    # We take only the first record, if it exists
    try:
        record: Mapping[Any, Any] = event["Records"][0]
    except IndexError:
        logger.error("Invalid SNS notification, no records found; exc:\n", exc_info=True)
        return

    try:
        sns_message: str = record["Sns"]["Message"]
    except KeyError:
        logger.error("No message found in SNS notification; exc:\n", exc_info=True)
        return

    try:
        alarm_info = json.loads(sns_message)
    except json.JSONDecodeError:
        logger.error("SNS message body was not valid JSON; exc:\n", exc_info=True)
        return

    try:
        # We do not escape "<" or ">" to allow for links to be embedded in the Alarm message
        unescaped_alarm_message: str = alarm_info["AlarmDescription"]
        alarm_message = unescaped_alarm_message.replace("&", "&amp;")

        alarm_name = slack_escape_str(alarm_info["AlarmName"])
        alarm_reason = slack_escape_str(alarm_info["NewStateReason"])
    except KeyError:
        logger.error("Unable to retrieve alarm information from SNS message; exc:\n", exc_info=True)
        return

    try:
        # For computed metric alarms (that is, alarms that alarm on a metric math expression), there
        # is no corresponding "MetricName" property and so this will fail. If it does, we specify
        # the metric name as "N/A"
        alarm_metric = slack_escape_str(alarm_info["Trigger"]["MetricName"])
    except KeyError:
        alarm_metric = slack_escape_str("N/A (computed metric)")

    try:
        wehbook_url: str = ssm_client.get_parameter(Name=WEBHOOK_SSM_PATH, WithDecryption=True)[
            "Parameter"
        ]["Value"]
    except KeyError:
        logger.error("SSM parameter %s not found; exc:\n", WEBHOOK_SSM_PATH, exc_info=True)
        return

    slack_message = {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"CloudWatch Alarm alert received from `{ENV}` environment:\n"
                        f"\t*Alarm Name:* `{alarm_name}`\n"
                        f"\t*Alarm Reason:* `{alarm_reason}`\n"
                        f"\t*Alarm Metric:* `{alarm_metric}`\n"
                        f"\t*Alarm Message:* {alarm_message}\n"
                    ),
                },
            }
        ]
    }

    request = Request(wehbook_url, method="POST")
    request.add_header("Content-Type", "application/json")
    try:
        with urlopen(request, data=json.dumps(slack_message).encode("utf-8")) as response:
            if response.status == 200:
                logger.info("Message posted successfully")
            else:
                logger.info("%s response received from Slack", response.status)
    except URLError:
        logger.error("Unable to connect to Slack; exc:\n", exc_info=True)
