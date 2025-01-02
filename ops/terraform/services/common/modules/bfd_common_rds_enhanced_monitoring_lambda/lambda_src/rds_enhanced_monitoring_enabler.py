"""Source code for rds-enhanced-monitoring-enabler Lambda.

This Lambda is invoked upon "CloudWatch Log Exports enabled" RDS events received from RDS
EventBridge Rules. These events roughly coincide with RDS instances nearing the "available" state
where their Enhanced Monitoring configuration can be modified. This Lambda enables Enhanced
Monitoring for Application Autoscaling DB Instances that emit such events as Enhanced Monitoring is
not automatically enabled for such Instances.
"""

import os
import time
from datetime import UTC, datetime, timedelta
from typing import Any

import boto3
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.parser import envelopes, parse
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from pydantic import BaseModel, Field

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
RDS_MONITORING_ROLE_ARN = os.environ.get("RDS_MONITORING_ROLE_ARN", "")
RDS_MONITORING_INTERVAL_SECONDS = os.environ.get("RDS_MONITORING_INTERVAL_SECONDS", "15")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)
TRY_MODIFY_DB_INSTANCE_INTERVAL_SEC = 5

logger = Logger()


class RDSEventModel(BaseModel):
    """Represents the relevant, parsed details of an RDS EventBridge Event."""

    date: datetime = Field(validation_alias="Date")
    source_instance: str = Field(validation_alias="SourceIdentifier")


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext) -> None:
    """Lambda invoke handler invoked by an RDS EventBridge Event.

    :param event: RDS EventBridge Event
    :type event: dict[Any, Any]
    :param context: Context about the Lambda invocation
    :type context: LambdaContext
    """
    try:
        if not all([
            REGION,
            BFD_ENVIRONMENT,
            RDS_MONITORING_ROLE_ARN,
            RDS_MONITORING_INTERVAL_SECONDS,
        ]):
            raise RuntimeError("Not all necessary environment variables were defined")

        rds_monitoring_interval = int(RDS_MONITORING_INTERVAL_SECONDS)

        parsed_event = parse(
            model=RDSEventModel, event=event, envelope=envelopes.EventBridgeEnvelope
        )

        stop_time = (
            datetime.now(UTC)
            + timedelta(milliseconds=context.get_remaining_time_in_millis())
            # Give some headroom for the loop to exit properly before the configured Lambda timeout
            - timedelta(seconds=TRY_MODIFY_DB_INSTANCE_INTERVAL_SEC * 2)
        )
        rds_client = boto3.client("rds", config=BOTO_CONFIG)
        try_num = 0
        logger.info(
            "Received CloudWatch Logs Export enablement event for %s timestamped %s. Attempting to "
            "enable RDS Enhanced Monitoring every %d second(s) for %s (until %s)...",
            parsed_event.source_instance,
            parsed_event.date.isoformat(),
            TRY_MODIFY_DB_INSTANCE_INTERVAL_SEC,
            stop_time - datetime.now(UTC),
            stop_time.isoformat(),
        )
        while datetime.now(UTC) < stop_time:
            logger.info(
                "try #%d: Attempting to enable RDS Enhanced Monitoring for %s...",
                try_num,
                parsed_event.source_instance,
            )
            try:
                rds_client.modify_db_instance(
                    DBInstanceIdentifier=parsed_event.source_instance,
                    MonitoringInterval=rds_monitoring_interval,
                    MonitoringRoleArn=RDS_MONITORING_ROLE_ARN,
                )
                logger.info(
                    "try #%d: RDS Enhanced Monitoring successfully enabled for %s",
                    try_num,
                    parsed_event.source_instance,
                )
                return
            except rds_client.exceptions.InvalidDBInstanceStateFault:
                logger.warning(
                    "try %d: DB Instance %s is not in the 'available' state yet. RDS Enhanced "
                    "Monitoring cannot be enabled. Sleeping for %d second(s)...",
                    try_num,
                    parsed_event.source_instance,
                    TRY_MODIFY_DB_INSTANCE_INTERVAL_SEC,
                )
                time.sleep(TRY_MODIFY_DB_INSTANCE_INTERVAL_SEC)
                try_num += 1

        # If we get here, the Lambda was unable to enable Enhanced Monitoring (the timeout was
        # reached, somehow). Throw an exception
        timeout_time = datetime.now(UTC) + timedelta(
            milliseconds=context.get_remaining_time_in_millis()
        )
        raise RuntimeError(
            f"Unable to enable RDS Enhanced Monitoring for {parsed_event.source_instance} after "
            f"{try_num} try(s) before Lambda times out at {timeout_time.isoformat()}"
        )
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
