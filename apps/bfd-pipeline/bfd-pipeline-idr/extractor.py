import logging
import os
from abc import ABC, abstractmethod
from collections.abc import Iterator, Mapping, Sequence
from datetime import datetime
from typing import Generic

import psycopg
import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from psycopg.rows import dict_row
from pydantic import TypeAdapter
from snowflake.connector import DictCursor, SnowflakeConnection

from constants import DEFAULT_MIN_DATE
from load_partition import LoadPartition
from loader import get_connection_string
from model import (
    DbType,
    LoadMode,
    LoadProgress,
    T,
    format_date_opt,
    get_min_transaction_date,
)
from settings import BATCH_SIZE, MIN_BATCH_COMPLETION_DATE
from timer import Timer

logger = logging.getLogger(__name__)


# TODO: UP046 seems to cause issues with pyright
class Extractor(ABC, Generic[T]):  # noqa: UP046
    def __init__(self, cls: type[T], partition: LoadPartition) -> None:
        self.cls = cls
        self.type_adapter = TypeAdapter(list[self.cls])
        self.partition = partition
        self.cursor_execute_timer = Timer("cursor_execute", cls, partition)
        self.cursor_fetch_timer = Timer("cursor_fetch", cls, partition)
        self.transform_timer = Timer("transform", cls, partition)

    @abstractmethod
    def extract_many(self, sql: str, params: dict[str, DbType]) -> Iterator[Sequence[T]]:
        pass

    @abstractmethod
    def reconnect(self) -> None:
        pass

    @abstractmethod
    def close(self) -> None:
        pass

    def _coalesce_dates(self, cols: list[str]) -> list[str]:
        return [f"COALESCE({col}, '{DEFAULT_MIN_DATE}')" for col in cols]

    def _greatest_col(self, cols: list[str]) -> str:
        return f"GREATEST({','.join(cols)})"

    def get_query(self, start_time: datetime, load_mode: LoadMode) -> str:
        query = self.cls.fetch_query(self.partition, start_time, load_mode)
        columns = ",".join(self.cls.column_aliases())
        columns_raw = ",".join(self.cls.columns_raw())
        return query.replace("{COLUMNS}", columns).replace("{COLUMNS_NO_ALIAS}", columns_raw)

    def extract_idr_data(
        self, progress: LoadProgress | None, start_time: datetime, load_mode: LoadMode
    ) -> Iterator[Sequence[T]]:
        is_historical = progress is None or progress.is_historical()
        fetch_query = self.get_query(start_time, load_mode)
        # GREATEST doesn't work with nulls so we need to coalesce here
        batch_timestamp_cols = self._coalesce_dates(
            self.cls.batch_timestamp_col_alias(is_historical)
        )
        update_timestamp_cols = self._coalesce_dates(self.cls.update_timestamp_col_alias())
        # We need to create batches using the most recent timestamp from all of the
        # insert/update timestamps
        batch_timestamp_clause = self._greatest_col([*batch_timestamp_cols, *update_timestamp_cols])
        min_transaction_date = get_min_transaction_date()

        batch_id_order = ""
        batch_id_clause = ""
        batch_id_col = self.cls.batch_id_col_alias()
        if batch_id_col is not None:
            batch_id_order = f", {batch_id_col}"
        logger.info("extracting %s", self.cls.table())
        order_by = f"ORDER BY {batch_timestamp_clause} {batch_id_order}"
        if progress is None:
            # No saved progress, process the whole table from the beginning
            return self.extract_many(
                fetch_query.replace(
                    "{WHERE_CLAUSE}",
                    f"WHERE ({batch_timestamp_clause} >= '{min_transaction_date}')",
                ).replace("{ORDER_BY}", order_by),
                {},
            )

        previous_batch_complete = progress.batch_complete_ts >= progress.job_start_ts
        min_batch_completion_date = format_date_opt(MIN_BATCH_COMPLETION_DATE)
        if (
            previous_batch_complete
            and min_batch_completion_date
            and progress.batch_complete_ts > min_batch_completion_date
        ):
            # If we've set a min completion date, we don't need to reprocess any batches that have
            # already completed within the given timeframe.
            # This helps for large loads that may have been interrupted recently.
            return iter([])

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

    def _transform(self, batch: list[dict[str, DbType]]) -> Sequence[T]:
        self.transform_timer.start()
        res = self.type_adapter.validate_python(
            [{k.lower(): v for k, v in row.items()} for row in batch]
        )
        self.transform_timer.stop()
        return res


class PostgresExtractor(Extractor[T]):
    def __init__(self, cls: type[T], partition: LoadPartition, load_mode: LoadMode) -> None:
        super().__init__(cls, partition)
        self.connection_string = get_connection_string(load_mode)
        self.conn = psycopg.connect(self.connection_string)

    def reconnect(self) -> None:
        self.conn = psycopg.connect(self.connection_string)

    def extract_many(
        self,
        sql: str,
        params: Mapping[str, DbType],
    ) -> Iterator[Sequence[T]]:
        logger.debug(sql)
        with self.conn.cursor(row_factory=dict_row) as cur:
            cur.execute(sql, params)  # type: ignore
            batch = cur.fetchmany(BATCH_SIZE)
            while len(batch) > 0:
                yield self._transform(batch)
                batch = cur.fetchmany(BATCH_SIZE)

    def extract_single(self, sql: str, params: dict[str, DbType]) -> T | None:
        with self.conn.cursor(row_factory=dict_row) as cur:
            cur.execute(sql, params)  # type: ignore
            res = cur.fetchone()
            if res:
                return self._transform([res])[0]
            return None

    def close(self) -> None:
        self.conn.close()


class SnowflakeExtractor(Extractor[T]):
    def __init__(self, cls: type[T], partition: LoadPartition) -> None:
        super().__init__(cls, partition)
        self.conn = SnowflakeExtractor._connect()

    def reconnect(self) -> None:
        self.conn = SnowflakeExtractor._connect()

    @staticmethod
    def _connect() -> SnowflakeConnection:
        private_key = serialization.load_pem_private_key(
            os.environ["IDR_PRIVATE_KEY"].encode(),
            password=None,
            backend=default_backend(),
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

    def extract_many(
        self,
        sql: str,
        params: dict[str, DbType],
    ) -> Iterator[Sequence[T]]:
        cur = None
        logger.debug(sql)
        try:
            self.cursor_execute_timer.start()
            cur = self.conn.cursor(DictCursor)
            cur.execute(sql, params)
            self.cursor_execute_timer.stop()

            self.cursor_fetch_timer.start()
            # fetchmany can return list[dict] or list[tuple] but we'll only use
            # queries that return dicts
            batch: list[dict[str, DbType]] = cur.fetchmany(BATCH_SIZE)
            self.cursor_fetch_timer.stop()

            while len(batch) > 0:  # type: ignore
                yield self._transform(batch)

                self.cursor_fetch_timer.start()
                batch = cur.fetchmany(BATCH_SIZE)
                self.cursor_fetch_timer.stop()
            return

        finally:
            if cur:
                cur.close()

    def close(self) -> None:
        self.conn.close()
