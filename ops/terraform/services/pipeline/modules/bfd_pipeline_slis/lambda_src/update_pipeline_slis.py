import calendar
import json
import logging
import os
import re
import sys
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Type
from urllib.parse import unquote

import boto3
from botocore import exceptions as botocore_exceptions
from botocore.config import Config

from backoff_retry import backoff_retry
from common import METRICS_NAMESPACE, PipelineMetric, RifFileType
from cw_metrics import MetricData, gen_all_dimensioned_metrics, put_metric_data
from sqs import (
    MessageFailedToDeleteException,
    PipelineLoadEvent,
    PipelineLoadEventType,
    delete_load_msg_from_queue,
    post_load_event,
    retrieve_load_event_msgs,
)

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
EVENTS_QUEUE_NAME = os.environ.get("SENTINEL_QUEUE_NAME", "")
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
    cw_client = boto3.client(service_name="cloudwatch", config=BOTO_CONFIG)  # type: ignore
    s3_resource = boto3.resource("s3", config=BOTO_CONFIG)  # type: ignore
    sqs_resource = boto3.resource("sqs", config=BOTO_CONFIG)  # type: ignore
    events_queue = sqs_resource.get_queue_by_name(QueueName=EVENTS_QUEUE_NAME)
    etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
except Exception:
    logger.error(
        "Unrecoverable exception occurred when attempting to create boto3 clients/resources: ",
        exc_info=True,
    )
    sys.exit(0)
common_unrecoverable_exceptions: list[Type[BaseException]] = [
    cw_client.exceptions.InvalidParameterValueException,
    cw_client.exceptions.MissingRequiredParameterException,
    cw_client.exceptions.InvalidParameterCombinationException,
    botocore_exceptions.ParamValidationError,
]


class S3EventType(str, Enum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    INCOMING = "incoming"
    DONE = "done"


def _is_pipeline_load_complete(bucket: Any, group: str) -> bool:
    done_prefix = f"{PipelineDataStatus.DONE.capitalize()}/{group}/"
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


def _is_incoming_folder_empty(bucket: Any, group: str) -> bool:
    incoming_key_prefix = f"{PipelineDataStatus.INCOMING.capitalize()}/{group}/"
    incoming_objects = list(bucket.objects.filter(Prefix=incoming_key_prefix))

    return len(incoming_objects) == 0


def handler(event: Any, context: Any):
    if not all([REGION, METRICS_NAMESPACE, ETL_BUCKET_ID, EVENTS_QUEUE_NAME]):
        logger.error("Not all necessary environment variables were defined, exiting...")
        return

    try:
        record: dict[str, Any] = event["Records"][0]
    except KeyError:
        logger.error("The incoming event was invalid", exc_info=True)
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
        event_time_iso: str = s3_event["eventTime"]
        event_datetime = datetime.fromisoformat(event_time_iso.removesuffix("Z")).astimezone(
            tz=timezone.utc
        )
    except KeyError:
        logger.error('Record did not contain any key with "eventTime": ', exc_info=True)
        return
    except ValueError:
        logger.error("Event timestamp was not in valid ISO format: ", exc_info=True)
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

    status_group_str = "|".join([e.value for e in PipelineDataStatus])
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    # The incoming file's key should match an expected format, as follows:
    # "<Incoming/Done>/<ISO date format>/<file name>".
    if not (
        match := re.search(
            rf"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$",
            decoded_file_key,
            re.IGNORECASE,
        )
    ):
        logger.error(
            "ETL file or path does not match expected format, skipping: %s", decoded_file_key
        )
        return

    pipeline_data_status = PipelineDataStatus(match.group(1).lower())
    group_iso_str = match.group(2)
    rif_file_type = RifFileType(match.group(3).lower())

    # Log data extracted from S3 object key now that we know this is a valid RIF file within a
    # valid data load
    logger.info("RIF type: %s", rif_file_type.name)
    logger.info("Load Status: %s", pipeline_data_status.name)
    logger.info("Data Load: %s", group_iso_str)

    rif_type_dimension = {"data_type": rif_file_type.name.lower()}
    group_timestamp_dimension = {"group_timestamp": group_iso_str}

    timestamp_metric = (
        PipelineMetric.TIME_DATA_AVAILABLE
        if pipeline_data_status == PipelineDataStatus.INCOMING
        else PipelineMetric.TIME_DATA_LOADED
    )

    utc_timestamp = calendar.timegm(event_datetime.utctimetuple())

    logger.info(
        'Putting data timestamp metrics "%s" up to CloudWatch with unix timestamp value %s',
        timestamp_metric.full_name(),
        utc_timestamp,
    )
    try:
        backoff_retry(
            # Store four metrics (gen_all_dimensioned_metrics() will generate all possible
            # dimensions of the given metric based upon the powerset of the dimensions):
            func=lambda: put_metric_data(
                cw_client=cw_client,
                metric_namespace=METRICS_NAMESPACE,
                metrics=gen_all_dimensioned_metrics(
                    metric_name=timestamp_metric.metric_name,
                    datetime=event_datetime,
                    value=utc_timestamp,
                    unit=timestamp_metric.unit,
                    dimensions=[rif_type_dimension, group_timestamp_dimension],
                ),
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
    except Exception:
        logger.error(
            "An unrecoverable error occurred when trying to call PutMetricData for metric %s: ",
            timestamp_metric.full_name(),
            exc_info=True,
        )
        return
    logger.info('Successfully put metrics to "%s"', timestamp_metric.full_name())

    if pipeline_data_status == PipelineDataStatus.INCOMING:
        logger.info(
            "RIF file location indicates data has been made available to load to the ETL"
            " pipeline. Posting %s event to %s for %s RIF in group %s...",
            PipelineLoadEventType.RIF_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
            rif_file_type,
            group_iso_str,
        )
        try:
            backoff_retry(
                func=lambda: post_load_event(
                    queue=events_queue,
                    message=PipelineLoadEvent(
                        event_type=PipelineLoadEventType.RIF_AVAILABLE,
                        datetime=event_datetime,
                        group_iso_str=group_iso_str,
                        rif_type=rif_file_type,
                    ),
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to post an event to the %s queue;"
                " err: ",
                EVENTS_QUEUE_NAME,
                exc_info=True,
            )
            return
        logger.info(
            "%s event posted to %s for %s RIF in group %s successfully",
            PipelineLoadEventType.RIF_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
            rif_file_type,
            group_iso_str,
        )

        logger.info(
            'Checking if this is the first time data load was discovered for group "%s"...',
            group_iso_str,
        )

        logger.info(
            "Retrieving the %s event for the current group/load, %s, from the %s queue... ",
            PipelineLoadEventType.LOAD_AVAILABLE.value,
            group_iso_str,
            EVENTS_QUEUE_NAME,
        )
        try:
            group_available_msg = backoff_retry(
                func=lambda: next(
                    (
                        message
                        for message in retrieve_load_event_msgs(
                            queue=events_queue,
                            timeout=10,
                            type_filter=[PipelineLoadEventType.LOAD_AVAILABLE],
                        )
                        if message.event.group_iso_str == group_iso_str
                    ),
                    None,
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to check the %s queue; err: ",
                EVENTS_QUEUE_NAME,
                exc_info=True,
            )
            return

        if group_available_msg:
            logger.info(
                "%s event was received from queue %s for current group %s. Incoming file is"
                " part of an ongoing, existing data load for group %s, and therefore does not"
                " indicate the time of the first data load for this group. Stopping...",
                PipelineLoadEventType.LOAD_AVAILABLE.value,
                EVENTS_QUEUE_NAME,
                group_iso_str,
                group_iso_str,
            )
            return

        logger.info(
            "No %s event was retrieved from queue %s for current group %s, this"
            " indicates that the incoming file is the start of a new data load for group %s."
            ' Putting data to metric "%s" and corresponding metric "%s" with value %s',
            PipelineLoadEventType.LOAD_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
            group_iso_str,
            group_iso_str,
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE.full_name(),
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.full_name(),
            utc_timestamp,
        )

        logger.info(
            'Putting time metric data to "%s" and corresponding "%s" with value %s...',
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE.full_name(),
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.full_name(),
            utc_timestamp,
        )
        try:
            backoff_retry(
                func=lambda: put_metric_data(
                    cw_client=cw_client,
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.metric_name,
                        dimensions=[group_timestamp_dimension],
                        datetime=event_datetime,
                        value=utc_timestamp,
                        unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.unit,
                    )
                    + [
                        MetricData(
                            metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.metric_name,
                            value=utc_timestamp,
                            unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.unit,
                            datetime=event_datetime,
                        )
                    ],
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to call PutMetricData; err: ",
                exc_info=True,
            )
            return
        logger.info(
            "Metrics put to %s and %s successfully",
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE.full_name(),
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.full_name(),
        )

        logger.info(
            "Posting %s event to %s SQS queue to indicate that data load has begun for"
            " group %s and that no additional data should be put to the %s metric for this"
            " group",
            PipelineLoadEventType.LOAD_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
            group_iso_str,
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE.full_name(),
        )
        try:
            backoff_retry(
                func=lambda: post_load_event(
                    queue=events_queue,
                    message=PipelineLoadEvent(
                        event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                        datetime=event_datetime,
                        group_iso_str=group_iso_str,
                        rif_type=rif_file_type,
                    ),
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to post message to the %s SQS queue: ",
                EVENTS_QUEUE_NAME,
                exc_info=True,
            )
        logger.info(
            "%s event posted to %s successfully",
            PipelineLoadEventType.LOAD_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
        )
    elif pipeline_data_status == PipelineDataStatus.DONE:
        logger.info(
            "Incoming file indicates data has been loaded. Calculating time deltas and checking"
            " if the incoming file was the last loaded file..."
        )
        logger.info(
            "Putting time delta metrics for the time taken between the current %s RIF file being"
            " made available and now (when it has been loaded)...",
            rif_file_type.name,
        )

        logger.info(
            "Retrieving %s message from %s queue for %s RIF in %s group...",
            PipelineLoadEventType.RIF_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
            rif_file_type.name,
            group_iso_str,
        )
        try:
            rif_available_msg = backoff_retry(
                func=lambda: next(
                    (
                        message
                        for message in retrieve_load_event_msgs(
                            queue=events_queue,
                            timeout=10,
                            type_filter=[PipelineLoadEventType.RIF_AVAILABLE],
                        )
                        if message.event.group_iso_str == group_iso_str
                        and message.event.rif_type == rif_file_type
                    ),
                    None,
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to check the %s queue; err: ",
                EVENTS_QUEUE_NAME,
                exc_info=True,
            )
            return

        if rif_available_msg:
            logger.info(
                "%s message retrieved successfully for %s RIF in %s group; value: %s",
                PipelineLoadEventType.RIF_AVAILABLE.value,
                rif_file_type,
                group_iso_str,
                rif_available_msg,
            )

            rif_last_available = rif_available_msg.event.datetime
            load_time_delta = event_datetime - rif_last_available

            logger.info(
                'Putting time delta metrics to "%s" with value %s s...',
                PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.full_name(),
                load_time_delta.seconds,
            )
            try:
                backoff_retry(
                    func=lambda: put_metric_data(
                        cw_client=cw_client,
                        metric_namespace=METRICS_NAMESPACE,
                        metrics=gen_all_dimensioned_metrics(
                            metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                            dimensions=[rif_type_dimension, group_timestamp_dimension],
                            value=round(load_time_delta.total_seconds()),
                            datetime=event_datetime,
                            unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                        ),
                    ),
                    ignored_exceptions=common_unrecoverable_exceptions,
                )
            except Exception:
                logger.error(
                    "An unrecoverable error occurred when trying to call PutMetricData for metric"
                    " %s: ",
                    PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.full_name(),
                    exc_info=True,
                )
                return
            logger.info(
                'Metrics put to "%s" successfully',
                PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.full_name(),
            )

            logger.info(
                "Removing %s message for %s RIF with group %s from %s queue...",
                PipelineLoadEventType.RIF_AVAILABLE.value,
                rif_file_type,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )
            try:
                backoff_retry(
                    func=lambda: delete_load_msg_from_queue(
                        queue=events_queue, message=rif_available_msg
                    ),
                    ignored_exceptions=common_unrecoverable_exceptions,
                )
            except MessageFailedToDeleteException:
                logger.error(
                    "%s message for %s RIF with group %s was NOT removed from %s queue;"
                    " reason(s): ",
                    PipelineLoadEventType.RIF_AVAILABLE.value,
                    rif_file_type,
                    group_iso_str,
                    EVENTS_QUEUE_NAME,
                    exc_info=True,
                )
            except Exception:
                logger.error(
                    "An unrecoverable error ocurred when attempting to delete message from %s"
                    " queue: ",
                    EVENTS_QUEUE_NAME,
                    exc_info=True,
                )
                return
            logger.info(
                "%s message for %s RIF with group %s removed from %s queue successfully",
                PipelineLoadEventType.RIF_AVAILABLE.value,
                rif_file_type,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )
        else:
            logger.error(
                "No corresponding messages found for %s RIF in group %s in queue %s; no time delta"
                " metrics can be computed for this RIF. Continuing...",
                rif_file_type.value,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )

        logger.info("Checking if the pipeline load has completed...")
        if not _is_pipeline_load_complete(
            bucket=etl_bucket, group=group_iso_str
        ) or not _is_incoming_folder_empty(bucket=etl_bucket, group=group_iso_str):
            logger.info(
                "Not all files have yet to be loaded for group %s. Data load is not complete."
                " Stopping...",
                group_iso_str,
            )
            return

        logger.info(
            "All files have been loaded for group %s. This indicates that the data load has"
            ' been completed for this group. Putting data to metric "%s" and corresponding'
            ' metric "%s" with value %s',
            group_iso_str,
            PipelineMetric.TIME_DATA_FULLY_LOADED.full_name(),
            PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.full_name(),
            utc_timestamp,
        )
        try:
            backoff_retry(
                func=lambda: put_metric_data(
                    cw_client=cw_client,
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                        dimensions=[group_timestamp_dimension],
                        datetime=event_datetime,
                        value=utc_timestamp,
                        unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                    )
                    + [
                        MetricData(
                            metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.metric_name,
                            value=utc_timestamp,
                            unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.unit,
                            datetime=event_datetime,
                        )
                    ],
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to call PutMetricData; err: ",
                exc_info=True,
            )
        logger.info(
            'Data put to "%s" and "%s" successfully',
            PipelineMetric.TIME_DATA_FULLY_LOADED.full_name(),
            PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.full_name(),
        )

        logger.info(
            "Retrieving %s message from %s queue for %s group...",
            PipelineLoadEventType.LOAD_AVAILABLE.value,
            EVENTS_QUEUE_NAME,
            group_iso_str,
        )
        try:
            load_available_msg = backoff_retry(
                func=lambda: next(
                    (
                        message
                        for message in retrieve_load_event_msgs(
                            queue=events_queue,
                            timeout=10,
                            type_filter=[PipelineLoadEventType.LOAD_AVAILABLE],
                        )
                        if message.event.group_iso_str == group_iso_str
                    ),
                    None,
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
        except Exception:
            logger.error(
                "An unrecoverable error occurred when trying to check the %s queue; err: ",
                EVENTS_QUEUE_NAME,
                exc_info=True,
            )
            return

        if load_available_msg:
            first_available_time = load_available_msg.event.datetime
            full_load_time_delta = event_datetime - first_available_time

            logger.info(
                'Putting to "%s" the total time delta (%s s) from start to finish for the current'
                " pipeline load for group %s",
                PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.full_name(),
                full_load_time_delta.seconds,
                group_iso_str,
            )
            try:
                backoff_retry(
                    func=lambda: put_metric_data(
                        cw_client=cw_client,
                        metric_namespace=METRICS_NAMESPACE,
                        metrics=gen_all_dimensioned_metrics(
                            metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                            dimensions=[group_timestamp_dimension],
                            datetime=event_datetime,
                            value=round(full_load_time_delta.total_seconds()),
                            unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                        ),
                    ),
                    ignored_exceptions=common_unrecoverable_exceptions,
                )
            except Exception:
                logger.error(
                    "An unrecoverable error occurred when trying to call PutMetricData; err: ",
                    exc_info=True,
                )
            logger.info(
                'Data put to metric "%s" successfully',
                PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.full_name(),
            )

            logger.info(
                "Removing %s message for group %s from %s queue...",
                PipelineLoadEventType.LOAD_AVAILABLE.value,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )
            try:
                backoff_retry(
                    func=lambda: delete_load_msg_from_queue(
                        queue=events_queue, message=load_available_msg
                    ),
                    ignored_exceptions=common_unrecoverable_exceptions,
                )
            except MessageFailedToDeleteException:
                logger.error(
                    "%s message for group %s was NOT removed from %s queue; reason(s): ",
                    PipelineLoadEventType.RIF_AVAILABLE.value,
                    group_iso_str,
                    EVENTS_QUEUE_NAME,
                    exc_info=True,
                )
            except Exception:
                logger.error(
                    "An unrecoverable error ocurred when attempting to delete message from %s"
                    " queue: ",
                    EVENTS_QUEUE_NAME,
                    exc_info=True,
                )
                return
            logger.info(
                "%s message for group %s removed from %s queue successfully",
                PipelineLoadEventType.RIF_AVAILABLE.value,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )
        else:
            logger.error(
                "No corresponding %s message found for group %s in queue %s; no time delta"
                " metrics can be computed for this data load",
                PipelineLoadEventType.LOAD_AVAILABLE.value,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )
