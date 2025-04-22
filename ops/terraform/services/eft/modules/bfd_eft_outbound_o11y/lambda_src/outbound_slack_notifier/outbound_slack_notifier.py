import json
import os
from dataclasses import asdict, dataclass
from enum import StrEnum
from typing import Any
from urllib.request import Request, urlopen

from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.data_classes import SNSEvent
from aws_lambda_powertools.utilities.typing import LambdaContext

BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")
SLACK_WEBHOOK_URL = os.environ.get("SLACK_WEBHOOK_URL", "")

logger = Logger()


class NotificationType(StrEnum):
    FILE_DISCOVERED = "FILE_DISCOVERED"
    TRANSFER_SUCCESS = "TRANSFER_SUCCESS"
    TRANSFER_FAILED = "TRANSFER_FAILED"
    UNKNOWN_ERROR = "UNKNOWN_ERROR"


@dataclass(frozen=True, eq=True)
class StatusNotification:
    type: NotificationType
    message: str
    details: dict[str, Any]


class PostSlackMessageError(Exception): ...


def _slack_status_notif(status_notification: StatusNotification):
    message_preamble: str
    match status_notification.type:
        case NotificationType.FILE_DISCOVERED:
            message_preamble = (
                ":information_source: A file was discovered by the BFD EFT Outbound Lambda in"
                f" `{BFD_ENVIRONMENT}` and will be transferred soon. See details below for more"
                " information :information_source:"
            )
        case NotificationType.TRANSFER_SUCCESS:
            message_preamble = (
                ":tada: A file was successfully transferred to the CMS EFT SFTP Server by the BFD"
                f" EFT Outbound Lambda in `{BFD_ENVIRONMENT}`. See details below for more"
                " information :tada:"
            )
        case NotificationType.TRANSFER_FAILED:
            message_preamble = (
                ":warning: A file was unable to be transferred to the CMS EFT SFTP Server by the"
                f" BFD EFT Outbound Lambda in `{BFD_ENVIRONMENT}`. See details below for more"
                " information :warning:"
            )
        case NotificationType.UNKNOWN_ERROR:
            message_preamble = (
                ":warning: An unexpected error occurred when the BFD EFT Outbound Lambda attempted"
                f" to transfer a file in `{BFD_ENVIRONMENT}`. See details below for more"
                " information :warning:"
            )

    slack_message = {
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"*{message_preamble}*"
                        "\n"
                        f"```{json.dumps(asdict(status_notification), indent=2)}```"
                    ),
                },
            }
        ]
    }

    logger.info("Attempt to post message %s", json.dumps(slack_message))
    request = Request(SLACK_WEBHOOK_URL, method="POST")
    request.add_header("Content-Type", "application/json")
    with urlopen(request, data=json.dumps(slack_message).encode("utf-8")) as response:
        if response.status == 200:
            logger.info("Message posted successfully")
        else:
            raise PostSlackMessageError(
                f"Non-200 response received; {response.status} response received from Slack"
            )


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[Any, Any], context: LambdaContext):  # pylint: disable=unused-argument
    try:
        if not all([BFD_ENVIRONMENT, SLACK_WEBHOOK_URL]):
            raise RuntimeError("Not all necessary environment variables were defined")

        sns_event = SNSEvent(event)
        if next(sns_event.records, None) is None:
            raise ValueError(
                "Invalid SNS event with empty records:"
                f" {json.dumps(sns_event.raw_event, default=str)}"
            )

        for sns_record in sns_event.records:
            raw_notification: dict[str, Any] = json.loads(sns_record.sns.message)
            raw_notification["type"] = NotificationType[raw_notification["type"]]
            status_notification = StatusNotification(**raw_notification)

            logger.append_keys(notification_type=status_notification.type.value)

            if status_notification.type in [
                NotificationType.TRANSFER_SUCCESS,
                NotificationType.FILE_DISCOVERED,
            ]:
                continue

            _slack_status_notif(status_notification=status_notification)
    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
