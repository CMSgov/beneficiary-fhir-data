from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_TABLE,
    COMBINED_CLAIM_PARTITION,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_SGNTR,
    BATCH_ID,
    BATCH_TIMESTAMP,
    HISTORICAL_BATCH_TIMESTAMP,
    INSERT_EXCLUDE,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    transform_null_date_to_min,
    transform_null_or_default_date_to_max,
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
                {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
                {{ORDER_BY}}
            )
            SELECT {{COLUMNS_NO_ALIAS}} FROM dupes WHERE row_order = 1
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [COMBINED_CLAIM_PARTITION]
