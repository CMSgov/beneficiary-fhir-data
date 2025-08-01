import os
from abc import ABC, abstractmethod
from collections.abc import Iterator, Mapping
from datetime import date, datetime

import psycopg
import snowflake.connector
from psycopg.rows import class_row
from snowflake.connector import DictCursor

from constants import DEFAULT_MAX_DATE
from model import LoadProgress, T
from timer import Timer

idr_query_timer = Timer("idr_query")
cursor_execute_timer = Timer("cursor_execute")
cursor_fetch_timer = Timer("cursor_fetch")
transform_timer = Timer("transform")

type DbType = str | float | int | bool | date | datetime


def print_timers() -> None:
    idr_query_timer.print_results()
    cursor_execute_timer.print_results()
    cursor_fetch_timer.print_results()
    transform_timer.print_results()


def get_min_transaction_date() -> str:
    min_date = os.environ.get("PIPELINE_MIN_TRANSACTION_DATE")
    if min_date is not None:
        return min_date
    return "0001-01-01"


class Extractor(ABC):
    @abstractmethod
    def extract_many(self, cls: type[T], sql: str, params: dict[str, DbType]) -> Iterator[list[T]]:
        pass

    def get_query(self, cls: type[T], is_historical: bool) -> str:
        query = cls.fetch_query(is_historical)
        columns = ",".join(cls.column_aliases())
        columns_raw = ",".join(cls.columns_raw())
        return query.replace("{COLUMNS}", columns).replace("{COLUMNS_NO_ALIAS}", columns_raw)

    def extract_idr_data(self, cls: type[T], progress: LoadProgress | None) -> Iterator[list[T]]:
        is_historical = progress is None or progress.is_historical()
        fetch_query = self.get_query(cls, is_historical)
        batch_timestamp_col = cls.batch_timestamp_col_alias(is_historical)
        update_timestamp_col = cls.update_timestamp_col_alias()
        if progress is None:
            idr_query_timer.start()
            # No saved progress, process the whole table from the beginning
            res = self.extract_many(
                cls,
                fetch_query.replace(
                    "{WHERE_CLAUSE}",
                    f"WHERE {batch_timestamp_col} >= '{get_min_transaction_date()}'",
                ).replace("{ORDER_BY}", f"ORDER BY {batch_timestamp_col}"),
                {},
            )
            idr_query_timer.stop()
            return res

        previous_batch_complete = progress.batch_completion_ts != DEFAULT_MAX_DATE
        op = ">" if previous_batch_complete else ">="
        idr_query_timer.start()
        # Saved progress found, start processing from where we left
        update_clause = (
            f"OR {update_timestamp_col} IS NOT NULL AND {update_timestamp_col} {op} %(timestamp)s"
            if update_timestamp_col is not None
            else ""
        )
        res = self.extract_many(
            cls,
            fetch_query.replace(
                "{WHERE_CLAUSE}",
                f"""
                    WHERE 
                        ({update_timestamp_col} IS NOT NULL 
                            AND {batch_timestamp_col} {op} %(timestamp)s {update_clause})
                        AND {batch_timestamp_col} {op} '{get_min_transaction_date()}' 
                    """,
            ).replace("{ORDER_BY}", f"ORDER BY {batch_timestamp_col}"),
            {"timestamp": progress.last_ts},
        )
        idr_query_timer.stop()
        return res


class PostgresExtractor(Extractor):
    def __init__(self, connection_string: str, batch_size: int) -> None:
        super().__init__()
        self.conn = psycopg.connect(connection_string)
        self.batch_size = batch_size

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
        self.conn = snowflake.connector.connect(  # type: ignore
            user=os.environ["IDR_USERNAME"],
            password=os.environ["IDR_PASSWORD"],
            account=os.environ["IDR_ACCOUNT"],
            warehouse=os.environ["IDR_WAREHOUSE"],
            database=os.environ["IDR_DATABASE"],
            schema=os.environ["IDR_SCHEMA"],
        )
        self.batch_size = batch_size

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
        finally:
            if cur:
                cur.close()
