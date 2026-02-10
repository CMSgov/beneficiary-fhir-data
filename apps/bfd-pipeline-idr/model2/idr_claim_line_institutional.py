from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_TABLE,
    INSTITUTIONAL_ADJUDICATED_PARTITIONS,
    INSTITUTIONAL_PAC_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_LINE,
    BATCH_ID,
    BATCH_TIMESTAMP,
    HISTORICAL_BATCH_TIMESTAMP,
    INSERT_EXCLUDE,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    transform_default_hipps_code,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_int,
)


class IdrClaimLineInstitutional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_line_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_rev_apc_hipps_cd: Annotated[str, BeforeValidator(transform_default_hipps_code)]
    clm_otaf_one_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rev_dscnt_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rev_packg_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rev_cntr_stus_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rev_pmt_mthd_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_ansi_sgntr_sk: Annotated[int, BeforeValidator(transform_null_int)]
    clm_ddctbl_coinsrnc_cd: str
    clm_line_instnl_rate_amt: float
    clm_line_instnl_adjstd_amt: float
    clm_line_instnl_rdcd_amt: float
    clm_line_instnl_msp1_pd_amt: float
    clm_line_instnl_msp2_pd_amt: float
    clm_line_instnl_rev_ctr_dt: date
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    clm_rev_cntr_tdapa_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_non_ehr_rdctn_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_add_on_pymt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def table() -> str:
        return "idr.claim_line_institutional"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        line = ALIAS_LINE
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_instnl {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_ADJUDICATED_PARTITIONS + INSTITUTIONAL_PAC_PARTITIONS
