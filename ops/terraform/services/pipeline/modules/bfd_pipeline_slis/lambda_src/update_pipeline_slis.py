import calendar
import json
import logging
import os
import re
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Type
from urllib.parse import unquote

import boto3
from aws_lambda_powertools.utilities.data_classes import S3Event, SNSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore import exceptions as botocore_exceptions
from botocore.config import Config
from mypy_boto3_s3.service_resource import Bucket

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
EVENTS_QUEUE_NAME = os.environ.get("EVENTS_QUEUE_NAME", "")
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


class S3EventType(str, Enum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"

    @classmethod
    def from_event_name(cls, event_name: str) -> "S3EventType":
        try:
            return next(x for x in S3EventType if event_name in x)
        except StopIteration as ex:
            raise ValueError(
                f"Invalid event name {event_name}; no corresponding, supported event found"
            ) from ex


class PipelineDataStatus(str, Enum):
    """Represents the possible states of data: either data is available to load, or has been loaded
    by the ETL pipeline. The value of each enum is the parent directory of the incoming file,
    indicating status"""

    INCOMING = "incoming"
    DONE = "done"


def _is_pipeline_load_complete(bucket: Bucket, group: str) -> bool:
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


def _is_incoming_folder_empty(bucket: Bucket, group: str) -> bool:
    incoming_key_prefix = f"{PipelineDataStatus.INCOMING.capitalize()}/{group}/"
    incoming_objects = list(bucket.objects.filter(Prefix=incoming_key_prefix))

    return len(incoming_objects) == 0


def _handle_s3_event(s3_event_time: datetime, s3_object_key: str):
    cw_client = boto3.client(service_name="cloudwatch", config=BOTO_CONFIG)  # type: ignore
    s3_resource = boto3.resource("s3", config=BOTO_CONFIG)  # type: ignore
    sqs_resource = boto3.resource("sqs", config=BOTO_CONFIG)  # type: ignore
    events_queue = sqs_resource.get_queue_by_name(QueueName=EVENTS_QUEUE_NAME)
    etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
    common_unrecoverable_exceptions: list[Type[BaseException]] = [
        cw_client.exceptions.InvalidParameterValueException,
        cw_client.exceptions.MissingRequiredParameterException,
        cw_client.exceptions.InvalidParameterCombinationException,
        botocore_exceptions.ParamValidationError,
    ]

    status_group_str = "|".join([e.value for e in PipelineDataStatus])
    rif_types_group_str = "|".join([e.value for e in RifFileType])
    # The incoming file's key should match an expected format, as follows:
    # "<Incoming/Done>/<ISO date format>/<file name>".
    if not (
        match := re.search(
            rf"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$",
            s3_object_key,
            re.IGNORECASE,
        )
    ):
        logger.warning(
            "ETL file or path does not match expected format, skipping: %s", s3_object_key
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

    utc_timestamp = calendar.timegm(s3_event_time.utctimetuple())

    logger.info(
        'Putting data timestamp metrics "%s" up to CloudWatch with unix timestamp value %s',
        timestamp_metric.full_name(),
        utc_timestamp,
    )
    backoff_retry(
        # Store four metrics (gen_all_dimensioned_metrics() will generate all possible
        # dimensions of the given metric based upon the powerset of the dimensions):
        func=lambda: put_metric_data(
            cw_client=cw_client,
            metric_namespace=METRICS_NAMESPACE,
            metrics=gen_all_dimensioned_metrics(
                metric_name=timestamp_metric.metric_name,
                date_time=s3_event_time,
                value=utc_timestamp,
                unit=timestamp_metric.unit,
                dimensions=[rif_type_dimension, group_timestamp_dimension],
            ),
        ),
        ignored_exceptions=common_unrecoverable_exceptions,
    )
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
        backoff_retry(
            func=lambda: post_load_event(
                queue=events_queue,
                message=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.RIF_AVAILABLE,
                    date_time=s3_event_time,
                    group_iso_str=group_iso_str,
                    rif_type=rif_file_type,
                ),
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
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
        backoff_retry(
            func=lambda: put_metric_data(
                cw_client=cw_client,
                metric_namespace=METRICS_NAMESPACE,
                metrics=gen_all_dimensioned_metrics(
                    metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.metric_name,
                    dimensions=[group_timestamp_dimension],
                    date_time=s3_event_time,
                    value=utc_timestamp,
                    unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.unit,
                )
                + [
                    MetricData(
                        metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.metric_name,
                        value=utc_timestamp,
                        unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.unit,
                        date_time=s3_event_time,
                    )
                ],
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
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
        backoff_retry(
            func=lambda: post_load_event(
                queue=events_queue,
                message=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                    date_time=s3_event_time,
                    group_iso_str=group_iso_str,
                    rif_type=rif_file_type,
                ),
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
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
        if rif_available_msg:
            logger.info(
                "%s message retrieved successfully for %s RIF in %s group; value: %s",
                PipelineLoadEventType.RIF_AVAILABLE.value,
                rif_file_type,
                group_iso_str,
                rif_available_msg,
            )

            rif_last_available = rif_available_msg.event.date_time
            load_time_delta = s3_event_time - rif_last_available

            logger.info(
                'Putting time delta metrics to "%s" with value %s s...',
                PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.full_name(),
                load_time_delta.seconds,
            )
            backoff_retry(
                func=lambda: put_metric_data(
                    cw_client=cw_client,
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                        dimensions=[rif_type_dimension, group_timestamp_dimension],
                        value=round(load_time_delta.total_seconds()),
                        date_time=s3_event_time,
                        unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                    ),
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
            )
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
                    ignored_exceptions=common_unrecoverable_exceptions
                    + [MessageFailedToDeleteException],
                )
                logger.info(
                    "%s message for %s RIF with group %s removed from %s queue successfully",
                    PipelineLoadEventType.RIF_AVAILABLE.value,
                    rif_file_type,
                    group_iso_str,
                    EVENTS_QUEUE_NAME,
                )
            except MessageFailedToDeleteException:
                logger.warning(
                    "%s message for %s RIF with group %s was NOT removed from %s queue; ",
                    PipelineLoadEventType.RIF_AVAILABLE.value,
                    rif_file_type,
                    group_iso_str,
                    EVENTS_QUEUE_NAME,
                    exc_info=True,
                )
        else:
            logger.warning(
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
        backoff_retry(
            func=lambda: put_metric_data(
                cw_client=cw_client,
                metric_namespace=METRICS_NAMESPACE,
                metrics=gen_all_dimensioned_metrics(
                    metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                    dimensions=[group_timestamp_dimension],
                    date_time=s3_event_time,
                    value=utc_timestamp,
                    unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                )
                + [
                    MetricData(
                        metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.metric_name,
                        value=utc_timestamp,
                        unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.unit,
                        date_time=s3_event_time,
                    )
                ],
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
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
        if load_available_msg:
            first_available_time = load_available_msg.event.date_time
            full_load_time_delta = s3_event_time - first_available_time

            logger.info(
                'Putting to "%s" the total time delta (%s s) from start to finish for the current'
                " pipeline load for group %s",
                PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.full_name(),
                full_load_time_delta.seconds,
                group_iso_str,
            )
            backoff_retry(
                func=lambda: put_metric_data(
                    cw_client=cw_client,
                    metric_namespace=METRICS_NAMESPACE,
                    metrics=gen_all_dimensioned_metrics(
                        metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                        dimensions=[group_timestamp_dimension],
                        date_time=s3_event_time,
                        value=round(full_load_time_delta.total_seconds()),
                        unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                    ),
                ),
                ignored_exceptions=common_unrecoverable_exceptions,
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
                    ignored_exceptions=common_unrecoverable_exceptions
                    + [MessageFailedToDeleteException],
                )
                logger.info(
                    "%s message for group %s removed from %s queue successfully",
                    PipelineLoadEventType.RIF_AVAILABLE.value,
                    group_iso_str,
                    EVENTS_QUEUE_NAME,
                )
            except MessageFailedToDeleteException:
                logger.warning(
                    "%s message for %s RIF with group %s was NOT removed from %s queue; ",
                    PipelineLoadEventType.LOAD_AVAILABLE.value,
                    rif_file_type,
                    group_iso_str,
                    EVENTS_QUEUE_NAME,
                    exc_info=True,
                )
        else:
            logger.warning(
                "No corresponding %s message found for group %s in queue %s; no time delta"
                " metrics can be computed for this data load",
                PipelineLoadEventType.LOAD_AVAILABLE.value,
                group_iso_str,
                EVENTS_QUEUE_NAME,
            )


def handler(event: dict[Any, Any], context: LambdaContext):
    try:
        if not all([REGION, METRICS_NAMESPACE, ETL_BUCKET_ID, EVENTS_QUEUE_NAME]):
            raise RuntimeError("Not all necessary environment variables were defined")

        sns_event = SNSEvent(event)
        if next(sns_event.records, None) is None:
            raise ValueError(f"Invalid SNS event {sns_event.raw_event}; empty records")

        for sns_record in sns_event.records:
            s3_event = S3Event(json.loads(sns_record.sns.message))
            if next(s3_event.records, None) is None:
                raise ValueError(f"Invalid inner S3 event {s3_event.raw_event}; empty records")

            for s3_record in s3_event.records:
                s3_event_time = datetime.fromisoformat(
                    s3_record.event_time.removesuffix("Z")
                ).astimezone(tz=timezone.utc)
                s3_object_key = unquote(s3_record.s3.get_object.key)

                # Log the various bits of data extracted from the invoking event to aid debugging:
                logger.info("Invoked at: %s UTC", datetime.utcnow().isoformat())
                logger.info("S3 Object Key: %s", s3_object_key)
                logger.info(
                    "S3 Event Type: %s, Specific Event Name: %s",
                    S3EventType.from_event_name(s3_record.event_name).name,
                    s3_record.event_name,
                )

                _handle_s3_event(s3_event_time=s3_event_time, s3_object_key=s3_object_key)
    except Exception:
        logger.error("An unrecoverable exception occurred upon Lambda invocation", exc_info=True)
