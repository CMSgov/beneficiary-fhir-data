from datetime import datetime
import json
import os
from typing import Annotated, Any

from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import SQSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext
from pydantic.fields import Field
from pydantic.main import BaseModel

BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
DB_ENDPOINT = os.environ.get("DB_ENDPOINT", default="")

logger = Logger()

class IdrLoadEventModel(BaseModel):
    id: str
    job: Annotated[str, Field(validation_alias="jobName")]
    message: Annotated[str, Field(validation_alias="jobMessage")]
    event_time: datetime

def insert_event(event: IdrLoadEventModel) -> None:
    pass

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
