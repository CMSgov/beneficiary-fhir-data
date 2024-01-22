import calendar
import json
import uuid
from dataclasses import asdict, dataclass
from datetime import datetime
from enum import Enum
from typing import Optional, Union

from mypy_boto3_sqs.service_resource import Queue

from common import RifFileType


class PipelineLoadEventType(str, Enum):
    LOAD_AVAILABLE = "LOAD_AVAILABLE"
    RIF_AVAILABLE = "RIF_AVAILABLE"


@dataclass(frozen=True, eq=True)
class PipelineLoadEvent:
    event_type: PipelineLoadEventType
    date_time: datetime
    group_iso_str: str
    rif_type: RifFileType


@dataclass(frozen=True, eq=True)
class PipelineLoadEventMessage:
    receipt_handle: str
    event: PipelineLoadEvent

    def __str__(self) -> str:
        return json.dumps(asdict(self), default=str)


class MessageFailedToDeleteException(Exception):
    pass


def retrieve_load_event_msgs(
    queue: Queue,
    timeout: int = 3,
    type_filter: list[PipelineLoadEventType] = list(PipelineLoadEventType),
) -> list[PipelineLoadEventMessage]:
    """Checks the sentinel SQS queue for messages and returns their values

    Args:
        timeout (int, optional): Amount of time to poll for messages in queue. Defaults to 1 second

    Returns:
        list[SentinelMessage]: The list of sentinel messages in the queue
    """
    responses = queue.receive_messages(WaitTimeSeconds=timeout)

    def load_json_safe(json_str: str) -> Optional[dict[str, Union[str, int, float, bool]]]:
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            return None
        except TypeError:
            return None

    raw_messages = (
        (response.receipt_handle, load_json_safe(response.body)) for response in responses
    )
    messages = (
        PipelineLoadEventMessage(
            receipt_handle=receipt_handle,
            event=PipelineLoadEvent(
                event_type=PipelineLoadEventType(str(message["event_type"])),
                date_time=datetime.utcfromtimestamp(int(message["date_time"])),
                group_iso_str=str(message["group_iso_str"]),
                rif_type=RifFileType(str(message["rif_type"])),
            ),
        )
        for (receipt_handle, message) in raw_messages
        if message is not None
        and "event" in message
        and "date_time" in message
        and "group_iso_str" in message
        and "rif_type" in message
    )
    filtered_messages = [message for message in messages if message.event.event_type in type_filter]

    return filtered_messages


def post_load_event(queue: Queue, message: PipelineLoadEvent):
    """Posts a sentinel message to the provided queue indicating that the given group has started
    to load data

    Args:
        sentinel_queue (Any): boto3 SQS Queue
        group_timestamp (str): The timestamp of the pipeline data load
    """
    queue.send_message(
        MessageBody=json.dumps(
            {
                "event_type": message.event_type.value,
                "date_time": calendar.timegm(message.date_time.utctimetuple()),
                "group_iso_str": message.group_iso_str,
                "rif_type": message.rif_type.value,
            }
        )
    )


def delete_load_msg_from_queue(queue: Queue, message: PipelineLoadEventMessage):
    request_uuid = str(uuid.uuid4())
    delete_response = queue.delete_messages(
        Entries=[
            {
                "Id": request_uuid,
                "ReceiptHandle": message.receipt_handle,
            }
        ]
    )

    if failed_response := next(
        (resp for resp in delete_response["Failed"] if resp["Id"] == request_uuid), None
    ):
        raise MessageFailedToDeleteException(
            f"Failed to delete {str(message)}; reason: {json.dumps(failed_response)}"
        )
