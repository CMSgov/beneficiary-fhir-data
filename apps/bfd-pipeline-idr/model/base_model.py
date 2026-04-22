from abc import ABC, abstractmethod
from collections.abc import Callable, Iterable
from datetime import UTC, date, datetime, timedelta
from enum import Enum, StrEnum
from typing import Any, TypeVar, cast

from pydantic import BaseModel

from constants import (
    ALL_CLAIM_TYPE_CODES,
    ALTERNATE_DEFAULT_DATE,
    BENEFICIARY_TABLE,
    CLAIM_INSTITUTIONAL_NCH_TABLE,
    CLAIM_INSTITUTIONAL_SS_TABLE,
    CLAIM_PROFESSIONAL_NCH_TABLE,
    CLAIM_PROFESSIONAL_SS_TABLE,
    CLAIM_RX_TABLE,
    DEATH_DATE_CUTOFF_YEARS,
    DEFAULT_MAX_DATE,
    DEFAULT_MIN_DATE,
    EMPTY_PARTITION,
    IDR_BENE_HISTORY_TABLE,
    IDR_CLAIM_ANSI_SIGNATURE_TABLE,
    IDR_CLAIM_DATE_SIGNATURE_TABLE,
    IDR_CLAIM_OCCURRENCE_SIGNATURE_TABLE,
    IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE,
    IDR_CLAIM_RELATED_OCCURRENCE_SIGNATURE_TABLE,
    IDR_CLAIM_TABLE,
    INSTITUTIONAL_NCH_PARTITIONS,
    INSTITUTIONAL_SS_PARTITIONS,
    NON_CLAIM_PARTITION,
    PART_D_CLAIM_TYPE_CODES,
    PART_D_PARTITIONS,
    PROFESSIONAL_NCH_PARTITIONS,
    PROFESSIONAL_SS_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup, PartitionType
from settings import (
    LATEST_CLAIMS,
    MIN_CLAIM_LOAD_DATE,
    MIN_CLAIM_NCH_TRANSACTION_DATE,
    MIN_CLAIM_SS_TRANSACTION_DATE,
)

type DbType = str | float | int | bool | date | datetime


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
    IDR = ""


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
        load_mode: LoadMode,
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


def deceased_bene_filter(alias: str) -> str:
    return f"""
            SELECT bene_sk
            FROM {IDR_BENE_HISTORY_TABLE} {alias}
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

    claim_type_codes = partition.claim_type_codes or ALL_CLAIM_TYPE_CODES
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


def claim_related_conditions_cte(load_mode: LoadMode) -> str:
    rlt_cond = ALIAS_RLT_COND
    clm_rlt_cond_cd_agg = ""
    if load_mode == LoadMode.IDR:
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
