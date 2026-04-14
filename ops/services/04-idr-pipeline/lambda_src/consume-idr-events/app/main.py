import json
import os
import re
from datetime import datetime
from typing import Any

import boto3
import psycopg
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities import parameters
from aws_lambda_powertools.utilities.data_classes import SQSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from botocore.config import Config
from psycopg import sql
from pydantic.main import BaseModel

REGION = os.environ.get("AWS_CURRENT_REGION", default="us-east-1")
BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
DB_ENDPOINT = os.environ.get("DB_ENDPOINT") or os.environ.get("PGHOST", default="")
DB_PORT = int(os.environ.get("DB_PORT") or os.environ.get("PGPORT", default="5432"))
DB_USERNAME = os.environ.get("DB_USERNAME") or os.environ.get("PGUSER")
DB_PASSWORD = os.environ.get("DB_PASSWORD") or os.environ.get("PGPASSWORD")
DB_DATABASE = os.environ.get("DB_DATABASE") or os.environ.get("PGDATABASE", default="fhirdb")
DB_SCHEMA = os.environ.get("DB_SCHEMA", default="idr")
RUN_IDR_PIPELINE_LAMBDA_NAME = os.environ.get("RUN_IDR_PIPELINE_LAMBDA_NAME")

BOTO_CONFIG = Config(
    region_name=REGION,
    # Instructs boto3 to retry upto 10 times using an exponential backoff
    retries={
        "total_max_attempts": 10,
        "mode": "adaptive",
    },
    # Double the read timeout for some extra safety when synchronously invoking the run-idr-pipeline
    # Lambda
    read_timeout=120,
)
SOURCE_LOAD_EVENTS_TABLE = "source_load_events"

logger = Logger()


class IdrLoadEventModel(BaseModel):
    id: str
    job_name: str
    job_message: str
    event_time: datetime


class RunIdrPipelineResultModel(BaseModel):
    result_type: str
    details: dict[str, Any]


def insert_event(event: IdrLoadEventModel) -> None:
    db_username = DB_USERNAME or parameters.get_parameter(
        f"/bfd/{BFD_ENVIRONMENT}/idr-pipeline/sensitive/db/username", decrypt=True
    )
    db_password = DB_PASSWORD or parameters.get_parameter(
        f"/bfd/{BFD_ENVIRONMENT}/idr-pipeline/sensitive/db/password", decrypt=True
    )
    with (
        psycopg.connect(
            host=DB_ENDPOINT,
            user=db_username,
            password=db_password,
            port=DB_PORT,
            dbname=DB_DATABASE,
        ) as conn,
        conn.cursor() as curs,
    ):
        logger.info("Connected to %s", DB_ENDPOINT)

        event_dict = event.model_dump()
        query_template = t"""
            INSERT INTO {DB_SCHEMA:i}.{SOURCE_LOAD_EVENTS_TABLE:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in event_dict):q}
            )
            VALUES (
                {sql.SQL(", ").join(event_dict.values()):q}
            )
        """
        logger.info(
            "Executing query: %s",
            re.sub(r"\s+", " ", sql.as_string(query_template)),
        )
        curs.execute(query_template)
        logger.info(
            "Inserted '%s' into %s successfully", event.model_dump_json(), SOURCE_LOAD_EVENTS_TABLE
        )


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:  # noqa: ARG001
    try:
        if not BFD_ENVIRONMENT or not DB_ENDPOINT or not RUN_IDR_PIPELINE_LAMBDA_NAME:
            raise RuntimeError("Not all necessary environment variables were defined")

        sqs_event = SQSEvent(event)
        if next(sqs_event.records, None) is None:
            raise ValueError(
                "Invalid SQS event with empty records:"
                f" {json.dumps(sqs_event.raw_event, default=str)}"
            )

        for sqs_record in sqs_event.records:
            raw_message: dict[str, Any] = json.loads(sqs_record.body)

            idr_load_event = IdrLoadEventModel.model_validate(
                {
                    "id": sqs_record.message_id,
                    "job_name": raw_message["jobName"],
                    "job_message": raw_message["jobMessage"],
                    "event_time": sqs_record.attributes.sent_timestamp,
                },
            )
            insert_event(idr_load_event)

        logger.info(
            "Invoking the %s Lambda to start the IDR Pipeline if it is not already running...",
            RUN_IDR_PIPELINE_LAMBDA_NAME,
        )
        lambda_client = boto3.client("lambda", config=BOTO_CONFIG)
        response = lambda_client.invoke(
            FunctionName=RUN_IDR_PIPELINE_LAMBDA_NAME,
            InvocationType="RequestResponse",
            Payload=json.dumps({"reschedule": True}),
        )
        lambda_result = RunIdrPipelineResultModel.model_validate_json(response["Payload"].read())
        logger.info(
            "Successfully invoked the %s Lambda. See next log for result",
            RUN_IDR_PIPELINE_LAMBDA_NAME,
        )
        logger.info(lambda_result.model_dump())
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
