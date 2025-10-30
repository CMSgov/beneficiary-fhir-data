"""This module contains constants and utilty functions related to filtering SQS messages shared by
both the node and controller modules.
"""

from typing import Any

QUEUE_STOP_SIGNAL_FILTERS = [{"Stop": "Signal"}]
"""Filters that indicate an operator posted an immediate stop signal"""
WARM_POOL_INSTANCE_LAUNCH_FILTERS = [
    {"Origin": "EC2", "Destination": "WarmPool"},
    {"Origin": "WarmPool", "Destination": "AutoScalingGroup"},
]
"""Filters that indicate a scaling event against the target ASG has occurred"""


def filter_message_by_keys(message: dict[str, Any], message_filters: list[dict[str, Any]]) -> bool:
    """Return if a message (typically the inner Dict of an SQS message, but could be any Dict)
    matches any of the message filters given.

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
