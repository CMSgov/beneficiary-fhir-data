from abc import ABC, abstractmethod
from collections.abc import Callable, Iterable, Sequence
from datetime import UTC, date, datetime, timedelta
from enum import StrEnum
from typing import Annotated, Any, TypeVar, cast

from pydantic import BaseModel, BeforeValidator

from constants import (
    ALL_CLAIM_TYPE_CODES,
    ALTERNATE_DEFAULT_DATE,
    BENEFICIARY_TABLE,
    DEATH_DATE_CUTOFF_YEARS,
    DEFAULT_MAX_DATE,
    DEFAULT_MIN_DATE,
    MIN_CLAIM_LOAD_DATE,
    NON_CLAIM_PARTITION,
    PART_D_CLAIM_TYPE_CODES,
)
from load_partition import LoadPartition, LoadPartitionGroup, PartitionType
from settings import LATEST_CLAIMS, MIN_TRANSACTION_DATE

type DbType = str | float | int | bool | date | datetime


class LoadMode(StrEnum):
    LOCAL = "local"
    SYNTHETIC = "synthetic"
    PRODUCTION = ""


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


def provider_type_expr(alias: str) -> str:
    provider_type_organization = "1"
    provider_type_individual = "2"
    return f"""
        CASE 
            WHEN {alias}.prvdr_npi_num IS NULL 
            THEN NULL
            WHEN {alias}.prvdr_lgl_name IS NULL OR {alias}.prvdr_lgl_name IN ('', '~')
            THEN {provider_type_individual}
            ELSE {provider_type_organization}
        END
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


def _deceased_bene_filter(alias: str) -> str:
    return f"""
            SELECT bene_sk
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry {alias}
            WHERE {alias}.bene_vrfy_death_day_sw = 'Y'
            AND {alias}.bene_death_dt < CURRENT_DATE - INTERVAL '{DEATH_DATE_CUTOFF_YEARS} years'
    """


def _idr_dates_from_meta_sk() -> str:
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


class IdrBeneficiary(IdrBaseModel):
    # columns from V2_MDCR_BENE_HSTRY
    bene_sk: Annotated[
        int,
        {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_HSTRY, LAST_UPDATED_TIMESTAMP: True},
    ]
    bene_xref_efctv_sk: int
    bene_mbi_id: str
    bene_1st_name: str
    bene_midl_name: Annotated[str, BeforeValidator(transform_null_string)]
    bene_last_name: str
    bene_brth_dt: date
    bene_death_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    bene_vrfy_death_day_sw: Annotated[str, BeforeValidator(transform_default_string)]
    bene_sex_cd: str
    bene_race_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_usps_state_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_zip5_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_zip_plc_name: Annotated[str, BeforeValidator(transform_default_string)]
    bene_line_1_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_2_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_3_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_4_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_5_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_6_adr: Annotated[str, BeforeValidator(transform_null_string)]
    cntct_lang_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts_bene: Annotated[
        datetime, {BATCH_TIMESTAMP: True, ALIAS: ALIAS_HSTRY, COLUMN_MAP: "idr_insrt_ts"}
    ]
    idr_updt_ts_bene: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_HSTRY, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from V2_MDCR_BENE_XREF
    bene_kill_cred_cd: Annotated[str, BeforeValidator(transform_default_string)]
    src_rec_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date_to_min)]
    idr_insrt_ts_xref: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_XREF, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_xref: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_XREF, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def computed_keys() -> list[str]:
        return ["bene_xref_efctv_sk_computed"]

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_patient_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        xref = ALIAS_XREF
        # There can be multiple xref records for the same bene_sk/bene_ref_sk combo
        # so we need to find the most recent one based on src_rec_updt_ts.

        # Unlike idr_updt_ts, src_rec_updt_ts will be set to the created timestamp
        # if no update has been applied. Therefore, we can just check the updated timestamp
        # without caring about the created timestamp.

        # There can also be duplicate values with the same idr_insrt_ts, so we have to rely on
        # src_rec_insrt_ts/src_rec_updt_ts for this.
        return f"""
            WITH ordered_xref AS (
                SELECT bene_sk,
                    bene_xref_sk,
                    bene_hicn_num,
                    src_rec_crte_ts,
                    ROW_NUMBER() OVER (
                        PARTITION BY bene_sk, bene_xref_sk
                        ORDER BY src_rec_updt_ts DESC
                    ) AS row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref
            ),
            current_xref AS (
                SELECT
                    ox.bene_sk,
                    ox.bene_xref_sk,
                    bx.bene_kill_cred_cd,
                    bx.src_rec_updt_ts,
                    bx.idr_insrt_ts,
                    bx.idr_updt_ts
                FROM ordered_xref ox
                JOIN cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref bx
                    ON bx.bene_sk = ox.bene_sk
                    AND bx.bene_xref_sk = ox.bene_xref_sk
                    AND bx.bene_hicn_num = ox.bene_hicn_num
                    AND bx.src_rec_crte_ts = ox.src_rec_crte_ts
                WHERE ox.row_order = 1
            ),
            deceased_benes AS (
                {_deceased_bene_filter(hstry)}
            )
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry {hstry}
            -- NOTE: the join condition is intentionally inverted here
            -- In the xref table, the bene_sk and bene_xref_sk fields are mirrored
            LEFT JOIN current_xref {xref}
                ON {xref}.bene_sk = {hstry}.bene_xref_sk
                AND {xref}.bene_xref_sk = {hstry}.bene_sk
            {{WHERE_CLAUSE}}
            AND {hstry}.bene_mbi_id IS NOT NULL
            AND NOT EXISTS (SELECT 1 FROM deceased_benes db WHERE db.bene_sk = {hstry}.bene_sk)
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryMbiId(IdrBaseModel):
    bene_mbi_id: Annotated[str, {PRIMARY_KEY: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_mbi_efctv_dt: date
    bene_mbi_obslt_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_mbi_id"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_patient_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return """
               SELECT {COLUMNS}
               FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id
                   {WHERE_CLAUSE}
                   {ORDER_BY} \
               """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryOvershareMbi(IdrBaseModel):
    bene_mbi_id: Annotated[str, {PRIMARY_KEY: True}]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_overshare_mbi"

    @staticmethod
    def should_replace() -> bool:
        return True

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        # The xref data in the bene_hstry table is not completely reliable
        # because sometimes HICNs can be reused, causing two records to be
        # xref'd even if they're not the same person.

        # We'll only trust xref records that have a valid entry in the bene_xref
        # table (this means it's coming from CME). For any MBIs tied to more than
        # one bene_sk that doesn't have a valid xref, we will prevent it from being
        # shown since it may be incorrectly linked to more than one person.
        return """
               SELECT hstry.bene_mbi_id
               FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry hstry
               WHERE NOT EXISTS (
                   SELECT 1
                   FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref xref
                   WHERE hstry.bene_xref_efctv_sk = xref.bene_sk
                     AND hstry.bene_sk = xref.bene_xref_sk
                     AND xref.bene_kill_cred_cd = '2'
               ) AND hstry.bene_mbi_id IS NOT NULL
               GROUP BY hstry.bene_mbi_id
               HAVING COUNT(DISTINCT hstry.bene_sk) > 1 \
               """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryThirdParty(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_buyin_cd: Annotated[str, BeforeValidator(transform_default_string)]
    bene_tp_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_third_party"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_a_coverage_updated_ts",
            "bfd_part_b_coverage_updated_ts",
        ]

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp tp
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {_deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = tp.bene_sk
            )
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryStatus(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_mdcr_stus_cd: str
    mdcr_stus_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    mdcr_stus_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_status"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_a_coverage_updated_ts",
            "bfd_part_b_coverage_updated_ts",
        ]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_stus stus
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {_deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = stus.bene_sk
            )
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryEntitlement(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_stus_cd: str
    bene_mdcr_enrlmt_rsn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_entitlement"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_a_coverage_updated_ts",
            "bfd_part_b_coverage_updated_ts",
        ]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt entlmt
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {_deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = entlmt.bene_sk
            )
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryEntitlementReason(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_rsn_cd: str
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_entitlement_reason"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_a_coverage_updated_ts",
            "bfd_part_b_coverage_updated_ts",
        ]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt_rsn rsn
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {_deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = rsn.bene_sk
            )
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryDualEligibility(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_mdcd_elgblty_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcd_elgblty_end_dt: date
    bene_dual_stus_cd: str
    bene_dual_type_cd: str
    geo_usps_state_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_dual_eligibility"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_dual_coverage_updated_ts",
        ]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_cmbnd_dual_mdcr dual
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {_deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = dual.bene_sk
            )
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrContractPbpNumber(IdrBaseModel):
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_PBP_NUM}]
    cntrct_drug_plan_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_name: Annotated[str, BeforeValidator(transform_null_string)]
    cntrct_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_sgmt_num: Annotated[
        str, ALIAS:ALIAS_CNTRCT_SGMT, BeforeValidator(transform_default_string)
    ]
    bfd_contract_version_rank: Annotated[int, {DERIVED: True}]

    @staticmethod
    def table() -> str:
        return "idr.contract_pbp_number"

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        pbp_num = ALIAS_PBP_NUM
        # We need to include obsolete records since some bene_mapd records are tied to
        # obsolete pbp_sks.
        # Additionally, some contracts are marked obsolete and no non-obsolete record
        # is created, so we have to use RANK to get the latest version of each contract.
        # Then, these can be queries by searching for rows where
        # bfd_contract_version_rank = 1
        return f"""
            WITH sgmt as (
                SELECT
                    cntrct_pbp_sk,
                    cntrct_pbp_sgmt_num
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_sgmt
                GROUP BY cntrct_pbp_sk, cntrct_pbp_sgmt_num
                HAVING COUNT(*) = 1
            )
            SELECT 
                {{COLUMNS}},
                RANK() OVER (
                    PARTITION BY cntrct_num, cntrct_pbp_num 
                    ORDER BY cntrct_pbp_sk_obslt_dt DESC) AS bfd_contract_version_rank
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num {pbp_num}
            LEFT JOIN sgmt
                    ON {pbp_num}.cntrct_pbp_sk = sgmt.cntrct_pbp_sk
            WHERE {pbp_num}.cntrct_pbp_sk != 0
            """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrContractPbpContact(IdrBaseModel):
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True}]
    cntrct_plan_cntct_obslt_dt: date
    cntrct_plan_cntct_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_free_extnsn_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_free_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_extnsn_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_tel_num: Annotated[str, BeforeValidator(transform_null_string)]
    cntrct_pbp_end_dt: date
    cntrct_pbp_bgn_dt: date
    cntrct_plan_cntct_st_1_adr: Annotated[str, BeforeValidator(transform_null_string)]
    cntrct_plan_cntct_st_2_adr: Annotated[str, BeforeValidator(transform_null_string)]
    cntrct_plan_cntct_city_name: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_state_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_zip_cd: Annotated[str, BeforeValidator(transform_default_string)]

    @staticmethod
    def table() -> str:
        return "idr.contract_pbp_contact"

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return f"""
            WITH contract_contacts as (
                SELECT {{COLUMNS}}, ROW_NUMBER() OVER (
                    PARTITION BY cntrct_pbp_sk
                ORDER BY cntrct_pbp_bgn_dt,
                CASE
                    WHEN cntrct_plan_cntct_type_cd = '62' THEN 1
                    WHEN cntrct_plan_cntct_type_cd = '30' THEN 2
                    ELSE 3
                END) as row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_cntct cntct
                WHERE cntrct_plan_cntct_obslt_dt >= '{DEFAULT_MAX_DATE}'
                AND cntrct_pbp_bgn_dt >= DATE_TRUNC('MONTH', CURRENT_DATE)
                AND cntrct_pbp_bgn_dt < cntrct_pbp_end_dt
            )
            SELECT {{COLUMNS}} FROM contract_contacts WHERE row_order = 1
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryMaPartDEnrollment(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    cntrct_pbp_sk: int
    bene_pbp_num: str
    bene_enrlmt_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_enrlmt_end_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    bene_cntrct_num: str
    bene_cvrg_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    bene_enrlmt_pgm_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_enrlmt_emplr_sbsdy_sw: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_ma_part_d_enrollment"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_c_coverage_updated_ts",
            "bfd_part_d_coverage_updated_ts",
        ]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        # There are only a very few instances where non-obsolete records have a
        # bene_enrlmt_pgm_type_cd set to '~' and these are all from the 80s,
        # so it should be safe to filter these.
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mapd_enrlmt enrlmt
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {_deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = enrlmt.bene_sk
            )
            AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
            AND bene_enrlmt_pgm_type_cd != '~'
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryMaPartDEnrollmentRx(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    cntrct_pbp_sk: int
    bene_cntrct_num: Annotated[str, {PRIMARY_KEY: True}]
    bene_pbp_num: Annotated[str, {PRIMARY_KEY: True}]
    bene_enrlmt_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_pdp_enrlmt_mmbr_id_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_pdp_enrlmt_grp_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_pdp_enrlmt_prcsr_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_pdp_enrlmt_bank_id_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_enrlmt_pdp_rx_info_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_ma_part_d_enrollment_rx"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_part_d_coverage_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
                SELECT {{COLUMNS}}
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mapd_enrlmt_rx enrlmt_rx
                {{WHERE_CLAUSE}}
                AND NOT EXISTS (
                    {_deceased_bene_filter(hstry)}
                    AND {hstry}.bene_sk = enrlmt_rx.bene_sk
                )
                AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
                {{ORDER_BY}}
            """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


class IdrBeneficiaryLowIncomeSubsidy(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_rng_bgn_dt: Annotated[datetime, {PRIMARY_KEY: True}]
    bene_rng_end_dt: date
    bene_lis_copmt_lvl_cd: str
    bene_lis_ptd_prm_pct: str
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_low_income_subsidy"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_part_d_coverage_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
                SELECT {{COLUMNS}}
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_lis bene_lis
                {{WHERE_CLAUSE}}
                AND NOT EXISTS (
                    {_deceased_bene_filter(hstry)}
                    AND {hstry}.bene_sk = bene_lis.bene_sk
                )
                AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
                {{ORDER_BY}}
            """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]


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
        AND NOT EXISTS ({_deceased_bene_filter(hstry)} AND {hstry}.bene_sk = {clm}.bene_sk)
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


class IdrProviderHistory(IdrBaseModel):
    prvdr_npi_num: Annotated[str, {PRIMARY_KEY: True, BATCH_ID: True}]
    prvdr_sk: int
    prvdr_hstry_efctv_dt: datetime
    prvdr_mdl_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_txnmy_cmpst_cd: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_oscar_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_1st_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_hstry_obslt_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    prvdr_lgl_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_emplr_id_num: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_last_name: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, DERIVED: True},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, DERIVED: True},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.provider_history"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return f"""
            SELECT
            {_idr_dates_from_meta_sk()},
            {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry
            WHERE prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return []


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
