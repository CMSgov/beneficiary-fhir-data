"""Create a URL path from a base path and query parameters."""

from collections.abc import Mapping
from urllib.parse import urlencode


def create_url_path(path: str, query_params: Mapping[str, str | int | list] | None = None) -> str:
    """Create a query path from a base path (i.e. /v2/fhir/Coverage) and a dictionary of query
    parameters.
    """
    if not query_params:
        return path

    cleaned_params = {}
    for index, value in query_params.items():
        cleaned_params[index] = value.pop() if isinstance(value, list) else value

    return f"{path}?{urlencode(cleaned_params)}"
