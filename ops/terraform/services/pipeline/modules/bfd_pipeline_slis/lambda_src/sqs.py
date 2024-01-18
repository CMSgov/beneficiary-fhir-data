import calendar
import json
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Optional, Union

from common import RifFileType


class PipelineLoadEventType(str, Enum):
    LOAD_AVAILABLE = "LOAD_AVAILABLE"
    RIF_AVAILABLE = "RIF_AVAILABLE"


@dataclass
class PipelineLoadEvent:
    event_type: PipelineLoadEventType
    timestamp: datetime
    group_timestamp: str
    rif_type: RifFileType


def retrieve_load_events(
    queue: Any,
    timeout: int = 3,
    type_filter: list[PipelineLoadEventType] = list(PipelineLoadEventType),
) -> list[PipelineLoadEvent]:
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

    raw_messages = [load_json_safe(response.body) for response in responses]
    load_events = [
        PipelineLoadEvent(
            event_type=PipelineLoadEventType(str(message["event_type"])),
            timestamp=datetime.utcfromtimestamp(int(message["timestamp"])),
            group_timestamp=str(message["group_timestamp"]),
            rif_type=RifFileType(str(message["rif_type"])),
        )
        for message in raw_messages
        if message is not None
        and "event" in message
        and "timestamp" in message
        and "group_timestamp" in message
        and "rif_type" in message
    ]
    filtered_events = [event for event in load_events if event.event_type in type_filter]

    return filtered_events


def post_load_event(queue: Any, message: PipelineLoadEvent):
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
                "timestamp": calendar.timegm(message.timestamp.utctimetuple()),
                "group_timestamp": message.group_timestamp,
                "rif_type": message.rif_type.value,
            }
        )
    )
