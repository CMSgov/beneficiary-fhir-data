from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Any, Iterator, Mapping, TypeVar
import psycopg
from psycopg.rows import class_row
from pydantic import BaseModel
import snowflake.connector


T = TypeVar("T", bound=BaseModel)


class Fetcher(ABC):
    @abstractmethod
    def fetch(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> Iterator[list[T]]:
        pass


class PostgresFetcher(Fetcher):
    def __init__(self, batch_size: int):
        super().__init__()
        self.conn = psycopg.connect(
            "host=localhost dbname=idr user=bfd password=InsecureLocalDev"
        )
        self.batch_size = batch_size

    def fetch(
        self, cls: type[T], sql: str, params: Mapping[str, Any]
    ) -> Iterator[list[T]]:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)
            batch: list[T] = cur.fetchmany(self.batch_size)
            while len(batch) > 0:
                yield batch
                batch = cur.fetchmany(self.batch_size)


class SnowflakeFetcher(Fetcher):
    def __init__(self, batch_size: int):
        super().__init__()
        self.conn = snowflake.connector.connect(user="", password="", account="")
        self.batch_size = batch_size

    def fetch(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> Iterator[list[T]]:
        try:
            cur = self.conn.cursor()
            cur.execute(sql, params)

            # fetchmany can return list[dict] or list[tuple] but we'll only use queries that return dicts
            batch: list[dict] = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
            while len(batch) > 0:
                data = [cls(**row) for row in batch]
                yield data

                batch = cur.fetchmany(self.batch_size)  # type: ignore[assignment]
        finally:
            cur.close()


def copy_data(
    conn: psycopg.Connection,
    table: str,
    temp_table: str,
    primary_key: str,
    exclude_cols: list[str],
    insert_cols: list[str],
    fetch_results: Iterator[list[T]],
):

    insert_cols.sort()
    cols_str = ", ".join(insert_cols)

    update_set = ", ".join(
        [f"{v}=EXCLUDED.{v}" for v in insert_cols if v != primary_key]
    )
    ts = datetime.now(timezone.utc)
    with conn.cursor() as cur:
        for results in fetch_results:
            cur.execute(
                f"CREATE TEMPORARY TABLE {temp_table} (LIKE {table}) ON COMMIT DROP"
            )
            exclude_cols = ["created_ts", "updated_ts"] + exclude_cols
            for col in exclude_cols:
                cur.execute(f"ALTER TABLE {temp_table} DROP COLUMN {col}")

            with cur.copy(f"COPY {temp_table} ({cols_str}) FROM STDIN") as copy:
                for row in results:
                    model_dump = row.model_dump()
                    copy.write_row([model_dump[k] for k in insert_cols])
            cur.execute(
                f"""
                INSERT INTO {table}({cols_str}, created_ts)
                SELECT {cols_str}, %(ts)s FROM {temp_table}
                ON CONFLICT ({primary_key}) DO UPDATE SET {update_set}, updated_ts=%(ts)s
                """,
                {"ts": ts},
            )
            conn.commit()
