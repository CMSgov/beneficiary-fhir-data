import json
import os
from urllib.error import URLError
from urllib.request import Request, urlopen

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")
WEBHOOK_SSM_PATH = os.environ.get("WEBHOOK_SSM_PATH", "")

boto_config = Config(region_name=REGION)
ssm_client = boto3.client("ssm", config=boto_config)


def slack_escape_str(str_to_escape: str) -> str:
    """Escapes a string such that Slack can properly display it.
    See https://api.slack.com/reference/surfaces/formatting#escaping

    Args:
        str_to_escape (str): The string to escape

    Returns:
        str: A string escaped such that Slack can properly display it
    """
    return str_to_escape.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def handler(event, context):
    if not ENV:
        print("ENV was not defined, exiting...")
        return

    if not WEBHOOK_SSM_PATH:
        print("WEBHOOK_SSM_PATH was not defined, exiting...")
        return

    # We take only the first record, if it exists
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
        alarm_info = json.loads(sns_message)
    except json.JSONDecodeError:
        print("SNS message body was not valid JSON")
        return

    try:
        # We do not escape "<" or ">" to allow for links to be embedded in the Alarm message
        unescaped_alarm_message: str = alarm_info["AlarmDescription"]
        alarm_message = unescaped_alarm_message.replace("&", "&amp;")

        alarm_name = slack_escape_str(alarm_info["AlarmName"])
        alarm_reason = slack_escape_str(alarm_info["NewStateReason"])
        alarm_metric = slack_escape_str(alarm_info["Trigger"]["MetricName"])
    except KeyError as exc:
        print(f"Unable to retrieve alarm information from SNS message: {exc}")
        return

    try:
        wehbook_url = ssm_client.get_parameter(
            Name=WEBHOOK_SSM_PATH,
            WithDecryption=True,
        )["Parameter"]["Value"]
    except KeyError as exc:
        print(f"SSM parameter {WEBHOOK_SSM_PATH} not found: {exc.reason}")
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
        with urlopen(
            request, data=json.dumps(slack_message).encode("utf-8")
        ) as response:
            if response.status == 200:
                print(f"Message posted successfully")
            else:
                print(f"{response.status} response received from Slack")
    except URLError as e:
        print(f"Unable to connect to Slack: {e.reason}")
