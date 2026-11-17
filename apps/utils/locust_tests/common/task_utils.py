"""Misc. Locust task utility functions."""

import itertools
from collections.abc import Collection, Mapping
from typing import Any


def params_to_str(params_dict: Mapping[str, Any]) -> str:
    """Return a string representation of a query parameters dict suitable for use in a task's name.

    Args:
        params_dict (dict[str, Any]): Dict of params used in a task's request

    Returns:
        str: A string representation of the task request's query parameters
    """
    return ", ".join(f"{k}={v}" for k, v in params_dict.items())


def check_empty_collection[T](data: Collection[T], name: str) -> itertools.cycle[T]:
    """Check for an empty collection from the db.
    This most likely means the db query failed for some reason.
    We need to force an early failure to prevent StopIteration errors from spamming the outpt.
    """
    if not data:
        raise Exception(f"{name} empty")
    return itertools.cycle(list(data))
