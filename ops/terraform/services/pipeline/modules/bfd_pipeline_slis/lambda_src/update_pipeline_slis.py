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
from sqs import check_sentinel_queue, post_sentinel_message

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
    TIME_DELTA_FULL_DATA_LOAD_TIME = PipelineMetricMetadata(
        "time-delta/data-full-load-time", "Seconds"
    )

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
        metric_metadata: PipelineMetricMetadata = self.value
        return f"{METRICS_NAMESPACE}/{metric_metadata.metric_name}"


def _is_pipeline_load_complete(bucket: Any, group_timestamp: str) -> bool:
    done_prefix = f"{PipelineDataStatus.DONE.capitalize()}/{group_timestamp}/"
    # Returns the file names of all text files within the "done" folder for the current bucket
    finished_rifs = [
        str(object.key).removeprefix(done_prefix)
        for object in bucket.objects.filter(Prefix=done_prefix)
        if str(object.key).endswith(".txt")
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


def handler(event: Any, context: Any):
    if not all([REGION, METRICS_NAMESPACE, ETL_BUCKET_ID, SENTINEL_QUEUE_NAME]):
        print("Not all necessary environment variables were defined, exiting...")
        return

    try:
        cw_client = boto3.client(service_name="cloudwatch", config=BOTO_CONFIG)
        s3_resource = boto3.resource("s3", config=BOTO_CONFIG)
        sqs_resource = boto3.resource("sqs", config=BOTO_CONFIG)
        sentinel_queue = sqs_resource.get_queue_by_name(QueueName=SENTINEL_QUEUE_NAME)
        etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
    except Exception as exc:
        print(
            "Unrecoverable exception occurred when attempting to create boto3 clients/resources:"
            f" {exc}"
        )
        return

    common_unrecoverable_exceptions: list[Type[BaseException]] = [
        cw_client.exceptions.InvalidParameterValueException,
        cw_client.exceptions.MissingRequiredParameterException,
        cw_client.exceptions.InvalidParameterCombinationException,
        botocore_exceptions.ParamValidationError,
    ]

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
        group_timestamp = match.group(2)
        rif_file_type = RifFileType(match.group(3).lower())

        rif_type_dimension = {"data_type": rif_file_type.name.lower()}
        group_timestamp_dimension = {"group_timestamp": group_timestamp}

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
                ignored_exceptions=common_unrecoverable_exceptions,
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
                "RIF file location indicates data has been made available to load to the ETL"
                " pipeline. Checking if this is the first time data is available for group"
                f' "{group_timestamp}"...'
            )

            try:
                print(
                    f"Checking if the {SENTINEL_QUEUE_NAME} queue contains any sentinel messages"
                    f" for the current group ({group_timestamp})..."
                )
                queue_is_empty = backoff_retry(
                    func=lambda: len(
                        [
                            msg
                            for msg in check_sentinel_queue(
                                sentinel_queue=sentinel_queue, timeout=10
                            )
                            if msg.group_timestamp == group_timestamp
                        ]
                    )
                    == 0,
                    ignored_exceptions=common_unrecoverable_exceptions,
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
                    f" group {group_timestamp}, this indicates that the incoming file is the start"
                    f" of a new data load for group {group_timestamp}. Putting data to metric"
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
                        ignored_exceptions=common_unrecoverable_exceptions,
                    )
                    print(
                        "Metrics put to"
                        f" {PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()} successfully"
                    )
                except Exception as exc:
                    print(
                        "An unrecoverable error occurred when trying to call PutMetricData; err:"
                        f" {exc}"
                    )
                    return

                print(
                    f"Posting sentinel message to {SENTINEL_QUEUE_NAME} SQS queue to indicate that"
                    f" data load has begun for group {group_timestamp} and that no additional data"
                    " should be put to the"
                    f" {PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()} metric for this"
                    " group"
                )

                try:
                    backoff_retry(
                        func=sentinel_queue.purge,
                        ignored_exceptions=common_unrecoverable_exceptions,
                    )
                    backoff_retry(
                        func=lambda: post_sentinel_message(
                            sentinel_queue=sentinel_queue, group_timestamp=group_timestamp
                        ),
                        ignored_exceptions=common_unrecoverable_exceptions,
                    )

                    print(f"Sentinel message posted to {SENTINEL_QUEUE_NAME} successfully")
                except Exception as exc:
                    print(
                        "An unrecoverable error occurred when trying to post message to the"
                        f" {SENTINEL_QUEUE_NAME} SQS queue: {exc}"
                    )
            else:
                print(
                    f"Sentinel value was received from queue {SENTINEL_QUEUE_NAME} for current"
                    f" group {group_timestamp}. Incoming file is part of an ongoing, existing data"
                    f" load for group {group_timestamp}, and therefore does not indicate the time"
                    " of the first data load for this group. Stopping..."
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
                    f' time metric for the current RIF file type "{rif_file_type.name}" and'
                    f' "{PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()}" time metric in'
                    f' group "{group_timestamp}"...'
                )
                # We get both the last available metric for the current RIF file type _and_ the
                # current load's first available time metric to reduce the number of API calls. The
                # first available time metric is only used if the load has finished (the
                # notification that started this lambda was for the last-loaded file), otherwise
                # it's discarded
                result = backoff_retry(
                    func=lambda: get_metric_data(
                        cw_client=cw_client,
                        metric_data_queries=[
                            MetricDataQuery(
                                metric_namespace=METRICS_NAMESPACE,
                                metric_name=PipelineMetrics.TIME_DATA_AVAILABLE.metric_name,
                                dimensions=rif_type_dimension | group_timestamp_dimension,
                            ),
                            MetricDataQuery(
                                metric_namespace=METRICS_NAMESPACE,
                                metric_name=PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.metric_name,
                                dimensions=group_timestamp_dimension,
                            ),
                        ],
                        statistic="Maximum",
                    ),
                    ignored_exceptions=common_unrecoverable_exceptions + [KeyError],
                )
                print(
                    f'"{PipelineMetrics.TIME_DATA_AVAILABLE.full_name()}" with dimensions'
                    f" {rif_type_dimension | group_timestamp_dimension} and"
                    f' "{PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()}" with dimensions'
                    f" {group_timestamp_dimension} retrieved successfully"
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
                # As explained above, this metric's data will only be used if this lambda was
                # invoked for the last-loaded file (thus, the pipeline load has finished).
                # Otherwise, this metric data is discarded
                data_first_available_metric_data = [
                    x
                    for x in result
                    if x.label == PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name() and x.values
                ][0]
            except IndexError as exc:
                print(
                    "No metric data result was found for metric"
                    f' "{PipelineMetrics.TIME_DATA_AVAILABLE.full_name()}" or'
                    f' "{PipelineMetrics.TIME_DATA_FIRST_AVAILABLE.full_name()}", no time delta(s)'
                    " can be computed. Stopping..."
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
                    f" {load_time_delta.seconds} s..."
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
                    ignored_exceptions=common_unrecoverable_exceptions,
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

            print("Checking if the pipeline load has completed...")
            if not _is_pipeline_load_complete(bucket=etl_bucket, group_timestamp=group_timestamp):
                print(
                    f"Not all files have yet to be loaded for group {group_timestamp}. Data load is"
                    " not complete. Stopping..."
                )
                return

            try:
                print(
                    f"All files have been loaded for group {group_timestamp}. This indicates that"
                    " the data load has been completed for this group. Putting data to metric"
                    f' "{PipelineMetrics.TIME_DATA_FULLY_LOADED.full_name()}" with value'
                    f" {utc_timestamp}"
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
                    ignored_exceptions=common_unrecoverable_exceptions,
                )
                print(
                    f'Data put to "{PipelineMetrics.TIME_DATA_FULLY_LOADED.full_name()}"'
                    " successfully"
                )
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call PutMetricData; err: {exc}"
                )

            # There should only ever be one single data point for the first available metric for the
            # current group, so we don't need to sort or otherwise filter the list of values
            first_available_time = datetime.utcfromtimestamp(
                data_first_available_metric_data.values[0]
            )
            full_load_time_delta = (
                event_timestamp.replace(tzinfo=first_available_time.tzinfo) - first_available_time
            )

            try:
                print(
                    f'Putting to "{PipelineMetrics.TIME_DELTA_FULL_DATA_LOAD_TIME.full_name()}" the'
                    f" total time delta ({full_load_time_delta.seconds} s) from start to finish for"
                    f" the current pipeline load for group {group_timestamp}"
                )
                backoff_retry(
                    func=lambda: put_metric_data(
                        cw_client=cw_client,
                        metric_namespace=METRICS_NAMESPACE,
                        metrics=gen_all_dimensioned_metrics(
                            metric_name=PipelineMetrics.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                            dimensions=[group_timestamp_dimension],
                            timestamp=event_timestamp,
                            value=full_load_time_delta.seconds,
                            unit=PipelineMetrics.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                        ),
                    ),
                    ignored_exceptions=common_unrecoverable_exceptions,
                )
                print(
                    "Data put to metric"
                    f' "{PipelineMetrics.TIME_DELTA_FULL_DATA_LOAD_TIME.full_name()}" successfully'
                )
            except Exception as exc:
                print(
                    f"An unrecoverable error occurred when trying to call PutMetricData; err: {exc}"
                )
    else:
        print(f"ETL file or path does not match expected format, skipping: {decoded_file_key}")
