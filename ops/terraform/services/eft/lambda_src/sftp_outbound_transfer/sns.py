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
class FileDiscoveredDetails:
    partner: str
    timestamp: datetime
    object_key: str
    file_type: str


@dataclass(frozen=True, eq=True)
class TransferSuccessDetails:
    partner: str
    timestamp: datetime
    object_key: str
    file_type: str
    transfer_duration: int


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
    message: str = field(init=False, default="")
    details: (
        FileDiscoveredDetails | TransferSuccessDetails | TransferFailedDetails | UnknownErrorDetails
    )

    def __post_init__(self) -> None:
        match self.details:
            case FileDiscoveredDetails():
                self.type = "FILE_DISCOVERED"
                self.message = (
                    "A valid, recognized file has been discovered by the BFD EFT Outbound process."
                    " The file will be transferred to the CMS EFT SFTP server. See the inner"
                    " details for more information"
                )
            case TransferSuccessDetails():
                self.type = "TRANSFER_SUCCESS"
                self.message = (
                    "A file has been successfully transferred to the CMS EFT SFTP server. See the"
                    " inner details for more information"
                )
            case TransferFailedDetails():
                self.type = "TRANSFER_FAILED"
                self.message = (
                    "A file has failed to be transferred to the CMS EFT SFTP server. See the inner"
                    " details for more information"
                )
            case UnknownErrorDetails():
                self.type = "UNKNOWN_ERROR"
                self.message = (
                    "An unknown error occurred during the BFD EFT Outbound process. See the inner"
                    " details for more information"
                )
            case _:  # pyright: ignore [reportUnnecessaryComparison]
                raise ValueError(
                    "Invalid details, must be one of: "
                    + ", ".join(
                        [
                            x.__name__
                            for x in [
                                FileDiscoveredDetails,
                                TransferSuccessDetails,
                                TransferFailedDetails,
                                UnknownErrorDetails,
                            ]
                        ]
                    )
                )

    def as_sns_message(self) -> str:
        return json.dumps(asdict(self), default=str)


def send_notification(topic: Topic, notification: StatusNotification) -> None:
    topic.publish(
        Message=notification.as_sns_message(),
        MessageAttributes={"type": {"DataType": "String", "StringValue": notification.type}},
    )
    logger.info("Published %s to %s", notification.as_sns_message(), topic.arn)
