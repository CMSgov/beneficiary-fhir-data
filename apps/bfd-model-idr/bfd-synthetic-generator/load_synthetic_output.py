import csv
import io
import itertools
import logging
import string
import time
from abc import ABC, abstractmethod
from collections import OrderedDict, defaultdict
from collections.abc import Iterable, Iterator, Sequence
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from enum import StrEnum, auto
from functools import cached_property, reduce
from pathlib import Path
from typing import Any, cast, override

import click
import pandas as pd
import tqdm
from constants import (
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
    CLM_DT_SGNTR_SK,
    CLM_FISS,
    CLM_INSTNL,
    CLM_LCTN_HSTRY,
    CLM_LINE,
    CLM_LINE_DCMTN,
    CLM_LINE_FISS,
    CLM_LINE_INSTNL,
    CLM_LINE_MCS,
    CLM_LINE_NUM,
    CLM_LINE_PRFNL,
    CLM_LINE_RX,
    CLM_NUM_SK,
    CLM_OCRNC_SGNTR_MBR,
    CLM_OCRNC_SGNTR_SK,
    CLM_PRFNL,
    CLM_PROD,
    CLM_RLT_COND_SGNTR_MBR,
    CLM_RLT_COND_SGNTR_SK,
    CLM_RLT_OCRNC_SGNTR_MBR,
    CLM_RLT_OCRNC_SGNTR_SK,
    CLM_TYPE_CD,
    CLM_VAL,
    CNTRCT_PBP_NUM,
    GEO_BENE_SK,
    MBI_NUM,
    PRAUC,
    PRVDR_HSTRY,
)
from id_generators import (
    MBI_ALPHABETS,
    RandomIdGenerator,
    SequentialIdGenerator,
    SnowflakeIdState,
    decode_identifier,
    expand_parts,
)
from row_adapter import RowAdapter, RowValue
from schema_utils import TABLE_OVERRIDES, TableSchema, fetch_schemas, get_table_name_from_file_name
from snowflake.snowpark import Column, DataFrame, Session, Table
from snowflake.snowpark.functions import col, when_matched, when_not_matched
from snowflake.snowpark.functions import hash as snowpark_hash
from snowflake.snowpark.row import Row
from snowflake.snowpark.table import WhenNotMatchedClause
from utils import parse_input

logger = logging.getLogger(__name__)

_SYNTHETIC_PREFIX = "SYNTHETIC"
_TABLE_PREFIX = "V2_MDCR"
_CAST_TO_INT_FIELDS = [
    CLM_NUM_SK,
    GEO_BENE_SK,
    CLM_TYPE_CD,
    CLM_DT_SGNTR_SK,
    CLM_LINE_NUM,
    CLM_RLT_COND_SGNTR_SK,
    CLM_OCRNC_SGNTR_SK,
    CLM_RLT_OCRNC_SGNTR_SK,
]


class BeneSkMode(StrEnum):
    BENE_HSTRY = auto()
    CLM = auto()
    BOTH = auto()


class OutputDestinationWriter(ABC):
    def __init__(self, overwrite_fields: str | None = None) -> None:
        self.fields_to_overwrite = overwrite_fields

    @abstractmethod
    def fetch_basic_schema(self) -> dict[str, TableSchema]: ...

    @cached_property
    def basic_schema(self) -> dict[str, TableSchema]:
        return self.fetch_basic_schema()

    @cached_property
    def overwrites(self) -> dict[str, set[str]]:
        return self.parse_overwrites(self.fields_to_overwrite)

    @abstractmethod
    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
    ) -> None: ...

    @abstractmethod
    def get_rows(
        self,
        table_name: str,
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]: ...

    @abstractmethod
    def get_provider_histories(self, files: dict[str, list[RowAdapter]]) -> list[RowAdapter]: ...

    @abstractmethod
    def get_cntrct_pbp_nums(
        self,
        files: dict[str, list[RowAdapter]],
    ) -> list[dict[str, Any]]: ...

    @abstractmethod
    def get_bene_sk_to_mbi(
        self,
        bene_sks: list[int],
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]: ...

    @abstractmethod
    def get_prior_auths(
        self,
        bene_sk_to_mbi: list[RowAdapter],
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]: ...

    @abstractmethod
    def get_bene_sks(
        self,
        batch_size: int,
        bene_sk_mode: BeneSkMode = BeneSkMode.BENE_HSTRY,
        files: dict[str, list[RowAdapter]] | None = None,
    ) -> Iterator[list[int]]: ...

    @abstractmethod
    def get_patient_batch(
        self,
        bene_sks: list[int],
        table_names: list[str],
        files: dict[str, list[RowAdapter]],
    ) -> dict[str, list[RowAdapter]]: ...

    @abstractmethod
    def get_claims_batch(
        self,
        bene_sks: list[int],
        table_names: list[str],
        files: dict[str, list[RowAdapter]],
        truncate: bool = False,
    ) -> dict[str, list[RowAdapter]]: ...

    @abstractmethod
    def truncate_tables(self, table_names: Iterable[str]) -> None: ...

    @abstractmethod
    def load_id_state(self, truncate: bool = False) -> SnowflakeIdState: ...

    @abstractmethod
    def load_files(
        self,
        files: dict[str, list[RowAdapter]],
        paths: Sequence[str | Path],
        exclude_empty: bool = False,
    ) -> dict[str, list[RowAdapter]]: ...

    def parse_overwrites(self, qualified_fields: str | None) -> dict[str, set[str]]:
        fields_to_overwrite: dict[str, set[str]] = defaultdict(set)

        if not qualified_fields:
            return fields_to_overwrite

        cleaned_qualified_fields = parse_input(qualified_fields)

        for qualified_field in cleaned_qualified_fields:
            table, _, field = qualified_field.partition(".")
            if not table or not field:
                raise click.UsageError(
                    f"Invalid --overwrite entry {qualified_field} expected TABLE.FIELD "
                    "for Snowflake flow or SYNTHETIC_FILE_NAME.FIELD if CSV flow"
                )

            # If this is the CSV flow we need to get the name of the target table since the files
            # do not match the table names
            if table.startswith(_SYNTHETIC_PREFIX):
                resolved_table_name = get_table_name_from_file_name(table)
                table_schema = self.basic_schema.get(resolved_table_name)
            else:
                table_schema = self.basic_schema.get(table)

            if table_schema and field in table_schema.primary_keys:
                raise click.UsageError(
                    f"{qualified_field} cannot be overwritten since it's a primary key"
                )

            fields_to_overwrite[table].add(field)

        return fields_to_overwrite


def _load_file(file: Iterable[dict[str, Any]]) -> list[RowAdapter]:
    return [RowAdapter(kv=row, loaded_from_file=True) for row in file]


class CsvWriter(OutputDestinationWriter):
    def __init__(
        self,
        out_dir: str | Path = "./out",
        overwrite_fields: str | None = None,
    ) -> None:
        super().__init__(overwrite_fields=overwrite_fields)
        self.out_dir: Path = Path(out_dir)
        self._tables: set[str] = set()
        self.id_gen: RandomIdGenerator = RandomIdGenerator()

    @override
    def fetch_basic_schema(self) -> dict[str, TableSchema]:
        return fetch_schemas()

    @override
    def load_files(
        self,
        files: dict[str, list[RowAdapter]],
        paths: Sequence[str | Path],
        exclude_empty: bool = False,
    ) -> dict[str, list[RowAdapter]]:

        file_paths = set(
            itertools.chain.from_iterable(
                [
                    [as_path] if as_path.is_file() else list(as_path.glob("*.csv"))
                    for x in paths
                    if (as_path := Path(x))
                ]
            )
        )
        if not file_paths:
            return files

        print(
            f"Loading all provided files, {
                'ignoring empty columns'
                if exclude_empty
                else 'treating empty columns as meaningful'
            }..."
        )
        file_tupls = [
            (file_path, file_name)
            for file_path in file_paths
            for file_name in files
            if f"{file_name}.csv" in str(file_path)
        ]
        with tqdm.tqdm(file_tupls) as t:
            for file_path, file_name in t:
                t.set_postfix(file=file_path)  # type: ignore
                csv_data = pd.read_csv(  # type: ignore
                    file_path,
                    dtype=str,
                    na_filter=exclude_empty,
                    keep_default_na=False,
                )

                # Add any missing columns defined in our idr schema even if we have not generated
                # data for it so our CSV file always matches the actual schema
                schema = self.basic_schema
                table_name = get_table_name_from_file_name(file_name)
                if schema and (table_schema := schema.get(table_name)):
                    for col in table_schema.columns:
                        if col not in csv_data.columns:
                            csv_data[col] = pd.NA

                for field in _CAST_TO_INT_FIELDS:
                    if field in csv_data.columns:
                        csv_data[field] = pd.to_numeric(csv_data[field], errors="coerce").astype(
                            "Int64"
                        )

                if fields_to_overwrite := self.overwrites.get(file_name):
                    for field in fields_to_overwrite:
                        csv_data.pop(field)

                file_as_dictlist = csv_data.to_dict(orient="records")  # type: ignore
                if not exclude_empty:
                    files[file_name] = _load_file(file_as_dictlist)  # type: ignore
                    continue

                files[file_name] = _load_file(
                    [{str(k): v for k, v in x.items() if pd.notna(v)} for x in file_as_dictlist]
                )
        print("All files loaded")
        return files

    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
    ) -> None:
        if not data:
            return

        df = pd.json_normalize(data)
        resolved_table_name = get_table_name_from_file_name(table_name)
        table_schema = self.basic_schema[resolved_table_name]
        df = df.reindex(columns=table_schema.primary_keys + table_schema.columns)

        for column in df.columns:
            if df[column].dtype == "float64":
                df[column] = df[column].apply(
                    lambda x: str(int(x)) if isinstance(x, (int, float)) and not pd.isna(x) else ""
                )

        out_path = self.out_dir / f"{table_name}.csv"
        out_path.parent.mkdir(parents=True, exist_ok=True)
        first_write = table_name not in self._tables
        df.to_csv(out_path, index=False, mode="w" if first_write else "a", header=first_write)
        self._tables.add(table_name)

    @override
    def get_rows(
        self,
        table_name: str,
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]:
        return files[table_name]

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

    @override
    def get_bene_sks(
        self,
        batch_size: int,
        bene_sk_mode: BeneSkMode = BeneSkMode.BENE_HSTRY,
        files: dict[str, list[RowAdapter]] | None = None,
    ) -> Iterator[list[int]]:
        if files is None:
            raise ValueError("CsvWriter.get_bene_sks requires the input files to read BENE_SK from")
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
        ordered = list(OrderedDict.fromkeys(x for x in all_bene_sks))
        for i in range(0, len(ordered), batch_size):
            yield ordered[i : i + batch_size]

    @override
    def get_patient_batch(
        self,
        bene_sks: list[int],
        table_names: list[str],
        files: dict[str, list[RowAdapter]],
    ) -> dict[str, list[RowAdapter]]:
        return files

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

    def get_prior_auths(
        self,
        bene_sk_to_mbi: list[RowAdapter],
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]:
        mbis = {row["BENE_MBI_ID"] for row in bene_sk_to_mbi}
        return [row for row in files[PRAUC] if row[MBI_NUM] in mbis]

    @override
    def get_claims_batch(
        self,
        bene_sks: list[int],
        table_names: list[str],
        files: dict[str, list[RowAdapter]],
        truncate: bool = False,
    ) -> dict[str, list[RowAdapter]]:
        return files

    @override
    def truncate_tables(self, table_names: Iterable[str]) -> None:
        return

    @override
    def load_id_state(self, truncate: bool = False) -> SnowflakeIdState:
        # no-op since obtaining state is particular to the Snowflake flow
        return SnowflakeIdState()


def _row_values(row: Row) -> dict[str, RowValue]:
    return cast("dict[str, RowValue]", row.as_dict())


def _join_expression(target: Table, source: Table, primary_keys: list[str]) -> Column:
    conditions: list[Column] = [cast(Column, target[key] == source[key]) for key in primary_keys]
    return reduce(lambda left, right: left & right, conditions)


class SnowflakeWriter(OutputDestinationWriter):
    def __init__(
        self,
        session: Session,
        database: str,
        schema: str,
        overwrite_fields: str | None = None,
        truncate: bool = False,
    ) -> None:
        super().__init__(overwrite_fields=overwrite_fields)
        self.session = session
        self.database = database
        self.schema = schema
        self.id_state = self.load_id_state(truncate)
        self.id_gen: SequentialIdGenerator = SequentialIdGenerator(self.id_state)

    @override
    def fetch_basic_schema(self) -> dict[str, TableSchema]:
        pks_by_table: defaultdict[str, TableSchema] = defaultdict(TableSchema)

        self.session.sql(f'SHOW PRIMARY KEYS IN DATABASE "{self.database}"').collect()
        pks_by_table_rows = self.session.sql(
            """
                SELECT "table_name", "column_name"
                FROM TABLE(RESULT_SCAN(LAST_QUERY_ID()))
                """
        ).collect()

        for row in pks_by_table_rows:
            row_dict = _row_values(row)
            table_name = str(row_dict["table_name"])
            column_name = str(row_dict["column_name"])
            pks_by_table[table_name].primary_keys.append(column_name)

        return dict(pks_by_table)

    def resolve_target_table(self, table_name: str) -> tuple[str, str, str]:
        resolved_name = get_table_name_from_file_name(table_name)
        overrides = TABLE_OVERRIDES.get(table_name)
        database = (overrides.database if overrides else None) or self.database
        schema = (overrides.schema if overrides else None) or self.schema
        return (resolved_name, database, schema)

    def qualified_table(self, table_name: str) -> str:
        resolved_table_name, database, schema = self.resolve_target_table(table_name)
        return f"{database}.{schema}.{resolved_table_name}"

    def _qualified_session_table(self, table_name: str) -> DataFrame | Table:
        qualified_table_name = self.qualified_table(table_name)
        table = self.session.table(qualified_table_name)
        # If fields need to be overwritten, drop them from the query so they aren't fetched. This
        # allows us to generate specified fields from scratch during a regeneration.
        resolved_table_name = get_table_name_from_file_name(table_name)
        fields_to_overwrite = self.overwrites.get(resolved_table_name)
        return table.drop(*fields_to_overwrite) if fields_to_overwrite else table

    @override
    def get_bene_sks(
        self,
        batch_size: int,
        bene_sk_mode: BeneSkMode = BeneSkMode.BENE_HSTRY,
        files: dict[str, list[RowAdapter]] | None = None,
    ) -> Iterator[list[int]]:
        tables_by_mode = {
            BeneSkMode.BENE_HSTRY: [BENE_HSTRY],
            BeneSkMode.CLM: [CLM],
            BeneSkMode.BOTH: [BENE_HSTRY, CLM],
        }
        tables = tables_by_mode[bene_sk_mode]
        bene_sks_df = self._qualified_session_table(tables[0]).select("BENE_SK")
        for table in tables[1:]:
            bene_sks_df = bene_sks_df.union(self._qualified_session_table(table).select("BENE_SK"))
        bene_sks_df = bene_sks_df.distinct().sort("BENE_SK")

        bene_sks = [int(cast(int, row["BENE_SK"])) for row in bene_sks_df.collect()]
        for start in range(0, len(bene_sks), batch_size):
            yield bene_sks[start : start + batch_size]

    @override
    def get_rows(
        self,
        table_name: str,
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]:
        rows = [_row_values(row) for row in self._qualified_session_table(table_name).collect()]
        return [RowAdapter(row, loaded_from_file=True) for row in rows]

    def get_bene_sk_to_mbi(
        self,
        bene_sks: list[int],
        files: dict[str, list[RowAdapter]],  # noqa: ARG002
    ) -> list[RowAdapter]:
        rows = (
            self._qualified_session_table(BENE_HSTRY)
            .filter(col("BENE_SK").isin(bene_sks))  # pyright: ignore[reportUnknownMemberType]
            .select("BENE_SK", "BENE_MBI_ID", "IDR_LTST_TRANS_FLG")
            .collect()
        )

        return [
            RowAdapter(
                {
                    "BENE_SK": str(values["BENE_SK"]),
                    "BENE_MBI_ID": values["BENE_MBI_ID"],
                    "IDR_LTST_TRANS_FLG": values["IDR_LTST_TRANS_FLG"],
                },
                loaded_from_file=True,
            )
            for values in map(_row_values, rows)
            if values["BENE_MBI_ID"]
        ]

    @override
    def get_patient_batch(
        self,
        bene_sks: list[int],
        table_names: list[str],
        files: dict[str, list[RowAdapter]],
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
            table_df = self._qualified_session_table(table_name)
            if _TABLE_RELATIONS[table_name] is KeyRelation.BENE_SK:
                df = table_df.filter(col("BENE_SK").isin(bene_sks))  # pyright: ignore[reportUnknownMemberType]
            else:
                df = table_df.join(mbi_ids_df, using_columns=["BENE_MBI_ID"])  # pyright: ignore[reportUnknownMemberType]
            result[table_name] = [
                RowAdapter(_row_values(row), loaded_from_file=True) for row in df.collect()
            ]
        return result

    @override
    def get_claims_batch(
        self,
        bene_sks: list[int],
        table_names: list[str],
        files: dict[str, list[RowAdapter]],
        truncate: bool = False,
    ) -> dict[str, list[RowAdapter]]:
        if truncate:
            # If truncate is enabled then this is not a regeneration flow.
            # Do not pull existing claims.
            return files

        perf_start = time.perf_counter()
        # Dumping bene_sks into a transient table to take advantage of scaling with a join
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
            KeyRelation.CLM_RLT_OCRNC_SGNTR_SK: clm_df.select("CLM_RLT_OCRNC_SGNTR_SK")
            .distinct()
            .cache_result(),
            KeyRelation.CLM_OCRNC_SGNTR_SK: clm_df.select("CLM_OCRNC_SGNTR_SK")
            .distinct()
            .cache_result(),
        }

        result: dict[str, list[RowAdapter]] = {}
        result[CLM] = [
            RowAdapter(row.as_dict(), loaded_from_file=True) for row in clm_df.to_local_iterator()
        ]

        def fetch_table_data(table_name: str) -> tuple[str, list[RowAdapter]]:
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

        self.session.table(temp_table).drop_table()

        duration = time.perf_counter() - perf_start
        logger.info("Took %.3f seconds to fetch existing claims", duration)

        return result

    def write_table(
        self,
        data: list[dict[str, Any]],
        table_name: str,
    ) -> None:
        if not data:
            return

        perf_start = time.perf_counter()
        resolved_table_name, database, schema = self.resolve_target_table(table_name)
        qualified_table = f"{database}.{schema}.{resolved_table_name}"
        target = self.session.table(qualified_table)
        columns = target.schema.names

        csv_buffer = io.StringIO()
        writer = csv.DictWriter(csv_buffer, fieldnames=columns, extrasaction="ignore", restval=None)

        writer.writeheader()
        for row_dict in data:
            writer.writerow(row_dict)

        csv_payload = csv_buffer.getvalue().encode("utf-8")
        csv_buffer.close()

        duration = time.perf_counter() - perf_start
        logger.info("Packaging data into CSV completed in %.3f seconds", duration)

        session_schema = (self.session.get_current_schema() or "").replace('"', "")
        staging_table_name = f"{database}.{session_schema}.MERGE_STAGE_{resolved_table_name}"
        target_filename = f"batch_{resolved_table_name}.csv"

        perf_start = time.perf_counter()

        self.session.sql(f"DROP TABLE IF EXISTS {staging_table_name}").collect()
        self.session.sql(
            f"CREATE TRANSIENT TABLE {staging_table_name} LIKE {qualified_table}"
        ).collect()

        # Note: Snowpark has session.create_dataframe() but it serializes data locally and chokes
        # on wide tables a lot of rows. To scale, this we buffer raw rows to CSV and stream
        # compressed chunks to staging table and then run an upsert using Snowpark's merge
        file_input_stream = io.BytesIO(csv_payload)
        self.session.file.put_stream(
            input_stream=file_input_stream,
            stage_location=f"@{database}.{session_schema}.generator_files_stage/{target_filename}",
            auto_compress=True,
            overwrite=True,
        )

        self.session.sql(
            f"""
            COPY INTO {staging_table_name}
            FROM @{database}.{session_schema}.generator_files_stage/{target_filename}.gz
            FILE_FORMAT = (
                TYPE = 'CSV'
                SKIP_HEADER = 1
                FIELD_OPTIONALLY_ENCLOSED_BY = '"'
            )
            PURGE = TRUE
            """
        ).collect()

        source = self.session.table(staging_table_name)  # pyright: ignore[reportUnknownMemberType]

        duration = time.perf_counter() - perf_start
        logger.info("Staged data to merge in %.3f seconds", duration)

        table_schema = self.basic_schema[resolved_table_name]
        pks = table_schema.primary_keys
        update_cols = [c for c in columns if c not in pks]

        join_expr = _join_expression(target, source, pks)

        merge_clauses = []

        if update_cols:
            target_hash = snowpark_hash(*[target[c] for c in update_cols])
            source_hash = snowpark_hash(*[source[c] for c in update_cols])
            change_condition = target_hash != source_hash

            merge_clauses.append(
                when_matched(change_condition).update({c: source[c] for c in update_cols})
            )

        merge_clauses.append(
            cast(
                WhenNotMatchedClause,
                when_not_matched().insert({c: source[c] for c in columns}),  # pyright: ignore[reportUnknownMemberType]
            )
        )

        perf_start = time.perf_counter()
        result = target.merge(
            source,
            join_expr,
            merge_clauses,
        )

        self.session.table(staging_table_name).drop_table()
        duration = time.perf_counter() - perf_start
        message = (
            f"Wrote to {resolved_table_name} in {duration:.3f} seconds: "
            f"{result.rows_inserted} inserted, {result.rows_updated} updated"
        )
        logger.info(message)

    def get_provider_histories(
        self,
        files: dict[str, list[RowAdapter]],
    ) -> list[RowAdapter]:
        return self.get_rows(PRVDR_HSTRY, files)

    def get_cntrct_pbp_nums(
        self,
        files: dict[str, list[RowAdapter]],
    ) -> list[dict[str, Any]]:
        rows = self.get_rows(CNTRCT_PBP_NUM, files)
        return [row.kv for row in rows]

    def get_prior_auths(
        self,
        bene_sk_to_mbi: list[RowAdapter],
        files: dict[str, list[RowAdapter]],  # noqa: ARG002
    ) -> list[RowAdapter]:
        if not bene_sk_to_mbi:
            return []

        mbis = {row["BENE_MBI_ID"] for row in bene_sk_to_mbi}
        temp_table = f"{self.database}.{self.schema}.TEMP_BATCH_MBIS"
        self.session.sql(f"DROP TABLE IF EXISTS {temp_table}").collect()
        bene_df = self.session.create_dataframe([[m] for m in mbis], schema=["MBI_NUM"])
        bene_df.write.mode("overwrite").save_as_table(temp_table, table_type="transient")
        rows = (
            self._qualified_session_table(PRAUC)
            .join(self.session.table(temp_table), on="MBI_NUM")
            .collect()
        )
        self.session.table(temp_table).drop_table()

        return [RowAdapter(row.as_dict(), loaded_from_file=True) for row in rows]

    @override
    def truncate_tables(self, table_names: Iterable[str]) -> None:
        for table_name in table_names:
            resolved_table_name, database, schema = self.resolve_target_table(table_name)
            self.session.sql(
                f"TRUNCATE TABLE IF EXISTS {database}.{schema}.{resolved_table_name}"
            ).collect()

    def _query_single_value(self, sql: str) -> RowValue | None:
        rows = self.session.sql(sql).collect()
        if not rows:
            return None
        values = list(_row_values(rows[0]).values())
        return values[0] if values else None

    def _query_int(self, sql: str) -> int | None:
        value = self._query_single_value(sql)
        return int(value) if isinstance(value, int | float | str | Decimal) else None

    def _query_str(self, sql: str) -> str | None:
        value = self._query_single_value(sql)
        return None if value is None else str(value)

    def _query_multiple(self, sql: str) -> list[Row]:
        return self.session.sql(sql).collect()

    @override
    def load_id_state(self, truncate: bool = False) -> SnowflakeIdState:
        state = SnowflakeIdState()

        # if truncate is set then the tables are emptied so every field starts from scratch
        if not truncate:
            # Get or set the min ben_sk
            existing_min_bene_sk = self._query_int(
                f"SELECT MIN(BENE_SK) FROM {self.qualified_table(BENE_HSTRY)}"
            )
            if existing_min_bene_sk is not None:
                state.bene_sk_next = existing_min_bene_sk - 1

            # Get or set the max mbi
            existing_max_mbi = self._query_str(
                f"SELECT MAX(BENE_MBI_ID) FROM {self.qualified_table(BENE_MBI_ID)}"
            )
            if existing_max_mbi is not None:
                state.mbi_next = decode_identifier(existing_max_mbi, MBI_ALPHABETS) + 1

            # Get or set the max npi
            existing_max_npi = self._query_str(
                f"SELECT MAX(PRVDR_NPI_NUM) FROM {self.qualified_table(PRVDR_HSTRY)}"
            )
            if existing_max_npi is not None:
                state.npi_next = int(existing_max_npi[:9]) + 1

        _NUMERIC_FIELDS: dict[str, str] = {
            "CNTRCT_PBP_SK": CNTRCT_PBP_NUM,
            "CLM_UNIQ_ID": CLM,
            "CLM_DT_SGNTR_SK": CLM_DT_SGNTR,
            "GEO_BENE_SK": CLM,
            "CLM_RLT_COND_SGNTR_SK": CLM_RLT_COND_SGNTR_MBR,
            "CLM_OCRNC_SGNTR_SK": CLM_OCRNC_SGNTR_MBR,
            "CLM_RLT_OCRNC_SGNTR_SK": CLM_RLT_OCRNC_SGNTR_MBR,
            "BENE_PDP_ENRLMT_MMBR_ID_NUM": BENE_MAPD_ENRLMT_RX,
        }

        _TEXT_MULTIPART_FIELDS: dict[str, tuple[str, list[tuple[str, int]]]] = {
            "CLM_LINE_PMD_UNIQ_TRKNG_NUM": (
                CLM_LINE,
                [(string.ascii_uppercase + string.digits, 13)],
            ),
            "CLM_CNTL_NUM": (CLM, [(string.digits, 14), (string.ascii_uppercase, 3)]),
            "CLM_ORIG_CNTL_NUM": (CLM, [(string.digits, 14), (string.ascii_uppercase, 3)]),
            "PRVDR_EMPLR_ID_NUM": (PRVDR_HSTRY, [(string.digits, 9)]),
            "PRVDR_OSCAR_NUM": (PRVDR_HSTRY, [(string.digits, 6)]),
            "CLM_PTNT_CNTL_NUM": (CLM_DCMTN, [(string.digits, 14), (string.ascii_uppercase, 3)]),
        }

        # Get or set the min numeric ids
        for field_name, table_name in _NUMERIC_FIELDS.items():
            existing_min = None
            if not truncate:
                existing_min = self._query_int(
                    f"SELECT MIN({field_name}) FROM {self.qualified_table(table_name)}"
                )
            if existing_min is not None:
                state.numeric_id_next[field_name] = existing_min - 1
            else:
                state.numeric_id_next[field_name] = 0

        # Get or set the max multipart ids. These are strings but we still run a SELECT MAX on them.
        # That is intentional since the counter below relies on alphabetical sorting so "00...12ABC"
        # will always come right before "00...12ABD" This holds since every position is fixed width
        # and possible values (alphabet) are sorted with _sorted_chars
        for field_name, (table_name, parts) in _TEXT_MULTIPART_FIELDS.items():
            existing_max = None
            if not truncate:
                existing_max = self._query_str(
                    f"SELECT MAX({field_name}) FROM {self.qualified_table(table_name)}"
                )

            if existing_max is not None:
                value = existing_max.removeprefix("-")
                alphabets = expand_parts(parts)
                state.multipart_id_next[field_name] = decode_identifier(value, alphabets) + 1
            else:
                state.multipart_id_next[field_name] = 0

        if not truncate:
            # Get or set the max clm_num_sk per (clm_type_cd, clm_dt_sgntr_sk, geo_bene_sk)
            claims_rows = self._query_multiple(
                f"""
                SELECT
                    CLM_TYPE_CD,
                    CLM_DT_SGNTR_SK,
                    GEO_BENE_SK,
                    MAX(CLM_NUM_SK) AS MAX_CLM_NUM_SK
                FROM {self.qualified_table(CLM)}
                GROUP BY
                    CLM_TYPE_CD,
                    CLM_DT_SGNTR_SK,
                    GEO_BENE_SK
                """,
            )

            if claims_rows:
                for row in claims_rows:
                    row_dict = _row_values(row)
                    clm_type_cd = int(str(row_dict["CLM_TYPE_CD"]))
                    clm_dt_sgntr_sk = int(str(row_dict["CLM_DT_SGNTR_SK"]))
                    geo_bene_sk = int(str(row_dict["GEO_BENE_SK"]))
                    max_clm_num_sk = int(str(row_dict["MAX_CLM_NUM_SK"]))
                    key_columns = (clm_type_cd, clm_dt_sgntr_sk, geo_bene_sk)

                    state.claim_num_sk_next[key_columns] = max_clm_num_sk + 1

        return state

    @override
    def load_files(
        self,
        files: dict[str, list[RowAdapter]],
        paths: Sequence[str | Path],
        exclude_empty: bool = False,
    ) -> dict[str, list[RowAdapter]]:
        return files


class KeyRelation(StrEnum):
    BENE_SK = auto()
    BENE_MBI_ID = auto()
    FOUR_PART_KEY = auto()
    CLM_UNIQ_ID = auto()
    CLM_RLT_COND_SGNTR_SK = auto()
    CLM_OCRNC_SGNTR_SK = auto()
    CLM_RLT_OCRNC_SGNTR_SK = auto()
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
    CLM_OCRNC_SGNTR_MBR: KeyRelation.CLM_OCRNC_SGNTR_SK,
    CLM_RLT_OCRNC_SGNTR_MBR: KeyRelation.CLM_RLT_OCRNC_SGNTR_SK,
    CLM_LINE_MCS: KeyRelation.FOUR_PART_KEY,
    CLM_LINE_FISS: KeyRelation.FOUR_PART_KEY,
}
