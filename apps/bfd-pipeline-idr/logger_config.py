import logging
import os
import sys

from loguru import logger


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


def configure_logger() -> None:
    # Loguru requires that there be a single Logger configured at the very beginning of the
    # program. That Logger is then inherited by all processes such that each process has the same
    # configuration and "sink"

    # This line intercepts all logs from the standard logging module for compatibility with Loguru
    logging.basicConfig(handlers=[InterceptHandler()], level=0, force=True)

    logger.remove()
    logger.add(
        sink=sys.stderr,
        level=os.getenv("IDR_LOG_LEVEL", "INFO").upper(),
        enqueue=True,  # Ensures non-blocking and async+multiprocessing-safe
        diagnose=False,  # Ensures local variables are not logged for exceptions
    )
