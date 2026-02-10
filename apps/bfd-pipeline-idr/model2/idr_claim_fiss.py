from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_INSTITUTIONAL_NCH_TABLE,
    CLAIM_PROFESSIONAL_NCH_TABLE,
    CLAIM_TABLE,
    DEFAULT_MAX_DATE,
    INSTITUTIONAL_ADJUDICATED_PARTITIONS,
    INSTITUTIONAL_PAC_PARTITIONS,
    PROFESSIONAL_ADJUDICATED_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_CLM_GRP,
    ALIAS_DCMTN,
    ALIAS_FISS,
    ALIAS_INSTNL,
    ALIAS_LCTN_HSTRY,
    ALIAS_LINE,
    ALIAS_LINE_DCMTN,
    ALIAS_LINE_MCS,
    ALIAS_LINE_PRFNL,
    ALIAS_PRFNL,
    ALIAS_PROCEDURE,
    ALIAS_PRVDR_ATNDG,
    ALIAS_PRVDR_BLG,
    ALIAS_PRVDR_OPRTG,
    ALIAS_PRVDR_OTHR,
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
    transform_default_date_to_null,
    transform_default_int_to_null,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_int,
    transform_null_string,
    transform_provider_name,
)


class IdrClaimFiss(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_crnt_stus_cd: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_FISS},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_FISS},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_fiss"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        fiss = ALIAS_FISS
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_fiss {fiss} ON
                {clm}.geo_bene_sk = {fiss}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {fiss}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {fiss}.clm_type_cd AND
                {clm}.clm_num_sk = {fiss}.clm_num_sk
            {{WHERE_CLAUSE}} AND {_claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_PAC_PARTITIONS
