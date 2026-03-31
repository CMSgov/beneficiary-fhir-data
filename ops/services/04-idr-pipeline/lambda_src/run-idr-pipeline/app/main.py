import os
from typing import Any

from aws_lambda_powertools import Logger
from aws_lambda_powertools.utilities.typing import LambdaContext

BFD_ENVIRONMENT = os.environ.get("BFD_ENVIRONMENT", "")

logger = Logger()


@logger.inject_lambda_context(clear_state=True, log_event=True)
def handler(event: dict[str, Any], context: LambdaContext) -> None:  # noqa: ARG001
    try:
        if not all([BFD_ENVIRONMENT]):
            raise RuntimeError("Not all necessary environment variables were defined")

    except Exception:
        logger.exception("Unrecoverable exception raised")
        raise
