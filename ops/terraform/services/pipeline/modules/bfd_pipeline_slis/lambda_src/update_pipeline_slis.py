import calendar
import json
import os
import re
from datetime import datetime, timezone
from enum import StrEnum
from typing import TYPE_CHECKING, Any, Type
from urllib.parse import unquote

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import S3Event, SNSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore import exceptions as botocore_exceptions
from botocore.config import Config

from backoff_retry import backoff_retry
from common import METRICS_NAMESPACE, PipelineMetric, RifFileType
from cw_metrics import MetricData, gen_all_dimensioned_metrics, put_metric_data
from dynamo_db import (
    LoadAvailableEvent,
    RifAvailableEvent,
    get_load_available_event,
    get_rif_available_event,
    put_load_available_event,
    put_rif_available_event,
)

# Solve typing issues in Lambda as mypy_boto3 will not be included in the Lambda
if TYPE_CHECKING:
    from mypy_boto3_s3.service_resource import Bucket
else:
    Bucket = object


REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
RIF_AVAILABLE_DDB_TBL = os.environ.get("RIF_AVAILABLE_DDB_TBL", "")
LOAD_AVAILABLE_DDB_TBL = os.environ.get("LOAD_AVAILABLE_DDB_TBL", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logger = Logger()


class S3EventType(StrEnum):
    """Represents the types of S3 events that this Lambda is invoked by and supports. The value of
    each Enum is a substring that is matched for on the "eventName" property of an invocation
    event"""

    OBJECT_CREATED = "ObjectCreated"

    @classmethod
    def from_event_name(cls, event_name: str) -> "S3EventType":
        try:
            return next(x for x in S3EventType if x in event_name)
        except StopIteration as ex:
            raise ValueError(
                f"Invalid event name {event_name}; no corresponding, supported event found"
            ) from ex


class PipelineDataStatus(StrEnum):
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
    dynamo_resource = boto3.resource("dynamodb", config=BOTO_CONFIG)  # type: ignore
    load_available_tbl = dynamo_resource.Table(LOAD_AVAILABLE_DDB_TBL)
    rif_available_tbl = dynamo_resource.Table(RIF_AVAILABLE_DDB_TBL)
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
    expected_object_key_regex = rf"^({status_group_str})/([\d\-:TZ]+)/.*({rif_types_group_str}).*$"
    if not (match := re.search(expected_object_key_regex, s3_object_key, re.IGNORECASE)):
        logger.warning(
            "ETL file or path does not match expected format: %s", expected_object_key_regex
        )
        return

    rif_load_status = PipelineDataStatus(match.group(1).lower())
    data_load_iso_str = match.group(2)
    rif_file_type = RifFileType(match.group(3).lower())

    # Append data extracted from S3 object key to all future log messages now that we know this is a
    # valid RIF file within a valid data load
    logger.append_keys(
        rif_file_type=rif_file_type.name,
        rif_load_status=rif_load_status,
        data_load_iso_str=data_load_iso_str,
    )

    rif_type_dimension = {"data_type": rif_file_type.name.lower()}
    # Group timestamp, or just "group", is a legacy term used interchangibly with "data load" or
    # "data load ISO string" throughout this Lambda, supporting documentation, and metrics
    group_timestamp_dimension = {"group_timestamp": data_load_iso_str}

    timestamp_metric = (
        PipelineMetric.TIME_DATA_AVAILABLE
        if rif_load_status == PipelineDataStatus.INCOMING
        else PipelineMetric.TIME_DATA_LOADED
    )

    utc_timestamp = calendar.timegm(s3_event_time.utctimetuple())

    logger.info(
        'Putting value %s to CloudWatch Metric "%s"', utc_timestamp, timestamp_metric.full_name()
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

    if rif_load_status == PipelineDataStatus.INCOMING:
        logger.info(
            "RIF file location indicates data has been made available to load to the ETL pipeline."
            " Putting event to %s table when RIF was made available",
            RIF_AVAILABLE_DDB_TBL,
        )
        backoff_retry(
            func=lambda: put_rif_available_event(
                table=rif_available_tbl,
                event=RifAvailableEvent(
                    date_time=s3_event_time, group_iso_str=data_load_iso_str, rif=rif_file_type
                ),
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
        logger.info("Event put to %s table successfully", RIF_AVAILABLE_DDB_TBL)

        logger.info(
            "Attempting to retrieve the event indicating when the current load was first made"
            " available for current group/load, if it exists, from table %s",
            LOAD_AVAILABLE_DDB_TBL,
        )
        load_available_event = backoff_retry(
            func=lambda: get_load_available_event(
                table=load_available_tbl, group_iso_str=data_load_iso_str
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
        if load_available_event:
            logger.info(
                "Item indicating first availability of data load was received from table %s for"
                " current group %s. Incoming file is part of an ongoing, existing data load for"
                " group %s, and therefore does not indicate the time of the first data load for"
                " this group. Stopping...",
                LOAD_AVAILABLE_DDB_TBL,
                data_load_iso_str,
                data_load_iso_str,
            )
            return

        logger.info(
            "No event indicating data load availability was retrieved from table %s for current"
            " group %s. This indicates that the incoming file is the start of a new data load for"
            ' group %s. Putting data to metric "%s" and corresponding metric "%s" with value %s',
            LOAD_AVAILABLE_DDB_TBL,
            data_load_iso_str,
            data_load_iso_str,
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
            "Putting event indicating that the current data load, %s, has been made available to %s"
            " table. No additional data should be put to the %s metric for this group",
            data_load_iso_str,
            LOAD_AVAILABLE_DDB_TBL,
            PipelineMetric.TIME_DATA_FIRST_AVAILABLE.full_name(),
        )
        backoff_retry(
            func=lambda: put_load_available_event(
                table=load_available_tbl,
                event=LoadAvailableEvent(date_time=s3_event_time, group_iso_str=data_load_iso_str),
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
        logger.info("Event put to %s table successfully", LOAD_AVAILABLE_DDB_TBL)
    elif rif_load_status == PipelineDataStatus.DONE:
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
            "Retrieving event indicating when %s RIF was loaded from %s table for current data load"
            " %s...",
            rif_file_type.name,
            RIF_AVAILABLE_DDB_TBL,
            data_load_iso_str,
        )
        rif_available_event = backoff_retry(
            func=lambda: get_rif_available_event(
                table=rif_available_tbl, group_iso_str=data_load_iso_str, rif=rif_file_type
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
        if rif_available_event:
            logger.info(
                "Event indicating when %s RIF was first made available was retrieved successfully."
                " %s RIF was made available to load for group %s at %s",
                rif_file_type.name,
                rif_file_type.name,
                data_load_iso_str,
                str(rif_available_event.date_time),
            )

            rif_last_available = rif_available_event.date_time
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
        else:
            logger.warning(
                "No corresponding event found for %s RIF in group %s in table %s; no time delta"
                " metrics can be computed for this RIF. Continuing...",
                rif_file_type.name,
                data_load_iso_str,
                RIF_AVAILABLE_DDB_TBL,
            )

        logger.info("Checking if the pipeline load has completed...")
        if not _is_pipeline_load_complete(
            bucket=etl_bucket, group=data_load_iso_str
        ) or not _is_incoming_folder_empty(bucket=etl_bucket, group=data_load_iso_str):
            logger.info(
                "Not all files have yet to be loaded for group %s. Data load is not complete."
                " Stopping...",
                data_load_iso_str,
            )
            return

        logger.info(
            "All files have been loaded for group %s. This indicates that the data load has"
            ' been completed for this group. Putting data to metric "%s" and corresponding'
            ' metric "%s" with value %s',
            data_load_iso_str,
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
            "Retrieving event indicating when current load/group, %s, was first made available to"
            " load in S3 from %s table...",
            data_load_iso_str,
            LOAD_AVAILABLE_DDB_TBL,
        )
        load_available_event = backoff_retry(
            func=lambda: get_load_available_event(
                table=load_available_tbl, group_iso_str=data_load_iso_str
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
        if load_available_event:
            first_available_time = load_available_event.date_time
            full_load_time_delta = s3_event_time - first_available_time

            logger.info(
                'Putting to "%s" the total time delta (%s s) from start to finish for the current'
                " pipeline load for group %s",
                PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.full_name(),
                full_load_time_delta.seconds,
                data_load_iso_str,
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
        else:
            logger.warning(
                "No event found for group %s in table %s; no time delta metrics can be computed for"
                " this data load",
                data_load_iso_str,
                LOAD_AVAILABLE_DDB_TBL,
            )


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext):
    # The Lambda this handler is defined for is invoked asynchronously, so by default AWS retries
    # any failing invocations twice before dropping the event. This handler has side effects, so
    # this default has been lowered to 0 such that if the function fails it does not attempt to
    # retry. This is why this handler, and the functions it calls, raise exceptions that are not
    # explicitly handled.
    # See https://docs.aws.amazon.com/lambda/latest/dg/invocation-async.html#invocation-async-errors
    try:
        if not all([
            REGION,
            METRICS_NAMESPACE,
            ETL_BUCKET_ID,
            RIF_AVAILABLE_DDB_TBL,
            LOAD_AVAILABLE_DDB_TBL,
        ]):
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
                ).replace(tzinfo=timezone.utc)
                s3_object_key = unquote(s3_record.s3.get_object.key)
                s3_event_name = s3_record.event_name
                s3_event_type = S3EventType.from_event_name(s3_event_name)

                # Append to all future logs information about the S3 Event
                logger.append_keys(
                    s3_object_key=s3_object_key,
                    s3_event_name=s3_event_name,
                    s3_event_type=s3_event_type.name,
                    s3_event_time=s3_event_time.isoformat(),
                )

                _handle_s3_event(s3_event_time=s3_event_time, s3_object_key=s3_object_key)
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
