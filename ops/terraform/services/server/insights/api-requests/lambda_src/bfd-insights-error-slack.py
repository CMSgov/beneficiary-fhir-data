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
    if not ENV:
        print("ENV was not defined, exiting...")
        return

    # We take only the first record, if it exists
    try:
        record = event["Records"][0]
    except IndexError:
        print("Invalid event notification, no records found")
        return

    try:
        file_key = record["s3"]["object"]["key"]
    except KeyError as exc:
        print(f"No bucket file found in event notification: {exc}")
        return

    try:
        file_size = record["s3"]["object"]["size"]
    except KeyError as exc:
        print(f"No bucket file size found in event notification: {exc}")
        return

    try:
        wehbook_url = ssm_client.get_parameter(
            Name=f"/bfd/mgmt/common/sensitive/slack_webhook_bfd_alerts",
            WithDecryption=True,
        )["Parameter"]["Value"]
    except KeyError as exc:
        print(
            f'SSM parameter "/bfd/mgmt/common/sensitive/slack_webhook_bfd_alerts" not found: {exc.reason}'
        )
        return

    slack_message = {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"Warning: A new file was found in the BFD Insights Error Folder from `{ENV}` environment:\n"
                        f"\t*File Path:* `{file_key}`\n"
                        f"\t*File Size:* `{file_size}`\n"
                        f"For help troubleshooting, reference the following <https://github.com/CMSgov/beneficiary-fhir-data/blob/master/runbooks/how-to-investigate-firehose-ingestion-processing-failures.md|runbook>\n"
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
