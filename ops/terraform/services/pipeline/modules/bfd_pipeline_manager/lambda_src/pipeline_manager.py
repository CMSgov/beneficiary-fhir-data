import os
import re
from enum import Enum
from typing import Any
from urllib.parse import unquote

import boto3
from botocore.config import Config

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    INCOMING = "incoming"
    DONE = "done"


class RifFileType(str, Enum):
    """Represents all of the possible RIF file types that can be loaded by the BFD ETL Pipeline. The
    value of each enum is a specific substring that is used to match on each type of file"""

    BENEFICIARY_HISTORY = "beneficiary_history"
    BENEFICIARY = "bene"
    CARRIER = "carrier"
    DME = "dme"
    HHA = "hha"
    HOSPICE = "hospice"
    INPATIENT = "inpatient"
    OUTPATIENT = "outpatient"
    PDE = "pde"
    SNF = "snf"


def _is_pipeline_load_complete(bucket: Any, group_timestamp: str) -> bool:
    done_prefix = f"{PipelineDataStatus.DONE.capitalize()}/{group_timestamp}/"
    # Returns the file names of all text files within the "done" folder for the current bucket
    finished_rifs = [
        str(object.key).removeprefix(done_prefix)
        for object in bucket.objects.filter(Prefix=done_prefix)
        if str(object.key).endswith(".txt") or str(object.key).endswith(".csv")
    ]

    # We check for all RIFs _except_ beneficiary history as beneficiary history is a RIF type not
    # expected to exist in CCW-provided loads -- it only appears in synthetic loads
    rif_types_to_check = [e for e in RifFileType if e != RifFileType.BENEFICIARY_HISTORY]

    # Check, for each rif file type, if any finished rif has the corresponding rif type prefix.
    # Essentially, this ensures that all non-optional (excluding beneficiary history) RIF types have
    # been loaded and exist in the Done/ folder
    return all(
        any(rif_type.value in rif_file_name.lower() for rif_file_name in finished_rifs)
        for rif_type in rif_types_to_check
    )


def _is_incoming_folder_empty(bucket: Any, group_timestamp: str) -> bool:
    incoming_key_prefix = f"{PipelineDataStatus.INCOMING.capitalize()}/{group_timestamp}/"
    incoming_objects = list(bucket.objects.filter(Prefix=incoming_key_prefix))

    return len(incoming_objects) == 0


def handler(event: Any, context: Any):
    if not all([REGION, ETL_BUCKET_ID]):
        print("Not all necessary environment variables were defined, exiting...")
        return

    try:
        s3_resource = boto3.resource("s3", config=BOTO_CONFIG)
        etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
    except Exception as exc:
        print(
            "Unrecoverable exception occurred when attempting to create boto3 clients/resources:"
            f" {exc}"
        )
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError as exc:
        print(f"The incoming event was invalid: {exc}")
        return
    except IndexError:
        print("Invalid event notification, no records found")
        return

    try:
        file_key: str = record["s3"]["object"]["key"]
    except KeyError as exc:
        print(f"No bucket file found in event notification: {exc}")
        return

    decoded_file_key = unquote(file_key)
    status_group_str = "|".join([e.value for e in PipelineDataStatus])
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    # The incoming file's key should match an expected format, as follows:
    # "<Incoming/Done>/<ISO date format>/<file name>".
    if match := re.search(
        rf"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$",
        decoded_file_key,
        re.IGNORECASE,
    ):
        pipeline_data_status = PipelineDataStatus(match.group(1).lower())
        group_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3).lower())

        if pipeline_data_status == PipelineDataStatus.INCOMING:
            pass
        elif pipeline_data_status == PipelineDataStatus.DONE:
            pass
