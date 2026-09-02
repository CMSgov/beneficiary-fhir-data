import os
import sys
from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
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

    @abstractmethod
    def get_bene_sks(self) -> list[int]: ...

    @abstractmethod
    def get_cntrct_pbp_nums(self) -> list[dict[str, Any]]: ...

    @abstractmethod
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

    def get_bene_sks(self) -> list[int]:
        return []

    def get_cntrct_pbp_nums(self) -> list[dict[str, Any]]:
        return []

    def close(self) -> None:
        pass


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
        self._column_types_cache: dict[str, dict[str, str]] = {}

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
        )

    def resolve_target_table(self, table_name: str) -> tuple[str, str, str]:
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

    def _get_column_types(self, database: str, schema: str, table_name: str) -> dict[str, str]:
        qualified_table = f"{database}.{schema}.{table_name}"
        if qualified_table not in self._column_types_cache:
            with self.conn.cursor() as cur:
                cur.execute(
                    f"""
                    SELECT column_name, data_type, numeric_scale
                    FROM "{database}".information_schema.columns
                    WHERE table_schema = %(schema)s AND table_name = %(table)s
                    """,
                    {"schema": schema, "table": table_name},
                )
                self._column_types_cache[qualified_table] = {
                    row[0]: {"data_type": row[1], "scale": row[2]} for row in cur.fetchall()
                }
        return self._column_types_cache[qualified_table]

    def _coerce_dataframe_types(
        self, df: pd.DataFrame, database: str, schema: str, table_name: str
    ) -> pd.DataFrame:
        col_types = self._get_column_types(database, schema, table_name)
        for col in df.columns:
            metadata = col_types.get(col.upper())
            data_type = metadata["data_type"]
            scale = metadata["scale"]
            if data_type == "TIMESTAMP_TZ":
                df[col] = pd.to_datetime(df[col], errors="coerce", utc=True).astype(
                    "datetime64[ns, UTC]"
                )
            elif data_type == "NUMBER":
                df[col] = pd.to_numeric(
                    df[col],
                    errors="coerce",
                )
                if scale == 0:
                    df[col] = df[col].where(df[col].notnull(), None).astype(object)
            elif data_type == "TEXT":
                df[col] = df[col].apply(
                    lambda x: None
                    if pd.isna(x) or str(x).strip().lower() in ["nan", "none"]
                    else str(x)
                )
                df[col] = df[col].astype(object)
        return df

    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,  # noqa: ARG002
        truncate: bool = False,
    ) -> None:
        if not data:
            return

        resolved_table_name, database, schema = self.resolve_target_table(table_name)
        df = pd.DataFrame(data)

        # filter out columns not in our schema. Only used in generation for certain fields we
        # actually use
        col_types = self._get_column_types(database, schema, resolved_table_name)
        known_columns = {col.upper() for col in col_types}
        df = df[[col for col in df.columns if col.upper() in known_columns]]

        # coerce the types so Pandas doesn't guess the types
        df = self._coerce_dataframe_types(df, database, schema, resolved_table_name)

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
            use_logical_type=True,
        )

        if not success:
            raise RuntimeError(f"write_pandas reported failures writing to {table_name}")
        print(f"Wrote {num_rows} rows to {table_name}")

    def get_bene_sks(self) -> list[int]:
        resolved_table_name, database, schema = self.resolve_target_table("_BENE_HSTRY")
        qualified_table = f'"{database}"."{schema}"."{resolved_table_name}"'
        with self.conn.cursor() as cur:
            cur.execute(
                f"""
                SELECT DISTINCT BENE_SK
                FROM {qualified_table}
                ORDER BY BENE_SK
                """
            )
            return [int(row[0]) for row in cur.fetchall()]

    def get_cntrct_pbp_nums(self) -> list[dict[str, Any]]:
        resolved_table_name, database, schema = self.resolve_target_table("_CNTRCT_PBP_NUM")
        qualified_table = f'"{database}"."{schema}"."{resolved_table_name}"'
        with self.conn.cursor() as cur:
            cur.execute(
                f"""
                SELECT
                    CNTRCT_PBP_SK,
                    CNTRCT_NUM,
                    CNTRCT_PBP_NUM
                FROM {qualified_table}
                ORDER BY CNTRCT_PBP_SK
                """
            )
            columns = [col[0] for col in cur.description]
            return [dict(zip(columns, row, strict=True)) for row in cur.fetchall()]

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
