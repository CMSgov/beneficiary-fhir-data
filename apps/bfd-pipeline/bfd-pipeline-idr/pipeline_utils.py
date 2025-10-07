import logging
import sys
import time
from datetime import UTC, datetime, timedelta

from snowflake.connector import ProgrammingError
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from extractor import PostgresExtractor, SnowflakeExtractor
from loader import PostgresLoader
from model import (
    LoadProgress,
    T,
)


def configure_logger(name: str = "pipeline_worker") -> logging.Logger:
    logger = logging.getLogger(name)
    if not logger.handlers:
        console_handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(name)s %(message)s")
        console_handler.setFormatter(formatter)
        logger.addHandler(console_handler)
    logger.setLevel(logging.INFO)
    return logger


def get_progress(
    connection_string: str, table_name: str, start_time: datetime
) -> LoadProgress | None:
    return PostgresExtractor(connection_string, batch_size=1).extract_single(
        LoadProgress,
        LoadProgress.fetch_query(False, start_time),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load(
    cls: type[T], connection_string: str, mode: str, batch_size: int
) -> tuple[PostgresLoader, bool]:
    logger = configure_logger()

    if mode == "local" or mode == "synthetic":
        data_extractor = PostgresExtractor(
            connection_string=connection_string, batch_size=batch_size
        )
    else:
        data_extractor = SnowflakeExtractor(batch_size=batch_size)

    logger.info("loading %s", cls.table())
    batch_start = datetime.now()
    last_error = datetime.min.replace(tzinfo=UTC)
    loader = PostgresLoader(connection_string)
    error_count = 0
    max_errors = 3

    while True:
        try:
            progress = get_progress(connection_string, cls.table(), batch_start)

            logger.info(
                "progress for %s - last_ts: %s batch_start_ts: %s batch_complete_ts: %s",
                cls.table(),
                progress.last_ts if progress else "none",
                progress.batch_start_ts if progress else "none",
                progress.batch_complete_ts if progress else "none",
            )
            data_iter = data_extractor.extract_idr_data(cls, progress, batch_start)
            data_loaded = loader.load(data_iter, cls, batch_start, progress)
            return (loader, data_loaded)
        # Snowflake will throw a reauth error if the pipeline has been running for several hours
        # but it seems to be wrapped in a ProgrammingError.
        # Unclear the best way to handle this, it will require a bit more trial and error
        except (
            ReauthenticationRequest,
            RetryRequest,
            ForbiddenError,
            ProgrammingError,
        ) as ex:
            time_expired = datetime.now(UTC) - last_error > timedelta(seconds=10)
            if time_expired:
                error_count = 0
            error_count += 1
            if error_count < max_errors:
                last_error = datetime.now(UTC)
                logger.warning("received transient error, retrying...", exc_info=ex)
            else:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
