from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Any, Iterator, Mapping, TypeVar
import psycopg
from psycopg.rows import class_row
from pydantic import BaseModel
import snowflake.connector

from model import LoadProgress


T = TypeVar("T", bound=BaseModel)


class Extractor(ABC):
    @abstractmethod
    def extract_many(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> Iterator[list[T]]:
        pass

    @abstractmethod
    def extract_single(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> T | None:
        pass


class PostgresExtractor(Extractor):
    def __init__(self, connection_string: str, batch_size: int):
        super().__init__()
        self.conn = psycopg.connect(connection_string)
        self.batch_size = batch_size

    def extract_many(
        self, cls: type[T], sql: str, params: Mapping[str, Any]
    ) -> Iterator[list[T]]:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)
            batch: list[T] = cur.fetchmany(self.batch_size)
            while len(batch) > 0:
                yield batch
                batch = cur.fetchmany(self.batch_size)

    def extract_single(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> T | None:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)
            return cur.fetchone()


class SnowflakeExtractor(Extractor):
    def __init__(self, batch_size: int):
        super().__init__()
        self.conn = snowflake.connector.connect(user="", password="", account="")
        self.batch_size = batch_size

    def extract_many(
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


def get_progress(connection_string: str, table_name: str) -> LoadProgress | None:
    return PostgresExtractor(connection_string, batch_size=1).extract_single(
        LoadProgress,
        "SELECT table_name, last_id, last_timestamp FROM idr.load_progress WHERE table_name = %(table_name)s",
        {"table_name": table_name},
    )


def fetch_bene_data(
    extractor: Extractor,
    cls: type[T],
    connection_string: str,
    table: str,
    fetch_query: str,
) -> Iterator[list[T]]:
    columns = ",".join(cls.model_fields.keys())
    fetch_query = fetch_query.replace("{COLUMNS}", columns)
    progress = get_progress(connection_string, table)
    if progress is not None:
        return extractor.extract_many(
            cls,
            fetch_query.replace(
                "{WHERE_CLAUSE}",
                """
                WHERE (
                    idr_trans_efctv_ts = %(timestamp)s AND idr_updt_ts <= %(timestamp)s AND bene_sk > %(bene_sk)s
                ) OR idr_trans_efctv_ts > %(timestamp)s OR idr_updt_ts > %(timestamp)s""",
            ),
            {"timestamp": progress.last_timestamp, "bene_sk": progress.last_id},
        )

    else:
        return extractor.extract_many(
            cls, fetch_query.replace("{WHERE_CLAUSE}", ""), {}
        )
