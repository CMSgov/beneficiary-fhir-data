from __future__ import annotations

import os
import sys
from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import pandas as pd
import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from snowflake.connector import SnowflakeConnection
from snowflake.connector.pandas_tools import write_pandas

ALL_KEYS = "all_keys"
_SYNTHETIC_PREX = "SYNTHETIC"
_TABLE_PREFIX = "V2_MDCR"


@dataclass
class TableTarget:
    name: str | None = None
    schema: str | None = None
    database: str | None = None


_TABLE_OVERRIDES: dict[str, TableTarget] = {
    "SYNTHETIC_PRAUC": TableTarget(name="PRAUC", schema="CMS_EDP_VIEW_CVM_PRAU_PRD")
}


class OutputDestinationWriter(ABC):
    @abstractmethod
    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,
        truncate: bool = False,
    ) -> None: ...

    def close(self) -> None: ...


class CsvWriter(OutputDestinationWriter):
    def __init__(self, out_dir: str = "out") -> None:
        self.out_dir = Path(out_dir)
        self.out_dir.mkdir(exist_ok=True)

    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,
        truncate: bool = False,  # noqa: ARG002
    ) -> None:
        df = pd.json_normalize(data)
        if cols != ALL_KEYS:
            df = df[cols]
        df.to_csv(self.out_dir / f"{table_name}.csv", index=False)


class SnowflakeWriter(OutputDestinationWriter):
    def __init__(
        self,
        chunk_size: int = 20000,
        parallel: int = 8,
        compression: str = "snappy",
    ) -> None:
        self.chunk_size = chunk_size
        self.parallel = parallel
        self.compression = compression
        self.database = _require_env("IDR_DATABASE")
        self.schema = _require_env("IDR_SCHEMA")
        self.conn = self._connect(
            account=_require_env("IDR_ACCOUNT"),
            user=_require_env("IDR_USERNAME"),
            private_key=_require_env("IDR_PRIVATE_KEY"),
            warehouse=_require_env("IDR_WAREHOUSE"),
        )

    def _connect(
        self, account: str, user: str, private_key: str, warehouse: str
    ) -> SnowflakeConnection:
        pk = serialization.load_pem_private_key(
            private_key.encode(),
            password=None,
            backend=default_backend(),
        )
        pk_bytes = pk.private_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        return snowflake.connector.connect(  # type: ignore
            user=user,
            private_key=pk_bytes,
            account=account,
            warehouse=warehouse,
            database=self.database,
            schema=self.schema,
        )

    def _resolve_target_table(self, table_name: str) -> tuple[str, str, str]:
        override = _TABLE_OVERRIDES.get(table_name)
        if override is not None:
            return (
                override.name or table_name,
                override.database or self.database,
                override.schema or self.schema,
            )
        return (
            _TABLE_PREFIX + table_name.removeprefix(_SYNTHETIC_PREX),
            self.database,
            self.schema,
        )

    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,  # noqa: ARG002
        truncate: bool = False,
    ) -> None:
        if not data:
            return

        resolved_table_name, database, schema = self._resolve_target_table(table_name)
        df = pd.DataFrame(data)

        table_name.replace("SYNTHETIC", "V2_MDCR")

        success, _, num_rows, _ = write_pandas(
            conn=self.conn,
            df=df,
            table_name=resolved_table_name,
            database=database,
            schema=schema,
            overwrite=truncate,
            chunk_size=self.chunk_size,
            parallel=self.parallel,
            compression=self.compression,
        )

        if not success:
            raise RuntimeError(f"write_pandas reported failures writing to {table_name}")
        print(f"Wrote {num_rows} rows to {table_name}")

    def close(self) -> None:
        self.conn.close()


def _require_env(name: str) -> str:
    val = os.environ.get(name)
    if not val:
        print(
            f"Missing required env variable {name}. "
            + "Source load-credentials.sh with BFD_ENV set to synthetic before running."
        )
        sys.exit(1)
    return val
