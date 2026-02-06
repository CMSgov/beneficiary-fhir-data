from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_INSTITUTIONAL_NCH_TABLE,
    CLAIM_PROFESSIONAL_NCH_TABLE,
    DEFAULT_MAX_DATE,
    INSTITUTIONAL_ADJUDICATED_PARTITIONS,
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
    clm_finl_actn_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_bill_clsfctn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bill_fac_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bill_freq_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pd_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ltst_clm_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_disp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prvdr_pmt_amt: float | None
    clm_adjstmt_type_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_cntrctr_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_alowd_chrg_amt: float | None
    clm_sbmt_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_blood_pt_frnsh_qty: Annotated[int | None, {ALIAS: ALIAS_CLM}]
    clm_bene_pd_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_blg_prvdr_zip5_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_null_string)
    ]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, INSERT_EXCLUDE: True, ALIAS: ALIAS_CLM, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, INSERT_EXCLUDE: True, ALIAS: ALIAS_CLM, COLUMN_MAP: "idr_updt_ts"},
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
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_DCMTN, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_dcmtn: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_DCMTN, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_insrt_ts_dcmtn: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            ALIAS: ALIAS_DCMTN,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_dcmtn: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_DCMTN,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_dt_sngtr
    clm_cms_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_actv_care_from_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_dschrg_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_submsn_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_ncvrd_from_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_ncvrd_thru_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_actv_care_thru_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_mdcr_exhstd_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_nch_wkly_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_qlfy_stay_from_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_qlfy_stay_thru_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    idr_insrt_ts_sgntr: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_SGNTR,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_sgntr: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_SGNTR,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_instnl
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
    idr_insrt_ts_instnl: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_INSTNL,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_instnl: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_INSTNL,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_prvdr_hstry
    prvdr_atndg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_atndg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_atndg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_ATNDG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_last_name: Annotated[
        str,
        {EXPR: provider_last_name_expr(ALIAS_PRVDR_ATNDG, "clm_atndg_prvdr_last_name")},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_oprtg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_oprtg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_OPRTG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_last_name: Annotated[
        str,
        {EXPR: provider_last_name_expr(ALIAS_PRVDR_OPRTG, "clm_rfrg_prvdr_last_name")},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_othr_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_othr_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_othr_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_OTHR},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_rndrng_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_rndrng_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_rndrng_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_blg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_blg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_blg_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_rfrg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_rfrg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_rfrg_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_RFRG},
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
        lctn_hstry = ALIAS_LCTN_HSTRY
        prvdr_atng = ALIAS_PRVDR_ATNDG
        prvdr_oprtg = ALIAS_PRVDR_OPRTG
        prvdr_othr = ALIAS_PRVDR_OTHR
        prvdr_blg = ALIAS_PRVDR_BLG
        prvdr_rfrg = ALIAS_PRVDR_RFRG
        prvdr_rndrg = ALIAS_PRVDR_RNDRNG
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
                WHERE {_claim_filter(start_time, partition)}
            ),
            latest_clm_lctn_hstry AS (
                SELECT
                    claims.geo_bene_sk,
                    claims.clm_type_cd,
                    claims.clm_dt_sgntr_sk,
                    claims.clm_num_sk,
                    MAX({lctn_hstry}.clm_lctn_cd_sqnc_num) AS max_clm_lctn_cd_sqnc_num
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_lctn_hstry {lctn_hstry}
                JOIN claims ON
                    {lctn_hstry}.geo_bene_sk = claims.geo_bene_sk AND
                    {lctn_hstry}.clm_type_cd = claims.clm_type_cd AND
                    {lctn_hstry}.clm_dt_sgntr_sk = claims.clm_dt_sgntr_sk AND
                    {lctn_hstry}.clm_num_sk = claims.clm_num_sk
                GROUP BY
                    claims.geo_bene_sk,
                    claims.clm_type_cd,
                    claims.clm_dt_sgntr_sk,
                    claims.clm_num_sk
            )
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dt_sgntr {sgntr} ON 
                {sgntr}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_instnl {instnl} ON
                {clm}.geo_bene_sk = {instnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {instnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {instnl}.clm_type_cd AND
                {clm}.clm_num_sk = {instnl}.clm_num_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dcmtn {dcmtn} ON
                {clm}.geo_bene_sk = {dcmtn}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {dcmtn}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {dcmtn}.clm_type_cd AND
                {clm}.clm_num_sk = {dcmtn}.clm_num_sk
            LEFT JOIN latest_clm_lctn_hstry latest_lctn ON
                {clm}.geo_bene_sk = latest_lctn.geo_bene_sk AND
                {clm}.clm_type_cd = latest_lctn.clm_type_cd AND
                {clm}.clm_dt_sgntr_sk = latest_lctn.clm_dt_sgntr_sk AND
                {clm}.clm_num_sk = latest_lctn.clm_num_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_lctn_hstry {lctn_hstry} ON
                {clm}.geo_bene_sk = {lctn_hstry}.geo_bene_sk AND
                {clm}.clm_type_cd = {lctn_hstry}.clm_type_cd AND
                {clm}.clm_dt_sgntr_sk = {lctn_hstry}.clm_dt_sgntr_sk AND
                {clm}.clm_num_sk = {lctn_hstry}.clm_num_sk AND
                {lctn_hstry}.clm_lctn_cd_sqnc_num = latest_lctn.max_clm_lctn_cd_sqnc_num
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_atng} ON 
                {prvdr_atng}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_atng}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_rfrg} ON 
                {prvdr_rfrg}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_rfrg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_blg} ON 
                {prvdr_blg}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_blg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_oprtg} ON 
                {prvdr_oprtg}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_oprtg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_rndrg} ON 
                {prvdr_rndrg}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_rndrg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_othr} ON 
                {prvdr_othr}.prvdr_npi_num = {clm}.prvdr_atndg_prvdr_npi_num AND
                {prvdr_othr}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            {{WHERE_CLAUSE}} AND {_claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def _fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_ADJUDICATED_PARTITIONS
