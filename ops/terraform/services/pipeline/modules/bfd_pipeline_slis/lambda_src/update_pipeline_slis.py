import calendar
import operator
import os
import re
import time
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from functools import reduce
from itertools import chain, combinations
from typing import Callable, Optional, Type, TypeVar
from urllib.parse import unquote

import boto3
from botocore import exceptions as botocore_exceptions
from botocore.config import Config

T = TypeVar("T")

PUT_METRIC_DATA_MAX_RETRIES = 10
"""The maxmium number of exponentially backed-off retries to attempt when trying to call the AWS
PutMetricData API"""
REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
METRICS_NAMESPACE = os.environ.get("METRICS_NAMESPACE")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

cw_client = boto3.client(service_name="cloudwatch", config=BOTO_CONFIG)
s3_resource = boto3.resource("s3", config=BOTO_CONFIG)

COMMON_UNRECOVERABLE_EXCEPTIONS: list[Type[BaseException]] = [
    cw_client.exceptions.InvalidParameterValueException,
    cw_client.exceptions.MissingRequiredParameterException,
    cw_client.exceptions.InvalidParameterCombinationException,
    botocore_exceptions.ParamValidationError,
]
"""Exceptions common to CloudWatch Metrics operations that cannot be retried upon, and so should be
immediately raised to the calling function (in this case, the handler)"""


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    AVAILABLE = "incoming"
    LOADED = "done"


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


@dataclass
class MetricData:
    """Dataclass representing the data needed to "put" a metric up to CloudWatch Metrics. Represents
    both the metric itself (name, dimensions, unit) and the value that is put to said metric
    (timestamp, value)"""

    metric_name: str
    timestamp: datetime
    value: float
    unit: str
    dimensions: dict[str, str] = field(default_factory=dict)


@dataclass
class MetricDataQuery:
    """Dataclass representing the data needed to get a metric from CloudWatch Metrics. Metrics are
    identified by their namespace, name, and dimensions"""

    metric_namespace: str
    metric_name: str
    dimensions: dict[str, str] = field(default_factory=dict)


@dataclass
class MetricDataResult:
    """Dataclass representing the result of a successful GetMetricData operation"""

    label: str
    timestamps: list[datetime]
    values: list[float]


def powerset(items: list[T]) -> chain[T]:
    """This function computes the powerset (the set of all subsets including the set itself and the
    null set) of the incoming list. Used to automatically generate all possible
    dimensioned metrics for a given metric. Implementation adapted from Python's official
    itertools-recipes documentation

    Example:
        powerset([1,2,3]) --> () (1,) (2,) (3,) (1,2) (1,3) (2,3) (1,2,3)

    Args:
        items (list[T]): A list of items to compute the powerset from

    Returns:
        chain[T]: A generator that will yield subsets starting with the null set upto the set
    """
    return chain.from_iterable(combinations(items, r) for r in range(len(items) + 1))


def gen_all_dimensioned_metrics(
    metric_name: str, timestamp: datetime, value: float, unit: str, dimensions: list[dict[str, str]]
) -> list[MetricData]:
    """Generates all of the possible dimensioned (and single undimensioned) metrics from the
    powerset of the list of dimensions passed-in. Useful as all metrics created by this Lambda
    have the same value, timestamp, and name and only differ on their aggregations

    Args:
        metric_name (str): Name of the metric
        timestamp (datetime): Timestamp to store with the metrics
        value (float): Value to store with the metrics in each dimension
        unit (str): The Unit of the metric
        dimensions (list[dict[str, str]]): The list of dimensions to compute the powerset; this
        determines the number of metrics that will be stored (2**dimensions.count)

    Returns:
        list[MetricData]: A list of metrics with each being a set in the powerset of dimensions
    """

    return [
        MetricData(
            metric_name=metric_name,
            timestamp=timestamp,
            value=value,
            # Merge the chain/generator of dimensions of arbitrary size using the "|" operator
            dimensions=reduce(operator.ior, x, {}),
            unit=unit,
        )
        for x in powerset(dimensions)
    ]


def backoff_retry(
    func: Callable[[], T],
    retries: int = PUT_METRIC_DATA_MAX_RETRIES,
    ignored_exceptions: Optional[list[Type[BaseException]]] = None,
) -> Optional[T]:
    """Generic function for wrapping another callable (function) that may raise errors and require
    some form of retry mechanism. Supports passing a list of exceptions/errors for the retry logic
    to ignore and instead raise to the calling function to handle

    Args:
        func (Callable[[], T]): The function to retry
        retries (int, optional): The number of times to retry before raising the error causing the
        failure. Defaults to PUT_METRIC_DATA_MAX_RETRIES.
        ignored_exceptions (list[Type[BaseException]] , optional): A list of exceptions to skip
        iretrying and nstead immediately raise to the calling function. Defaults to [].

    Raises:
        exc: Any exception in ignored_exceptions, or the exception thrown on the final retry

    Returns:
        T: The return type of func
    """
    if ignored_exceptions is None:
        ignored_exceptions = []

    for try_number in range(1, retries):
        try:
            return func()
        # Pylint will complain this is too broad, and it is, but unfortunately it appears that
        # boto3 client exceptions extend directly from BaseException rather than the more correct
        # Exception type. Rather than deal with the dynamic type headache from boto3's exceptions,
        # we just catch BaseException and re-raise certain BaseExceptions that should never be
        # caught
        except BaseException as exc:  # pylint: disable=W0718
            # Raise the exception if it is any of the explicitly ignored exceptions or if this
            # was the last try or if the exception is one of a few special base exceptions
            if (
                any(isinstance(exc, ignored_exc) for ignored_exc in ignored_exceptions)
                or try_number == retries
                or isinstance(exc, (KeyboardInterrupt, SystemExit))
            ):
                raise exc

            # Exponentially back-off from hitting the API to ensure we don't hit the API limit.
            # See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            sleep_time = (2**try_number * 100.0) / 1000.0
            print(
                f"Unhandled error occurred, retrying in {sleep_time} seconds; attempt"
                f" #{try_number} of {retries}, err: {exc}"
            )
            time.sleep(sleep_time)

    return None


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


def get_metric_data(
    metric_data_queries: list[MetricDataQuery],
    statistic: str,
    period: int = 60,
    start_time: datetime = datetime.utcnow() - timedelta(days=15),
    end_time: datetime = datetime.utcnow(),
) -> list[MetricDataResult]:
    """Wraps the GetMetricData CloudWatch Metrics API operation to allow for easier usage. By
    default, standard resolution metrics from the current time to 15 days in the past are retrieved
    from CloudWatch Metrics.

    Args:
        metric_data_queries (list[MetricDataQuery]): A list of data queries to return metric data
        for
        statistic (str): The statistic for the queried metric(s) to return
        period (int, optional): The period of the metric, correlates to its storage resolution.
        Defaults to 60.
        start_time (datetime, optional): The start of the time period to search. Defaults to
        datetime.utcnow()-timedelta(days=15).
        end_time (datetime, optional): The end of the time period to search. Defaults to
        datetime.utcnow().

    Returns:
        list[MetricDataResult]: A list of results for each data query with each label matching the
        namespace and metric name of its corresponding metric

    Raises:
        KeyError: Raised if the inner GetMetricData query fails for an unknown reason that is
        unhandled or its return value does not conform to its expected definition
    """

    # Transform the list of MetricDataQuery into a list of dicts that the boto3 GetMetricData
    # function understands
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
                        for dim_name, dim_value in m.dimensions.items()
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
    if not all([REGION, METRICS_NAMESPACE, ETL_BUCKET_ID]):
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
    # The incoming file's key should match an expected format, as follows:
    # "<Incoming/Done>/<ISO date format>/<file name>".
    if match := re.search(
        rf"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$",
        decoded_file_key,
        re.IGNORECASE,
    ):
        pipeline_data_status = PipelineDataStatus(match.group(1).lower())
        ccw_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3).lower())

        rif_type_dimension = {"data_type": rif_file_type.name.lower()}
        group_timestamp_dimension = {"group_timestamp": ccw_timestamp}

        timestamp_metric_name = f"time/data-{pipeline_data_status.name.lower()}"

        utc_timestamp = calendar.timegm(event_timestamp.utctimetuple())

        # An inline function is defined here to pass to backoff_retry() as Python does not support
        # multiple line lambdas, so this is the next-best option
        def put_timestamp_metrics():
            # Store four metrics (gen_all_dimensioned_metrics() will generate all possible
            # dimensions of the given metric based upon the powerset of the dimensions):
            put_metric_data(
                metric_namespace=METRICS_NAMESPACE,
                metrics=gen_all_dimensioned_metrics(
                    metric_name=timestamp_metric_name,
                    timestamp=event_timestamp,
                    value=utc_timestamp,
                    unit="Seconds",
                    dimensions=[rif_type_dimension, group_timestamp_dimension],
                ),
            )

        try:
            print(
                f'Putting data timestamp metrics "{METRICS_NAMESPACE}/{timestamp_metric_name}" up'
                f" to CloudWatch with unix timestamp value {utc_timestamp}"
            )
            backoff_retry(
                func=put_timestamp_metrics, ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS
            )
            print(f'Successfully put metrics to "{METRICS_NAMESPACE}/{timestamp_metric_name}"')
        except Exception as exc:
            print(
                "An unrecoverable error occurred when trying to call PutMetricData for metric"
                f" {METRICS_NAMESPACE}/{timestamp_metric_name}: {exc}"
            )
            return

        if pipeline_data_status == PipelineDataStatus.AVAILABLE:
            print(
                "Incoming file indicates data has been made available to load to the ETL pipeline."
                f' Checking if this is the first time data is available for group "{ccw_timestamp}"'
            )

            data_first_available_name = f"time/data-first-{pipeline_data_status.name.lower()}"

            def get_data_first_available_for_group():
                return get_metric_data(
                    metric_data_queries=[
                        MetricDataQuery(
                            metric_namespace=METRICS_NAMESPACE,
                            metric_name=data_first_available_name,
                            dimensions=group_timestamp_dimension,
                        )
                    ],
                    statistic="Maximum",
                )

            try:
                print(
                    f'Retrieving metric data from "{METRICS_NAMESPACE}/{data_first_available_name}"'
                    f" with dimensions {group_timestamp_dimension}..."
                )
                result = backoff_retry(
                    func=get_data_first_available_for_group,
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS + [KeyError],
                )
                print(
                    f'Metric "{METRICS_NAMESPACE}/{data_first_available_name}" with dimensions'
                    f" {group_timestamp_dimension} retrieved successfully"
                )
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call GetMetricData; err: {exc}"
                )
                return

            if [
                x
                for x in result
                if x.label == f"{METRICS_NAMESPACE}/{data_first_available_name}" and x.values
            ]:
                print(
                    f'Metric data exists for "{METRICS_NAMESPACE}/{data_first_available_name}" with'
                    f" dimensions {group_timestamp_dimension}. Incoming file is part of an ongoing,"
                    " existing data load, and therefore does not indicate the time of the first"
                    " data load for its group. Stopping..."
                )
                return

            print(
                "No metric data result was found for metric"
                f" {METRICS_NAMESPACE}/{data_first_available_name}, this indicates that the"
                " incoming file is the start of a new data load. Putting data to metric"
                f' "{METRICS_NAMESPACE}/{data_first_available_name}" with value {utc_timestamp}'
            )

            def put_data_first_available_metrics():
                return put_metric_data(
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=data_first_available_name,
                        dimensions=[group_timestamp_dimension],
                        timestamp=event_timestamp,
                        value=utc_timestamp,
                        unit="Seconds",
                    ),
                )

            try:
                print(
                    f'Putting time metric data to "{METRICS_NAMESPACE}/{data_first_available_name}"'
                    f" with value {utc_timestamp}..."
                )
                backoff_retry(
                    func=put_data_first_available_metrics,
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
                )
                print("Data put successfully")
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call PutMetricData; err: {exc}"
                )
        elif pipeline_data_status == PipelineDataStatus.LOADED:
            print(
                "Incoming file indicates data has been loaded. Calculating time deltas and checking"
                " if the incoming file was the last loaded file..."
            )

            print(
                "Putting time delta metrics for the time taken between the current"
                f" {rif_file_type.name} RIF file being made available and now (when it has been"
                " loaded)..."
            )
            time_delta_metric_name = "time-delta/data-load-time"
            data_available_metric_name = f"time/data-{PipelineDataStatus.AVAILABLE.name.lower()}"

            def get_data_available_metric():
                return get_metric_data(
                    metric_data_queries=[
                        MetricDataQuery(
                            metric_namespace=METRICS_NAMESPACE,
                            metric_name=data_available_metric_name,
                            dimensions=rif_type_dimension | group_timestamp_dimension,
                        ),
                    ],
                    statistic="Maximum",
                )

            try:
                print(
                    f'Getting corresponding "{METRICS_NAMESPACE}/{data_available_metric_name}" time'
                    f' metric for the current RIF file type "{rif_file_type.name}" in group'
                    f' "{ccw_timestamp}"...'
                )
                result = backoff_retry(
                    func=get_data_available_metric,
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS + [KeyError],
                )
                print(
                    f'Metric "{METRICS_NAMESPACE}/{data_available_metric_name}" with dimensions'
                    f" {rif_type_dimension | group_timestamp_dimension} retrieved successfully"
                )
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call GetMetricData; err: {exc}"
                )
                return

            try:
                data_available_metric_data = [
                    x
                    for x in result
                    if x.label == f"{METRICS_NAMESPACE}/{data_available_metric_name}" and x.values
                ][0]
            except IndexError as exc:
                print(
                    "No metric data result was found for metric"
                    f" {METRICS_NAMESPACE}/{data_available_metric_name}, no time delta can be"
                    " computed. Stopping..."
                )
                return

            # Get the the unix time (in UTC) of the most recent point in time when the now-loaded
            # file that invoked this Lambda was made available in order to calculate the time it
            # took to load said file in the ETL pipeline. We take the value (unix timestamp) instead
            # of the point's timestamp as it will be a higher resolution and more accurate since
            # CloudWatch truncates and reduces the precision of data timestamps over time
            try:
                latest_value_index = data_available_metric_data.timestamps.index(
                    max(data_available_metric_data.timestamps)
                )
                last_available = datetime.utcfromtimestamp(
                    data_available_metric_data.values[latest_value_index]
                )
            except ValueError as exc:
                print(
                    "No values were returned for metric"
                    f" {METRICS_NAMESPACE}/{data_available_metric_name}, no time delta can be"
                    " computed. Stopping..."
                )
                return

            load_time_delta = event_timestamp.replace(tzinfo=last_available.tzinfo) - last_available

            def put_time_delta_metrics():
                put_metric_data(
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=time_delta_metric_name,
                        dimensions=[rif_type_dimension, group_timestamp_dimension],
                        value=load_time_delta.seconds,
                        timestamp=event_timestamp,
                        unit="Seconds",
                    ),
                )

            try:
                print(
                    f'Putting time delta metrics to "{METRICS_NAMESPACE}/{time_delta_metric_name}"'
                    f" with value {load_time_delta.seconds}..."
                )
                backoff_retry(
                    func=put_time_delta_metrics, ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS
                )
                print(f'Metrics put to "{METRICS_NAMESPACE}/{time_delta_metric_name}" successfully')
            except Exception as exc:
                print(
                    "An unrecoverable error occurred when trying to call PutMetricData for metric"
                    f" {METRICS_NAMESPACE}/{timestamp_metric_name}: {exc}"
                )
                return

            print("Checking if the incoming file is the last file to be loaded...")
            etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
            incoming_path_prefix = f"{PipelineDataStatus.AVAILABLE.capitalize()}/{ccw_timestamp}/"
            if list(etl_bucket.objects.filter(Prefix=incoming_path_prefix)):
                print(
                    f"Objects still exist in {incoming_path_prefix}. Incoming file is likely not"
                    " the last to be loaded, stopping..."
                )
                return

            data_finished_load_metric_name = f"time/data-fully-{pipeline_data_status.name.lower()}"
            print(
                f"No objects found in {incoming_path_prefix}, incoming file is likely last to"
                " be loaded. Putting data to"
                f' "{METRICS_NAMESPACE}/{data_finished_load_metric_name}"...'
            )

            def put_data_fully_loaded():
                return put_metric_data(
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=data_finished_load_metric_name,
                        dimensions=[group_timestamp_dimension],
                        timestamp=event_timestamp,
                        value=utc_timestamp,
                        unit="Seconds",
                    ),
                )

            try:
                print(
                    "Putting time metric data to"
                    f' "{METRICS_NAMESPACE}/{data_finished_load_metric_name}" with value'
                    f" {utc_timestamp}..."
                )
                backoff_retry(
                    func=put_data_fully_loaded,
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
                )
                print("Data put successfully")
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call PutMetricData; err: {exc}"
                )
    else:
        print(f"ETL file or path does not match expected format, skipping: {decoded_file_key}")
