from typing import Any, Dict, List

QUEUE_STOP_SIGNAL_FILTER = {"Stop": "Signal"}
WARM_POOL_INSTANCE_LAUNCH_FILTER = {"Origin": "EC2", "Destination": "WarmPool"}


def filter_message_by_keys(message: Dict[str, Any], message_filters: List[Dict[str, Any]]) -> bool:
    """Returns if a message (typically the inner Dict of an SQS message, but could be any Dict)
    matches any of the message filters given

    Args:
        message (Dict[str, Any]): The message dict to filter
        message_filters (List[Dict[str, Any]]): List of message filters that the message should
        match

    Returns:
        bool: True if any message filter matched the message, false otherwise
    """
    return any(
        all(k in message and message[k] == message_filter[k] for k in message_filter)
        for message_filter in message_filters
    )
