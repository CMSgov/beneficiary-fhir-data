from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Any, Iterator, Mapping, TypeVar
import psycopg
from psycopg.rows import class_row
from pydantic import BaseModel
import snowflake.connector


T = TypeVar("T", bound=BaseModel)


class Extractor(ABC):
    @abstractmethod
    def extract(
        self, cls: type[T], sql: str, params: dict[str, Any]
    ) -> Iterator[list[T]]:
        pass


class PostgresExtractor(Extractor):
    def __init__(self, connection_string: str, batch_size: int):
        super().__init__()
        self.conn = psycopg.connect(connection_string)
        self.batch_size = batch_size

    def extract(
        self, cls: type[T], sql: str, params: Mapping[str, Any]
    ) -> Iterator[list[T]]:
        with self.conn.cursor(row_factory=class_row(cls)) as cur:
            cur.execute(sql, params)
            batch: list[T] = cur.fetchmany(self.batch_size)
            while len(batch) > 0:
                yield batch
                batch = cur.fetchmany(self.batch_size)


class SnowflakeExtractor(Extractor):
    def __init__(self, batch_size: int):
        super().__init__()
        self.conn = snowflake.connector.connect(user="", password="", account="")
        self.batch_size = batch_size

    def extract(
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
