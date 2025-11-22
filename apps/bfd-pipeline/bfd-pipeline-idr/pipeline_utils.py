import logging
import time
from datetime import UTC, datetime, timedelta

from snowflake.connector import ProgrammingError
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from extractor import PostgresExtractor, SnowflakeExtractor
from load_partition import DEFAULT_PARTITION, LoadPartition
from loader import PostgresLoader
from model import (
    LoadProgress,
    T,
)

console_handler = logging.StreamHandler()
formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
console_handler.setFormatter(formatter)
logging.basicConfig(level=logging.INFO, handlers=[console_handler])

logger = logging.getLogger(__name__)


def get_progress(
    connection_string: str,
    table_name: str,
    start_time: datetime,
    partition: LoadPartition,
) -> LoadProgress | None:
    return PostgresExtractor(
        connection_string=connection_string, batch_size=1, cls=LoadProgress, partition=partition
    ).extract_single(
        LoadProgress.fetch_query(partition, False, start_time),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load(
    cls: type[T],
    connection_string: str,
    mode: str,
    batch_size: int,
    batch_start: datetime,
    partition: LoadPartition | None = None,
) -> bool:
    partition = partition or DEFAULT_PARTITION
    if mode == "local" or mode == "synthetic":
        data_extractor = PostgresExtractor(
            connection_string=connection_string, batch_size=batch_size, cls=cls, partition=partition
        )
    else:
        data_extractor = SnowflakeExtractor(batch_size=batch_size, cls=cls, partition=partition)

    logger.info("loading %s", cls.table())
    last_error = datetime.min.replace(tzinfo=UTC)
    loader = PostgresLoader(connection_string)
    error_count = 0
    max_errors = 3

    while True:
        try:
            progress = get_progress(connection_string, cls.table(), batch_start, partition)

            logger.info(
                "progress for %s - last_ts: %s batch_start_ts: %s batch_complete_ts: %s",
                cls.table(),
                progress.last_ts if progress else "none",
                progress.batch_start_ts if progress else "none",
                progress.batch_complete_ts if progress else "none",
            )
            data_iter = data_extractor.extract_idr_data(progress, batch_start)
            return loader.load(data_iter, cls, batch_start, partition, progress)
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
                data_extractor.reconnect()
            else:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
