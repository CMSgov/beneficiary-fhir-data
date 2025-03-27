import os
import time
from typing import Any

import boto3

RETRY_TIMES = [15.0, 15.0, 30.0, 30.0, 60.0, 60.0, 60.0, 120.0, 120.0]
"""Constant specifying a list of progressively longer wait times to continuously retry running the
glue crawler in case multiple S3 event notifications are consumed by this lambda at once"""

crawler_name = os.environ.get("CRAWLER_NAME")

glue = boto3.client(service_name="glue", region_name="us-east-1")  # type: ignore


def try_run_crawler(name: str) -> bool:
    for wait_time in RETRY_TIMES:
        try:
            glue.start_crawler(Name=name)
            return True
        except glue.exceptions.CrawlerRunningException:
            print(
                f"{crawler_name} was already running, waiting {wait_time} seconds before "
                "retrying..."
            )

        time.sleep(wait_time)

    return False


def handler(event: dict[str, Any], context: Any) -> None:
    if not crawler_name:
        print('"CRAWLER_NAME" environment variable unspecified, stopping...')
        return

    if try_run_crawler(crawler_name):
        print(f"{crawler_name} ran successfully")
    else:
        print(f"{crawler_name} was not able to be ran, stopping...")
