import logging
import os
from abc import ABC, abstractmethod
from collections.abc import Iterator, Mapping
from datetime import UTC, datetime

import psycopg
import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from psycopg.rows import class_row
from snowflake.connector import DictCursor, SnowflakeConnection

from constants import DEFAULT_MIN_DATE
from hamilton_timer import Timer
from model import DbType, LoadProgress, T

cursor_execute_timer = Timer("cursor_execute")
cursor_fetch_timer = Timer("cursor_fetch")
transform_timer = Timer("transform")

logger = logging.getLogger("pipeline_worker")

def get_min_transaction_date() -> datetime:
    min_date = os.environ.get("PIPELINE_MIN_TRANSACTION_DATE")
    if min_date is not None:
        return datetime.strptime(min_date, "%Y-%m-%d").replace(tzinfo=UTC)
    return datetime.strptime("0001-01-01", "%Y-%m-%d").replace(tzinfo=UTC)


class Extractor(ABC):
    @abstractmethod
    def extract_many(self, cls: type[T], sql: str, params: dict[str, DbType]) -> Iterator[list[T]]:
        pass

    def _coalesce_dates(self, cols: list[str]) -> list[str]:
        return [f"COALESCE({col}, '{DEFAULT_MIN_DATE}')" for col in cols]

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
        # GREATEST doesn't work with nulls so we need to coalesce here
        batch_timestamp_cols = self._coalesce_dates(cls.batch_timestamp_col_alias(is_historical))
        update_timestamp_cols = self._coalesce_dates(cls.update_timestamp_col_alias())
        # We need to create batches using the most recent timestamp from all of the
        # insert/update timestamps
        batch_timestamp_clause = self._greatest_col([*batch_timestamp_cols, *update_timestamp_cols])
        min_transaction_date = get_min_transaction_date()

        batch_id_order = ""
        batch_id_clause = ""
        batch_id_col = cls.batch_id_col_alias()
        if batch_id_col is not None:
            batch_id_order = f", {batch_id_col}"
        logger.info("extracting %s", cls.table())
        order_by = f"ORDER BY {batch_timestamp_clause} {batch_id_order}"
        if progress is None:
            # No saved progress, process the whole table from the beginning
            return self.extract_many(
                cls,
                fetch_query.replace(
                    "{WHERE_CLAUSE}",
                    f"WHERE ({batch_timestamp_clause} >= '{min_transaction_date}')",
                ).replace("{ORDER_BY}", order_by),
                {},
            )

        previous_batch_complete = progress.batch_complete_ts >= progress.batch_start_ts
        # If we've completed the last batch, there shouldn't be any additional records
        # with the same timestamp/id.
        # Additionally, if there's a batch_id column, records with the same timestamp will be
        # filtered by the batch_id filter.
        filter_op = ">" if previous_batch_complete or batch_id_col is not None else ">="
        # insertion timestamps aren't always representative of the time the data is available in
        # Snowflake, so we should always start loading from the most recent timestamp
        # that we've already fetched
        compare_timestamp = max(min_transaction_date, progress.last_ts)

        if batch_id_col is not None:
            batch_id_clause = f"""
                OR (
                    {batch_timestamp_clause} = %(timestamp)s 
                    AND {batch_id_col} {filter_op} {progress.last_id}
                )"""

        # Saved progress found, start processing from where we left off
        return self.extract_many(
            cls,
            fetch_query.replace(
                "{WHERE_CLAUSE}",
                f"""
                    WHERE (
                        {batch_timestamp_clause} {filter_op} %(timestamp)s
                        {batch_id_clause}
                    )
                    """,
            ).replace("{ORDER_BY}", order_by),
            {"timestamp": compare_timestamp},
        )


class PostgresExtractor(Extractor):
    def __init__(self, connection_string: str, batch_size: int) -> None:
        super().__init__()
        self.connection_string = connection_string
        self.batch_size = batch_size

    def extract_many(
            self, cls: type[T], sql: str, params: Mapping[str, DbType]
    ) -> Iterator[list[T]]:
        conn = psycopg.connect(self.connection_string)
        try:
            with conn.cursor(row_factory=class_row(cls)) as cur:
                cur.execute(sql, params)  # type: ignore
                batch: list[T] = cur.fetchmany(self.batch_size)
                while len(batch) > 0:
                    yield batch
                    batch = cur.fetchmany(self.batch_size)
        finally:
            if conn:
                conn.close()

    def extract_single(self, cls: type[T], sql: str, params: dict[str, DbType]) -> T | None:
        conn = psycopg.connect(self.connection_string)
        try:
            with conn.cursor(row_factory=class_row(cls)) as cur:
                cur.execute(sql, params)  # type: ignore
                return cur.fetchone()
        finally:
            if conn:
                conn.close()


class SnowflakeExtractor(Extractor):
    def __init__(self, batch_size: int) -> None:
        super().__init__()
        self.batch_size = batch_size

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
        conn = self._connect()

        try:
            cursor_execute_timer.start()
            cur = conn.cursor(DictCursor)
            cur.execute(sql, params)
            cursor_execute_timer.stop(cls)

            cursor_fetch_timer.start()
            # fetchmany can return list[dict] or list[tuple] but we'll only use
            # queries that return dicts
            batch: list[dict[str, DbType]] = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
            cursor_fetch_timer.stop(cls)

            while len(batch) > 0:  # type: ignore
                transform_timer.start()
                data = [cls(**{k.lower(): v for k, v in row.items()}) for row in batch]
                transform_timer.stop(cls)

                yield data

                cursor_fetch_timer.start()
                batch = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
                cursor_fetch_timer.stop(cls)
            return

        finally:
            if cur:
                cur.close()
            if conn:
                conn.close()