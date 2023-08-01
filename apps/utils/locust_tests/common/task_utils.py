"""Misc. Locust task utility functions"""

from typing import Any, Mapping


def params_to_str(params_dict: Mapping[str, Any]) -> str:
    """Returns a string representation of a query parameters dict suitable for use in a task's name

    Args:
        params_dict (dict[str, Any]): Dict of params used in a task's request

    Returns:
        str: A string representation of the task request's query parameters
    """
    return ", ".join(f"{k}={v}" for k, v in params_dict.items())
