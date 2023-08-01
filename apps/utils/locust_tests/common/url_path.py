"""Create a URL path from a base path and query parameters."""
from typing import List, Mapping, Optional, Union
from urllib.parse import urlencode


def create_url_path(
    path: str, query_params: Optional[Mapping[str, Union[str, int, List]]] = None
) -> str:
    """Creates a query path from a base path (i.e. /v2/fhir/Coverage) and a dictionary of query
    parameters."""
    if not query_params:
        return path

    cleaned_params = {}
    for index, value in query_params.items():
        cleaned_params[index] = value.pop() if isinstance(value, list) else value

    return f"{path}?{urlencode(cleaned_params)}"
