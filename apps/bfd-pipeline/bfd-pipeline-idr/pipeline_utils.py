import logging
import sys
from datetime import datetime
import time

from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from loader import PostgresLoader
from extractor import Extractor, PostgresExtractor, SnowflakeExtractor

from model import (
    LoadProgress,
    T,
)

def configure_worker_logger() -> logging.Logger:
    logger = logging.getLogger("pipeline_worker")
    if not logger.handlers:
        console_handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
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

def make_extractor(mode: str, connection_string: str, batch_size: int) -> Extractor:
    if mode == "local":
        return PostgresExtractor(connection_string=connection_string,
                                 batch_size=batch_size)
    elif mode == "synthetic":
        return PostgresExtractor(connection_string=connection_string,
                                 batch_size=batch_size)
    else:
        return SnowflakeExtractor(batch_size=batch_size)


def extract_and_load(
        cls: type[T],
        make_extractor: Extractor,
        connection_string: str,
        mode: str,
        batch_size: int
) -> tuple[PostgresLoader, bool]:
    logger = configure_worker_logger()
    data_extractor = make_extractor(mode, connection_string, batch_size)

    logger.info("loading %s", cls.table())
    batch_start = datetime.now()
    progress = get_progress(connection_string, cls.table(), batch_start)

    logger.info(
        "progress for %s - last_ts: %s batch_start_ts: %s batch_complete_ts: %s",
        cls.table(),
        progress.last_ts if progress else "none",
        progress.batch_start_ts if progress else "none",
        progress.batch_complete_ts if progress else "none",
    )
    max_attempts = 5
    loader = PostgresLoader(connection_string)
    for attempt in range(max_attempts):
        try:
            data_iter = data_extractor.extract_idr_data(cls, progress, batch_start)
            data_loaded = loader.load(data_iter, cls, batch_start, progress)
            return (loader, data_loaded)
        # Snowflake will throw a reauth error if the pipeline has been running for several hours
        # but it seems to be wrapped in a ProgrammingError.
        # Unclear the best way to handle this, it will require a bit more trial and error
        except (ReauthenticationRequest, RetryRequest) as ex:
            logger.warning("received transient error, retrying...", exc_info=ex)
            data_extractor.reconnect()
            if attempt == max_attempts - 1:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
    return (loader, False)