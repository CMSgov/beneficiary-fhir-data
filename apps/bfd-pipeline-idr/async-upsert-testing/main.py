import asyncio
import csv
import itertools
import logging
import os
import sys
import time
from abc import ABC, abstractmethod
from collections import defaultdict
from collections.abc import Callable, Generator, Iterable, Iterator, Mapping, Sequence
from dataclasses import dataclass
from datetime import UTC, date, datetime, timedelta
from enum import Enum, IntFlag, StrEnum, auto
from os import getenv
from pathlib import Path
from typing import Annotated, Any, Generic, TypeVar, cast, override

import anyio
import psycopg
import psycopg_pool
import snowflake.connector
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from dateutil.relativedelta import relativedelta
from psycopg.rows import DictRow, dict_row
from pydantic import BaseModel, BeforeValidator, TypeAdapter
from snowflake.connector import DictCursor, ProgrammingError, SnowflakeConnection
from snowflake.connector.errors import ForbiddenError
from snowflake.connector.network import ReauthenticationRequest, RetryRequest
from snowflake.snowpark import Session

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


class LoadType(StrEnum):
    INITIAL = "initial"
    INCREMENTAL = "incremental"


class PartitionType(IntFlag):
    PART_D = auto()
    INSTITUTIONAL = auto()
    PROFESSIONAL = auto()
    PAC = auto()
    ALL = auto()


@dataclass
class LoadPartition:
    name: str
    claim_type_codes: list[int]
    partition_type: PartitionType
    start_date: date | None
    end_date: date | None
    priority: int


@dataclass
class LoadPartitionGroup:
    name: str
    claim_type_codes: list[int]
    partition_type: PartitionType
    date_interval: relativedelta | None
    priority: int = 0

    def generate_ranges(self, load_type: LoadType, start_date: date) -> Generator[LoadPartition]:
        if (
            self.date_interval is None
            or not ENABLE_DATE_PARTITIONS
            or load_type == LoadType.INCREMENTAL
        ):
            yield LoadPartition(
                self.name, self.claim_type_codes, self.partition_type, None, None, self.priority
            )
            return

        start = date(year=start_date.year, month=start_date.month, day=1)
        now = datetime.date(datetime.now(UTC))
        while start < now:
            end = start + self.date_interval - relativedelta(days=1)
            start_str = start.strftime("%Y-%m-%d")
            end_str = end.strftime("%Y-%m-%d")
            yield LoadPartition(
                f"{self.name}-{start_str}-{end_str}",
                self.claim_type_codes,
                self.partition_type,
                start,
                end,
                self.priority,
            )
            start += self.date_interval


MIN_CLAIM_LOAD_DATE = "2014-06-30"


def get_connection_string(load_mode: LoadMode) -> str:
    if load_mode == LoadMode.LOCAL:
        return "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"

    return f"host={bfd_db_endpoint()} port={bfd_db_port()} dbname={bfd_db_name()} \
        user={bfd_db_username()} password={bfd_db_password()}"


def _parse_bool_default_false(var_name: str) -> bool:
    return getenv(var_name, "").lower() in ("1", "true")


def _parse_bool_default_true(var_name: str) -> bool:
    return getenv(var_name, "1").lower() not in ("0", "false")


# Tracking load progress is disabled for synthetic data loads.
# Use this to force enabling load progress for testing.
def force_load_progress() -> bool:
    # We don't normally want to store the load progress info for synthetic data since the dates
    # won't be in order like in prod. However, we need a way to override this for the tests.
    return _parse_bool_default_false("IDR_FORCE_LOAD_PROGRESS")


def bfd_test_date() -> datetime | None:
    test_date = getenv("BFD_TEST_DATE", "")
    return datetime.fromisoformat(test_date) if test_date else None


ENABLE_DATE_PARTITIONS = _parse_bool_default_true("IDR_ENABLE_DATE_PARTITIONS")
"""Enables partitioning claims data based on dates.
It's useful to disable this for synthetic loads since
the smaller volume of data means this will probably be much slower"""

MIN_CLAIM_NCH_TRANSACTION_DATE = getenv("IDR_MIN_CLAIM_NCH_TRANSACTION_DATE", MIN_CLAIM_LOAD_DATE)
"""Minimum claim date to load for NCH (and DDPS).
Any claims created before this date will be skipped.
Useful for partial loads with large amounts of data."""

MIN_CLAIM_SS_TRANSACTION_DATE = getenv("IDR_MIN_CLAIM_SS_TRANSACTION_DATE", MIN_CLAIM_LOAD_DATE)
"""Minimum claim date to load for shared systems.
Any claims created before this date will be skipped.
Useful for partial loads with large amounts of data."""

PARTITION_TYPE = getenv("IDR_PARTITION_TYPE", "year").lower()
"""Partition type (year/month/day).
This should be set to "day" in prod to reduce the batch sizes"""

LATEST_CLAIMS = _parse_bool_default_false("IDR_LATEST_CLAIMS")
"""Only pull in latest claims.
Useful for the initial data pull since we only want to pull in
the latest version of each claim."""

BATCH_MULTIPLIER = int(getenv("IDR_BATCH_MULTIPLIER", "2_000_000"))
"""Batch sizes are calculated based on the number of columns in the table
in order to keep memory usage stable relative to the number of concurrent tasks.
Change this to increase or decrease the number of rows loaded per batch.
Increasing this means the memory per task will also increase and
you will likely need to decrease the number of concurrent tasks
to prevent the server from running out of memory."""

MIN_BATCH_COMPLETION_DATE = getenv("IDR_MIN_BATCH_COMPLETION_DATE")
"""Minimum batch completion date to process
This is useful if you've already loaded some data and you do not want to reprocess
any batches that have already completed before this date."""

MAX_TASKS = int(getenv("IDR_MAX_TASKS", "32"))
"""Maximum concurrent tasks to run.
Changing this has a drastic effect on the runtime.
In prod, we want to run as many tasks as possible without running out of memory."""

_IDR_TABLES = getenv("IDR_TABLES", None)
TABLES_TO_LOAD = {t.strip().lower() for t in _IDR_TABLES.split(",")} if _IDR_TABLES else None
"""List of tables to include - any table not included will be skipped.
Useful if you only want to load a subset of data and don't want to wait
for the other tables to load. Takes precedence over source_load_events table in incremental mode."""

INCREMENTAL_IDR_JOB_GRACE_PERIOD = timedelta(
    hours=int(getenv("INCREMENTAL_IDR_JOB_GRACE_PERIOD_HRS", default="24"))
)
"""Amount of time to tolerate no new incoming IDR Job Events for a given IDR Job type
before simply loading the relevant tables. Defaults to 24 hours."""

# IDR credentials, these are pulled from SSM in prod.
# You likely don't want to touch these otherwise.
IDR_PRIVATE_KEY = getenv("IDR_PRIVATE_KEY", "")
IDR_USERNAME = getenv("IDR_USERNAME", "")
IDR_ACCOUNT = getenv("IDR_ACCOUNT", "")
IDR_WAREHOUSE = getenv("IDR_WAREHOUSE", "")
IDR_DATABASE = getenv("IDR_DATABASE", "")
IDR_SCHEMA = getenv("IDR_SCHEMA", "")

# These need to be lazy-loaded since we override them in the tests


# Database credentials/settings


def bfd_db_port() -> str:
    return getenv("BFD_DB_PORT", "5432")


def bfd_db_name() -> str:
    return getenv("BFD_DB_NAME", "fhirdb")


def bfd_db_endpoint() -> str:
    return getenv("BFD_DB_ENDPOINT", "")


def bfd_db_username() -> str:
    return getenv("BFD_DB_USERNAME", "")


def bfd_db_password() -> str:
    return getenv("BFD_DB_PASSWORD", "")


type DbType = str | float | int | bool | date | datetime


DEFAULT_MAX_DATE = "9999-12-31"
DEFAULT_MIN_DATE = "0001-01-01"
ALTERNATE_DEFAULT_DATE = "1000-01-01"
BENEFICIARY_TABLE = "idr.beneficiary"
CLAIM_RX_TABLE = "idr.claim_rx"
CLAIM_PROFESSIONAL_NCH_TABLE = "idr.claim_professional_nch"
CLAIM_PROFESSIONAL_SS_TABLE = "idr.claim_professional_ss"
CLAIM_INSTITUTIONAL_NCH_TABLE = "idr.claim_institutional_nch"
CLAIM_INSTITUTIONAL_SS_TABLE = "idr.claim_institutional_ss"

IDR_PREFIX = "cms_vdm_view_mdcr_prd"
IDR_BENE_HISTORY_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_hstry"
IDR_BENE_MBI_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mbi_id"
IDR_BENE_XREF_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_xref"
IDR_BENE_ENTITLEMENT_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mdcr_entlmt"
IDR_BENE_ENTITLEMENT_REASON_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mdcr_entlmt_rsn"
IDR_BENE_STATUS_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mdcr_stus"
IDR_BENE_THIRD_PARTY_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_tp"
IDR_BENE_COMBINED_DUAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_cmbnd_dual_mdcr"
IDR_BENE_LOW_INCOME_SUBSIDY_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_lis"
IDR_BENE_MA_PART_D_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mapd_enrlmt"
IDR_BENE_MA_PART_D_RX_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mapd_enrlmt_rx"

IDR_CLAIM_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm"
IDR_CLAIM_ANSI_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_ansi_sgntr"
IDR_CLAIM_DATE_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_dt_sgntr"
IDR_CLAIM_INSTITUTIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_instnl"
IDR_CLAIM_PROFESSIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_prfnl"
IDR_CLAIM_DOCUMENTATION_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_dcmtn"
IDR_CLAIM_LINE_DOCUMENTATION_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_dcmtn"
IDR_CLAIM_VAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_val"
IDR_CLAIM_LINE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line"
IDR_CLAIM_LINE_INSTITUTIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_instnl"
IDR_CLAIM_LINE_PROFESSIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_prfnl"
IDR_CLAIM_PROD_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_prod"
IDR_CLAIM_FISS_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_fiss"
IDR_CLAIM_LINE_RX_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_rx"
IDR_CLAIM_LINE_FISS_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_fiss"
IDR_CLAIM_LINE_MCS_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_mcs"
IDR_CLAIM_LINE_FISS_BENEFIT_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_fiss_bnft_svg"
IDR_CLAIM_LOCATION_HISTORY_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_lctn_hstry"
IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_rlt_cond_sgntr_mbr"
IDR_CLAIM_OCCURRENCE_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_ocrnc_sgntr_mbr"
IDR_CLAIM_RELATED_OCCURRENCE_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_clm_rlt_ocrnc_sgntr_mbr"
IDR_PROVIDER_HISTORY_TABLE = f"{IDR_PREFIX}.v2_mdcr_prvdr_hstry"
IDR_CONTRACT_PBP_NUM_TABLE = f"{IDR_PREFIX}.v2_mdcr_cntrct_pbp_num"
IDR_CONTRACT_PBP_CONTACT_TABLE = f"{IDR_PREFIX}.v2_mdcr_cntrct_pbp_cntct"
IDR_CONTRACT_PBP_SEGMENT_TABLE = f"{IDR_PREFIX}.v2_mdcr_cntrct_pbp_sgmt"

DEATH_DATE_CUTOFF_YEARS = 4

match PARTITION_TYPE:
    case "year" | "years":
        partition_range = relativedelta(years=1)
    case "month" | "months":
        partition_range = relativedelta(months=1)
    case "day" | "days":
        partition_range = relativedelta(days=1)
    case _:
        raise ValueError("invalid partition type " + PARTITION_TYPE)

PART_D_CLAIM_TYPE_CODES = [1, 2, 3, 4]

PART_D_PARTITIONS = [
    LoadPartitionGroup("part_d_original", [1], PartitionType.PART_D, partition_range),
    LoadPartitionGroup("part_d_adjustment", [2, 3, 4], PartitionType.PART_D, partition_range),
]

INSTITUTIONAL_NCH_PARTITIONS = [
    # Outpatient
    LoadPartitionGroup("outpatient", [40], PartitionType.INSTITUTIONAL, partition_range),
    # HHA, SNF, Hospice, Inpatient, MA
    LoadPartitionGroup(
        "institutional",
        [10, 20, 30, 50, 60, 61, 62, 63, 64],
        PartitionType.INSTITUTIONAL,
        partition_range,
    ),
]

INSTITUTIONAL_SS_PARTITIONS = [
    LoadPartitionGroup(
        "institututional_pac",
        [
            1000,
            1011,
            1012,
            1013,
            1014,
            1018,
            1019,
            1021,
            1022,
            1023,
            1028,
            1029,
            1032,
            1033,
            1034,
            1039,
            1041,
            1042,
            1043,
            1049,
            1065,
            1066,
            1069,
            1071,
            1072,
            1073,
            1074,
            1075,
            1076,
            1077,
            1078,
            1079,
            1081,
            1082,
            1083,
            1084,
            1085,
            1086,
            1087,
            1088,
            1089,
            1091,
            1092,
            1093,
            1094,
            1095,
            1096,
            1097,
            1098,
            1099,
            1900,
            2000,
            2011,
            2012,
            2013,
            2014,
            2018,
            2019,
            2021,
            2022,
            2023,
            2028,
            2029,
            2032,
            2033,
            2034,
            2039,
            2041,
            2042,
            2043,
            2049,
            2065,
            2066,
            2069,
            2071,
            2072,
            2073,
            2074,
            2075,
            2076,
            2077,
            2078,
            2079,
            2081,
            2082,
            2083,
            2084,
            2085,
            2086,
            2087,
            2088,
            2089,
            2091,
            2092,
            2093,
            2094,
            2095,
            2096,
            2097,
            2098,
            2099,
            2900,
        ],
        PartitionType.INSTITUTIONAL | PartitionType.PAC,
        partition_range,
    )
]

PROFESSIONAL_NCH_PARTITIONS = [
    LoadPartitionGroup(
        "professional",
        [71, 72, 81, 82],
        PartitionType.PROFESSIONAL,
        partition_range,
    ),
]

PROFESSIONAL_SS_PARTITIONS = [
    LoadPartitionGroup(
        "professional_pac",
        [1700, 1800, 2700, 2800],
        PartitionType.PROFESSIONAL | PartitionType.PAC,
        partition_range,
    )
]


ALL_CLAIM_PARTITIONS = [
    *PART_D_PARTITIONS,
    *INSTITUTIONAL_NCH_PARTITIONS,
    *INSTITUTIONAL_SS_PARTITIONS,
    *PROFESSIONAL_NCH_PARTITIONS,
    *PROFESSIONAL_SS_PARTITIONS,
]

ALL_CLAIM_TYPE_CODES = [code for c in ALL_CLAIM_PARTITIONS for code in c.claim_type_codes]

COMBINED_CLAIM_PARTITION = LoadPartitionGroup(
    "all_claims",
    [code for partition in ALL_CLAIM_PARTITIONS for code in partition.claim_type_codes],
    PartitionType.INSTITUTIONAL
    | PartitionType.PROFESSIONAL
    | PartitionType.PART_D
    | PartitionType.PAC,
    None,
)

DEFAULT_PARTITION = LoadPartition("default", [], PartitionType.ALL, None, None, 0)

NON_CLAIM_PARTITION = LoadPartitionGroup("default", [], PartitionType.ALL, None, 1)

# Need to declare this separately because python struggles
# with type-hinting empty arrays :(
EMPTY_PARTITION: list[LoadPartitionGroup] = []


def transform_null_date_to_max(value: date | None) -> date:
    if value is None:
        return date.fromisoformat(DEFAULT_MAX_DATE)
    return value


def transform_null_date_to_min(value: date | None) -> date:
    if value is None:
        return date.fromisoformat(DEFAULT_MIN_DATE)
    return value


def transform_null_or_default_date_to_max(value: date | None) -> date:
    if value is None or value in (
        date.fromisoformat(ALTERNATE_DEFAULT_DATE),
        date.fromisoformat(DEFAULT_MIN_DATE),
    ):
        return date.fromisoformat(DEFAULT_MAX_DATE)
    return value


def transform_default_date_to_null(value: date | None) -> date | None:
    if value in (
        date.fromisoformat(ALTERNATE_DEFAULT_DATE),
        date.fromisoformat(DEFAULT_MIN_DATE),
        date.fromisoformat(DEFAULT_MAX_DATE),
    ):
        return None
    return value


def transform_default_string(value: str | None) -> str:
    if value is None or value == "~":
        return ""
    return value


def transform_null_float(value: float | None) -> float:
    if value is None:
        return 0.0
    return value


def transform_null_int(value: int | None) -> int:
    if value is None:
        return 0
    return value


def transform_default_int_to_null(value: int | None) -> int | None:
    if value == 0:
        return None
    return value


def format_date_opt(date_str: str | None) -> datetime | None:
    if not date_str:
        return None
    return datetime.strptime(date_str, "%Y-%m-%d").replace(tzinfo=UTC)


def format_date(date_str: str) -> datetime:
    return cast(datetime, format_date_opt(date_str))


def provider_last_or_legal_name_expr(alias: str) -> str:
    return f"COALESCE({alias}.prvdr_lgl_name, {alias}.prvdr_last_name)"


def _normalize(col: str) -> str:
    return f"CASE WHEN {col} = '~' THEN '' ELSE {col} END"


def _normalize_to_null(col: str) -> str:
    return f"CASE WHEN {col} IN ('', '~') THEN NULL ELSE {col} END"


def clm_orig_cntl_num_expr() -> str:
    return f"""CASE
                WHEN {ALIAS_CLM}.clm_cntl_num = {ALIAS_CLM}.clm_orig_cntl_num
                THEN ''
                ELSE {ALIAS_CLM}.clm_orig_cntl_num
                END"""


def provider_careteam_name_expr(alias: str, type: str | None) -> str:
    # Note: Snowflake throws an error if you do COALESCE with a single argument
    # so we need to explicitly pass null here

    # This handles both provider types
    # - Take the provider name from the claim table if present
    # - If not, take prvdr_lgl_name for organizations
    # Otherwise, take last name, first_name
    return f"""
        COALESCE(
            {f"{_normalize_to_null(f'{ALIAS_CLM}.clm_{type}_prvdr_name')}" if type else "NULL"},
            CASE
                WHEN {alias}.prvdr_last_name IS NULL
                    OR {_normalize(f"{alias}.prvdr_last_name")} = ''
                THEN {_normalize(f"{alias}.prvdr_lgl_name")}
                ELSE {_normalize(f"{alias}.prvdr_last_name")}
                    || COALESCE(', ' || {_normalize(f"{alias}.prvdr_1st_name")}, '')
            END
        )
    """


MEDICARE_EXHAUSTED_CD = "A3"
ACTIVE_CARE_CD = "22"
QUALIFYING_STAY_CD = "70"
NON_COVERED_STAY_CD = "74"


def clm_base_query(start_time: datetime, partition: LoadPartition, model_type: ModelType) -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            clm_uniq_id,
            geo_bene_sk,
            clm_type_cd,
            clm_num_sk,
            clm_dt_sgntr_sk,
            clm_ocrnc_sgntr_sk,
            clm_rlt_cond_sgntr_sk,
            clm_rlt_ocrnc_sgntr_sk,
            clm_idr_ld_dt,
            idr_insrt_ts,
            idr_updt_ts
        FROM {IDR_CLAIM_TABLE} {clm}
        WHERE
            {claim_filter(start_time, partition)} AND
            {clm}.clm_idr_ld_dt >= '{model_type.min_transaction_date}'
    """


def clm_query() -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
        FROM claim_base {clm}
        WHERE (
            {clm}.clm_idr_ld_dt {{FILTER_OP}} {{LAST_TS}}
            OR {clm}.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
            OR {clm}.idr_updt_ts {{FILTER_OP}} {{LAST_TS}}
        )
    """


def clm_child_query(table: str) -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
        FROM {table} temp
        JOIN claim_base clm ON
            {clm}.geo_bene_sk = temp.geo_bene_sk AND
            {clm}.clm_dt_sgntr_sk = temp.clm_dt_sgntr_sk AND
            {clm}.clm_type_cd = temp.clm_type_cd AND
            {clm}.clm_num_sk = temp.clm_num_sk
        WHERE (temp.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
            OR temp.idr_updt_ts {{FILTER_OP}} {{LAST_TS}})
        """


def clm_ansi_sgntr_query() -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
        FROM {IDR_CLAIM_ANSI_SIGNATURE_TABLE} sgntr
        JOIN claim_base clm ON
            {clm}.clm_dt_sgntr_sk = sgntr.clm_ansi_sgntr_sk
        WHERE (sgntr.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
            OR sgntr.idr_updt_ts {{FILTER_OP}} {{LAST_TS}})
    """


def clm_dt_sgntr_query() -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
        FROM {IDR_CLAIM_DATE_SIGNATURE_TABLE} sgntr
        JOIN claim_base clm ON
            {clm}.clm_dt_sgntr_sk = sgntr.clm_dt_sgntr_sk
        WHERE (sgntr.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
            OR sgntr.idr_updt_ts {{FILTER_OP}} {{LAST_TS}})
        """


def clm_ocrnc_sgntr_query() -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
        FROM {IDR_CLAIM_OCCURRENCE_SIGNATURE_TABLE} sgntr
        JOIN claim_base clm ON
            {clm}.clm_ocrnc_sgntr_sk = sgntr.clm_ocrnc_sgntr_sk
        WHERE sgntr.clm_ocrnc_span_cd IN ('{QUALIFYING_STAY_CD}', '{NON_COVERED_STAY_CD}')
        AND (
            sgntr.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
            OR sgntr.idr_updt_ts {{FILTER_OP}} {{LAST_TS}}
        )
    """


def clm_rlt_ocrnc_clause() -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
        FROM {IDR_CLAIM_RELATED_OCCURRENCE_SIGNATURE_TABLE} sgntr
        JOIN claim_base clm ON
            {clm}.clm_rlt_ocrnc_sgntr_sk = sgntr.clm_rlt_ocrnc_sgntr_sk
        WHERE sgntr.clm_rlt_ocrnc_cd IN ('{MEDICARE_EXHAUSTED_CD}', '{ACTIVE_CARE_CD}') AND (
            sgntr.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
            OR sgntr.idr_updt_ts {{FILTER_OP}} {{LAST_TS}}
            )
    """


def clm_rlt_cond_sgntr_query() -> str:
    clm = ALIAS_CLM
    return f"""
        SELECT
            {clm}.clm_uniq_id,
            {clm}.geo_bene_sk,
            {clm}.clm_type_cd,
            {clm}.clm_num_sk,
            {clm}.clm_dt_sgntr_sk,
            {clm}.clm_idr_ld_dt
            FROM {IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE} sgntr
            JOIN claim_base {clm} ON
                {clm}.clm_rlt_cond_sgntr_sk = sgntr.clm_rlt_cond_sgntr_sk
            WHERE sgntr.clm_rlt_cond_sgntr_sk NOT IN (0, 1, -1)
                AND sgntr.clm_rlt_cond_cd != '~'
                AND (
                    sgntr.idr_insrt_ts {{FILTER_OP}} {{LAST_TS}}
                    OR sgntr.idr_updt_ts {{FILTER_OP}} {{LAST_TS}}
                )
    """


def base_claim_filter(partition: LoadPartition) -> str:
    clm = ALIAS_CLM
    claim_type_codes = partition.claim_type_codes or ALL_CLAIM_TYPE_CODES
    return f"""
    ({clm}.clm_type_cd IN ({",".join([str(c) for c in claim_type_codes])})
    AND {clm}.clm_from_dt >= '{MIN_CLAIM_LOAD_DATE}')
    """


PRIMARY_KEY = "primary_key"
BATCH_TIMESTAMP = "batch_timestamp"
HISTORICAL_BATCH_TIMESTAMP = "historical_batch_timestamp"
BATCH_ID = "batch_id"
UPDATE_TIMESTAMP = "update_timestamp"
ALIAS = "alias"
INSERT_EXCLUDE = "insert_exclude"
LAST_UPDATED_TIMESTAMP = "last_updated_timestamp"
EXPR = "expr"
DERIVED = "derived"
COLUMN_MAP = "column_map"
FISS_CLM_SOURCE = "21000"
MCS_CLM_SOURCE = "22000"
VMS_CLM_SOURCE = "23000"


ALIAS_CLM = "clm"
ALIAS_DCMTN = "dcmtn"
ALIAS_LINE_DCMTN = "line_dcmtn"
ALIAS_LINE_MCS = "line_mcs"
ALIAS_LINE_PRFNL = "line_prfnl"
ALIAS_LINE_INSTNL = "line_instnl"
ALIAS_LINE_FISS = "line_fiss"
ALIAS_LINE_FISS_BFNT = "line_fiss_bnft"
ALIAS_SGNTR = "sgntr"
ALIAS_ANSI_SGNTR = "ansi_sgntr"
ALIAS_LINE = "line"
ALIAS_RX_LINE = "rx_line"
ALIAS_FISS = "fiss"
ALIAS_PROCEDURE = "prod"
ALIAS_INSTNL = "instnl"
ALIAS_PRFNL = "prfnl"
ALIAS_VAL = "val"
ALIAS_HSTRY = "hstry"
ALIAS_PRVDR_PRSCRBNG = "prvdr_prscrbng"
ALIAS_PRVDR_SRVC = "prvdr_srvc"
ALIAS_PRVDR_BLG = "prvdr_blg"
ALIAS_PRVDR_RFRG = "prvdr_rfrg"
ALIAS_PRVDR_RNDRNG = "prvdr_rndrng"
ALIAS_PRVDR_ATNDG = "prvdr_atndg"
ALIAS_PRVDR_OPRTG = "prvdr_oprtg"
ALIAS_PRVDR_OTHR = "prvdr_othr"
ALIAS_XREF = "xref"
ALIAS_LCTN_HSTRY = "lctn_hstry"
ALIAS_CLM_GRP = "clm_grp"
ALIAS_RLT_COND = "rltcond"
ALIAS_PBP_NUM = "pbp_num"
ALIAS_CNTRCT_SGMT = "sgmt"
ALIAS_OCRNC_SGNTR = "ocrnc_sgntr_mbr"
ALIAS_RLT_OCRNC_SGNTR = "rlt_ocrnc_sgntr_mbr"
ALIAS_OCRNC_SGNTR_DERIVED_DATES = "ocrnc_sgntr_mbr_derived_dates"
ALIAS_RLT_OCRNC_SGNTR_DERIVED_DATES = "rlt_ocrnc_sgntr_mbr_derived_dates"
ALIAS_LINE_FISS_BFNT_FLATTENED = "line_fiss_bnft_flattened"

INSERT_FIELD = {BATCH_TIMESTAMP: True, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_insrt_ts"}
UPDATE_FIELD = {UPDATE_TIMESTAMP: True, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_updt_ts"}


class LoadMode(StrEnum):
    LOCAL = "local"
    SYNTHETIC = "synthetic"
    PROD = "prod"


class Source(StrEnum):
    POSTGRES = "postgres"
    SNOWFLAKE = "snowflake"


class ModelType(Enum):
    CLAIM_INSTITUTIONAL_NCH = (
        MIN_CLAIM_NCH_TRANSACTION_DATE,
        CLAIM_INSTITUTIONAL_NCH_TABLE,
        INSTITUTIONAL_NCH_PARTITIONS,
    )
    CLAIM_INSTITUTIONAL_SS = (
        MIN_CLAIM_SS_TRANSACTION_DATE,
        CLAIM_INSTITUTIONAL_SS_TABLE,
        INSTITUTIONAL_SS_PARTITIONS,
    )
    CLAIM_PROFESSIONAL_NCH = (
        MIN_CLAIM_NCH_TRANSACTION_DATE,
        CLAIM_PROFESSIONAL_NCH_TABLE,
        PROFESSIONAL_NCH_PARTITIONS,
    )
    CLAIM_PROFESSIONAL_SS = (
        MIN_CLAIM_SS_TRANSACTION_DATE,
        CLAIM_PROFESSIONAL_SS_TABLE,
        PROFESSIONAL_SS_PARTITIONS,
    )
    CLAIM_RX = (MIN_CLAIM_NCH_TRANSACTION_DATE, CLAIM_RX_TABLE, PART_D_PARTITIONS)
    BENEFICIARY = (DEFAULT_MIN_DATE, BENEFICIARY_TABLE, [NON_CLAIM_PARTITION])
    LOAD_PROGRESS = (DEFAULT_MIN_DATE, "", EMPTY_PARTITION)

    def __init__(
        self,
        min_transaction_date: str,
        last_updated_table: str,
        partitions: list[LoadPartitionGroup],
    ) -> None:
        self._min_transaction_date = format_date(min_transaction_date)
        self._last_updated_table = last_updated_table
        self._partitions = partitions

    @property
    def min_transaction_date(self) -> datetime:
        return self._min_transaction_date

    @property
    def last_updated_table(self) -> str:
        return self._last_updated_table

    @property
    def partitions(self) -> list[LoadPartitionGroup]:
        return self._partitions


class IdrBaseModel(BaseModel, ABC):
    @staticmethod
    @abstractmethod
    def table() -> str:
        """BFD table name populated by this model."""

    @classmethod
    def last_updated_date_table(cls) -> str:
        if not cls.last_updated_date_column():
            return ""
        return cls.model_type().last_updated_table

    @staticmethod
    @abstractmethod
    def last_updated_date_column() -> list[str]:
        """BFD column to keep track of last updated date for this model."""

    @staticmethod
    def should_replace() -> bool:
        """Whether to merge or replace data when loading this table."""
        return False

    @classmethod
    @abstractmethod
    def fetch_query(
        cls,
        partition: LoadPartition,
        start_time: datetime,
        source: Source,
    ) -> str:
        """Query used to fetch the data."""

    @staticmethod
    @abstractmethod
    def model_type() -> ModelType:
        """Value representing the type of model."""

    @classmethod
    def unique_key(cls) -> list[str]:
        return cls._extract_meta_keys(PRIMARY_KEY)

    @classmethod
    def batch_timestamp_col(cls, is_historical: bool) -> list[str]:
        if is_historical:
            historical_cols = cls._extract_meta_keys(HISTORICAL_BATCH_TIMESTAMP)
            if len(historical_cols) > 0:
                return historical_cols
        return cls._extract_meta_keys(BATCH_TIMESTAMP)

    @classmethod
    def batch_timestamp_col_alias(cls, is_historical: bool) -> list[str]:
        return [cls._format_column_alias(col) for col in cls.batch_timestamp_col(is_historical)]

    @classmethod
    def update_timestamp_col(cls) -> list[str]:
        return cls._extract_meta_keys(UPDATE_TIMESTAMP)

    @classmethod
    def batch_id_col_alias(cls) -> str | None:
        col = cls._single_or_default(BATCH_ID)
        if col:
            return cls._format_column_alias(col)
        return None

    @classmethod
    def batch_id_col(cls) -> str | None:
        return cls._single_or_default(BATCH_ID)

    @classmethod
    def last_updated_timestamp_col(cls) -> str | None:
        return cls._single_or_default(LAST_UPDATED_TIMESTAMP)

    @classmethod
    def update_timestamp_col_alias(cls) -> list[str]:
        return [cls._format_column_alias(col) for col in cls.update_timestamp_col()]

    @classmethod
    def _extract_meta_keys(cls, meta_key: str) -> list[str]:
        return [key for key in cls.model_fields if cls._extract_meta(key, meta_key)]

    @classmethod
    def _single_or_default(cls, meta_key: str) -> str | None:
        keys = cls._extract_meta_keys(meta_key)
        if len(keys) > 1:
            raise LookupError(f"cls {cls.__name__} has more than one key for {meta_key}")
        return keys[0] if len(keys) == 1 else None

    @classmethod
    def _extract_meta(cls, key: str, meta_key: str) -> object | None:
        metadata = cls.model_fields[key].metadata

        for meta in metadata:
            if isinstance(meta, Iterable) and meta_key in meta:
                return meta[meta_key]  # type: ignore
        return None

    @classmethod
    def _map_meta_if_present(
        cls, key: str, meta_key: str, map_output: Callable[[Any], str]
    ) -> str | None:
        metadata = cls.model_fields[key].metadata
        extracted = cls._extract_meta(key, meta_key)
        if extracted is not None:
            for meta in metadata:
                if meta_key in meta:
                    return map_output(meta)
        return None

    @classmethod
    def _format_column_alias(cls, key: str) -> str:
        meta_expr = cls._map_meta_if_present(key, EXPR, lambda meta: meta[EXPR])
        if meta_expr:
            return meta_expr
        alias = cls._map_meta_if_present(
            key, ALIAS, lambda meta: f"{meta[ALIAS]}.{cls._get_column_map(key)}"
        )
        if alias:
            return alias
        return key

    @classmethod
    def _get_column_map_alias(cls, key: str) -> str:
        alias = cls._format_column_alias(key)
        return f"{alias} AS {key}"

    @classmethod
    def _get_column_map(cls, key: str) -> str:
        mapped_col = cls._extract_meta(key, COLUMN_MAP)
        return str(mapped_col) if mapped_col else key

    @classmethod
    def column_aliases(cls) -> list[str]:
        return [cls._get_column_map_alias(key) for key in cls._columns_raw() if key]

    @classmethod
    def columns_raw(cls) -> list[str]:
        return [cls._get_column_map(key) for key in cls._columns_raw()]

    @classmethod
    def _columns_raw(cls) -> list[str]:
        return [key for key in cls.model_fields if not cls._extract_meta(key, DERIVED)]

    @classmethod
    def insert_keys(cls) -> list[str]:
        return [
            key for key in cls.model_fields if not cls._extract_meta(key, INSERT_EXCLUDE)
        ] + list(cls.model_computed_fields)


T = TypeVar("T", bound=IdrBaseModel)


def deceased_bene_filter(alias: str, start_time: datetime) -> str:
    return f"""
            SELECT bene_sk
            FROM {IDR_BENE_HISTORY_TABLE} {alias}
            WHERE {alias}.bene_vrfy_death_day_sw = 'Y'
            AND {alias}.bene_death_dt < DATE '{start_time.strftime("%Y-%m-%d")}'
            - INTERVAL '{DEATH_DATE_CUTOFF_YEARS} years'
    """


def idr_dates_from_meta_sk() -> str:
    """
    Generate SQL expressions to derive insert and update timestamps from META keys.

    Assumptions about META_SK / META_LST_UPDT_SK format:
      - The key encodes a date as: (YYYYMMDD - DATE_BASE_OFFSET) * DATE_SEQUENCE_DIVISOR + sequence
      - The last 3 digits represent a sequence and are discarded
      - Adding DATE_BASE_OFFSET restores a YYYYMMDD-formatted date
    """
    # Base offset added to reconstruct a YYYYMMDD date (e.g., 12501023 + 19000000 = 20250123)
    date_base_offset = 19000000
    # Divisor used to strip the sequence portion from the key
    date_sequence_divisor = 1000
    date_from_meta_sk = f"""(meta_sk / {date_sequence_divisor}) + {date_base_offset}"""
    date_from_meta_updt_sk = (
        f"""(meta_lst_updt_sk / {date_sequence_divisor}) + {date_base_offset}"""
    )

    # Logic for computing INSRT/UPDT timestamp on tables that don't have it based on META_SK
    # META_SK is an IDR-derived field and the calculations here have no significance outside of
    # IDR's internal encoding logic.
    return f"""
            meta_sk,
            meta_lst_updt_sk,
            -- When (META_SK = 501), fall back to META_LST_UPDT_SK.
            CASE
                WHEN meta_sk = 501 THEN
                    TO_TIMESTAMP(
                            TO_DATE(
                                    TRUNC({date_from_meta_updt_sk})::text,
                                    'YYYYMMDD'
                            )::text || ' 00:00:00',
                            'YYYY-MM-DD HH24:MI:SS'
                    )
                ELSE
                    TO_TIMESTAMP(
                            TO_DATE(
                                    TRUNC({date_from_meta_sk})::text,
                                    'YYYYMMDD'
                            )::text || ' 00:00:00',
                            'YYYY-MM-DD HH24:MI:SS'
                    )
                END AS idr_insrt_ts,

            CASE
                WHEN meta_sk != 501 AND meta_lst_updt_sk > 0 THEN
                    TO_TIMESTAMP(
                            TO_DATE(
                                    TRUNC({date_from_meta_updt_sk})::text,
                                    'YYYYMMDD'
                            )::text || ' 00:00:00',
                            'YYYY-MM-DD HH24:MI:SS'
                    )
                ELSE NULL
                END AS idr_updt_ts
    """


def claim_filter(start_time: datetime, partition: LoadPartition) -> str:
    clm = ALIAS_CLM
    # For part D, we want ALL the claims
    # For other claim types, we can filter only latest claims if LATEST_CLAIMS is enabled
    if LATEST_CLAIMS and (
        (PartitionType.PART_D | PartitionType.ALL) & partition.partition_type > 0
    ):
        codes = ",".join(str(code) for code in PART_D_CLAIM_TYPE_CODES)
        latest_claim_ind = f" AND ({clm}.clm_ltst_clm_ind = 'Y' OR {clm}.clm_type_cd IN ({codes})) "
    elif LATEST_CLAIMS:
        latest_claim_ind = f" AND ({clm}.clm_ltst_clm_ind = 'Y') "
    else:
        latest_claim_ind = ""

    # PAC data older than 60 days should be filtered
    pac_cutoff_date = start_time - timedelta(days=60)
    start_time_sql = pac_cutoff_date.strftime("'%Y-%m-%d %H:%M:%S'")
    pac_phase_1_min = 1000
    pac_phase_1_max = 1999
    # Note: checking clm_type_cd as the first branch of the OR here might be more efficient
    # Since it's more likely to return true
    pac_filter = (
        f"""
        AND
        (
            {clm}.clm_type_cd NOT BETWEEN {pac_phase_1_min} AND {pac_phase_1_max}
            OR
            (
                {clm}.clm_src_id IN (
                    '{FISS_CLM_SOURCE}',
                    '{MCS_CLM_SOURCE}',
                    '{VMS_CLM_SOURCE}'
                )
                AND
                COALESCE(
                    {clm}.idr_updt_ts,
                    {clm}.idr_insrt_ts,
                    {clm}.clm_idr_ld_dt) >= {start_time_sql}
            )
        )
    """
        if (PartitionType.PAC | PartitionType.ALL) & partition.partition_type != 0
        else ""
    )

    clm_from_filter = (
        (
            f" AND ({clm}.clm_from_dt BETWEEN "
            f"'{partition.start_date.strftime('%Y-%m-%d')}' AND "
            f"'{partition.end_date.strftime('%Y-%m-%d')}')"
        )
        if (partition.start_date is not None and partition.end_date is not None)
        else f" AND {clm}.clm_from_dt >= '{MIN_CLAIM_LOAD_DATE}'"
    )

    claim_type_codes = partition.claim_type_codes or ALL_CLAIM_TYPE_CODES
    hstry = ALIAS_HSTRY
    return f"""
    (
        {clm}.bene_sk != 0
        AND NOT EXISTS ({deceased_bene_filter(hstry, start_time)}
            AND {hstry}.bene_sk = {clm}.bene_sk)
        AND {clm}.clm_type_cd IN ({",".join([str(c) for c in claim_type_codes])})
        {clm_from_filter}
        {latest_claim_ind}
        {pac_filter}
        AND {ALIAS_CLM}.clm_from_dt <= {ALIAS_CLM}.clm_thru_dt
    )
    """


def transform_default_hipps_code(value: str | None) -> str:
    if value is None or value == "00000":
        return ""
    return value


def claim_occurrence_cte() -> str:
    ocrnc_sgntr = ALIAS_OCRNC_SGNTR
    # Note: idr_updt_ts is always null
    return f"""
            SELECT
                clm_ocrnc_sgntr_sk,
                MAX(CASE WHEN clm_ocrnc_span_cd = '{NON_COVERED_STAY_CD}'
                    THEN clm_ocrnc_span_from_dt END) AS bfd_clm_ncvrd_from_dt,
                MAX(CASE WHEN clm_ocrnc_span_cd = '{NON_COVERED_STAY_CD}'
                    THEN clm_ocrnc_span_thru_dt END) AS bfd_clm_ncvrd_thru_dt,
                MAX(CASE WHEN clm_ocrnc_span_cd = '{QUALIFYING_STAY_CD}'
                    THEN clm_ocrnc_span_from_dt END) AS bfd_clm_qlfy_stay_from_dt,
                MAX(CASE WHEN clm_ocrnc_span_cd = '{QUALIFYING_STAY_CD}'
                    THEN clm_ocrnc_span_thru_dt END) AS bfd_clm_qlfy_stay_thru_dt,
                MAX(idr_insrt_ts) AS idr_insrt_ts
            FROM {IDR_CLAIM_OCCURRENCE_SIGNATURE_TABLE} {ocrnc_sgntr}
            WHERE clm_ocrnc_span_cd IN ('{QUALIFYING_STAY_CD}', '{NON_COVERED_STAY_CD}')
            GROUP BY clm_ocrnc_sgntr_sk"""


def claim_related_occurrences_cte() -> str:
    rlt_ocrnc_sgntr = ALIAS_RLT_OCRNC_SGNTR

    # Note: idr_updt_ts is always null
    return f"""
            SELECT
                clm_rlt_ocrnc_sgntr_sk,
                MAX(CASE WHEN clm_rlt_ocrnc_cd = '{MEDICARE_EXHAUSTED_CD}'
                    THEN clm_rlt_ocrnc_dt END) AS bfd_clm_mdcr_exhstd_dt,
                MAX(CASE WHEN clm_rlt_ocrnc_cd = '{ACTIVE_CARE_CD}'
                    THEN clm_rlt_ocrnc_dt END) AS bfd_clm_actv_care_thru_dt,
                MAX(idr_insrt_ts) AS idr_insrt_ts
            FROM {IDR_CLAIM_RELATED_OCCURRENCE_SIGNATURE_TABLE} {rlt_ocrnc_sgntr}
            WHERE clm_rlt_ocrnc_cd in ('{MEDICARE_EXHAUSTED_CD}', '{ACTIVE_CARE_CD}')
            GROUP BY clm_rlt_ocrnc_sgntr_sk
    """


def claim_related_conditions_cte(source: Source) -> str:
    rlt_cond = ALIAS_RLT_COND
    clm_rlt_cond_cd_agg = ""
    if source == Source.SNOWFLAKE:
        clm_rlt_cond_cd_agg = """
            ARRAY_AGG(
                CASE
                    WHEN LENGTH(clm_rlt_cond_cd) = 2 THEN clm_rlt_cond_cd
                    ELSE '~' || clm_rlt_cond_cd
                END
            ) WITHIN GROUP (ORDER BY clm_rlt_cond_sgntr_sqnc_num, clm_rlt_cond_cd)
        """
    else:
        clm_rlt_cond_cd_agg = """
            ARRAY_AGG(
                CASE
                    WHEN LENGTH(clm_rlt_cond_cd) = 2 THEN clm_rlt_cond_cd
                    ELSE '~' || clm_rlt_cond_cd
                END
                ORDER BY clm_rlt_cond_sgntr_sqnc_num, clm_rlt_cond_cd
            )
        """
    # Note: idr_updt_ts is always null
    return f"""
            SELECT
                clm_rlt_cond_sgntr_sk,
                ARRAY_TO_STRING({clm_rlt_cond_cd_agg}, '') AS clm_rlt_cond_cd,
                MAX(idr_insrt_ts) AS idr_insrt_ts
            FROM {IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE} {rlt_cond}
            WHERE clm_rlt_cond_sgntr_sk NOT IN (0, 1, -1)
            AND clm_rlt_cond_cd != '~'
            GROUP BY clm_rlt_cond_sgntr_sk
    """


class IdrClaimRx(IdrBaseModel):
    # Columns from v2_mdcr_clm
    clm_uniq_id: Annotated[
        int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_CLM, LAST_UPDATED_TIMESTAMP: True}
    ]
    clm_type_cd: Annotated[int, {ALIAS: ALIAS_CLM}]
    bene_sk: Annotated[int, ALIAS:ALIAS_CLM]
    clm_cntl_num: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_prnt_cntl_num: Annotated[
        str,
        {
            ALIAS: ALIAS_CLM,
            EXPR: f"""CASE
                WHEN {ALIAS_CLM}.clm_cntl_num = {ALIAS_CLM}.clm_prnt_cntl_num
                THEN ''
                ELSE {ALIAS_CLM}.clm_prnt_cntl_num
                END""",
        },
        BeforeValidator(transform_default_string),
    ]
    clm_orig_cntl_num: Annotated[
        str,
        {
            ALIAS: ALIAS_CLM,
            EXPR: clm_orig_cntl_num_expr(),
        },
        BeforeValidator(transform_default_string),
    ]
    clm_from_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_thru_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_efctv_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_obslt_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_finl_actn_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_pd_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ltst_clm_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_adjstmt_type_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_sbmt_frmt_cd: Annotated[str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)]
    clm_sbmtr_cntrct_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_sbmtr_cntrct_pbp_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_othr_tp_pd_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    prvdr_srvc_id_qlfyr_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_srvc_prvdr_gnrc_id_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    prvdr_prsbng_id_qlfyr_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_prsbng_prvdr_gnrc_id_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    idr_insrt_ts_clm: Annotated[
        datetime,
        {ALIAS: ALIAS_CLM, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_clm: Annotated[
        datetime,
        {ALIAS: ALIAS_CLM, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    clm_idr_ld_dt: Annotated[date, {HISTORICAL_BATCH_TIMESTAMP: True, ALIAS: ALIAS_CLM}]

    # Columns from v2_mdcr_clm_line
    clm_line_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_cvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_from_dt: Annotated[date, {ALIAS: ALIAS_LINE}]
    clm_line_thru_dt: Annotated[date, {ALIAS: ALIAS_LINE}]
    clm_line_ndc_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_line_ndc_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ndc_qty_qlfyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_srvc_unit_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_rx_num: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_line_othr_tp_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ncvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    idr_insrt_ts_line: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from V2_MDCR_CLM_LINE_RX
    clm_brnd_gnrc_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_cmpnd_cd: Annotated[str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)]
    clm_ctstrphc_cvrg_ind_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_daw_prod_slctn_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_drug_cvrg_stus_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_line_days_suply_qty: Annotated[int | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_grs_above_thrshld_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_grs_blw_thrshld_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_ingrdnt_cst_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_lis_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_plro_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_rx_fill_num: Annotated[int | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_rx_orgn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_sls_tax_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_srvc_cst_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_troop_tot_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_vccn_admin_fee_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_ltc_dspnsng_mthd_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_phrmcy_srvc_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcng_excptn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_ptnt_rsdnc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_rptd_gap_dscnt_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_rptd_mftr_dscnt_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_rebt_passthru_pos_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_cms_calcd_mftr_dscnt_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_grs_cvrd_cst_tot_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_phrmcy_price_dscnt_at_pos_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    idr_insrt_ts_line_rx: Annotated[
        datetime,
        {ALIAS: ALIAS_RX_LINE, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_rx: Annotated[
        datetime,
        {ALIAS: ALIAS_RX_LINE, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_cntrct_pbp_num
    cntrct_pbp_name: Annotated[
        str, {ALIAS: ALIAS_PBP_NUM}, BeforeValidator(transform_default_string)
    ]

    # Columns from v2_mdcr_clm_dt_sgntr
    clm_cms_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_submsn_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    idr_insrt_ts_sgntr: Annotated[
        datetime,
        {ALIAS: ALIAS_SGNTR, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_sgntr: Annotated[
        datetime,
        {ALIAS: ALIAS_SGNTR, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_prvdr_hstry
    prvdr_srvc_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_last_or_lgl_name: Annotated[
        str,
        {EXPR: provider_last_or_legal_name_expr(ALIAS_PRVDR_SRVC)},
        BeforeValidator(transform_default_string),
    ]

    prvdr_prscrbng_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_prscrbng_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_PRSCRBNG, None)},
        BeforeValidator(transform_default_string),
    ]

    @override
    @staticmethod
    def table() -> str:
        return CLAIM_RX_TABLE

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.CLAIM_RX

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        clm = ALIAS_CLM
        line = ALIAS_LINE
        rx_line = ALIAS_RX_LINE
        sgntr = ALIAS_SGNTR
        prvdr_srvc = ALIAS_PRVDR_SRVC
        prvdr_prscrbng = ALIAS_PRVDR_PRSCRBNG
        pbp_num = ALIAS_PBP_NUM
        return f"""
            WITH claim_base AS (
                {clm_base_query(start_time, partition, cls.model_type())}
            ),
            claims AS (
                {clm_query()}
                UNION
                {clm_dt_sgntr_query()}
                UNION
                {clm_child_query(IDR_CLAIM_LINE_TABLE)}
                UNION
                {clm_child_query(IDR_CLAIM_LINE_RX_TABLE)}
            ),
            contracts AS (
                SELECT cntrct_pbp_name, cntrct_num, cntrct_pbp_num,
                RANK() OVER (
                    PARTITION BY cntrct_num, cntrct_pbp_num
                    ORDER BY cntrct_pbp_sk_obslt_dt DESC
                ) AS contract_version_rank
                FROM {IDR_CONTRACT_PBP_NUM_TABLE}
            )
            SELECT {{COLUMNS}}
            FROM claims c
            JOIN {IDR_CLAIM_TABLE} {clm} ON
                {clm}.geo_bene_sk = c.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = c.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = c.clm_type_cd AND
                {clm}.clm_num_sk = c.clm_num_sk
            JOIN {IDR_CLAIM_DATE_SIGNATURE_TABLE} {sgntr} ON
                {sgntr}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
            JOIN {IDR_CLAIM_LINE_TABLE} {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            LEFT JOIN {IDR_CLAIM_LINE_RX_TABLE} {rx_line} ON
                {clm}.geo_bene_sk = {rx_line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {rx_line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {rx_line}.clm_type_cd AND
                {clm}.clm_num_sk = {rx_line}.clm_num_sk
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_srvc}
                ON {prvdr_srvc}.prvdr_npi_num = {clm}.prvdr_srvc_prvdr_npi_num
                AND {prvdr_srvc}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_prscrbng}
                ON {prvdr_prscrbng}.prvdr_npi_num = {clm}.prvdr_prscrbng_prvdr_npi_num
                AND {prvdr_prscrbng}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN contracts {pbp_num}
                ON {pbp_num}.cntrct_num = {clm}.clm_sbmtr_cntrct_num
                AND {pbp_num}.cntrct_pbp_num = {clm}.clm_sbmtr_cntrct_pbp_num
                AND {pbp_num}.contract_version_rank = 1
            {{WHERE_CLAUSE}} AND {base_claim_filter(partition)}
            {{ORDER_BY}}
        """


class Timer:
    def __init__(self, name: str, model: type[T], partition: LoadPartition) -> None:
        self.perf_start = 0.0
        self.name = name
        self.model = model
        self.partition = partition

    def start(self) -> None:
        self.perf_start = time.perf_counter()

    def stop(self) -> None:
        segment = time.perf_counter() - self.perf_start
        print(f"{self.model.table()}-{self.partition.name} {self.name}: {segment:.6f} seconds")


@dataclass
class CsvFile:
    cols: list[str]
    table: str
    csv_file: Path

    def cols_str(self) -> str:
        return ",".join(self.cols)

    def full_table(self) -> str:
        return f"{IDR_PREFIX}.{self.table}"


# TODO: UP046 seems to cause issues with pyright
class Extractor(ABC, Generic[T]):  # noqa: UP046
    def __init__(self, cls: type[T], partition: LoadPartition) -> None:
        self.cls = cls
        self.type_adapter = TypeAdapter(list[self.cls])
        self.partition = partition
        self.cursor_execute_timer = Timer("cursor_execute", cls, partition)
        self.cursor_fetch_timer = Timer("cursor_fetch", cls, partition)
        self.transform_timer = Timer("transform", cls, partition)

    @abstractmethod
    def extract_many(self, sql: str, params: dict[str, DbType]) -> Iterator[Sequence[T]]:
        pass

    @abstractmethod
    def reconnect(self) -> None:
        pass

    @abstractmethod
    def close(self) -> None:
        pass

    def _coalesce_dates(self, cols: list[str]) -> list[str]:
        return [f"COALESCE({col}, '{DEFAULT_MIN_DATE}')" for col in cols]

    def _greatest_col(self, cols: list[str]) -> str:
        return f"GREATEST({','.join(cols)})"

    def _get_batch_size(self) -> int:
        if ENABLE_DATE_PARTITIONS:
            # Larger tables take up more memory, so we'll try to normalize
            # the total memory used here based on the number of columns
            return round(BATCH_MULTIPLIER / len(self.cls.columns_raw()))
        # If date partitioning is not enabled, the number of concurrent jobs will be small
        return 100_000

    def get_query(self, start_time: datetime, source: Source) -> str:
        query = self.cls.fetch_query(self.partition, start_time, source)
        columns = ",".join(self.cls.column_aliases())
        columns_raw = ",".join(self.cls.columns_raw())
        return query.replace("{COLUMNS}", columns).replace("{COLUMNS_NO_ALIAS}", columns_raw)

    def extract_idr_data(
        self, progress: LoadProgress | None, start_time: datetime, source: Source
    ) -> Iterator[Sequence[T]]:
        is_historical = progress is None or progress.is_historical()
        fetch_query = self.get_query(start_time, source)
        # GREATEST doesn't work with nulls so we need to coalesce here
        batch_timestamp_cols = self._coalesce_dates(
            self.cls.batch_timestamp_col_alias(is_historical)
        )
        update_timestamp_cols = self._coalesce_dates(self.cls.update_timestamp_col_alias())
        # We need to create batches using the most recent timestamp from all of the
        # insert/update timestamps
        batch_timestamp_clause = self._greatest_col([*batch_timestamp_cols, *update_timestamp_cols])
        min_transaction_date = self.cls.model_type().min_transaction_date

        batch_id_order = ""
        batch_id_clause = ""
        batch_id_col = self.cls.batch_id_col_alias()
        if batch_id_col is not None:
            batch_id_order = f", {batch_id_col}"
        logger.info("extracting %s", self.cls.table())
        order_by = f"ORDER BY {batch_timestamp_clause} {batch_id_order}"
        if progress is None:
            # No saved progress, process the whole table from the beginning
            return self.extract_many(
                fetch_query.replace(
                    "{WHERE_CLAUSE}",
                    f"WHERE ({batch_timestamp_clause} >= %(timestamp)s)",
                )
                .replace("{FILTER_OP}", ">=")
                .replace("{LAST_TS}", "%(timestamp)s")
                .replace("{ORDER_BY}", order_by),
                {"timestamp": min_transaction_date},
            )

        previous_batch_complete = progress.batch_complete_ts >= progress.job_start_ts
        min_batch_completion_date = format_date_opt(MIN_BATCH_COMPLETION_DATE)
        if (
            previous_batch_complete
            and min_batch_completion_date
            and progress.batch_complete_ts > min_batch_completion_date
        ):
            # If we've set a min completion date, we don't need to reprocess any batches that have
            # already completed within the given timeframe.
            # This helps for large loads that may have been interrupted recently.
            return iter([])

        # If we've completed the last batch, there shouldn't be any additional records
        # with the same timestamp/id.
        # Additionally, if there's a batch_id column, records with the same timestamp will be
        # filtered by the batch_id filter.
        filter_op = ">" if previous_batch_complete or batch_id_col is not None else ">="
        # insertion timestamps aren't always representative of the time the data is available in
        # Snowflake, so we should always start loading from the most recent timestamp
        # that we've already fetched
        compare_timestamp = max(min_transaction_date, progress.last_ts)

        if batch_id_col is not None:
            batch_id_clause = f"""
                OR (
                    {batch_timestamp_clause} = %(timestamp)s
                    AND {batch_id_col} {filter_op} {progress.last_id}
                )"""

        # Saved progress found, start processing from where we left off
        return self.extract_many(
            fetch_query.replace(
                "{WHERE_CLAUSE}",
                f"""
                    WHERE (
                        {batch_timestamp_clause} {filter_op} %(timestamp)s
                        {batch_id_clause}
                    )
                    """,
            )
            .replace("{FILTER_OP}", filter_op)
            .replace("{LAST_TS}", "%(timestamp)s")
            .replace("{ORDER_BY}", order_by),
            {"timestamp": compare_timestamp},
        )

    def _transform(self, batch: list[dict[str, DbType]]) -> Sequence[T]:
        self.transform_timer.start()
        res = self.type_adapter.validate_python(
            [{k.lower(): v for k, v in row.items()} for row in batch]
        )
        self.transform_timer.stop()
        return res


class DbExecutor(ABC):
    @abstractmethod
    def copy(self, file: CsvFile) -> None:
        pass

    @abstractmethod
    def query(self, sql: str, params: dict[str, DbType] | None = None) -> list[dict[str, DbType]]:
        pass

    @abstractmethod
    def execute(self, sql: str, params: dict[str, DbType] | None = None) -> None:
        pass

    @abstractmethod
    def commit(self) -> None:
        pass


class PostgresExtractor(Extractor[T]):
    def __init__(self, cls: type[T], partition: LoadPartition, load_mode: LoadMode) -> None:
        super().__init__(cls, partition)
        self.connection_string = get_connection_string(load_mode)
        self.conn = psycopg.connect(self.connection_string)

    @override
    def reconnect(self) -> None:
        self.conn = psycopg.connect(self.connection_string)

    @override
    def extract_many(
        self,
        sql: str,
        params: Mapping[str, DbType],
    ) -> Iterator[Sequence[T]]:
        logger.debug(sql)
        batch_size = self._get_batch_size()
        with self.conn.cursor(row_factory=dict_row) as cur:
            cur.execute(sql, params)  # type: ignore
            batch = cur.fetchmany(batch_size)
            while len(batch) > 0:
                yield self._transform(batch)
                batch = cur.fetchmany(batch_size)

    def extract_single(self, sql: str, params: dict[str, DbType]) -> T | None:
        with self.conn.cursor(row_factory=dict_row) as cur:
            cur.execute(sql, params)  # type: ignore
            res = cur.fetchone()
            if res:
                return self._transform([res])[0]
            return None

    @override
    def close(self) -> None:
        self.conn.close()


class PostgresExecutor(DbExecutor):
    def __init__(self, conn: psycopg.connection.Connection) -> None:
        self.conn = conn

    @override
    def execute(self, sql: str, params: dict[str, DbType] | None = None) -> None:
        cur = self.conn.cursor(row_factory=dict_row)
        cur.execute(sql, params)  # type: ignore

    @override
    def query(self, sql: str, params: dict[str, DbType] | None = None) -> list[dict[str, DbType]]:
        cur = self.conn.cursor(row_factory=dict_row)
        res = cur.execute(sql, params)  # type: ignore
        return res.fetchall()  # type: ignore

    @override
    def commit(self) -> None:
        self.conn.commit()

    @override
    def copy(self, file: CsvFile) -> None:
        with self.conn.cursor(row_factory=dict_row) as cur, file.csv_file.open() as f:
            reader = csv.DictReader(f)
            # skip empty files
            if reader.fieldnames is None:
                return

            with cur.copy(
                f"COPY {file.full_table()} ({file.cols_str()}) FROM STDIN"  # type: ignore
            ) as copy:
                for row in reader:
                    copy.write_row([row[c] or None for c in file.cols])


class SnowflakeExtractor(Extractor[T]):
    def __init__(self, cls: type[T], partition: LoadPartition) -> None:
        super().__init__(cls, partition)
        self.conn = SnowflakeExtractor.connect()

    @override
    def reconnect(self) -> None:
        self.conn = SnowflakeExtractor.connect()

    @staticmethod
    def connect() -> SnowflakeConnection:
        private_key = serialization.load_pem_private_key(
            IDR_PRIVATE_KEY.encode(),
            password=None,
            backend=default_backend(),
        )
        private_key_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        return snowflake.connector.connect(  # type: ignore
            user=IDR_USERNAME,
            private_key=private_key_bytes,
            account=IDR_ACCOUNT,
            warehouse=IDR_WAREHOUSE,
            database=IDR_DATABASE,
            schema=IDR_SCHEMA,
        )

    @override
    def extract_many(
        self,
        sql: str,
        params: dict[str, DbType],
    ) -> Iterator[Sequence[T]]:
        cur = None
        logger.debug(sql)
        try:
            self.cursor_execute_timer.start()
            cur = self.conn.cursor(DictCursor)
            cur.execute(sql, params)
            self.cursor_execute_timer.stop()

            self.cursor_fetch_timer.start()
            # fetchmany can return list[dict] or list[tuple] but we'll only use
            # queries that return dicts
            batch_size = self._get_batch_size()
            batch: list[dict[str, DbType]] = cur.fetchmany(batch_size)
            self.cursor_fetch_timer.stop()

            while len(batch) > 0:  # type: ignore
                yield self._transform(batch)

                self.cursor_fetch_timer.start()
                batch = cur.fetchmany(batch_size)
                self.cursor_fetch_timer.stop()
            return

        finally:
            if cur:
                cur.close()

    @override
    def close(self) -> None:
        self.conn.close()


class SnowflakeExecutor(DbExecutor):
    def __init__(self) -> None:
        private_key = serialization.load_pem_private_key(
            IDR_PRIVATE_KEY.encode(),
            password=None,
            backend=default_backend(),
        )
        private_key_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        self.session = Session.builder.configs(
            {
                "account": IDR_ACCOUNT,
                "user": IDR_USERNAME,
                "private_key": private_key_bytes,  # type: ignore
                "warehouse": IDR_WAREHOUSE,
                "database": IDR_DATABASE,
                "schema": IDR_SCHEMA,
            }
        ).create()
        self.conn = SnowflakeExtractor.connect()

    @override
    def commit(self) -> None:
        self.conn.commit()

    @override
    def copy(self, file: CsvFile) -> None:
        self.session.sql("create or replace temp stage source_stage").collect()
        self.session.file.put(str(file.csv_file.absolute()), "@source_stage")
        self.session.sql(f"""COPY INTO
                            {file.full_table()}
                            FROM @source_stage/{file.csv_file.name}
                            FILE_FORMAT = (
                                TYPE = 'CSV',
                                PARSE_HEADER = TRUE
                                ERROR_ON_COLUMN_COUNT_MISMATCH = FALSE
                                FIELD_OPTIONALLY_ENCLOSED_BY = '"'
                            )
                            MATCH_BY_COLUMN_NAME = CASE_INSENSITIVE
                            """).collect()
        self.session.commit()

    @override
    def execute(self, sql: str, params: dict[str, DbType] | None = None) -> None:
        cur = self.conn.cursor(DictCursor)
        cur.execute(sql, params)

    @override
    def query(self, sql: str, params: dict[str, DbType] | None = None) -> list[dict[str, DbType]]:
        cur = self.conn.cursor(DictCursor)
        res = cur.execute(sql, params).fetchall()  # type: ignore
        return [{k.lower(): r[k] for k in r} for r in res]  # type: ignore


def should_track_load_progress(load_mode: LoadMode) -> bool:
    # Whether to read/write load progress, which is diabled for synthetic and testing loads.
    return load_mode == LoadMode.PROD or force_load_progress()


class LoadProgress(IdrBaseModel):
    table_name: str
    last_ts: datetime
    last_id: int
    batch_partition: str
    job_start_ts: datetime
    batch_complete_ts: datetime

    @staticmethod
    def query_placeholder() -> str:
        return "table_name"

    @override
    @staticmethod
    def table() -> str:
        return "idr.load_progress"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.LOAD_PROGRESS

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        return f"""
        SELECT table_name, last_ts, last_id, batch_partition, job_start_ts, batch_complete_ts
        FROM idr.load_progress
        WHERE table_name = %({LoadProgress.query_placeholder()})s
        AND batch_partition = '{partition.name}'
        """

    def is_historical(self) -> bool:
        # 2021-4-18 is the most recent date where idr_insrt_ts could be null in claims data
        return self.last_ts <= datetime(2021, 4, 19, tzinfo=UTC)


def get_progress(
    load_mode: LoadMode,
    source: Source,
    table_name: str,
    start_time: datetime,
    partition: LoadPartition,
) -> LoadProgress | None:
    if not should_track_load_progress(load_mode):
        return None

    return PostgresExtractor(
        load_mode=load_mode, cls=LoadProgress, partition=partition
    ).extract_single(
        LoadProgress.fetch_query(partition, start_time, source),
        {LoadProgress.query_placeholder(): table_name},
    )


def extract_and_load[T: IdrBaseModel](
    cls: type[T], job_start: datetime, partition: LoadPartition
) -> Iterator[Sequence[T]]:
    data_extractor = SnowflakeExtractor(cls=cls, partition=partition)

    logger.info("loading %s", cls.table())
    last_error = datetime.min.replace(tzinfo=UTC)
    error_count = 0
    max_errors = 3

    while True:
        try:
            progress = get_progress(
                LoadMode.PROD, Source.SNOWFLAKE, cls.table(), job_start, partition
            )

            if progress:
                logger.info(
                    "progress for %s %s - last_ts: %s job_start_ts: %s batch_complete_ts: %s",
                    cls.table(),
                    progress.batch_partition,
                    progress.last_ts,
                    progress.job_start_ts,
                    progress.batch_complete_ts,
                )
            else:
                logger.info("no previous progress for %s - %s", cls.table(), partition.name)

            return data_extractor.extract_idr_data(progress, job_start, Source.SNOWFLAKE)
        # Snowflake will throw a reauth error if the pipeline has been running for several hours
        # but it seems to be wrapped in a ProgrammingError.
        # Unclear the best way to handle this, it will require a bit more trial and error
        except (
            ReauthenticationRequest,
            RetryRequest,
            ForbiddenError,
            ProgrammingError,
        ) as ex:
            time_expired = datetime.now(UTC) - last_error > timedelta(seconds=10)
            if time_expired:
                error_count = 0
            error_count += 1
            if error_count < max_errors:
                last_error = datetime.now(UTC)
                logger.warning("received transient error, retrying...", exc_info=ex)
                data_extractor.reconnect()
            else:
                logger.error("max attempts exceeded")
                raise ex
            time.sleep(1)
        except Exception as ex:
            logger.error("error loading %s", cls.table(), exc_info=ex)
            raise ex


class DatabaseDetailsModel(BaseModel):
    host: str
    user: str
    password: str
    port: int = 5432
    dbname: str = "fhirdb"

    @classmethod
    def from_conn_str(cls, conn_str: str) -> DatabaseDetailsModel:
        return DatabaseDetailsModel.model_validate(
            dict(entry.split("=") for entry in conn_str.split(" "))
        )

    @classmethod
    def from_env(cls) -> DatabaseDetailsModel:
        return DatabaseDetailsModel.model_validate(
            {
                "host": os.environ.get("PGHOST"),
                "user": os.environ.get("PGUSER"),
                "password": os.environ.get("PGPASSWORD"),
                "port": os.environ.get("PGPORT"),
                "dbname": os.environ.get("PGDATABASE"),
            }
        )


def _sql_type(val: Any | None, field_name: str, col_types: dict[str, str]) -> str:
    if val is None:
        return f"::{col_types[field_name]}[]"

    if isinstance(val, str):
        return "::text[]"

    return ""


def _remove_null_bytes(val: DbType) -> DbType:
    # Some IDR strings have null bytes.
    # Postgres doesn't allow these in text fields.
    # We can't use a UTF-8 validator here since technically these are valid UTF-8
    # and we can't use string.printable because that only contains ASCII fields
    # so neither of those validation techniques will remove null bytes
    # and still allow other valid UTF-8 characters.
    if type(val) is str:
        return val.replace("\x00", "")
    return val


async def _get_col_types(cur: psycopg.AsyncCursor[DictRow], table: str) -> dict[str, str]:
    type_aliases = {"character varying": "text", "timestamp with time zone": "timestamptz"}
    await cur.execute(f"""
    SELECT column_name, data_type FROM information_schema.columns
    WHERE table_schema = 'idr' and table_name = '{table.split(".")[1]}'
    """)  # type: ignore
    return {
        row["column_name"]: type_aliases.get(str(row["data_type"]), str(row["data_type"]))
        for row in await cur.fetchall()
    }


async def upsert[T: IdrBaseModel](
    pool: psycopg_pool.AsyncConnectionPool,
    model: type[T],
    partition: LoadPartition,
    batch: tuple[int, list[T]],
    start_time: datetime,
) -> None:
    batch_num, data = batch
    insert_cols = list(model.insert_keys())
    upsert_timer = Timer(f"upsert_{batch_num}", model, partition)
    meta_keys = ["bfd_created_ts", "bfd_updated_ts"]

    params: dict[str, list[Any]] = defaultdict(list)
    for result in data:
        for k in insert_cols:
            params[k].append(_remove_null_bytes(getattr(result, k)))
    for col in meta_keys:
        params[col].append([start_time] * len(data))

    async with pool.connection() as conn, conn.cursor(row_factory=dict_row) as cur:
        unique_key = model.unique_key()
        update_set = ", ".join([f"{v}=EXCLUDED.{v}" for v in insert_cols if v not in unique_key])
        # For immutable tables, we may still be attempting to re-load some data
        # due to a batch cancellation.
        # In these cases, we can assume any conflicting rows have already been loaded so
        # "DO NOTHING" is appropriate here.
        # Additionally, if there are no extra columns to update, we can skip it.
        on_conflict = (
            f"DO UPDATE SET {update_set}, bfd_updated_ts=%(conflict_timestamp)s "
            "WHERE (t.*) IS DISTINCT FROM (EXCLUDED.*)"
        )

        col_types = await _get_col_types(cur, model.table())

        # Upsert into the main table
        upsert_timer.start()
        await cur.execute(
            f"""
            INSERT INTO {model.table()} AS t ({", ".join(insert_cols)}, {", ".join(meta_keys)})
            SELECT * FROM UNNEST({", ".join([f"%({x})s{_sql_type(params[x][0], x, col_types)}" for x in [*insert_cols, *meta_keys]])})
            ON CONFLICT ({",".join(unique_key)}) {on_conflict}
            """,  # type: ignore
            params | {"conflict_timestamp": start_time},
            binary=True,
        )
        upsert_timer.stop()


async def main() -> None:
    job_start = datetime.now(UTC)
    model = IdrClaimRx
    partition = next(
        model.model_type().partitions[0].generate_ranges(LoadType.INCREMENTAL, job_start)
    )
    idr_data_timer = Timer("idr_time", model, partition)
    total_insert_timer = Timer("total_insert", model, partition)
    source_data = extract_and_load(model, job_start, partition)
    db_details = DatabaseDetailsModel.from_env()
    async with psycopg_pool.AsyncConnectionPool(
        conninfo=" ".join(f"{k}={v}" for k, v in db_details.model_dump().items()),
        min_size=50,
        max_size=100,
    ) as pool:
        logger.info("Connected to database")
        while True:
            start_time = datetime.now(UTC)
            idr_data_timer.start()
            results_raw = next(source_data, None)
            results = list(results_raw) if results_raw else []
            idr_data_timer.stop()
            if not results:
                break

            logger.info("Loaded %d results, starting batched upserts", len(results))

            total_insert_timer.start()
            batched_data = [
                (i, list(x)) for i, x in enumerate(itertools.batched(results, 1000, strict=False))
            ]
            await asyncio.gather(
                *(upsert(pool, model, partition, batch, start_time) for batch in batched_data)  # pyright: ignore[reportArgumentType]
            )
            total_insert_timer.stop()


if __name__ == "__main__":
    if not anyio.run(main):
        sys.exit(1)
