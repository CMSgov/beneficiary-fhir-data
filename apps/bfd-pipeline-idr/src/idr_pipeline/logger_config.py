import contextlib
import logging
import sys

import loguru
from loguru import logger

from idr_pipeline.constants import SQL_LOG_TYPE
from idr_pipeline.settings import SETTINGS


class InterceptHandler(logging.Handler):
    def emit(self, record: logging.LogRecord) -> None:
        # Get corresponding Loguru level if it exists
        try:
            level = logger.level(record.levelname).name
        except ValueError:
            level = record.levelno

        # Find caller from where originated the logged message
        frame, depth = logging.currentframe(), 2
        while frame and frame.f_code.co_filename == logging.__file__:
            frame = frame.f_back
            depth += 1

        logger.opt(depth=depth, exception=record.exc_info).log(level, record.getMessage())


def _filter(record: loguru.Record) -> bool:
    if record["level"].name == SQL_LOG_TYPE:
        return SETTINGS.sql_log
    return True


def configure_logger() -> None:
    # Loguru requires that there be a single Logger configured at the very beginning of the
    # program. That Logger is then inherited by all processes such that each process has the same
    # configuration and "sink"

    # This line intercepts all logs from the standard logging module for compatibility with Loguru
    logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)

    logger.remove()
    # Fails if you try to create the level multiple times
    # There doesn't seem to be a way to check if it already exists or not
    with contextlib.suppress(ValueError):
        logger.level(SQL_LOG_TYPE, no=100)

    logger.add(
        # We intentionally use a lambda here to force sys.stderr to be re-evaluated
        # This is necessary for pytest since it overrides stdout and stderr
        # Without this, we'll get an IO error when tests fail due to the stream being dropped
        # Recommended per the docs:
        # https://loguru.readthedocs.io/en/stable/resources/troubleshooting.html#how-do-i-fix-valueerror-i-o-operation-error-on-closed-file
        sink=lambda m: sys.stderr.write(m),  # type: ignore  # noqa: PLW0108
        filter=_filter,
        level=SETTINGS.log_level,
        enqueue=True,  # Ensures non-blocking and async+multiprocessing-safe
        diagnose=False,  # Ensures local variables are not logged for exceptions
        serialize=SETTINGS.structured_logs,
    )
