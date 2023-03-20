import json
from dataclasses import dataclass
from typing import Any, Optional


@dataclass
class SentinelMessage:
    group_timestamp: str


def check_sentinel_queue(sentinel_queue: Any, timeout: int = 1) -> list[SentinelMessage]:
    """Checks the sentinel SQS queue for messages and returns their values

    Args:
        timeout (int, optional): Amount of time to poll for messages in queue. Defaults to 1 second

    Returns:
        list[SentinelMessage]: The list of sentinel messages in the queue
    """
    responses = sentinel_queue.receive_messages(WaitTimeSeconds=timeout)

    def load_json_safe(json_str: str) -> Optional[dict[str, str]]:
        try:
            return json.loads(json_str)
        except json.JSONDecodeError:
            return None
        except TypeError:
            return None

    raw_messages = [load_json_safe(response.body) for response in responses]
    filtered_messages = [
        SentinelMessage(**message)
        for message in raw_messages
        if message is not None and "group_timestamp" in message
    ]

    return filtered_messages
