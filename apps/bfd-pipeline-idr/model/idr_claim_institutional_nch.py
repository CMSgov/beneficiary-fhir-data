from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_INSTITUTIONAL_NCH_TABLE,
    DEFAULT_MAX_DATE,
    INSTITUTIONAL_ADJUDICATED_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_DCMTN,
    ALIAS_INSTNL,
    ALIAS_PRVDR_ATNDG,
    ALIAS_PRVDR_BLG,
    ALIAS_PRVDR_OPRTG,
    ALIAS_PRVDR_OTHR,
    ALIAS_PRVDR_RFRG,
    ALIAS_PRVDR_RNDRNG,
    ALIAS_PRVDR_SRVC,
    ALIAS_SGNTR,
    BATCH_ID,
    COLUMN_MAP,
    EXPR,
    HISTORICAL_BATCH_TIMESTAMP,
    INSERT_FIELD,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_FIELD,
    IdrBaseModel,
    claim_filter,
    provider_careteam_name_expr,
    provider_last_or_legal_name_expr,
    transform_default_date_to_null,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_string,
)


class IdrClaimInstitutionalNch(IdrBaseModel):
    # Columns from v2_mdcr_clm
    clm_uniq_id: Annotated[
        int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_CLM, LAST_UPDATED_TIMESTAMP: True}
    ]
    clm_type_cd: Annotated[int, {ALIAS: ALIAS_CLM}]
    bene_sk: Annotated[int, ALIAS:ALIAS_CLM]
    clm_cntl_num: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_orig_cntl_num: Annotated[
        str,
        {
            ALIAS: ALIAS_CLM,
            EXPR: f"""CASE 
                WHEN {ALIAS_CLM}.clm_cntl_num = {ALIAS_CLM}.clm_orig_cntl_num 
                THEN '' 
                ELSE {ALIAS_CLM}.clm_orig_cntl_num
                END""",
        },
        BeforeValidator(transform_null_string),
    ]
    clm_from_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_thru_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_efctv_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_obslt_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_mdcr_coinsrnc_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_blood_lblty_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_ncvrd_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_blood_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_finl_actn_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_bill_clsfctn_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_bill_fac_type_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_bill_freq_cd: Annotated[str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)]
    clm_pd_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ltst_clm_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_query_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_disp_cd: Annotated[str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)]
    clm_nch_prmry_pyr_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_prvdr_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_adjstmt_type_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_cntrctr_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_alowd_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_mdcr_ddctbl_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_sbmt_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_blood_ncvrd_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_blood_pt_frnsh_qty: Annotated[int | None, {ALIAS: ALIAS_CLM}]
    clm_bene_pd_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    geo_blg_ssa_state_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_blg_prvdr_zip5_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_null_string)
    ]
    clm_atndg_fed_prvdr_spclty_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_rfrg_fed_prvdr_spclty_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_othr_fed_prvdr_spclty_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_oprtg_fed_prvdr_spclty_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_rndrg_fed_prvdr_spclty_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    meta_src_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_src_id: Annotated[str, {ALIAS: ALIAS_CLM}]
    idr_insrt_ts_clm: Annotated[
        datetime,
        {ALIAS: ALIAS_CLM, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_clm: Annotated[
        datetime,
        {ALIAS: ALIAS_CLM, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    clm_idr_ld_dt: Annotated[date, {HISTORICAL_BATCH_TIMESTAMP: True, ALIAS: ALIAS_CLM}]

    # Columns from v2_mdcr_clm_dcmtn
    clm_nrln_ric_cd: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)]
    clm_bnft_enhncmt_1_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_2_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_3_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_4_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_5_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_ngaco_pbpmt_sw: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)]
    clm_ngaco_cptatn_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)
    ]
    clm_aco_care_mgmt_hcbs_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)
    ]
    clm_ngaco_pdschrg_hcbs_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)
    ]
    clm_ngaco_snf_wvr_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)
    ]
    clm_ngaco_tlhlth_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)
    ]
    idr_insrt_ts_dcmtn: Annotated[
        datetime,
        {ALIAS: ALIAS_DCMTN, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_dcmtn: Annotated[
        datetime,
        {ALIAS: ALIAS_DCMTN, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_dt_sngtr
    clm_cms_proc_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_actv_care_from_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_dschrg_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_submsn_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ncvrd_from_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ncvrd_thru_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_actv_care_thru_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_mdcr_exhstd_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_nch_wkly_proc_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_qlfy_stay_from_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_qlfy_stay_thru_dt: Annotated[
        date | None, {ALIAS: ALIAS_SGNTR}, BeforeValidator(transform_default_date_to_null)
    ]
    idr_insrt_ts_sgntr: Annotated[
        datetime,
        {ALIAS: ALIAS_SGNTR, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_sgntr: Annotated[
        datetime,
        {ALIAS: ALIAS_SGNTR, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_instnl
    clm_admsn_type_cd: Annotated[
        str, {ALIAS: ALIAS_INSTNL}, BeforeValidator(transform_default_string)
    ]
    bene_ptnt_stus_cd: Annotated[
        str, {ALIAS: ALIAS_INSTNL}, BeforeValidator(transform_default_string)
    ]
    dgns_drg_cd: Annotated[int, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_instnl_mco_pd_sw: Annotated[str, {ALIAS: ALIAS_INSTNL}]
    clm_admsn_src_cd: Annotated[
        str, {ALIAS: ALIAS_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_fi_actn_cd: Annotated[str, {ALIAS: ALIAS_INSTNL}, BeforeValidator(transform_default_string)]
    clm_mdcr_ip_lrd_use_cnt: Annotated[int | None, {ALIAS: ALIAS_INSTNL}]
    clm_hipps_uncompd_care_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_hha_rfrl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_hha_lup_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_hha_tot_visit_cnt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_mdcr_coins_day_cnt: Annotated[int | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_ncvrd_day_cnt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_per_diem_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_instnl_bene_pd_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_hospc_prd_cnt: Annotated[int | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_npmt_rsn_cd: Annotated[
        str, {ALIAS: ALIAS_INSTNL}, BeforeValidator(transform_null_string)
    ]
    clm_mdcr_ip_pps_drg_wt_num: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_pps_dsprprtnt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_pps_excptn_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_pps_cptl_fsp_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_pps_cptl_ime_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_pps_outlier_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_pps_cptl_hrmls_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_pps_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_ip_pps_cptl_tot_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_cvrd_day_cnt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_instnl_prmry_pyr_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_prfnl_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_bene_ddctbl_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_drg_outlier_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    dgns_drg_outlier_cd: Annotated[
        str, {ALIAS: ALIAS_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_mdcr_ip_scnd_yr_rate_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_instnl_low_vol_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_hipps_readmsn_rdctn_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_hipps_model_bndld_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_hipps_vbp_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_site_ntrl_ip_pps_pymt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_finl_stdzd_pymt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_pps_md_wvr_stdzd_val_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_hac_rdctn_pymt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_mdcr_ip_1st_yr_rate_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_site_ntrl_cst_bsd_pymt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_ss_outlier_std_pymt_amt: Annotated[float | None, {ALIAS: ALIAS_INSTNL}]
    clm_op_srvc_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts_instnl: Annotated[
        datetime,
        {ALIAS: ALIAS_INSTNL, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_instnl: Annotated[
        datetime,
        {ALIAS: ALIAS_INSTNL, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_prvdr_hstry
    prvdr_atndg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_atndg_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_ATNDG, "ATNDG")},
        BeforeValidator(transform_default_string),
    ]

    prvdr_rfrg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_rfrg_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_RFRG, "RFRG")},
        BeforeValidator(transform_default_string),
    ]

    prvdr_othr_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_othr_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_OTHR, "OTHR")},
        BeforeValidator(transform_default_string),
    ]

    prvdr_oprtg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_oprtg_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_OPRTG, "OPRTG")},
        BeforeValidator(transform_default_string),
    ]

    prvdr_rndrg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_rndrg_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_RNDRNG, "RNDRG")},
        BeforeValidator(transform_default_string),
    ]

    prvdr_srvc_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_srvc_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_SRVC, None)},
        BeforeValidator(transform_default_string),
    ]

    prvdr_blg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_blg_last_or_lgl_name: Annotated[
        str,
        {EXPR: provider_last_or_legal_name_expr(ALIAS_PRVDR_BLG)},
        BeforeValidator(transform_default_string),
    ]

    @staticmethod
    def table() -> str:
        return CLAIM_INSTITUTIONAL_NCH_TABLE

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_INSTITUTIONAL_NCH_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        dcmtn = ALIAS_DCMTN
        sgntr = ALIAS_SGNTR
        instnl = ALIAS_INSTNL
        prvdr_atng = ALIAS_PRVDR_ATNDG
        prvdr_oprtg = ALIAS_PRVDR_OPRTG
        prvdr_othr = ALIAS_PRVDR_OTHR
        prvdr_blg = ALIAS_PRVDR_BLG
        prvdr_rfrg = ALIAS_PRVDR_RFRG
        prvdr_rndrg = ALIAS_PRVDR_RNDRNG
        prvdr_srvc = ALIAS_PRVDR_SRVC
        return f"""
            WITH claims AS (
                SELECT
                    {clm}.clm_uniq_id,
                    {clm}.geo_bene_sk,
                    {clm}.clm_type_cd,
                    {clm}.clm_num_sk,
                    {clm}.clm_dt_sgntr_sk,
                    {clm}.clm_idr_ld_dt
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
                WHERE {claim_filter(start_time, partition)}
            )
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dt_sgntr {sgntr} ON 
                {sgntr}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_instnl {instnl} ON
                {clm}.geo_bene_sk = {instnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {instnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {instnl}.clm_type_cd AND
                {clm}.clm_num_sk = {instnl}.clm_num_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dcmtn {dcmtn} ON
                {clm}.geo_bene_sk = {dcmtn}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {dcmtn}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {dcmtn}.clm_type_cd AND
                {clm}.clm_num_sk = {dcmtn}.clm_num_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_atng} ON
                {prvdr_atng}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_atng}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_rfrg} ON 
                {prvdr_rfrg}.prvdr_npi_num = {clm}.prvdr_rfrg_prvdr_npi_num AND
                {prvdr_rfrg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_blg} ON 
                {prvdr_blg}.prvdr_npi_num = {clm}.prvdr_blg_prvdr_npi_num AND
                {prvdr_blg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_oprtg} ON 
                {prvdr_oprtg}.prvdr_npi_num = {clm}.prvdr_oprtg_prvdr_npi_num AND
                {prvdr_oprtg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_rndrg} ON 
                {prvdr_rndrg}.prvdr_npi_num = {clm}.prvdr_rndrng_prvdr_npi_num AND
                {prvdr_rndrg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_othr} ON 
                {prvdr_othr}.prvdr_npi_num = {clm}.prvdr_othr_prvdr_npi_num AND
                {prvdr_othr}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_srvc} ON 
                {prvdr_srvc}.prvdr_npi_num = {clm}.prvdr_srvc_prvdr_npi_num AND
                {prvdr_srvc}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_ADJUDICATED_PARTITIONS
