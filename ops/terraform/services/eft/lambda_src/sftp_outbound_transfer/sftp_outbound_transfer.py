import json
import logging
import os
import sys
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any
from urllib.parse import unquote

import boto3
from botocore.config import Config


class S3EventType(str, Enum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"


REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
BFD_EFT_BUCKET = os.environ.get("BFD_EFT_BUCKET", "")
BFD_EFT_SFTP_HOME_DIR = os.environ.get("BFD_EFT_SFTP_HOME_DIR", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()
try:
    s3_resource = boto3.resource("s3", config=BOTO_CONFIG)  # type: ignore
    etl_bucket = s3_resource.Bucket(BFD_EFT_BUCKET)  # type: ignore
except Exception:
    logger.error(
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources: ",
        exc_info=True,
    )
    sys.exit(0)


def handler(event: Any, context: Any):
    if not all([REGION, BFD_ENVIRONMENT, BFD_EFT_BUCKET, BFD_EFT_SFTP_HOME_DIR]):
        logger.error("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError:
        logger.error("The incoming event was invalid: ", exc_info=True)
        return
    except IndexError:
        logger.error("Invalid event notification, no records found: ", exc_info=True)
        return

    try:
        sns_message_json: str = record["Sns"]["Message"]
        sns_message = json.loads(sns_message_json)
    except KeyError:
        logger.error("No message found in SNS notification: ", exc_info=True)
        return
    except json.JSONDecodeError:
        logger.error("SNS message body was not valid JSON: ", exc_info=True)
        return

    try:
        s3_event = sns_message["Records"][0]
    except KeyError:
        logger.error("Invalid S3 event, no records found: ", exc_info=True)
        return
    except IndexError:
        logger.error("Invalid event notification, no records found: ", exc_info=True)
        return

    try:
        event_type_str = s3_event["eventName"]
        event_type: S3EventType
        if S3EventType.OBJECT_CREATED in event_type_str:
            event_type = S3EventType.OBJECT_CREATED
        else:
            logger.error("Event type %s is unsupported. Exiting...", event_type_str)
            return
    except KeyError:
        logger.error(
            "The incoming event record did not contain the type of S3 event: ", exc_info=True
        )
        return

    try:
        file_key: str = s3_event["s3"]["object"]["key"]
        decoded_file_key = unquote(file_key)
    except KeyError:
        logger.error("No bucket file found in event notification: ", exc_info=True)
        return

    # Log the various bits of data extracted from the invoking event to aid debugging:
    logger.info("Invoked at: %s UTC", datetime.utcnow().isoformat())
    logger.info("S3 Object Key: %s", decoded_file_key)
    logger.info("S3 Event Type: %s, Specific Event Name: %s", event_type.name, event_type_str)
