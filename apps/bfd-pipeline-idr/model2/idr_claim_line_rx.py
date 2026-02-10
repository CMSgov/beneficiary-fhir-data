from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_PROFESSIONAL_NCH_TABLE,
    CLAIM_TABLE,
    DEFAULT_MAX_DATE,
    PART_D_PARTITIONS,
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
    ALIAS_RX_LINE,
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
from model2.idr_claim import transform_null_int


class IdrClaimLineRx(IdrBaseModel):
    clm_uniq_id: Annotated[
        int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_CLM, LAST_UPDATED_TIMESTAMP: True}
    ]
    clm_line_num: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_RX_LINE}]
    clm_brnd_gnrc_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_cmpnd_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_ctstrphc_cvrg_ind_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_daw_prod_slctn_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_drug_cvrg_stus_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_dspnsng_stus_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_authrzd_fill_num: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_days_suply_qty: Annotated[int, BeforeValidator(transform_null_int)]
    clm_line_grs_above_thrshld_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_grs_blw_thrshld_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_ingrdnt_cst_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_lis_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_plro_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_rx_fill_num: Annotated[int, BeforeValidator(transform_null_int)]
    clm_line_rx_orgn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_sls_tax_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_srvc_cst_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_troop_tot_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_vccn_admin_fee_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_ltc_dspnsng_mthd_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_phrmcy_srvc_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcng_excptn_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_ptnt_rsdnc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rptd_mftr_dscnt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    clm_line_rebt_passthru_pos_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_cms_calcd_mftr_dscnt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_grs_cvrd_cst_tot_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_phrmcy_price_dscnt_at_pos_amt: Annotated[float, BeforeValidator(transform_null_float)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_RX_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_RX_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_line_rx"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        rx_line = ALIAS_RX_LINE
        line = ALIAS_LINE
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_rx {rx_line}
                ON {rx_line}.geo_bene_sk = {clm}.geo_bene_sk
                AND {rx_line}.clm_type_cd = {clm}.clm_type_cd
                AND {rx_line}.clm_num_sk = {clm}.clm_num_sk
                AND {rx_line}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                AND {rx_line}.clm_uniq_id = {clm}.clm_uniq_id
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line {line}
                ON {line}.geo_bene_sk = {rx_line}.geo_bene_sk
                AND {line}.clm_type_cd = {rx_line}.clm_type_cd
                AND {line}.clm_num_sk = {rx_line}.clm_num_sk
                AND {line}.clm_dt_sgntr_sk = {rx_line}.clm_dt_sgntr_sk
                AND {line}.clm_uniq_id = {rx_line}.clm_uniq_id
                AND {line}.clm_line_num = {rx_line}.clm_line_num
            {{WHERE_CLAUSE}} AND {_claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PART_D_PARTITIONS
