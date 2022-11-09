# imports
import json
import os
from urllib.error import URLError
from urllib.request import Request, urlopen

WEBHOOK_URL = os.environ.get("WEBHOOK_URL", "")
ENV = os.environ.get("ENV", "")

boto_config = Config(region_name=region)
ssm_client = boto3.client("ssm", config=boto_config)

def handler(event, context):
    if not WEBHOOK_URL:
        print("WEBHOOK_URL was not defined, exiting...")
        return

    if not ENV:
        print("ENV was not defined, exiting...")
        return

    # Read message posted on SNS Topic
    sns_message = event["Records"]
    slack_message = {
        "text": f"CloudWatch SLO Alarm alert received from {ENV} with message: {sns_message}"
    }

    request = Request(WEBHOOK_URL, method="POST")
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
