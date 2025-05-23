from abc import ABC, abstractmethod
from typing import Any, Iterator, Mapping, Optional
from snowflake.connector import DictCursor
import psycopg
import os
from psycopg.rows import class_row
from pydantic import BaseModel
import snowflake.connector

from model import LoadProgress, T
from timer import Timer


idr_query_timer = Timer("idr_query")
cursor_execute_timer = Timer("cursor_execute")
cursor_fetch_timer = Timer("cursor_fetch")
transform_timer = Timer("transform")


def print_timers():
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
    def extract_many(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> Iterator[list[T]]:
        pass

    def get_query(
        self,
        cls: type[T],
        fetch_query: str,
    ) -> str:
        columns = ",".join(cls.column_aliases())
        return fetch_query.replace("{COLUMNS}", columns)

    def extract_idr_data(
        self,
        cls: type[T],
        connection_string: str,
        fetch_query: str,
        table: str,
        batch_timestamp_col: str,
        update_timestamp_col: Optional[str] = None,
    ) -> Iterator[list[T]]:
        fetch_query = self.get_query(cls, fetch_query)
        progress = get_progress(connection_string, table)
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
        else:
            idr_query_timer.start()
            # Saved progress found, start processing from where we left
            update_clause = (
                f"OR {update_timestamp_col} >= %(timestamp)s"
                if update_timestamp_col is not None
                else ""
            )
            res = self.extract_many(
                cls,
                fetch_query.replace(
                    "{WHERE_CLAUSE}",
                    f"""
                    WHERE 
                        ({batch_timestamp_col} >= %(timestamp)s {update_clause})
                        AND {batch_timestamp_col} >= '{get_min_transaction_date()}' 
                    """,
                ).replace("{ORDER_BY}", "ORDER BY idr_trans_efctv_ts"),
                {"timestamp": progress.last_ts},
            )
            idr_query_timer.stop()
            return res


class PostgresExtractor(Extractor):
    def __init__(self, connection_string: str, batch_size: int):
        super().__init__()
        self.conn = psycopg.connect(connection_string)
        self.batch_size = batch_size

    def extract_many(
        self, cls: type[T], sql: str, params: Mapping[str, Any]
    ) -> Iterator[list[T]]:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)  # type: ignore
            batch: list[T] = cur.fetchmany(self.batch_size)
            while len(batch) > 0:
                yield batch
                batch = cur.fetchmany(self.batch_size)

    def extract_single(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> T | None:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)  # type: ignore
            return cur.fetchone()


class SnowflakeExtractor(Extractor):
    def __init__(self, batch_size: int):
        super().__init__()
        self.conn = snowflake.connector.connect(
            user=os.environ["IDR_USERNAME"],
            password=os.environ["IDR_PASSWORD"],
            account=os.environ["IDR_ACCOUNT"],
            warehouse=os.environ["IDR_WAREHOUSE"],
            database=os.environ["IDR_DATABASE"],
            schema=os.environ["IDR_SCHEMA"],
        )
        self.batch_size = batch_size

    def extract_many(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> Iterator[list[T]]:
        cur = None
        try:
            cursor_execute_timer.start()
            cur = self.conn.cursor(DictCursor)
            cur.execute(sql, params)
            cursor_execute_timer.stop()

            cursor_fetch_timer.start()
            # fetchmany can return list[dict] or list[tuple] but we'll only use queries that return dicts
            batch: list[dict] = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
            cursor_fetch_timer.stop()

            while len(batch) > 0:
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


def get_progress(connection_string: str, table_name: str) -> LoadProgress | None:
    return PostgresExtractor(connection_string, batch_size=1).extract_single(
        LoadProgress,
        """
        SELECT table_name, last_ts, batch_completion_ts 
        FROM idr.load_progress
        WHERE table_name = %(table_name)s
        """,
        {"table_name": table_name},
    )
