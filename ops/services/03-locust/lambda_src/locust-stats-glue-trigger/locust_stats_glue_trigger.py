import os
import re
import time
from typing import TYPE_CHECKING, Any
from urllib.parse import unquote

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parser import parse
from aws_lambda_powertools.utilities.parser.models import S3Model
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config

if TYPE_CHECKING:
    from mypy_boto3_glue import GlueClient
else:
    GlueClient = object

REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
CRAWLER_NAME = os.environ.get("CRAWLER_NAME", default="")
DATABASE_NAME = os.environ.get("DATABASE_NAME", default="")
TABLE_NAME = os.environ.get("TABLE_NAME", default="")
PARTITIONS = [
    partition.strip() for partition in os.environ.get("PARTITIONS", default="").split(",")
]

RETRY_TIMES = [15.0, 15.0, 30.0, 30.0, 60.0, 60.0, 60.0, 120.0, 120.0]
"""Constant specifying a list of progressively longer wait times to continuously retry running the
glue crawler in case multiple S3 event notifications are consumed by this lambda at once"""
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
S3_KEY_MATCH_REGEX = re.compile(
    pattern=rf"databases/{DATABASE_NAME}/{TABLE_NAME}/{'/'.join([f'{partition}=(.*)' for partition in PARTITIONS])}/.*",
    flags=re.IGNORECASE,
)

logger = Logger()


def try_run_crawler(glue_client: GlueClient, name: str) -> None:
    for wait_time in RETRY_TIMES:
        try:
            glue_client.start_crawler(Name=name)
        except glue_client.exceptions.CrawlerRunningException:
            logger.warning(
                "%s was already running, waiting %f seconds before retrying...", name, wait_time
            )

        time.sleep(wait_time)


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:
    if not all([REGION, CRAWLER_NAME, DATABASE_NAME, TABLE_NAME, PARTITIONS]):
        raise RuntimeError("Not all necessary environment variables were defined")

    try:
        s3_event = next(x for x in parse(model=S3Model, event=event).Records)
        glue_client = boto3.client(service_name="glue", region_name="us-east-1")  # type: ignore

        decoded_file_key = unquote(s3_event.s3.object.key)
        partition_matches = [
            m.group(1) for m in re.finditer(pattern=S3_KEY_MATCH_REGEX, string=decoded_file_key)
        ]
        if not partition_matches:
            raise ValueError(
                f"None of {','.join(PARTITIONS)} partitions found in path: {decoded_file_key}"
            )

        try:
            glue_client.get_partition(
                DatabaseName=DATABASE_NAME,
                TableName=TABLE_NAME,
                PartitionValues=partition_matches,
            )

            logger.info("Partitions for %s already exist, stopping...", ",".join(PARTITIONS))

            return
        except glue_client.exceptions.EntityNotFoundException:
            logger.info(
                "Partition values matching %s were not found, starting the %s crawler to "
                " add the new partitions to %s...",
                ",".join(PARTITIONS),
                CRAWLER_NAME,
                TABLE_NAME,
            )

            if try_run_crawler(glue_client=glue_client, name=CRAWLER_NAME):
                logger.info("%s started successfully", CRAWLER_NAME)
            else:
                logger.info("%s was not able to be started, stopping...", CRAWLER_NAME)

            return
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
