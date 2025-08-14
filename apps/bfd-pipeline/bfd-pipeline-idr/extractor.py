import logging
import os
from abc import ABC, abstractmethod
from collections.abc import Iterator, Mapping
from datetime import UTC, date, datetime, timezone

import psycopg
import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from psycopg.rows import class_row
from snowflake.connector import DictCursor, SnowflakeConnection

from model import LoadProgress, T
from timer import Timer

idr_query_timer = Timer("idr_query")
cursor_execute_timer = Timer("cursor_execute")
cursor_fetch_timer = Timer("cursor_fetch")
transform_timer = Timer("transform")

logger = logging.getLogger(__name__)

type DbType = str | float | int | bool | date | datetime


def print_timers() -> None:
    idr_query_timer.print_results()
    cursor_execute_timer.print_results()
    cursor_fetch_timer.print_results()
    transform_timer.print_results()


def get_min_transaction_date() -> datetime:
    min_date = os.environ.get("PIPELINE_MIN_TRANSACTION_DATE")
    if min_date is not None:
        return datetime.strptime(min_date, "%Y-%m-%d").replace(tzinfo=UTC)
    return datetime.strptime("0001-01-01", "%Y-%m-%d").replace(tzinfo=UTC)


class Extractor(ABC):
    @abstractmethod
    def extract_many(self, cls: type[T], sql: str, params: dict[str, DbType]) -> Iterator[list[T]]:
        pass

    @abstractmethod
    def reconnect(self) -> None:
        pass

    def _greatest_col(self, cols: list[str]) -> str:
        return f"GREATEST({','.join(cols)})"

    def get_query(self, cls: type[T], is_historical: bool, start_time: datetime) -> str:
        query = cls.fetch_query(is_historical, start_time)
        columns = ",".join(cls.column_aliases())
        columns_raw = ",".join(cls.columns_raw())
        return query.replace("{COLUMNS}", columns).replace("{COLUMNS_NO_ALIAS}", columns_raw)

    def extract_idr_data(
        self, cls: type[T], progress: LoadProgress | None, start_time: datetime
    ) -> Iterator[list[T]]:
        is_historical = progress is None or progress.is_historical()
        fetch_query = self.get_query(cls, is_historical, start_time)
        batch_timestamp_cols = cls.batch_timestamp_col_alias(is_historical)
        update_timestamp_cols = cls.update_timestamp_col_alias()
        # We need to create batches using the most recent timestamp from all of the
        # insert/update timestamps
        batch_timestamp_clause = self._greatest_col([*batch_timestamp_cols, *update_timestamp_cols])
        min_transaction_date = get_min_transaction_date()
        logger.info("extracting %s", cls.table())
        if progress is None:
            idr_query_timer.start()
            # No saved progress, process the whole table from the beginning
            res = self.extract_many(
                cls,
                fetch_query.replace(
                    "{WHERE_CLAUSE}",
                    f"WHERE {batch_timestamp_clause} >= '{min_transaction_date}'",
                ).replace("{ORDER_BY}", f"ORDER BY {batch_timestamp_clause}"),
                {},
            )
            idr_query_timer.stop()
            return res

        previous_batch_complete = progress.batch_complete_ts >= progress.batch_start_ts
        logger.info("previous batch complete: %s", previous_batch_complete)

        compare_timestamp = progress.batch_start_ts if previous_batch_complete else progress.last_ts
        compare_timestamp = max(min_transaction_date, compare_timestamp)
        idr_query_timer.start()
        # Saved progress found, start processing from where we left off
        res = self.extract_many(
            cls,
            fetch_query.replace(
                "{WHERE_CLAUSE}",
                f"""
                    WHERE
                        {batch_timestamp_clause} >= %(timestamp)s
                    """,
            ).replace("{ORDER_BY}", f"ORDER BY {batch_timestamp_clause}"),
            {"timestamp": compare_timestamp},
        )
        idr_query_timer.stop()
        return res


class PostgresExtractor(Extractor):
    def __init__(self, connection_string: str, batch_size: int) -> None:
        super().__init__()
        self.connection_string = connection_string
        self.conn = psycopg.connect(connection_string)
        self.batch_size = batch_size

    def reconnect(self) -> None:
        self.conn = psycopg.connect(self.connection_string)

    def extract_many(
        self, cls: type[T], sql: str, params: Mapping[str, DbType]
    ) -> Iterator[list[T]]:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)  # type: ignore
            batch: list[T] = cur.fetchmany(self.batch_size)
            while len(batch) > 0:
                yield batch
                batch = cur.fetchmany(self.batch_size)

    def extract_single(self, cls: type[T], sql: str, params: dict[str, DbType]) -> T | None:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)  # type: ignore
            return cur.fetchone()


class SnowflakeExtractor(Extractor):
    def __init__(self, batch_size: int) -> None:
        super().__init__()

        self.conn = SnowflakeExtractor._connect()
        self.batch_size = batch_size

    def reconnect(self) -> None:
        SnowflakeExtractor._connect()

    @staticmethod
    def _connect() -> SnowflakeConnection:
        private_key = serialization.load_pem_private_key(
            os.environ["IDR_PRIVATE_KEY"].encode(), password=None, backend=default_backend()
        )
        private_key_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        return snowflake.connector.connect(  # type: ignore
            user=os.environ["IDR_USERNAME"],
            private_key=private_key_bytes,
            account=os.environ["IDR_ACCOUNT"],
            warehouse=os.environ["IDR_WAREHOUSE"],
            database=os.environ["IDR_DATABASE"],
            schema=os.environ["IDR_SCHEMA"],
        )

    def extract_many(self, cls: type[T], sql: str, params: dict[str, DbType]) -> Iterator[list[T]]:
        cur = None

        try:
            cursor_execute_timer.start()
            cur = self.conn.cursor(DictCursor)
            cur.execute(sql, params)
            cursor_execute_timer.stop()

            cursor_fetch_timer.start()
            # fetchmany can return list[dict] or list[tuple] but we'll only use
            # queries that return dicts
            batch: list[dict[str, DbType]] = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
            cursor_fetch_timer.stop()

            while len(batch) > 0:  # type: ignore
                transform_timer.start()
                data = [cls(**{k.lower(): v for k, v in row.items()}) for row in batch]
                transform_timer.stop()

                yield data

                cursor_fetch_timer.start()
                batch = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
                cursor_fetch_timer.stop()
            return

        finally:
            if cur:
                cur.close()
