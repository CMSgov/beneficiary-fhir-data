import json
import os
from datetime import datetime
from typing import Annotated, Any

import psycopg
from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities import parameters
from aws_lambda_powertools.utilities.data_classes import SQSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from pydantic.fields import Field
from pydantic.main import BaseModel

BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
DB_ENDPOINT = os.environ.get("DB_ENDPOINT", default="")

IDR_LOAD_EVENTS_TABLE = "idr.load_events"

logger = Logger()


class IdrLoadEventModel(BaseModel):
    id: str
    job: Annotated[str, Field(validation_alias="jobName")]
    message: Annotated[str, Field(validation_alias="jobMessage")]
    event_time: datetime


def insert_event(event: IdrLoadEventModel) -> None:
    db_username = parameters.get_parameter(
        f"/bfd/{BFD_ENVIRONMENT}/idr-pipeline/sensitive/db/username", decrypt=True
    )
    db_password = parameters.get_parameter(
        f"/bfd/{BFD_ENVIRONMENT}/idr-pipeline/sensitive/db/password", decrypt=True
    )
    with (
        psycopg.connect(
            host=DB_ENDPOINT,
            user=db_username,
            password=db_password,
            port=5432,
            dbname="idr",
        ) as conn,
        conn.cursor() as curs,
    ):
        logger.info("Connected to %s", DB_ENDPOINT)

        curs.execute(t"""
            INSERT INTO {IDR_LOAD_EVENTS_TABLE}(
                id,
                job,
                message,
                event_time
            )
            VALUES(
                {event.id},
                {event.job},
                {event.message},
                {event.event_time}
            )
            """)

        logger.info(
            "Inserted '%s' into %s successfully", event.model_dump_json(), IDR_LOAD_EVENTS_TABLE
        )


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:  # noqa: ARG001
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
            raw_message["id"] = sqs_record.message_id
            raw_message["event_time"] = sqs_record.attributes.sent_timestamp

            idr_load_event = IdrLoadEventModel.model_validate(raw_message, by_alias=True)
            insert_event(idr_load_event)
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
