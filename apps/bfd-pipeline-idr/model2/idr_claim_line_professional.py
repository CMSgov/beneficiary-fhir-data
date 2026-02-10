from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_PROFESSIONAL_NCH_TABLE,
    CLAIM_TABLE,
    DEFAULT_MAX_DATE,
    PROFESSIONAL_ADJUDICATED_PARTITIONS,
    PROFESSIONAL_PAC_PARTITIONS,
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
    transform_default_date_to_null,
    transform_default_int_to_null,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_string,
    transform_provider_name,
)


class IdrClaimLineProfessional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_line_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_bene_prmry_pyr_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_fed_type_srvc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_carr_clncl_lab_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_carr_hpsa_scrcty_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_dmerc_scrn_svgs_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_hct_hgb_rslt_num: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_hct_hgb_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_prfnl_dme_price_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_prfnl_mtus_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mtus_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_physn_astnt_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pmt_80_100_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcng_lclty_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcsg_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prmry_pyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prvdr_spclty_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_srvc_ddctbl_sw: Annotated[str, BeforeValidator(transform_default_string)]
    clm_suplr_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_prfnl_intrst_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_carr_psych_ot_lmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_carr_clncl_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_prmry_pyr_alowd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_PRFNL},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_PRFNL},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_line_professional"

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
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_prfnl {prfnl} ON
                {clm}.geo_bene_sk = {prfnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {prfnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {prfnl}.clm_type_cd AND
                {clm}.clm_num_sk = {prfnl}.clm_num_sk
            {{WHERE_CLAUSE}} AND {_claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PROFESSIONAL_ADJUDICATED_PARTITIONS + PROFESSIONAL_PAC_PARTITIONS
