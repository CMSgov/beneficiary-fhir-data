"""This module contains utility functions related to conversions shared between node and controller
modules.
"""

from typing import Any


def to_bool(from_obj: Any | None) -> bool:
    """Convert any object with a valid string representation into a bool by checking if its string
    representation is "true", case-insensitive.

    Args:
        from_obj (Optional[Any]): The object to convert to a bool

    Returns:
        bool: True if the str representation of from_obj is "true" (case-insensitive), false
        otherwise
    """
    if from_obj is None:
        return False

    return str(from_obj).lower() == "true"
