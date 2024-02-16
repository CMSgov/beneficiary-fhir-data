import json
from dataclasses import asdict, dataclass, field
from datetime import datetime
from typing import TYPE_CHECKING

from aws_lambda_powertools import Logger

if TYPE_CHECKING:
    from mypy_boto3_sns.service_resource import Topic
else:
    Topic = object

logger = Logger()

@dataclass(frozen=True, eq=True)
class TransferSuccessDetails:
    partner: str
    timestamp: datetime
    object_key: str
    file_type: str


@dataclass(frozen=True, eq=True)
class TransferFailedDetails:
    partner: str
    timestamp: datetime
    object_key: str
    error_name: str
    reason: str


@dataclass(frozen=True, eq=True)
class UnknownErrorDetails:
    timestamp: datetime
    error_name: str
    reason: str


@dataclass
class StatusNotification:
    type: str = field(init=False, default="")
    details: TransferSuccessDetails | TransferFailedDetails | UnknownErrorDetails

    def __post_init__(self):
        match self.details:
            case TransferSuccessDetails():
                self.type = "TRANSFER_SUCCESS"
            case TransferFailedDetails():
                self.type = "TRANSFER_FAILED"
            case UnknownErrorDetails():
                self.type = "UNKNOWN_ERROR"
            case _:  # pyright: ignore [reportUnnecessaryComparison]
                raise ValueError(
                    f"Invalid details, must be one of: "
                    f"{(", ".join([
                        x.__name__ for x in [
                            TransferSuccessDetails,
                            TransferFailedDetails,
                            UnknownErrorDetails,
                        ]
                    ]))}"
                )

    def as_sns_message(self) -> str:
        return json.dumps(asdict(self), default=str)


def send_notification(topic: Topic, notification: StatusNotification):
    topic.publish(
        Message=notification.as_sns_message(),
        MessageAttributes={"type": {"DataType": "String", "StringValue": notification.type}},
    )
    logger.info("Published %s to %s", notification.as_sns_message(), topic.arn)
