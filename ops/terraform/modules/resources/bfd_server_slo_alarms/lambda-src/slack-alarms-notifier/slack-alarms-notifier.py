# imports
import json
import os
from urllib.error import URLError
from urllib.request import Request, urlopen

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ENV = os.environ.get("ENV", "")

boto_config = Config(region_name=REGION)
ssm_client = boto3.client("ssm", config=boto_config)


def handler(event, context):
    if not WEBHOOK_URL:
        print("WEBHOOK_URL was not defined, exiting...")
        return

    if not ENV:
        print("ENV was not defined, exiting...")
        return

    try:
        wehbook_url = ssm_client.get_parameter(
            Name=f"/bfd/mgmt/common/sensitive/slack_webhook_bfd_test",
            WithDecryption=True,
        )["Parameter"]["Value"]
    except KeyError as exc:
        print(
            f'SSM parameter "/bfd/mgmt/common/sensitive/slack_webhook_bfd_test" not found: {exc.reason}'
        )
        return

    # Read message posted on SNS Topic
    sns_message = event["Records"]
    slack_message = {
        "text": f"CloudWatch SLO Alarm alert received from {ENV} with message: {sns_message}"
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
