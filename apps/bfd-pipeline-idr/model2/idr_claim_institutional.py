from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    ALL_CLAIM_PARTITIONS,
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
    ALIAS_INSTNL,
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
    transform_null_string,
    transform_provider_name,
)


class IdrClaimInstitutional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_admsn_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    bene_ptnt_stus_cd: Annotated[str, BeforeValidator(transform_default_string)]
    dgns_drg_cd: int
    clm_mdcr_instnl_mco_pd_sw: str
    clm_admsn_src_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_fi_actn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_ip_lrd_use_cnt: Annotated[int, BeforeValidator(transform_null_int)]
    clm_hipps_uncompd_care_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_hha_rfrl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_hha_lup_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_hha_tot_visit_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_mdcr_coins_day_cnt: Annotated[int, BeforeValidator(transform_null_int)]
    clm_instnl_ncvrd_day_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_per_diem_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_instnl_bene_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_hospc_prd_cnt: Annotated[int, BeforeValidator(transform_null_int)]
    clm_mdcr_npmt_rsn_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_mdcr_ip_pps_drg_wt_num: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_pps_dsprprtnt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_pps_excptn_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_pps_cptl_fsp_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_pps_cptl_ime_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_pps_outlier_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_pps_cptl_hrmls_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_pps_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_ip_pps_cptl_tot_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_cvrd_day_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_instnl_prmry_pyr_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_prfnl_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_bene_ddctbl_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_drg_outlier_amt: Annotated[float, BeforeValidator(transform_null_float)]
    dgns_drg_outlier_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    clm_mdcr_ip_scnd_yr_rate_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_low_vol_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_hipps_readmsn_rdctn_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_hipps_model_bndld_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_hipps_vbp_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_site_ntrl_ip_pps_pymt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_finl_stdzd_pymt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_pps_md_wvr_stdzd_val_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_hac_rdctn_pymt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_1st_yr_rate_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_site_ntrl_cst_bsd_pymt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_ss_outlier_std_pymt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_op_srvc_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_INSTNL},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_INSTNL},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_institutional"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        instnl = ALIAS_INSTNL
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_instnl {instnl} ON
                {clm}.geo_bene_sk = {instnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {instnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {instnl}.clm_type_cd AND
                {clm}.clm_num_sk = {instnl}.clm_num_sk
            {{WHERE_CLAUSE}} AND {_claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_ADJUDICATED_PARTITIONS + INSTITUTIONAL_PAC_PARTITIONS
