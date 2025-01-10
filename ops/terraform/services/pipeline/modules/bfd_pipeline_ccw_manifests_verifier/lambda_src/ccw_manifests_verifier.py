"""Source code for ccw-manifests-verifier Lambda."""

import itertools
import os
from datetime import UTC, datetime, timedelta
from typing import Any

import boto3
import psycopg
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities import parameters
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from psycopg.rows import dict_row
from pydantic import BaseModel, TypeAdapter

REGION = os.environ.get("AWS_CURRENT_REGION", "us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
DB_CLUSTER_NAME = os.environ.get("DB_CLUSTER_NAME", "")
ETL_BUCKET_ID = os.environ.get("ETL_BUCKET_ID", "")
BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
)

logger = Logger()


class ManifestFilesResultModel(BaseModel):
    s3_key: str
    status: str


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext) -> None:  # noqa: ARG001
    """Lambda event handler function called when an EventBridge Scheduler Event is received.

    Args:
        event (dict[Any, Any]): EventBridge Scheduler event details. Unused
        context (LambdaContext): Lambda execution context. Unused

    Raises:
        RuntimeError: If any AWS API operations fail
    """
    try:
        if not all([REGION, BFD_ENVIRONMENT, DB_CLUSTER_NAME, ETL_BUCKET_ID]):
            raise RuntimeError("Not all necessary environment variables were defined")

        rds_client = boto3.client("rds", config=BOTO_CONFIG)
        described_clusters = rds_client.describe_db_clusters(
            DBClusterIdentifier=DB_CLUSTER_NAME
        ).get("DBClusters", [])
        try:
            reader_endpoint = next(
                cluster_detail["ReaderEndpoint"]
                for cluster_detail in described_clusters
                if "ReaderEndpoint" in cluster_detail
            )
        except StopIteration as exc:
            raise RuntimeError(
                "describe-db-clusters returned invalid data for "
                f"{DB_CLUSTER_NAME}: {described_clusters!s}"
            ) from exc
        db_username = parameters.get_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/pipeline/sensitive/db/username", decrypt=True
        )
        db_password = parameters.get_parameter(
            f"/bfd/{BFD_ENVIRONMENT}/pipeline/sensitive/db/password", decrypt=True
        )

        logger.info(
            "Connecting to %s as Pipeline user in %s cluster...",
            reader_endpoint,
            DB_CLUSTER_NAME,
        )
        with (
            psycopg.connect(
                host=reader_endpoint,
                user=db_username,
                password=db_password,
                port=5432,
                dbname="fhirdb",
            ) as conn,
            conn.cursor(row_factory=dict_row) as curs,
        ):
            logger.info("Connected to %s", reader_endpoint)
            two_weeks_ago = datetime.now(UTC) - timedelta(weeks=2)

            # Retrieve all manifests in the Incoming paths in s3 that were recently modified in the
            # past 2 weeks. These manifests will be reconciled against their status in the database
            s3_resource = boto3.resource("s3", config=BOTO_CONFIG)
            etl_bucket = s3_resource.Bucket(ETL_BUCKET_ID)
            all_incoming_objects = itertools.chain(
                etl_bucket.objects.filter(Prefix="Incoming/"),
                etl_bucket.objects.filter(Prefix="Synthetic/Incoming/"),
            )
            recent_s3_manifest_keys = [
                object.key
                for object in all_incoming_objects
                if "manifest" in object.key and object.last_modified.astimezone(UTC) > two_weeks_ago
            ]

            # Retrieve all manifests and their state that were discovered in the past 2 weeks from
            # the database. This list will be the source of truth against which the manifests in s3
            # will be verified against
            raw_results = curs.execute(
                """
                SELECT s3_key, status FROM ccw.s3_manifest_files
                WHERE discovery_timestamp > %s;
                """,
                (two_weeks_ago,),
            ).fetchall()
            db_manifest_files = TypeAdapter(list[ManifestFilesResultModel]).validate_python(
                raw_results
            )

            # Reconcile the retrieved recently modified s3 manifests in Incoming with the manifests
            # and their state in the database. If any s3 manifests either are not in the COMPLETED
            # state or simply aren't in the database, the Pipeline failed to finish loading over the
            # weekend and we should alert
            unprocessed_manifests = [
                s3_manifest
                for s3_manifest in recent_s3_manifest_keys
                if not any(
                    s3_manifest == db_manifest.s3_key and db_manifest.status == "COMPLETED"
                    for db_manifest in db_manifest_files
                )
            ]
            if unprocessed_manifests:
                logger.info("All manifests in S3 have been loaded by the Pipeline. Stopping")
                return

            # TODO: Alerting
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
