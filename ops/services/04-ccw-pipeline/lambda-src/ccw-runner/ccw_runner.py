"""Source code for ccw-runner Lambda.

This Lambda is invoked by S3 Event Notifications and EventBridge Scheduler Schedule(s). When ran, it
does a rudimentary check to determine if there are any CCW manifests waiting to be loaded by the CCW
Pipeline in S3. If there are, it will spawn an ECS Fargate Task to load the manifests into the
database. If there are no manifests waiting to be loaded, or if the ECS Fargate Task is already
running, it will simply exit.
"""

import itertools
import os
import re
from datetime import UTC, datetime
from typing import TYPE_CHECKING, Any, cast

import boto3
import psycopg
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities import parameters
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from psycopg.rows import dict_row
from pydantic import BaseModel, Field, TypeAdapter

if TYPE_CHECKING:
    from mypy_boto3_ecs.type_defs import CapacityProviderStrategyItemTypeDef
else:
    CapacityProviderStrategyItemTypeDef = object


class CapacityProviderStrategy(BaseModel):
    capacity_provider: str = Field(serialization_alias="capacityProvider")
    weight: int
    base: int


class ManifestFilesResultModel(BaseModel):
    """Pydantic model modeling columns returned from the s3_manifest_files database table."""

    s3_key: str
    status: str


REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", default="")
DB_ENDPOINT = os.environ.get("DB_ENDPOINT", default="")
CCW_BUCKET = os.environ.get("CCW_BUCKET", default="")
ECS_CLUSTER_ARN = os.environ.get("ECS_CLUSTER_ARN", default="")
CCW_TASK_DEFINITION_ARN = os.environ.get("CCW_TASK_DEFINITION_ARN", default="")
CCW_TASK_GROUP = os.environ.get("CCW_TASK_GROUP", default="")
CCW_TASK_SUBNETS = [
    subnet.strip() for subnet in os.environ.get("CCW_TASK_SUBNETS", default="").split(",")
]
CCW_TASK_SECURITY_GROUP_ID = os.environ.get("CCW_TASK_SECURITY_GROUP_ID", default="")
CCW_TASK_TAGS = TypeAdapter(dict[str, Any]).validate_json(
    os.environ.get("CCW_TASK_TAGS_JSON", default="")
)
CCW_TASK_CAPACITY_PROVIDER_STRATEGIES = TypeAdapter(list[CapacityProviderStrategy]).validate_json(
    os.environ.get("CCW_TASK_CAPACITY_PROVIDER_STRATEGIES", default="")
)
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

GROUP_MANIFEST_TIMESTAMP = "manifest_timestamp"
S3_KEY_PATTERN = re.compile(
    pattern=(
        r"^(?:Synthetic){0,1}/{0,1}"
        r"Incoming/"
        rf"(?P<{GROUP_MANIFEST_TIMESTAMP}>[\d\-:TZ]+)/"
        r".*manifest\.xml$"
    ),
    flags=re.IGNORECASE,
)

logger = Logger()


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext) -> None:  # noqa: ARG001
    """Lambda event handler function.

    Args:
        event (dict[Any, Any]): EventBridge Scheduler/S3 Bucket Notification event details. Unused
        context (LambdaContext): Lambda execution context. Unused

    Raises:
        RuntimeError: If any required environment variables are undefined
    """
    try:
        if not all([
            REGION,
            BFD_ENVIRONMENT,
            DB_ENDPOINT,
            CCW_BUCKET,
            ECS_CLUSTER_ARN,
            CCW_TASK_DEFINITION_ARN,
            CCW_TASK_GROUP,
            CCW_TASK_SUBNETS,
            CCW_TASK_SECURITY_GROUP_ID,
        ]):
            raise RuntimeError("Not all necessary environment variables were defined")

        utc_now = datetime.now(UTC)
        logger.info(
            "Discovering manifest(s) timestamped prior to %s from S3 in Incoming...",
            utc_now.isoformat(),
        )
        s3_resource = boto3.resource("s3", config=BOTO_CONFIG)  # type: ignore
        etl_bucket = s3_resource.Bucket(CCW_BUCKET)
        all_incoming_objects = itertools.chain(
            etl_bucket.objects.filter(Prefix="Incoming/"),
            etl_bucket.objects.filter(Prefix="Synthetic/Incoming/"),
        )
        valid_manifests = [
            object.key
            for object in all_incoming_objects
            if (match := re.search(pattern=S3_KEY_PATTERN, string=object.key))
            and (manifest_time := datetime.fromisoformat(match.group(GROUP_MANIFEST_TIMESTAMP)))
            and manifest_time <= utc_now
        ]
        if not valid_manifests:
            logger.info(
                "No eligible S3 manifest(s) found in Incoming/ or Synthetic/Incoming/. Stopping..."
            )
            return

        logger.info(
            "Discovered %d S3 manifest(s) timestamped prior to %s eligible for load",
            len(valid_manifests),
            utc_now.isoformat(),
        )

        logger.info("Retrieving manifest status of discovered S3 manifests from the database...")
        db_username = parameters.get_parameter(  # type: ignore
            f"/bfd/{BFD_ENVIRONMENT}/ccw-pipeline/sensitive/db/username", decrypt=True
        )
        db_password = parameters.get_parameter(  # type: ignore
            f"/bfd/{BFD_ENVIRONMENT}/ccw-pipeline/sensitive/db/password", decrypt=True
        )

        logger.info(
            "Connecting to %s as Pipeline user in %s cluster...",
            DB_ENDPOINT,
            BFD_ENVIRONMENT,
        )
        with (
            psycopg.connect(
                host=DB_ENDPOINT,
                user=db_username,
                password=db_password,
                port=5432,
                dbname="fhirdb",
            ) as conn,
            conn.cursor(row_factory=dict_row) as curs,
        ):
            logger.info("Connected to %s", DB_ENDPOINT)

            raw_results = curs.execute(
                """
                SELECT s3_key, status FROM ccw.s3_manifest_files
                WHERE s3_key = ANY(%s);
                """,
                [valid_manifests],
            ).fetchall()
            db_manifest_files = TypeAdapter(list[ManifestFilesResultModel]).validate_python(
                raw_results
            )
            logger.info(
                "Retrieved %d manifest(s) from the database timestamped prior to %s",
                len(db_manifest_files),
                utc_now.isoformat(),
            )

        logger.info("Verifying all %d S3 manifest(s) have been loaded...", len(valid_manifests))
        unprocessed_manifests = [
            s3_manifest
            for s3_manifest in valid_manifests
            if not any(
                s3_manifest == db_manifest.s3_key and db_manifest.status == "COMPLETED"
                for db_manifest in db_manifest_files
            )
        ]
        if len(unprocessed_manifests) == 0:
            logger.info("All manifests in S3 have been loaded by the Pipeline. Stopping")
            return

        logger.info(
            "%d manifest(s) waiting to load; checking if CCW Pipline Task is running...",
            len(unprocessed_manifests),
        )

        ecs_client = boto3.client("ecs", config=BOTO_CONFIG)  # type: ignore
        tasks = ecs_client.list_tasks(cluster=ECS_CLUSTER_ARN)["taskArns"]
        running_ccw_tasks = (
            [
                task
                for task in ecs_client.describe_tasks(cluster=ECS_CLUSTER_ARN, tasks=tasks)["tasks"]
                if "group" in task
                and task["group"] == CCW_TASK_GROUP
                and "desiredStatus" in task
                and task["desiredStatus"].lower()
                not in ["deactivating", "stopping", "deprovisioning", "stopped", "deleted"]
            ]
            if tasks
            else []
        )
        if len(running_ccw_tasks) > 0:
            logger.info(
                "%d CCW Pipeline Task(s) are running; no action needed. Stopping...",
                len(running_ccw_tasks),
            )
            return

        logger.info("No running CCW Pipeline Tasks found; proceeding to launch a new Task...")
        ecs_client.run_task(
            capacityProviderStrategy=[
                cast(CapacityProviderStrategyItemTypeDef, strategy.model_dump(by_alias=True))
                for strategy in CCW_TASK_CAPACITY_PROVIDER_STRATEGIES
            ],
            taskDefinition=CCW_TASK_DEFINITION_ARN,
            cluster=ECS_CLUSTER_ARN,
            group=CCW_TASK_GROUP,
            count=1,
            platformVersion="LATEST",
            networkConfiguration={
                "awsvpcConfiguration": {
                    "subnets": CCW_TASK_SUBNETS,
                    "securityGroups": [CCW_TASK_SECURITY_GROUP_ID],
                    "assignPublicIp": "DISABLED",
                }
            },
            tags=[{"key": k, "value": str(v)} for k, v in CCW_TASK_TAGS.items()],
        )
        logger.info("New CCW Pipeline Task launched successfully.")
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
