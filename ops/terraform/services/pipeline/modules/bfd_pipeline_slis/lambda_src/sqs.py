import calendar
import json
import uuid
from dataclasses import asdict, dataclass
from datetime import datetime
from enum import Enum
from typing import TYPE_CHECKING, Optional, Union

from common import RifFileType

# Solve typing issues in Lambda as mypy_boto3 is not included
if TYPE_CHECKING:
    from mypy_boto3_sqs.service_resource import Queue
else:
    Queue = object


class PipelineLoadEventType(str, Enum):
    """Represents the type of an event in time that occurred for a particular pipeline load"""

    LOAD_AVAILABLE = "LOAD_AVAILABLE"
    """An event indicating that a particular Pipeline load (or group) was made available; in other
    words, this type of event represents when the first RIF of a load was made available"""
    RIF_AVAILABLE = "RIF_AVAILABLE"
    """An event indicating when a particular RIF file in a particular load (or group) was made
    available"""


@dataclass(frozen=True, eq=True)
class PipelineLoadEvent:
    """Object that encodes metadata about a particular event for a particular load. Used to compute
    delta times per-RIF and per-load"""

    event_type: PipelineLoadEventType
    """The type of event this encodes metadata for"""
    date_time: datetime
    """The time at which this event occurred"""
    group_iso_str: str
    """The load/group that this event is associated with"""
    rif_type: RifFileType
    """The RIF that generated this event"""


@dataclass(frozen=True, eq=True)
class PipelineLoadEventMessage:
    """Object that represents a retrieved SQS message for a PipelineLoadEvent"""

    receipt_handle: str
    """A unique string that corresponds to the raw SQS message's receipt handle. Used to delete the
    message when processed"""
    event: PipelineLoadEvent
    """The inner event encoded in the raw SQS message"""

    def __str__(self) -> str:
        return json.dumps(asdict(self), default=str)


class MessageFailedToDeleteException(Exception):
    """An exception indicating that there was a failure when trying to delete a
    PipelineLoadEventMessage from the SQS events queue"""

    pass


def retrieve_load_event_msgs(
    queue: Queue,
    timeout: int = 3,
    type_filter: list[PipelineLoadEventType] = list(PipelineLoadEventType),
) -> list[PipelineLoadEventMessage]:
    """Retrieves a list of PipelineLoadEventMessages from the event queue, filtering upon the types
    of events desired.

    Args:
        queue (Queue): boto3 Queue corresponding to event queue
        timeout (int, optional): Timeout for how long to poll for messages. Defaults to 3.
        type_filter (list[PipelineLoadEventType], optional): List of event types to filter upon.
        Event types that are not specified will not be returned. Defaults to
        list(PipelineLoadEventType).

    Returns:
        list[PipelineLoadEventMessage]: A list of PipelineLoadEventMessages, filtered by
        type_filter, that exist in the event queue
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
        and "event_type" in message
        and "date_time" in message
        and "group_iso_str" in message
        and "rif_type" in message
    )
    filtered_messages = [message for message in messages if message.event.event_type in type_filter]

    return filtered_messages


def post_load_event(queue: Queue, message: PipelineLoadEvent):
    """Posts a PipelineLoadEvent to the provided SQS event queue

    Args:
        queue (Queue): boto3 Queue object representing the events queue
        message (PipelineLoadEvent): The event to post to the queue
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
    """Deletes a PipelineLoadEventMessage from the provided SQS event queue

    Args:
        queue (Queue): boto3 Queue object representing the events queue
        message (PipelineLoadEventMessage): The message to delete from the queue

    Raises:
        MessageFailedToDeleteException: If the message fails to delete. Message indicates the
        reason for failure
    """
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
