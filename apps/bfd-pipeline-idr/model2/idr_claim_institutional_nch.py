from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_PROFESSIONAL_NCH_TABLE,
    DEFAULT_MAX_DATE,
    PROFESSIONAL_ADJUDICATED_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_CLM_GRP,
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
    clm_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_bill_clsfctn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bill_fac_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bill_freq_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pd_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ltst_clm_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_disp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_query_cd: Annotated[str, BeforeValidator(transform_default_string)]
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
    clm_rfrg_prvdr_pin_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_blood_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
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

    # Columns from v2_mdcr_clm_dt_sngtr
    clm_cms_proc_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_actv_care_from_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_dschrg_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_submsn_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_ncvrd_from_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_ncvrd_thru_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_actv_care_thru_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_mdcr_exhstd_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_nch_wkly_proc_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_qlfy_stay_from_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
    clm_qlfy_stay_thru_dt: Annotated[date, BeforeValidator(transform_default_date_to_null)]
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

    # Columns from v2_mdcr_clm_prfnl
    clm_carr_pmt_dnl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_clncl_tril_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_prfnl_prmry_pyr_amt: float | None
    clm_mdcr_ddctbl_amt: float | None
    clm_mdcr_prfnl_prvdr_asgnmt_sw: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts_prfnl: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_PRFNL,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_prfnl: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_PRFNL,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_prvdr_hstry
    prvdr_atndg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_atndgmdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_atndg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_BLG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_atndg_last_name: Annotated[
        str,
        {EXPR: provider_last_name_expr(ALIAS_PRVDR_BLG, "clm_blg_prvdr_last_name")},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_oprtg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_oprtg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_oprtg_last_name: Annotated[
        str,
        {EXPR: provider_last_name_expr(ALIAS_PRVDR_RFRG, "clm_rfrg_prvdr_last_name")},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_othr_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_othr_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_othr_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_othr_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_rndrng_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_rndrng_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rndrng_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_rndrng_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_blg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_blg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_blg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_blg_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_rfrg_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_rfrg_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_rfrg_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_rfrg_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
