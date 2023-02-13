from enum import Enum
import os
import re
import time
from urllib.parse import unquote

import boto3
from botocore.config import Config

PUT_METRIC_DATA_MAX_RETRIES = 10
"""The maxmium number of exponentially backed-off retries to attempt when trying to call the AWS
PutMetricData API"""
REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
METRICS_NAMESPACE = os.environ.get("METRICS_NAMESPACE")

boto_config = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
cw_client = boto3.client(service_name="cloudwatch", config=boto_config)


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    AVAILABLE = "Incoming"
    LOADED = "Done"


class RifFileType(str, Enum):
    """Represents all of the possible RIF file types that can be loaded by the BFD ETL Pipeline. The
    value of each enum is a specific substring that is used to match on each type of file"""

    BENEFICIARY = "bene"
    CARRIER = "carrier"
    DME = "dme"
    HHA = "hha"
    HOSPICE = "hospice"
    INPATIENT = "inpatient"
    OUTPATIENT = "outpatient"
    PDE = "pde"
    SNF = "snf"


def handler(event, context):
    if not all([REGION]):
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
    status_group_str = "|".join([e.value for e in PipelineDataStatus])
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    if match := re.search(
        f"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$",
        decoded_file_key,
        re.IGNORECASE,
    ):
        pipeline_data_status = PipelineDataStatus(match.group(1))
        ccw_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3))

        for try_number in range(0, PUT_METRIC_DATA_MAX_RETRIES - 1):
            # Exponentially back-off from hitting the API to ensure we don't hit the API limit. The
            # first iteration will not sleep, but subsequent iterations will sleep at progressively
            # longer intervals. See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            if try_number != 0:
                time.sleep((2**try_number * 100.0) / 1000.0)
    else:
        print(f"ETL file or path does not match expected format, skipping: {decoded_file_key}")
