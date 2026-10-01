import csv
import io
import logging
import time
from abc import ABC, abstractmethod
from collections import OrderedDict
from collections.abc import Iterable, Iterator
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from enum import StrEnum, auto
from pathlib import Path
from typing import Any

import pandas as pd
from constants import (
    _INT_TO_STRING_COLS,
    BENE_DUAL,
    BENE_ENTLMT,
    BENE_ENTLMT_RSN,
    BENE_HSTRY,
    BENE_LIS_CMBND,
    BENE_MAPD_ENRLMT,
    BENE_MAPD_ENRLMT_RX,
    BENE_MBI_ID,
    BENE_SK,
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
    CNTRCT_PBP_NUM,
    PRAUC,
    PRVDR_HSTRY,
)
from row_adapter import RowAdapter
from snowflake.snowpark import Session
from snowflake.snowpark.functions import col, when_matched, when_not_matched
from snowflake.snowpark.functions import hash as snowpark_hash

logger = logging.getLogger(__name__)

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


class BeneSkMode(StrEnum):
    BENE_HSTRY = auto()
    CLM = auto()
    BOTH = auto()


class OutputDestinationWriter(ABC):
    @abstractmethod
    def write_table(
        self,
        data: list[Any],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,
    ) -> None: ...

    @abstractmethod
    def get_provider_histories(self, files: dict[str, list[RowAdapter]]) -> list[RowAdapter]: ...

    @abstractmethod
    def get_cntrct_pbp_nums(
        self,
        files: dict[str, list[RowAdapter]],
    ) -> list[dict[str, Any]]: ...

    @abstractmethod
    def get_bene_sks(
        self,
        files: dict[str, list[RowAdapter]],
        bene_sk_mode: BeneSkMode,
        batch_size: int,
    ) -> Iterator[list[int]]: ...


class CsvWriter(OutputDestinationWriter):
    def __init__(self, out_dir: str = "../out") -> None:
        self.out_dir = Path(out_dir)

    def _clean_int_columns(self, rows: list[dict[str, Any]]):
        for column in _INT_TO_STRING_COLS:
            for row in rows:
                if column in row:
                    row[column] = str(row[column])
        return rows

    def write_table(
        self,
        data: list[Any],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,
    ) -> None:
        if not data:
            return

        if hasattr(data[0], "kv"):
            data = [x.kv for x in data]
            data = self._clean_int_columns(data)

        df = pd.json_normalize(data)

        if cols is not None and not isinstance(cols, str):
            df = df.reindex(columns=cols).fillna("")

        out_path = self.out_dir / f"{table_name}.csv"
        Path(out_path).parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(out_path, index=False)

    def get_provider_histories(
        self,
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]:
        return files[PRVDR_HSTRY]

    def get_cntrct_pbp_nums(
        self,
        files: dict[str, list[RowAdapter]],
    ) -> list[dict[str, Any]]:
        return [row.kv for row in files[CNTRCT_PBP_NUM]]

    def get_bene_sks(
        self,
        files: dict[str, list[RowAdapter]],
        bene_sk_mode: BeneSkMode,
        batch_size: int,  # noqa: ARG002
    ) -> Iterator[list[int]]:
        clm_bene_sks = (
            [int(row[BENE_SK]) for row in files[CLM]]
            if bene_sk_mode == BeneSkMode.CLM or bene_sk_mode == BeneSkMode.BOTH
            else []
        )
        bene_hstry_bene_sks = (
            [int(row[BENE_SK]) for row in files[BENE_HSTRY]]
            if bene_sk_mode == BeneSkMode.BENE_HSTRY or bene_sk_mode == BeneSkMode.BOTH
            else []
        )
        all_bene_sks = clm_bene_sks + bene_hstry_bene_sks  # We take the order of CLM first
        yield list(OrderedDict.fromkeys(x for x in all_bene_sks))

    def get_bene_sk_to_mbi(
        self,
        bene_sks: list[int],
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]:
        return [
            RowAdapter(
                {
                    "BENE_SK": str(row["BENE_SK"]),
                    "BENE_MBI_ID": row["BENE_MBI_ID"],
                    "IDR_LTST_TRANS_FLG": row["IDR_LTST_TRANS_FLG"],
                },
                loaded_from_file=True,
            )
            for row in files[BENE_HSTRY]
            if row.get("BENE_MBI_ID") and int(row["BENE_SK"]) in bene_sks
        ]


class SnowflakeWriter(OutputDestinationWriter):
    def __init__(self, session: Session, database: str, schema: str) -> None:
        self.session = session
        self.database = database
        self.schema = schema
        self._column_types_cache: dict[tuple[str, str, str], dict[str, Any]] = {}

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

    def qualified_table(self, table_name: str) -> str:
        resolved_table_name, database, schema = self.resolve_target_table(table_name)
        return f"{database}.{schema}.{resolved_table_name}"

    def _qualified_session_table(self, table_name: str) -> Any:
        table = self.qualified_table(table_name)
        return self.session.table(table)

    def iter_bene_sk_batches(self, batch_size: int) -> Iterator[list[int]]:
        df = self._qualified_session_table(BENE_HSTRY).select("BENE_SK").distinct().sort("BENE_SK")
        batch = []
        for row in df.to_local_iterator():
            batch.append(int(row["BENE_SK"]))
            if len(batch) == batch_size:
                yield batch
                batch = []
        if batch:
            yield batch

    def get_rows(self, table_name: str) -> list[dict[str, Any]]:
        rows = self._qualified_session_table(table_name).collect()
        return [row.as_dict() for row in rows]

    def get_bene_sk_to_mbi(
        self,
        bene_sks: list[int],
        files: dict[str, list[RowAdapter]],  # noqa: ARG002
    ) -> list[RowAdapter]:
        rows = (
            self._qualified_session_table(BENE_HSTRY)
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

    def _get_known_columns(self, database: str, schema: str, table_name: str) -> set[str]:
        cache_key = (database, schema, table_name)

        if cache_key not in self._column_types_cache:
            rows = (
                self.session.table(f'"{database}".information_schema.columns')
                .filter(
                    (col("TABLE_SCHEMA") == schema.upper())
                    & (col("TABLE_NAME") == table_name.upper())
                )
                .select("COLUMN_NAME")
                .collect()
            )
            self._column_types_cache[cache_key] = {row["COLUMN_NAME"].upper() for row in rows}

        return self._column_types_cache[cache_key]

    def get_patient_batch(
        self, bene_sks: list[int], table_names: list[str]
    ) -> dict[str, list[RowAdapter]]:
        bene_hstry_df = self._qualified_session_table(BENE_HSTRY).filter(
            col("BENE_SK").isin(bene_sks)
        )
        result: dict[str, list[RowAdapter]] = {
            BENE_HSTRY: [
                RowAdapter(r.as_dict(), loaded_from_file=True) for r in bene_hstry_df.collect()
            ]
        }
        mbi_ids_df = (
            bene_hstry_df.select("BENE_MBI_ID").distinct().filter(col("BENE_MBI_ID").is_not_null())
        )

        for table_name in table_names:
            if _TABLE_RELATIONS[table_name] is KeyRelation.BENE_SK:
                rows = (
                    self._qualified_session_table(table_name)
                    .filter(col("BENE_SK").isin(bene_sks))
                    .collect()
                )
            else:
                rows = (
                    self._qualified_session_table(table_name)
                    .join(mbi_ids_df, using_columns=["BENE_MBI_ID"])
                    .collect()
                )
            result[table_name] = [RowAdapter(row.as_dict(), loaded_from_file=True) for row in rows]
        return result

    def get_claims_batch(
        self, bene_sks: list[int], table_names: list[str]
    ) -> dict[str, list[RowAdapter]]:
        perf_start = time.perf_counter()
        temp_table = f"{self.database}.{self.schema}.TEMP_BATCH_KEYS"
        self.session.sql(f"DROP TABLE IF EXISTS {temp_table}").collect()
        bene_df = self.session.create_dataframe([[b] for b in bene_sks], schema=["BENE_SK"])
        bene_df.write.mode("overwrite").save_as_table(temp_table, table_type="transient")
        temp_keys_df = self.session.table(temp_table)
        clm_df = self._qualified_session_table(CLM).join(temp_keys_df, on="BENE_SK")

        key_dfs = {
            KeyRelation.FOUR_PART_KEY: clm_df.select(
                "GEO_BENE_SK", "CLM_DT_SGNTR_SK", "CLM_TYPE_CD", "CLM_NUM_SK"
            ).cache_result(),
            KeyRelation.CLM_UNIQ_ID: clm_df.select("CLM_UNIQ_ID").distinct().cache_result(),
            KeyRelation.CLM_RLT_COND_SGNTR_SK: clm_df.select("CLM_RLT_COND_SGNTR_SK")
            .filter(col("CLM_RLT_COND_SGNTR_SK").is_not_null())
            .distinct()
            .cache_result(),
            KeyRelation.CLM_DT_SGNTR_SK: clm_df.select("CLM_DT_SGNTR_SK").distinct().cache_result(),
        }

        result: dict[str, list[RowAdapter]] = {}
        result[CLM] = [
            RowAdapter(row.as_dict(), loaded_from_file=True) for row in clm_df.to_local_iterator()
        ]

        def fetch_table_data(table_name):
            key_df = key_dfs[_TABLE_RELATIONS[table_name]]
            joined = self._qualified_session_table(table_name).join(
                key_df, using_columns=list(key_df.columns)
            )
            return table_name, [
                RowAdapter(row.as_dict(), loaded_from_file=True)
                for row in joined.to_local_iterator()
            ]

        with ThreadPoolExecutor(max_workers=min(len(table_names), 4)) as executor:
            futures = [executor.submit(fetch_table_data, name) for name in table_names]
            for future in futures:
                table_name, adapter_list = future.result()
                result[table_name] = adapter_list

        self.session.sql(f"DROP TABLE IF EXISTS {temp_table}").collect()

        duration = time.perf_counter() - perf_start
        logger.info(f"Took {duration:.6f} seconds to fetch existing claims")

        return result

    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
        cols: list[str] | str = ALL_KEYS,  # noqa: ARG002
    ) -> None:
        if not data:
            return

        perf_start = time.perf_counter()
        resolved_table_name, database, schema = self.resolve_target_table(table_name)
        qualified_table = f"{database}.{schema}.{resolved_table_name}"
        known_columns = self._get_known_columns(database, schema, resolved_table_name)

        # CSV file structure has to match the table schema order
        target = self._qualified_session_table(table_name)
        target_columns = [
            field.name for field in target.schema.fields if field.name in known_columns
        ]

        csv_buffer = io.StringIO()
        writer = csv.DictWriter(
            csv_buffer, fieldnames=target_columns, extrasaction="ignore", restval=None
        )

        writer.writeheader()
        for row_dict in data:
            writer.writerow(row_dict)

        csv_payload = csv_buffer.getvalue().encode("utf-8")
        csv_buffer.close()

        duration = time.perf_counter() - perf_start
        logger.info(f"Packaging data into CSV completed in {duration:.6f} seconds")

        staging_table_name = f"{database}.{schema}.MERGE_STAGE_{resolved_table_name}"
        target_filename = f"batch_{resolved_table_name}.csv"

        perf_start = time.perf_counter()

        self.session.sql(f"DROP TABLE IF EXISTS {staging_table_name}").collect()
        self.session.sql(
            f"CREATE TRANSIENT TABLE {staging_table_name} LIKE {qualified_table}"
        ).collect()

        self.session.sql(
            f"CREATE STAGE IF NOT EXISTS {database}.{schema}.generator_files_stage"
        ).collect()

        file_input_stream = io.BytesIO(csv_payload)
        self.session.file.put_stream(
            input_stream=file_input_stream,
            stage_location=f"@{database}.{schema}.generator_files_stage/{target_filename}",
            auto_compress=True,
            overwrite=True,
        )

        self.session.sql(
            f"""
            COPY INTO {staging_table_name}
            FROM @{database}.{schema}.generator_files_stage/batch_{resolved_table_name}.csv.gz
            FILE_FORMAT = (
                TYPE = 'CSV'
                SKIP_HEADER = 1
                FIELD_OPTIONALLY_ENCLOSED_BY = '"'
                ERROR_ON_COLUMN_COUNT_MISMATCH = FALSE
            )   
            PURGE = TRUE
            """
        ).collect()

        source = self.session.table(staging_table_name)

        duration = time.perf_counter() - perf_start
        logger.info(f"Staged data to merge in {duration:.6f} seconds")

        pks = self._get_primary_keys(table_name)
        update_cols = [c for c in target_columns if c not in pks]

        join_expr = None
        for pk in pks:
            cond = target[pk] == source[pk]
            join_expr = cond if join_expr is None else (join_expr & cond)

        merge_clauses = []

        if update_cols:
            target_hash = snowpark_hash(*[target[c] for c in update_cols])
            source_hash = snowpark_hash(*[source[c] for c in update_cols])
            change_condition = target_hash != source_hash

            merge_clauses.append(
                when_matched(change_condition).update({c: source[c] for c in update_cols})
            )

        merge_clauses.append(when_not_matched().insert({c: source[c] for c in target_columns}))

        perf_start = time.perf_counter()
        result = target.merge(
            source,
            join_expr,
            merge_clauses,
        )
        self.session.sql(f"DROP TABLE IF EXISTS {staging_table_name}").collect()
        duration = time.perf_counter() - perf_start

        message = f"Wrote to {resolved_table_name} in {duration:.6f} seconds: {result.rows_inserted} inserted, {result.rows_updated} updated"
        logger.info(message)

    def _get_primary_keys(self, table_name: str) -> list[str]:
        cleaned_table_name, database, schema = self.resolve_target_table(table_name)
        qualified_table = f"{database}.{schema}.{cleaned_table_name}"
        pk_df = self.session.sql(f"SHOW PRIMARY KEYS IN TABLE {qualified_table}")
        return [row["column_name"] for row in pk_df.select('"column_name"').collect()]

    def get_provider_histories(
        self,
        files: dict[str, list[RowAdapter]],  # noqa: ARG002
    ) -> list[RowAdapter]:
        return [RowAdapter(row, loaded_from_file=True) for row in self.get_rows(PRVDR_HSTRY)]

    def get_cntrct_pbp_nums(
        self,
        files: dict[str, list[RowAdapter]],  # noqa: ARG002
    ) -> list[dict[str, Any]]:
        return self.get_rows(CNTRCT_PBP_NUM)

    def get_bene_sks(
        self,
        files: dict[str, list[RowAdapter]],  # noqa: ARG002
        bene_sk_mode: BeneSkMode,  # noqa: ARG002
        batch_size: int,
    ) -> Iterator[list[int]]:
        yield from self.iter_bene_sk_batches(batch_size)

    def truncate_tables(self, table_names: Iterable[str]) -> None:
        for table_name in table_names:
            resolved_table_name, database, schema = self.resolve_target_table(table_name)
            self.session.sql(
                f"TRUNCATE TABLE IF EXISTS {database}.{schema}.{resolved_table_name}"
            ).collect()


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
