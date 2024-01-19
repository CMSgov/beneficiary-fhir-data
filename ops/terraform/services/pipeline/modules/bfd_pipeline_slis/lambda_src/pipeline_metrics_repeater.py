import logging
import os
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Type

import boto3
from botocore import exceptions as botocore_exceptions
from botocore.config import Config

from backoff_retry import backoff_retry
from common import METRICS_NAMESPACE, PipelineMetric
from cw_metrics import MetricData, MetricDataQuery, get_metric_data, put_metric_data

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

SOURCE_TO_REPEATING_METRICS = {
    PipelineMetric.TIME_DATA_FIRST_AVAILABLE: PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING,
    PipelineMetric.TIME_DATA_FULLY_LOADED: PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING,
}

logging.basicConfig(level=logging.INFO, force=True)
logger = logging.getLogger()


@dataclass
class MetricLatestValue:
    pipeline_metric: PipelineMetric
    latest_value: float


def handler(event: Any, context: Any):
    if not all([REGION, METRICS_NAMESPACE]):
        logger.error("Not all necessary environment variables were defined, exiting...")
        return

    try:
        cw_client = boto3.client(service_name="cloudwatch", config=BOTO_CONFIG)  # type: ignore
    except Exception:
        logger.error(
            "Unrecoverable exception occurred when attempting to create boto3 clients/resources:",
            exc_info=True,
        )
        return

    common_unrecoverable_exceptions: list[Type[BaseException]] = [
        cw_client.exceptions.InvalidParameterValueException,
        cw_client.exceptions.MissingRequiredParameterException,
        cw_client.exceptions.InvalidParameterCombinationException,
        botocore_exceptions.ParamValidationError,
    ]

    logger.info(
        "Getting latest values for metrics %s...",
        [x.full_name() for x in SOURCE_TO_REPEATING_METRICS],
    )
    try:
        result = backoff_retry(
            func=lambda: get_metric_data(
                cw_client=cw_client,
                metric_data_queries=[
                    MetricDataQuery(
                        metric_namespace=METRICS_NAMESPACE, metric_name=pipeline_metric.metric_name
                    )
                    for pipeline_metric in SOURCE_TO_REPEATING_METRICS
                ],
                statistic="Maximum",
            ),
            ignored_exceptions=common_unrecoverable_exceptions + [KeyError],
        )
    except Exception:
        logger.error(
            "An unrecoverable error occurred when trying to call GetMetricData; err:",
            exc_info=True,
        )
        return
    logger.info(
        "Retrieved metric data %s for metrics %s",
        str(result),
        [x.full_name() for x in SOURCE_TO_REPEATING_METRICS],
    )

    if not result:
        logger.error(
            "No data was returned for %s metrics, exiting",
            [x.full_name() for x in SOURCE_TO_REPEATING_METRICS],
        )
        return

    if any(not x.values or not x.timestamps for x in result):
        logger.error("Empty metric data returned from metric data result: %s", str(result))
        return

    logger.info(
        "Determining latest values for %s...",
        [x.full_name() for x in SOURCE_TO_REPEATING_METRICS],
    )
    latest_values = [
        MetricLatestValue(
            pipeline_metric=pipeline_metric,
            latest_value=metric_data.values[
                metric_data.timestamps.index(max(metric_data.timestamps))
            ],
        )
        for metric_data in result
        if not (
            pipeline_metric := next(
                (m for m in PipelineMetric if m.full_name() == metric_data.label), None
            )
        )
        is None
    ]
    logger.info("Latest values are %s", latest_values)

    logger.info(
        "Submitting latest values for metrics %s to corresponding CloudWatch Metric(s): %s",
        [x.full_name() for x in SOURCE_TO_REPEATING_METRICS],
        [SOURCE_TO_REPEATING_METRICS[m.pipeline_metric].full_name() for m in latest_values],
    )
    try:
        backoff_retry(
            func=lambda: put_metric_data(
                cw_client=cw_client,
                metric_namespace=METRICS_NAMESPACE,
                metrics=[
                    MetricData(
                        metric_name=SOURCE_TO_REPEATING_METRICS[v.pipeline_metric].metric_name,
                        value=v.latest_value,
                        unit=SOURCE_TO_REPEATING_METRICS[v.pipeline_metric].unit,
                        date_time=datetime.utcnow(),
                    )
                    for v in latest_values
                ],
            ),
            ignored_exceptions=common_unrecoverable_exceptions,
        )
    except Exception:
        logger.error(
            "An unrecoverable error occurred when trying to call PutMetricData; exc", exc_info=True
        )
        return
    logger.info(
        "Latest values for metrics %s submitted successfully",
        [x.full_name() for x in SOURCE_TO_REPEATING_METRICS.values()],
    )
