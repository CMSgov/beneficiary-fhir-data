import os
import re
import time
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Callable, TypeVar
from urllib.parse import unquote

import boto3
from botocore.config import Config

T = TypeVar("T")

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


@dataclass
class MetricData:
    metric_name: str
    timestamp: datetime
    value: float
    unit: str
    dimensions: dict[str, str] = {}


def backoff_retry(
    func: Callable[[], T],
    retries: int = PUT_METRIC_DATA_MAX_RETRIES,
    ignored_exceptions: list[Exception] = [],
) -> T:
    """Generic function for wrapping another callable (function) that may raise errors and require
    some form of retry mechanism. Supports passing a list of exceptions/errors for the retry logic
    to ignore and instead raise to the calling function to handle

    Args:
        func (Callable[[], T]): The function to retry
        retries (int, optional): The number of times to retry before raising the error causing the failure.
        Defaults to PUT_METRIC_DATA_MAX_RETRIES.
        ignored_exceptions (list[Exception], optional): A list of exceptions to skip retrying and
        instead immediately raise to the calling function. Defaults to [].

    Raises:
        exc: Any exception in ignored_exceptions, or the exception thrown on the final retry

    Returns:
        T: The return type of func
    """
    for try_number in range(0, retries - 1):
        try:
            return func()
        except Exception as exc:
            # Raise the exception if it is any of the explicitly ignored exceptions or if this
            # was the last try
            if (
                any([exc is ignored_exc for ignored_exc in ignored_exceptions])
                or try_number == retries - 1
            ):
                raise exc

            # Exponentially back-off from hitting the API to ensure we don't hit the API limit.
            # See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            sleep_time = (2**try_number * 100.0) / 1000.0
            time.sleep(sleep_time)
            print(
                f"Unhandled error occurred, retrying in {sleep_time} seconds; attempt"
                f" #{try_number + 1} of {retries}, err: {exc}"
            )


def put_metric_data(metric_namespace: str, metrics: list[MetricData]):
    """Wraps the boto3 CloudWatch PutMetricData API operation to allow for usage of the MetricData
    dataclass

    Args:
        metric_namespace (str): The Namespace of the metric(s) to store in CloudWatch
        metrics (list[MetricData]): The metrics to store
    """

    # Convert from a list of the MetricData class to a list of dicts that boto3 understands for this
    # API. See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/cloudwatch.html#CloudWatch.Client.put_metric_data
    metrics_dict_list = [
        {
            "MetricName": m.metric_name,
            "Timestamp": m.timestamp,
            "Value": m.value,
            "Unit": m.unit,
            "Dimensions": [
                {
                    "Name": dim_name,
                    "Value": dim_value,
                }
                for dim_name, dim_value in m.dimensions.items()
            ],
        }
        for m in metrics
    ]

    cw_client.put_metric_data(
        Namespace=metric_namespace,
        MetricData=metrics_dict_list,
    )


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
        pipeline_data_status = PipelineDataStatus(match.group(1).lower())
        ccw_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3).lower())

        event_timestamp = datetime.now()  # TODO: Get this timestamp from S3 event

        metric_name = f"data-{pipeline_data_status.name.lower()}"
        rif_type_dimension = {"data_type": rif_file_type.name.lower()}
        group_timestamp_dimension = {"group_timestamp": ccw_timestamp}

        # An inline function is defined here to pass to backoff_retry() as Python does not support
        # multiple line lambdas, so this is the next-best option
        def put_metrics():
            # Store three metrics:
            put_metric_data(
                metric_namespace=METRICS_NAMESPACE,
                metrics=[
                    # One undimensioned metric that can be used to get metrics aggregated across all
                    # data types and groups of data loads
                    MetricData(
                        metric_name=metric_name,
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                    # One dimensioned metric that aggregates across RIF file types
                    MetricData(
                        metric_name=metric_name,
                        dimensions=[rif_type_dimension],
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                    # And one dimensioned metric that aggregates across both the file type and the
                    # file's "group" (timestamped parent directory)
                    MetricData(
                        metric_name=metric_name,
                        dimensions=[rif_type_dimension, group_timestamp_dimension],
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                ],
            )

        try:
            backoff_retry(
                func=put_metrics,
                ignored_exceptions=[
                    cw_client.exceptions.InvalidParameterValueException,
                    cw_client.exceptions.MissingRequiredParameterException,
                    cw_client.exceptions.InvalidParameterCombinationException,
                ],
            )
        except Exception as exc:
            print(
                "An unrecoverable error occurred when trying to call PutMetricData for metric"
                f" {METRICS_NAMESPACE}/{metric_name}: {exc}"
            )
    else:
        print(f"ETL file or path does not match expected format, skipping: {decoded_file_key}")
