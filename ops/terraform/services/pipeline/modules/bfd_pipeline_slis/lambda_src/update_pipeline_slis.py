import calendar
import os
import re
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Type
from urllib.parse import unquote

import boto3
from botocore import exceptions as botocore_exceptions
from botocore.config import Config

from backoff_retry import backoff_retry
from cw_metrics import (
    MetricDataQuery,
    gen_all_dimensioned_metrics,
    get_metric_data,
    put_metric_data,
)
from sqs import check_sentinel_queue

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
METRICS_NAMESPACE = os.environ.get("METRICS_NAMESPACE", "")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
SENTINEL_QUEUE_NAME = os.environ.get("QUEUE_NAME", "")
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
sqs_resource = boto3.resource("sqs", config=BOTO_CONFIG)
sentinel_queue = sqs_resource.get_queue_by_name(QueueName=SENTINEL_QUEUE_NAME)

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


@dataclass
class PipelineMetricMetadata:
    """Encapsulates metadata about a given pipeline metric"""

    metric_name: str
    """The name of the metric in CloudWatch Metrics, excluding namespace"""
    unit: str
    """The unit of the metric. Must conform to the list of supported CloudWatch Metrics"""


class PipelineMetrics(PipelineMetricMetadata, Enum):
    """Enumeration of pipeline metrics that can be stored in CloudWatch Metrics"""

    TIME_DATA_AVAILABLE = PipelineMetricMetadata("time/data-available", "Seconds")
    TIME_DATA_FIRST_AVAILABLE = PipelineMetricMetadata("time/data-first-available", "Seconds")
    TIME_DATA_LOADED = PipelineMetricMetadata("time/data-loaded", "Seconds")
    TIME_DATA_FULLY_LOADED = PipelineMetricMetadata("time/data-fully-loaded", "Seconds")
    TIME_DELTA_DATA_LOAD_TIME = PipelineMetricMetadata("time-delta/data-load-time", "Seconds")

    def __init__(self, data: PipelineMetricMetadata):
        for key in data.__annotations__.keys():
            value = getattr(data, key)
            setattr(self, key, value)

    def full_name(self) -> str:
        """Returns the fully qualified name of the metric, which includes the metric namespace and
        metric name

        Returns:
            str: The "full name" of the metric
        """
        return f"{METRICS_NAMESPACE}/{self.value}"


def handler(event: Any, context: Any):
    if not all([REGION, METRICS_NAMESPACE, ETL_BUCKET_ID, SENTINEL_QUEUE_NAME]):
        print("Not all necessary environment variables were defined, exiting...")
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
        event_time_iso: str = record["eventTime"]
        event_timestamp = datetime.fromisoformat(event_time_iso.removesuffix("Z"))
    except KeyError as exc:
        print(f'Record did not contain any key with "eventTime": {exc}')
        return
    except ValueError as exc:
        print(f"Event timestamp was not in valid ISO format: {exc}")
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
        ccw_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3).lower())

        rif_type_dimension = {"data_type": rif_file_type.name.lower()}
        group_timestamp_dimension = {"group_timestamp": ccw_timestamp}

        timestamp_metric = (
            PipelineMetrics.TIME_DATA_AVAILABLE
            if pipeline_data_status == PipelineDataStatus.INCOMING
            else PipelineMetrics.TIME_DATA_LOADED
        )

        utc_timestamp = calendar.timegm(event_timestamp.utctimetuple())

        try:
            print(
                f'Putting data timestamp metrics "{timestamp_metric.full_name()}" up'
                f" to CloudWatch with unix timestamp value {utc_timestamp}"
            )
            backoff_retry(
                # Store four metrics (gen_all_dimensioned_metrics() will generate all possible
                # dimensions of the given metric based upon the powerset of the dimensions):
                func=lambda: put_metric_data(
                    cw_client=cw_client,
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=timestamp_metric.metric_name,
                        timestamp=event_timestamp,
                        value=utc_timestamp,
                        unit=timestamp_metric.unit,
                        dimensions=[rif_type_dimension, group_timestamp_dimension],
                    ),
                ),
                ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
            )
            print(f'Successfully put metrics to "{timestamp_metric.full_name()}"')
        except Exception as exc:
            print(
                "An unrecoverable error occurred when trying to call PutMetricData for metric"
                f" {METRICS_NAMESPACE}/{timestamp_metric}: {exc}"
            )
            return

        if pipeline_data_status == PipelineDataStatus.INCOMING:
            print(
                "Incoming file indicates data has been made available to load to the ETL pipeline."
                f' Checking if this is the first time data is available for group "{ccw_timestamp}"'
            )

            try:
                print(
                    f"Checking if the {SENTINEL_QUEUE_NAME} queue contains any sentinel messages"
                    f" for the current group ({ccw_timestamp})..."
                )
                queue_is_empty = backoff_retry(
                    func=lambda: len(
                        [
                            msg
                            for msg in check_sentinel_queue(
                                sentinel_queue=sentinel_queue, timeout=10
                            )
                            if msg.group_timestamp == ccw_timestamp
                        ]
                    )
                    == 0,
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
                )
            except Exception as exc:
                print(
                    "An unrecoverable error occurred when trying to check the"
                    f" {SENTINEL_QUEUE_NAME} queue; err: {exc}"
                )
                return

            if queue_is_empty:
                print(
                    f"No sentinel message was received from queue {SENTINEL_QUEUE_NAME} for current"
                    f" group {ccw_timestamp}, this indicates that the incoming file is the start of"
                    f" a new data load for group {ccw_timestamp}. Putting data to metric"
                    f' "{PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()}" with value'
                    f" {utc_timestamp}"
                )

                try:
                    print(
                        "Putting time metric data to"
                        f' "{PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()}" with value'
                        f" {utc_timestamp}..."
                    )
                    backoff_retry(
                        func=lambda: put_metric_data(
                            cw_client=cw_client,
                            metric_namespace=METRICS_NAMESPACE,
                            metrics=gen_all_dimensioned_metrics(
                                metric_name=PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.metric_name,
                                dimensions=[group_timestamp_dimension],
                                timestamp=event_timestamp,
                                value=utc_timestamp,
                                unit=PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.unit,
                            ),
                        ),
                        ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
                    )
                    print("Data put successfully")
                except Exception as exc:
                    print(
                        "An unrecoverable error occurred when trying to call PutMetricData; err:"
                        f" {exc}"
                    )
            else:
                print(
                    f"Sentinel value was received from queue {SENTINEL_QUEUE_NAME} for current"
                    f" group {ccw_timestamp}. Incoming file is part of an ongoing, existing data"
                    f" load for group {ccw_timestamp}, and therefore does not indicate the time of"
                    " the first data load for this group. Stopping..."
                )
        elif pipeline_data_status == PipelineDataStatus.DONE:
            print(
                "Incoming file indicates data has been loaded. Calculating time deltas and checking"
                " if the incoming file was the last loaded file..."
            )

            print(
                "Putting time delta metrics for the time taken between the current"
                f" {rif_file_type.name} RIF file being made available and now (when it has been"
                " loaded)..."
            )

            try:
                print(
                    f'Getting corresponding "{PipelineMetrics.TIME_DATA_AVAILABLE.full_name()}"'
                    f' time metric for the current RIF file type "{rif_file_type.name}" in group'
                    f' "{ccw_timestamp}"...'
                )
                result = backoff_retry(
                    func=lambda: get_metric_data(
                        cw_client=cw_client,
                        metric_data_queries=[
                            MetricDataQuery(
                                metric_namespace=METRICS_NAMESPACE,
                                metric_name=PipelineMetrics.TIME_DATA_AVAILABLE.metric_name,
                                dimensions=rif_type_dimension | group_timestamp_dimension,
                            ),
                        ],
                        statistic="Maximum",
                    ),
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS + [KeyError],
                )
                print(
                    f'Metric "{PipelineMetrics.TIME_DATA_AVAILABLE.full_name()}" with dimensions'
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
                    if x.label == PipelineMetrics.TIME_DATA_AVAILABLE.full_name() and x.values
                ][0]
            except IndexError as exc:
                print(
                    "No metric data result was found for metric"
                    f" {PipelineMetrics.TIME_DATA_AVAILABLE.full_name()}, no time delta can be"
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
                    f" {PipelineMetrics.TIME_DATA_AVAILABLE.full_name()}, no time delta can be"
                    " computed. Stopping..."
                )
                return

            load_time_delta = event_timestamp.replace(tzinfo=last_available.tzinfo) - last_available

            try:
                print(
                    "Putting time delta metrics to"
                    f' "{PipelineMetrics.TIME_DELTA_DATA_LOAD_TIME.full_name()}" with value'
                    f" {load_time_delta.seconds}..."
                )
                backoff_retry(
                    func=lambda: put_metric_data(
                        cw_client=cw_client,
                        metric_namespace=METRICS_NAMESPACE,
                        metrics=gen_all_dimensioned_metrics(
                            metric_name=PipelineMetrics.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                            dimensions=[rif_type_dimension, group_timestamp_dimension],
                            value=load_time_delta.seconds,
                            timestamp=event_timestamp,
                            unit=PipelineMetrics.TIME_DELTA_DATA_LOAD_TIME.unit,
                        ),
                    ),
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
                )
                print(
                    f'Metrics put to "{PipelineMetrics.TIME_DELTA_DATA_LOAD_TIME.full_name()}"'
                    " successfully"
                )
            except Exception as exc:
                print(
                    "An unrecoverable error occurred when trying to call PutMetricData for metric"
                    f" {PipelineMetrics.TIME_DELTA_DATA_LOAD_TIME.full_name()}: {exc}"
                )
                return

            print("Checking if the incoming file is the last file to be loaded...")
            etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
            incoming_path_prefix = f"{PipelineDataStatus.INCOMING.capitalize()}/{ccw_timestamp}/"
            if list(etl_bucket.objects.filter(Prefix=incoming_path_prefix)):
                print(
                    f"Objects still exist in {incoming_path_prefix}. Incoming file is likely not"
                    " the last to be loaded, stopping..."
                )
                return

            print(
                f"No objects found in {incoming_path_prefix}, incoming file is likely last to"
                " be loaded. Putting data to"
                f' "{PipelineMetrics.TIME_DATA_FULLY_LOADED.full_name()}"...'
            )

            try:
                print(
                    "Putting time metric data to"
                    f' "{PipelineMetrics.TIME_DATA_FULLY_LOADED.full_name()}" with value'
                    f" {utc_timestamp}..."
                )
                backoff_retry(
                    func=lambda: put_metric_data(
                        cw_client=cw_client,
                        metric_namespace=METRICS_NAMESPACE,
                        metrics=gen_all_dimensioned_metrics(
                            metric_name=PipelineMetrics.TIME_DATA_FULLY_LOADED.metric_name,
                            dimensions=[group_timestamp_dimension],
                            timestamp=event_timestamp,
                            value=utc_timestamp,
                            unit=PipelineMetrics.TIME_DATA_FULLY_LOADED.unit,
                        ),
                    ),
                    ignored_exceptions=COMMON_UNRECOVERABLE_EXCEPTIONS,
                )
                print("Data put successfully")
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call PutMetricData; err: {exc}"
                )
    else:
        print(f"ETL file or path does not match expected format, skipping: {decoded_file_key}")
