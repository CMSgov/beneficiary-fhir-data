from collections.abc import Sequence
from datetime import datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_TABLE,
    PROFESSIONAL_ADJUDICATED_PARTITIONS,
    PROFESSIONAL_PAC_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_LCTN_HSTRY,
    ALIAS_PRFNL,
    BATCH_ID,
    BATCH_TIMESTAMP,
    COLUMN_MAP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_string,
)


class IdrClaimProfessional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_carr_pmt_dnl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_clncl_tril_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_prfnl_prmry_pyr_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_prfnl_prvdr_asgnmt_sw: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prvdr_acnt_rcvbl_ofst_amt: Annotated[float, BeforeValidator(transform_null_float)]
    idr_insrt_ts_clm_prfnl: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_PRFNL, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_clm_prfnl: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_PRFNL, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_professional"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        prfnl = ALIAS_PRFNL
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_prfnl {prfnl} ON
                {clm}.geo_bene_sk = {prfnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {prfnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {prfnl}.clm_type_cd AND
                {clm}.clm_num_sk = {prfnl}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PROFESSIONAL_ADJUDICATED_PARTITIONS + PROFESSIONAL_PAC_PARTITIONS
