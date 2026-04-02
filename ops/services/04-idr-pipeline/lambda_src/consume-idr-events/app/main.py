import json
import os
import re
from datetime import datetime
from typing import Any

import psycopg
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities import parameters
from aws_lambda_powertools.utilities.data_classes import SQSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from psycopg import sql
from pydantic.main import BaseModel

BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
DB_ENDPOINT = os.environ.get("DB_ENDPOINT") or os.environ.get("PGHOST", default="")
DB_PORT = int(os.environ.get("DB_PORT") or os.environ.get("PGPORT", default="5432"))
DB_USERNAME = os.environ.get("DB_USERNAME") or os.environ.get("PGUSER")
DB_PASSWORD = os.environ.get("DB_PASSWORD") or os.environ.get("PGPASSWORD")
DB_DATABASE = os.environ.get("DB_DATABASE") or os.environ.get("PGDATABASE", default="fhirdb")
DB_SCHEMA = os.environ.get("DB_SCHEMA", default="idr")

IDR_LOAD_EVENTS_TABLE = "idr_load_events"

logger = Logger()


class IdrLoadEventModel(BaseModel):
    id: str
    job_name: str
    job_message: str
    event_time: datetime


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
        query_template = sql.SQL("INSERT INTO {}.{} ({}) VALUES ({})").format(
            sql.Identifier(DB_SCHEMA),
            sql.Identifier(IDR_LOAD_EVENTS_TABLE),
            sql.SQL(", ").join(map(sql.Identifier, event_dict.keys())),
            sql.SQL(", ").join(map(sql.Literal, event_dict.values())),
        )
        logger.info(
            "Executing query: %s",
            re.sub(r"\s+", " ", sql.as_string(query_template)),
        )
        curs.execute(query_template)
        logger.info(
            "Inserted '%s' into %s successfully", event.model_dump_json(), IDR_LOAD_EVENTS_TABLE
        )


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:
    try:
        if not all([BFD_ENVIRONMENT, DB_ENDPOINT]):
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

    # TODO: Trigger idr-pipeline after inserting events
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
