import os
import re
import time
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from typing import Callable, TypeVar
from urllib.parse import unquote

import boto3
from botocore import exceptions as boto3_exceptions
from botocore.config import Config

T = TypeVar("T")

PUT_METRIC_DATA_MAX_RETRIES = 10
"""The maxmium number of exponentially backed-off retries to attempt when trying to call the AWS
PutMetricData API"""
REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
METRICS_NAMESPACE = os.environ.get("METRICS_NAMESPACE")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

cw_client = boto3.client(service_name="cloudwatch", config=BOTO_CONFIG)

COMMON_UNRECOVERABLE_EXCEPTIONS = [
    cw_client.exceptions.InvalidParameterValueException,
    cw_client.exceptions.MissingRequiredParameterException,
    cw_client.exceptions.InvalidParameterCombinationException,
    boto3_exceptions.ParamValidationError,
]


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    AVAILABLE = "incoming"
    LOADED = "done"


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
    dimensions: dict[str, str] = field(default_factory=dict)


@dataclass
class MetricDataQuery:
    metric_namespace: str
    metric_name: str
    dimensions: dict[str, str] = field(default_factory=dict)


@dataclass
class MetricDataResult:
    label: str
    timestamps: list[datetime]
    values: list[float]


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
                any([type(exc) is ignored_exc for ignored_exc in ignored_exceptions])
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
                for d in m.dimensions
                for dim_name, dim_value in d.items()
            ],
        }
        for m in metrics
    ]

    cw_client.put_metric_data(
        Namespace=metric_namespace,
        MetricData=metrics_dict_list,
    )


def get_metric_data(
    metric_data_queries: list[MetricDataQuery],
    statistic: str,
    period: int = 60,
    start_time: datetime = datetime.utcnow() - timedelta(days=15),
    end_time: datetime = datetime.utcnow(),
) -> list[MetricDataResult]:
    data_queries_dict_list = [
        {
            "Id": "m1",
            "MetricStat": {
                "Metric": {
                    "Namespace": m.metric_namespace,
                    "MetricName": m.metric_name,
                    "Dimensions": [
                        {
                            "Name": dim_name,
                            "Value": dim_value,
                        }
                        for d in m.dimensions
                        for dim_name, dim_value in d.items()
                    ],
                },
                "Period": period,
                "Stat": statistic,
            },
            "Label": f"{m.metric_namespace}/{m.metric_name}",
            "ReturnData": True,
        }
        for m in metric_data_queries
    ]

    result = cw_client.get_metric_data(
        MetricDataQueries=data_queries_dict_list,
        StartTime=start_time,
        EndTime=end_time,
    )

    return [
        MetricDataResult(
            label=result["Label"], timestamps=result["Timestamps"], values=result["Values"]
        )
        for result in result["MetricDataResults"]
    ]


def handler(event, context):
    if not all([REGION, METRICS_NAMESPACE]):
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
        event_time_iso: str = record["eventTime"]
        event_timestamp = datetime.fromisoformat(event_time_iso.removesuffix("Z"))
    except KeyError as exc:
        print(f'Record did not contain any key with "eventTime": {exc}')
        return
    except ValueError as exc:
        print(f"Event timestamp was not in valid ISO format: {exc}")
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

        rif_type_dimension = {"data_type": rif_file_type.name.lower()}
        group_timestamp_dimension = {"group_timestamp": ccw_timestamp}

        count_metric_name = f"count/data-{pipeline_data_status.name.lower()}"

        # An inline function is defined here to pass to backoff_retry() as Python does not support
        # multiple line lambdas, so this is the next-best option
        def put_count_metrics():
            print(
                f'Putting data counts metrics "{METRICS_NAMESPACE}/{count_metric_name}" up to'
                f" CloudWatch with timestamp {datetime.isoformat(event_timestamp)}"
            )
            # Store four metrics:
            put_metric_data(
                metric_namespace=METRICS_NAMESPACE,
                metrics=[
                    # One undimensioned metric that can be used to get metrics aggregated across all
                    # data types and groups of data loads
                    MetricData(
                        metric_name=count_metric_name,
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                    # One dimensioned metric that aggregates across RIF file types
                    MetricData(
                        metric_name=count_metric_name,
                        dimensions=[rif_type_dimension],
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                    # One dimensioned metric that aggregates across the entire group of RIFs
                    MetricData(
                        metric_name=count_metric_name,
                        dimensions=[group_timestamp_dimension],
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                    # And one dimensioned metric that aggregates across both the file type and the
                    # file's "group" (timestamped parent directory)
                    MetricData(
                        metric_name=count_metric_name,
                        dimensions=[rif_type_dimension, group_timestamp_dimension],
                        timestamp=event_timestamp,
                        value=1,
                        unit="Count",
                    ),
                ],
            )
            print(f'Successfully put metrics to "{METRICS_NAMESPACE}/{count_metric_name}"')

        try:
            backoff_retry(
                func=put_count_metrics,
                ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS
            )
        except Exception as exc:
            print(
                "An unrecoverable error occurred when trying to call PutMetricData for metric"
                f" {METRICS_NAMESPACE}/{count_metric_name}: {exc}"
            )

        if pipeline_data_status == PipelineDataStatus.AVAILABLE:
            print(
                f"Incoming file does not indicate data has been loaded, no time delta can be"
                f" calculated. Stopping..."
            )
            return

        time_delta_metric_name = "time-delta/data-load-time"
        data_available_metric_name = f"count/data-{PipelineDataStatus.AVAILABLE.name.lower()}"

        def get_data_available_metric():
            return get_metric_data(
                metric_data_queries=[
                    MetricDataQuery(
                        metric_namespace=METRICS_NAMESPACE,
                        metric_name=data_available_metric_name,
                        dimensions=[rif_type_dimension, group_timestamp_dimension],
                    ),
                ],
                statistic="Maximum",
            )

        try:
            result = backoff_retry(
                func=get_data_available_metric,
                ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS + [KeyError],
            )
        except Exception as exc:
            print(f"An unrecoverable error occurred when trying to call GetMetricData; err: {exc}")
            return

        try:
            data_available_metric_data = [
                x for x in result if x.label == f"{METRICS_NAMESPACE}/{data_available_metric_name}"
            ][0]
        except IndexError as exc:
            print(
                "No metric data result was found for metric"
                f" {METRICS_NAMESPACE}/{data_available_metric_name}, no time delta can be computed."
                " Stopping..."
            )
            return

        # Get the latest timestamp
        try:
            last_available = sorted(data_available_metric_data.timestamps, reverse=True)[0]
        except IndexError as exc:
            print(
                "No timestamps were returned for metric"
                f" {METRICS_NAMESPACE}/{data_available_metric_name}, no time delta can be computed."
                " Stopping..."
            )
            return

        load_time_delta = event_timestamp.replace(tzinfo=last_available.tzinfo) - last_available

        def put_time_delta_metrics():
            put_metric_data(
                metric_namespace=METRICS_NAMESPACE,
                metrics=[
                    MetricData(
                        metric_name=time_delta_metric_name,
                        timestamp=
                    )
                ]
            )

    else:
        print(f"ETL file or path does not match expected format, skipping: {decoded_file_key}")
