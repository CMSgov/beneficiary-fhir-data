import datetime
import os
import sys
import time
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass
from enum import StrEnum, auto
from pathlib import Path
from typing import Any

import numpy as np
import pandas as pd
import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from snowflake.connector import SnowflakeConnection
from snowflake.connector.pandas_tools import write_pandas
from snowflake.snowpark import Session
from snowflake.snowpark.functions import col, when_matched, when_not_matched

from constants import (
    BENE_DUAL,
    BENE_ENTLMT,
    BENE_ENTLMT_RSN,
    BENE_HSTRY,
    BENE_LIS_CMBND,
    BENE_MAPD_ENRLMT,
    BENE_MAPD_ENRLMT_RX,
    BENE_MBI_ID,
    BENE_STUS,
    BENE_TP,
    BENE_XREF,
    CLM,
    CLM_DCMTN,
    CLM_DT_SGNTR,
    CLM_FISS,
    CLM_INSTNL,
    CLM_LCTN_HSTRY,
    CLM_LINE,
    CLM_LINE_DCMTN,
    CLM_LINE_INSTNL,
    CLM_LINE_PRFNL,
    CLM_LINE_RX,
    CLM_PRFNL,
    CLM_PROD,
    CLM_RLT_COND_SGNTR_MBR,
    CLM_VAL,
    CNTRCT_PBP_CNTCT,
    CNTRCT_PBP_NUM,
    IDR_INSRT_TS,
    IDR_UPDT_TS,
    PRAUC,
    PRVDR_HSTRY,
)
from row_adapter import RowAdapter

ALL_KEYS = "all_keys"
_SYNTHETIC_PREX = "SYNTHETIC"
_TABLE_PREFIX = "V2_MDCR"


@dataclass
class TableTarget:
    name: str | None = None
    schema: str | None = None
    database: str | None = None


_TABLE_OVERRIDES: dict[str, TableTarget] = {
    PRAUC: TableTarget(name="PRAUC", schema="CMS_EDP_VIEW_CVM_PRAU_PRD")
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

    def close(self) -> None:
        pass


class SnowflakeWriter(OutputDestinationWriter):
    def __init__(
        self,
        parallel: int = 8,
        compression: str = "snappy",
    ) -> None:
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
        self.session = Session.builder.configs({"connection": self.conn}).create()
        self._column_types_cache: dict[str, dict[str, Any]] = {}

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
            role="TEST_SERVICE_USER",
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

    def qualified_session_table(self, table_name: str) -> Any:
        resolved_table_name, database, schema = self.resolve_target_table(table_name)
        return self.session.table(f'"{database}"."{schema}"."{resolved_table_name}"')

    def iter_bene_sk_batches(self, batch_size: int) -> Iterator[list[int]]:
        df = self.qualified_session_table(BENE_HSTRY).select("BENE_SK").distinct().sort("BENE_SK")
        buffer: list[int] = []
        for pdf in df.to_pandas_batches():
            buffer.extend(int(bene_sk) for bene_sk in pdf["BENE_SK"])
            while len(buffer) >= batch_size:
                yield buffer[:batch_size]
                buffer = buffer[batch_size:]
        if buffer:
            print(f"length of buffer {len(buffer)}")
            yield buffer

    def get_cntrct_pbp_nums(self) -> list[dict[str, Any]]:
        rows = self.qualified_session_table(CNTRCT_PBP_NUM).collect()
        return [row.as_dict() for row in rows]

    def get_cntrct_pbp_cntcts(self) -> list[dict[str, Any]]:
        rows = self.qualified_session_table(CNTRCT_PBP_CNTCT).collect()
        return [row.as_dict() for row in rows]

    def get_provider_histories(self) -> list[dict[str, Any]]:
        rows = self.qualified_session_table(PRVDR_HSTRY).collect()
        return [row.as_dict() for row in rows]

    def get_bene_sk_to_mbi(self, bene_sks: list[int]) -> list[RowAdapter]:
        rows = (
            self.qualified_session_table(BENE_HSTRY)
            .filter(col("BENE_SK").isin(bene_sks))
            .select("BENE_SK", "BENE_MBI_ID", "IDR_LTST_TRANS_FLG")
            .collect()
        )

        return [
            RowAdapter(
                {
                    "BENE_SK": str(row["BENE_SK"]),
                    "BENE_MBI_ID": row["BENE_MBI_ID"],
                    "IDR_LTST_TRANS_FLG": row["IDR_LTST_TRANS_FLG"],
                },
                loaded_from_file=True,
            )
            for row in rows
            if row["BENE_MBI_ID"]
        ]

    def _get_column_types(self, database: str, schema: str, table_name: str) -> dict[str, Any]:
        qualified_table = f"{database}.{schema}.{table_name}"
        if qualified_table not in self._column_types_cache:
            rows = (
                self.session.table(f'"{database}".information_schema.columns')
                .filter((col("TABLE_SCHEMA") == schema) & (col("TABLE_NAME") == table_name))
                .select("COLUMN_NAME", "DATA_TYPE", "NUMERIC_SCALE")
                .collect()
            )
            self._column_types_cache[qualified_table] = {
                row["COLUMN_NAME"]: {"data_type": row["DATA_TYPE"], "scale": row["NUMERIC_SCALE"]}
                for row in rows
            }
        return self._column_types_cache[qualified_table]

    def _coerce_dataframe_types(
        self, df: pd.DataFrame, database: str, schema: str, table_name: str
    ) -> pd.DataFrame:
        col_types = self._get_column_types(database, schema, table_name)
        for column in df.columns:
            metadata = col_types.get(column.upper())
            data_type = metadata["data_type"]
            scale = metadata["scale"]
            if data_type == "TIMESTAMP_TZ":
                df[column] = df[column].apply(
                    lambda x: datetime.datetime.fromisoformat(str(x)).replace(tzinfo=None)
                    if pd.notna(x) and str(x) not in ("", "NaT")
                    else None
                )
                df[column] = df[column].apply(lambda x: pd.Timestamp(x, unit="us", tz="UTC"))
            if data_type == "DATE":
                df[column] = df[column].apply(lambda x: pd.Timestamp(x, unit="us", tz="UTC"))
            elif data_type == "NUMBER":
                df[column] = pd.to_numeric(
                    df[column],
                    errors="coerce",
                )
                if scale == 0:
                    df[column] = np.where(df[column].notnull(), df[column], None)
            elif data_type == "TEXT":
                stringified_col = df[column].astype(str)
                cleaned_col = stringified_col.str.strip().str.lower()
                mask = df[column].isna() | cleaned_col.isin(["nan", "none"])
                df[column] = np.where(mask, None, stringified_col)
                df[column] = df[column].astype(object)
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

        perf_start = time.perf_counter()
        success, _, num_rows, _ = write_pandas(
            conn=self.conn,
            df=df,
            table_name=resolved_table_name,
            database=database,
            schema=schema,
            overwrite=truncate,
            parallel=self.parallel,
            compression=self.compression,
            use_logical_type=True,
        )
        duration = time.perf_counter() - perf_start
        print(f"It took {duration:.6f} seconds to insert rows {table_name}")

        if not success:
            raise RuntimeError(f"write_pandas reported failures writing to {table_name}")
        print(f"Inserted {num_rows} rows to {table_name}")

    def get_patient_batch(
        self, bene_sks: list[int], table_names: list[str]
    ) -> dict[str, list[RowAdapter]]:
        bene_hstry_df = self.qualified_session_table(BENE_HSTRY).filter(
            col("BENE_SK").isin(bene_sks)
        )
        result: dict[str, list[RowAdapter]] = {
            BENE_HSTRY: [
                RowAdapter(r.as_dict(), loaded_from_file=True) for r in bene_hstry_df.collect()
            ]
        }
        mbi_ids_df = bene_hstry_df.select("BENE_MBI_ID").filter(col("BENE_MBI_ID").is_not_null())

        for table_name in table_names:
            if _TABLE_RELATIONS[table_name] is KeyRelation.BENE_SK:
                rows = (
                    self.qualified_session_table(table_name)
                    .filter(col("BENE_SK").isin(bene_sks))
                    .collect()
                )
            else:
                rows = (
                    self.qualified_session_table(table_name)
                    .join(mbi_ids_df, using_columns=["BENE_MBI_ID"])
                    .collect()
                )
            result[table_name] = [RowAdapter(row.as_dict(), loaded_from_file=True) for row in rows]
        return result

    def get_claims_batch(
        self, bene_sks: list[int], table_names: list[str]
    ) -> dict[str, list[RowAdapter]]:
        self.session.use_database(self.database)
        self.session.use_schema(self.schema)
        clm_df = (
            self.qualified_session_table(CLM).filter(col("BENE_SK").isin(bene_sks)).cache_result()
        )

        key_dfs = {
            KeyRelation.FOUR_PART_KEY: clm_df.select(
                "GEO_BENE_SK", "CLM_DT_SGNTR_SK", "CLM_TYPE_CD", "CLM_NUM_SK"
            ),
            KeyRelation.CLM_UNIQ_ID: clm_df.select("CLM_UNIQ_ID").distinct(),
            KeyRelation.CLM_RLT_COND_SGNTR_SK: clm_df.select("CLM_RLT_COND_SGNTR_SK")
            .filter(col("CLM_RLT_COND_SGNTR_SK").is_not_null())
            .distinct(),
            KeyRelation.CLM_DT_SGNTR_SK: clm_df.select("CLM_DT_SGNTR_SK").distinct(),
        }

        result: dict[str, list[RowAdapter]] = {}
        result[CLM] = [
            RowAdapter(row.as_dict(), loaded_from_file=True) for row in clm_df.to_local_iterator()
        ]

        for table_name in table_names:
            key_df = key_dfs[_TABLE_RELATIONS[table_name]]
            joined = self.qualified_session_table(table_name).join(
                key_df, using_columns=list(key_df.columns)
            )
            result[table_name] = [
                RowAdapter(row.as_dict(), loaded_from_file=True)
                for row in joined.to_local_iterator()
            ]

        return result

    def merge_batch(self, data: list[dict[str, Any]], table_name: str) -> None:
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

        perf_start = time.perf_counter()
        source = self.session.write_pandas(
            df=df,
            table_name=f"{resolved_table_name}_STAGING",
            database=database,
            schema=schema,
            auto_create_table=True,
            overwrite=True,
            table_type="temporary",
            parallel=self.parallel,
            compression=self.compression,
            use_logical_type=True,
        )
        duration = time.perf_counter() - perf_start
        print(f"{duration:.6f} seconds to writer to staging table")
        target = self.session.table(f'"{database}"."{schema}"."{resolved_table_name}"')
        pks = self.get_primary_keys(table_name)

        update_cols = [c for c in df.columns if c not in pks]

        join_expr = None
        for pk in pks:
            cond = target[pk] == source[pk]
            join_expr = cond if join_expr is None else (join_expr & cond)

        merge_clauses = []

        if update_cols:
            # v2_mdcr_clm_rlt_cond_sgntr_mbr would not have any columns left to check for updates
            # after the exclusion from above
            change_condition = None
            for c in update_cols:
                is_changed = target[c] != source[c]
                change_condition = (
                    is_changed if change_condition is None else (change_condition | is_changed)
                )

            merge_clauses.append(
                when_matched(change_condition).update({c: source[c] for c in update_cols})
            )

        merge_clauses.append(when_not_matched().insert({c: source[c] for c in df.columns}))

        perf_start = time.perf_counter()
        result = target.merge(
            source,
            join_expr,
            merge_clauses,
        )
        duration = time.perf_counter() - perf_start
        print(f"{duration:.6f} seconds to merge into {table_name}")
        print(
            f"Merged {resolved_table_name}: {result.rows_inserted} inserted, {result.rows_updated} updated"
        )

    def close(self) -> None:
        self.session.close()
        self.conn.close()

    def get_primary_keys(self, table_name: str) -> list[str]:
        cleaned_table_name, database, schema = self.resolve_target_table(table_name)
        qualified_table = f"{database}.{schema}.{cleaned_table_name}"
        pk_df = self.session.sql(f"SHOW PRIMARY KEYS IN TABLE {qualified_table}")
        return [row["column_name"] for row in pk_df.select('"column_name"').collect()]


def _require_env(name: str) -> str:
    val = os.environ.get(name)
    if not val:
        print(
            f"Missing required env variable {name}. "
            + "Source load-synthetic-credentials.sh with BFD_ENV set before running."
        )
        sys.exit(1)
    return val


class KeyRelation(StrEnum):
    BENE_SK = auto()
    BENE_MBI_ID = auto()
    FOUR_PART_KEY = auto()
    CLM_UNIQ_ID = auto()
    CLM_RLT_COND_SGNTR_SK = auto()
    CLM_DT_SGNTR_SK = auto()


_TABLE_RELATIONS: dict[str, KeyRelation] = {
    BENE_MBI_ID: KeyRelation.BENE_MBI_ID,
    BENE_STUS: KeyRelation.BENE_SK,
    BENE_ENTLMT_RSN: KeyRelation.BENE_SK,
    BENE_ENTLMT: KeyRelation.BENE_SK,
    BENE_TP: KeyRelation.BENE_SK,
    BENE_XREF: KeyRelation.BENE_SK,
    BENE_DUAL: KeyRelation.BENE_SK,
    BENE_MAPD_ENRLMT: KeyRelation.BENE_SK,
    BENE_MAPD_ENRLMT_RX: KeyRelation.BENE_SK,
    BENE_LIS_CMBND: KeyRelation.BENE_SK,
    CLM: KeyRelation.BENE_SK,
    CLM_DT_SGNTR: KeyRelation.CLM_DT_SGNTR_SK,
    CLM_DCMTN: KeyRelation.FOUR_PART_KEY,
    CLM_FISS: KeyRelation.FOUR_PART_KEY,
    CLM_INSTNL: KeyRelation.FOUR_PART_KEY,
    CLM_VAL: KeyRelation.FOUR_PART_KEY,
    CLM_PROD: KeyRelation.FOUR_PART_KEY,
    CLM_PRFNL: KeyRelation.FOUR_PART_KEY,
    CLM_LINE: KeyRelation.FOUR_PART_KEY,
    CLM_LINE_INSTNL: KeyRelation.FOUR_PART_KEY,
    CLM_LINE_PRFNL: KeyRelation.FOUR_PART_KEY,
    CLM_LINE_DCMTN: KeyRelation.FOUR_PART_KEY,
    CLM_LCTN_HSTRY: KeyRelation.FOUR_PART_KEY,
    CLM_LINE_RX: KeyRelation.CLM_UNIQ_ID,
    CLM_RLT_COND_SGNTR_MBR: KeyRelation.CLM_RLT_COND_SGNTR_SK,
}
