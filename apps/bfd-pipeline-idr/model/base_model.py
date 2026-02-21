from abc import ABC, abstractmethod
from collections.abc import Callable, Iterable, Sequence
from datetime import UTC, date, datetime, timedelta
from enum import StrEnum
from typing import Any, TypeVar, cast

from pydantic import BaseModel

from constants import (
    ALL_CLAIM_TYPE_CODES,
    ALTERNATE_DEFAULT_DATE,
    DEATH_DATE_CUTOFF_YEARS,
    DEFAULT_MAX_DATE,
    DEFAULT_MIN_DATE,
    MIN_CLAIM_LOAD_DATE,
    PART_D_CLAIM_TYPE_CODES,
)
from load_partition import LoadPartition, LoadPartitionGroup, PartitionType
from settings import LATEST_CLAIMS, MIN_TRANSACTION_DATE

type DbType = str | float | int | bool | date | datetime


class LoadMode(StrEnum):
    LOCAL = "local"
    SYNTHETIC = "synthetic"
    IDR = ""


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


def transform_null_string(value: str | None) -> str:
    if value is None:
        return ""
    return value


def transform_provider_name(value: str | None) -> str:
    if value is None or value == "<UNAVAIL>":
        return ""
    return value


def transform_default_string(value: str | None) -> str:
    if value is None or value == "~":
        return ""
    return value


def transform_default_and_zero_string(value: str | None) -> str:
    if value is None or value == "~" or value == "0":
        return ""
    return value


def transform_empty_string(value: str | None) -> str:
    if value is None:
        return ""
    return value.strip()


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


def get_min_transaction_date(default_date: str = DEFAULT_MIN_DATE) -> datetime:
    if MIN_TRANSACTION_DATE is not None:
        return format_date(MIN_TRANSACTION_DATE)
    return format_date(default_date)


def provider_last_name_expr(alias: str, claim_field: str) -> str:
    return f"""
        CASE WHEN {alias}.prvdr_last_name IS NULL OR {alias}.prvdr_last_name IN ('', '~')
        THEN {ALIAS_CLM}.{claim_field}
        ELSE {alias}.prvdr_last_name
        END
    """


def provider_last_or_legal_name_expr(alias: str) -> str:
    return f"COALESCE({alias}.prvdr_lgl_name, {alias}.prvdr_last_name)"


def provider_careteam_name_expr(alias: str, type: str | None) -> str:
    return f"""
        COALESCE(
            {f"{ALIAS_CLM}.clm_{type}_prvdr_name," if type else ""}
            {provider_last_or_legal_name_expr(alias)} || ',' || {alias}.prvdr_1st_name
        )
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

INSERT_FIELD = {BATCH_TIMESTAMP: True, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_insrt_ts"}
UPDATE_FIELD = {UPDATE_TIMESTAMP: True, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_updt_ts"}


class IdrBaseModel(BaseModel, ABC):
    @staticmethod
    @abstractmethod
    def table() -> str:
        """BFD table name populated by this model."""

    @staticmethod
    @abstractmethod
    def last_updated_date_table() -> str:
        """BFD table to keep track of last updated date for this model."""

    @staticmethod
    @abstractmethod
    def last_updated_date_column() -> list[str]:
        """BFD column to keep track of last updated date for this model."""

    @staticmethod
    def should_replace() -> bool:
        """Whether to merge or replace data when loading this table."""
        return False

    @staticmethod
    @abstractmethod
    def fetch_query(
        partition: LoadPartition,
        start_time: datetime,
        load_mode: LoadMode,
    ) -> str:
        """Query used to fetch the data."""

    @staticmethod
    @abstractmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        """Partitions fetch queries of this model to allow for parallel fetching of data via ray.

        [] is returned for models that do not do any such partitioning.
        """

    @staticmethod
    def computed_keys() -> list[str]:
        return []

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
        return [key for key in cls.model_fields if not cls._extract_meta(key, INSERT_EXCLUDE)]


T = TypeVar("T", bound=IdrBaseModel)


def deceased_bene_filter(alias: str) -> str:
    return f"""
            SELECT bene_sk
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry {alias}
            WHERE {alias}.bene_vrfy_death_day_sw = 'Y'
            AND {alias}.bene_death_dt < CURRENT_DATE - INTERVAL '{DEATH_DATE_CUTOFF_YEARS} years'
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

    claim_type_codes = (
        partition.claim_type_codes if partition.claim_type_codes else ALL_CLAIM_TYPE_CODES
    )
    hstry = ALIAS_HSTRY
    return f"""
    (
        {clm}.bene_sk != 0
        AND NOT EXISTS ({deceased_bene_filter(hstry)} AND {hstry}.bene_sk = {clm}.bene_sk)
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

    @staticmethod
    def table() -> str:
        return "idr.load_progress"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return f"""
        SELECT table_name, last_ts, last_id, batch_partition, job_start_ts, batch_complete_ts
        FROM idr.load_progress
        WHERE table_name = %({LoadProgress.query_placeholder()})s 
        AND batch_partition = '{partition.name}'
        """

    def is_historical(self) -> bool:
        # 2021-4-18 is the most recent date where idr_insrt_ts could be null in claims data
        return self.last_ts <= datetime(2021, 4, 19, tzinfo=UTC)

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return []
