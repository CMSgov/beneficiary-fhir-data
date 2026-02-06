from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    ALL_CLAIM_PARTITIONS,
    CLAIM_PROFESSIONAL_NCH_TABLE,
    CLAIM_TABLE,
    COMBINED_CLAIM_PARTITION,
    DEFAULT_MAX_DATE,
    PROFESSIONAL_ADJUDICATED_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_CLM_GRP,
    ALIAS_DCMTN,
    ALIAS_LINE,
    ALIAS_LINE_DCMTN,
    ALIAS_LINE_MCS,
    ALIAS_LINE_PRFNL,
    ALIAS_PRFNL,
    ALIAS_PROCEDURE,
    ALIAS_PRVDR_BLG,
    ALIAS_PRVDR_RFRG,
    ALIAS_PRVDR_RNDRNG,
    ALIAS_PRVDR_SRVC,
    ALIAS_SGNTR,
    ALIAS_VAL,
    BATCH_ID,
    BATCH_TIMESTAMP,
    COLUMN_MAP,
    EXPR,
    HISTORICAL_BATCH_TIMESTAMP,
    INSERT_EXCLUDE,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    _claim_filter,
    get_min_transaction_date,
    provider_last_name_expr,
    transform_default_and_zero_string,
    transform_default_date_to_null,
    transform_default_int_to_null,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_int,
    transform_null_or_default_date_to_max,
    transform_null_string,
    transform_provider_name,
)


class IdrClaimDateSignature(IdrBaseModel):
    clm_dt_sgntr_sk: Annotated[
        int,
        {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_SGNTR, LAST_UPDATED_TIMESTAMP: True},
    ]
    clm_cms_proc_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_actv_care_from_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_dschrg_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_submsn_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_ncvrd_from_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_ncvrd_thru_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_actv_care_thru_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_mdcr_exhstd_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_nch_wkly_proc_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_qlfy_stay_from_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_qlfy_stay_thru_dt: Annotated[date, BeforeValidator(transform_null_or_default_date_to_max)]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_SGNTR},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_SGNTR},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_date_signature"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        sgntr = ALIAS_SGNTR
        return f"""
            WITH dupes as (
                SELECT {{COLUMNS}}, ROW_NUMBER() OVER (
                    PARTITION BY {sgntr}.clm_dt_sgntr_sk
                    {{ORDER_BY}} DESC)
                AS row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
                JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dt_sgntr {sgntr}
                ON {clm}.clm_dt_sgntr_sk = {sgntr}.clm_dt_sgntr_sk
                {{WHERE_CLAUSE}} AND {_claim_filter(start_time, partition)}
                {{ORDER_BY}}
            )
            SELECT {{COLUMNS_NO_ALIAS}} FROM dupes WHERE row_order = 1
        """

    @staticmethod
    def _fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [COMBINED_CLAIM_PARTITION]
