import os
import re
import time
from urllib.parse import unquote

import boto3
from botocore.config import Config

START_CRAWLER_RETRY_TIMES = [15.0, 15.0, 30.0, 30.0, 60.0, 60.0, 60.0, 120.0, 120.0]
"""Constant specifying a list of progressively longer wait times to continuously retry running
the glue crawler in case multiple S3 event notifications are consumed by this lambda at once"""
GET_PARTITION_RETRY_TIMES = [1.0, 2.0, 3.0, 6.0, 10.0, 10.0, 15.0, 30.0, 60.0]
"""Constant specifying a list of progressively longer wait times to continuously retry getting
the specified table's partition corresponding to the incoming S3 event notification if an error
occurs that can be retried upon"""
REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
CRAWLER_NAME = os.environ.get("CRAWLER_NAME")
DATABASE_NAME = os.environ.get("GLUE_DATABASE_NAME")
TABLE_NAME = os.environ.get("GLUE_TABLE_NAME")

boto_config = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
glue_client = boto3.client(service_name="glue", config=boto_config)


def try_start_crawler(name: str) -> bool:
    for wait_time in START_CRAWLER_RETRY_TIMES:
        try:
            glue_client.start_crawler(Name=name)
            return True
        except glue_client.exceptions.CrawlerRunningException:
            print(
                f"{CRAWLER_NAME} was already running, waiting {wait_time} seconds before"
                " retrying..."
            )

        time.sleep(wait_time)

    return False


def handler(event, context):
    if not all([REGION, CRAWLER_NAME, DATABASE_NAME, TABLE_NAME]):
        print("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record = event["Records"][0]
    except KeyError as exc:
        print(f"The incoming event was invalid: {exc}")
        return
    except IndexError:
        print("Invalid event notification, no records found")
        return

    try:
        file_key = record["s3"]["object"]["key"]
    except KeyError as exc:
        print(f"No bucket file found in event notification: {exc}")
        return

    decoded_file_key = unquote(file_key)
    if match := re.search(
        f"databases/{DATABASE_NAME}/{TABLE_NAME}/year=(\d{{4}})/month=(\d{{2}})/.*",
        decoded_file_key,
        re.IGNORECASE,
    ):
        year = match.group(1)
        month = match.group(2)
        for retry_time in GET_PARTITION_RETRY_TIMES:
            try:
                glue_client.get_partition(
                    DatabaseName=DATABASE_NAME,
                    TableName=TABLE_NAME,
                    PartitionValues=[year, month],
                )

                print(f"A partition for year {year} and month {month} already exists, stopping...")
                return
            except glue_client.exceptions.EntityNotFoundException:
                print(
                    f"A partition for year {year} and month {month} was not found, starting the"
                    f" {CRAWLER_NAME} crawler to add the new partition to {TABLE_NAME}..."
                )

                if try_start_crawler(CRAWLER_NAME):
                    print(f"{CRAWLER_NAME} started successfully")
                else:
                    print(f"{CRAWLER_NAME} was not able to be started, stopping...")

                return
            except (
                glue_client.exceptions.InternalServiceException,
                glue_client.exceptions.OperationTimeoutException,
            ) as exc:
                print(
                    "A timeout or internal service exception occurred, retrying in"
                    f" {retry_time} seconds; error: {exc}"
                )
            except Exception as exc:
                print(
                    f"An unknown error occurred when trying to get partitions for {TABLE_NAME}:"
                    f" {exc}"
                )
                return

            time.sleep(retry_time)
    else:
        print(f"No year and month partitions found in path: {decoded_file_key}")
