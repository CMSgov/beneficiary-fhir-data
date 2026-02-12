from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_PROFESSIONAL_SS_TABLE,
    PROFESSIONAL_PAC_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_DCMTN,
    ALIAS_LCTN_HSTRY,
    ALIAS_PRFNL,
    ALIAS_SGNTR,
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
    claim_filter,
    transform_default_date_to_null,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_string,
)


class IdrClaimProfessionalSs(IdrBaseModel):
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
    clm_src_id: Annotated[str, {ALIAS: ALIAS_CLM}]
    meta_src_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    clm_pd_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ltst_clm_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
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
    clm_mdcr_coinsrnc_amt: float | None
    clm_blood_lblty_amt: float | None
    clm_ncvrd_chrg_amt: float | None
    clm_nch_prmry_pyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_sbmt_frmt_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bene_intrst_pd_amt: float | None
    clm_bene_pmt_coinsrnc_amt: float | None
    clm_cob_ptnt_resp_amt: float | None
    clm_prvdr_otaf_amt: float | None
    clm_othr_tp_pd_amt: float | None
    clm_prvdr_rmng_due_amt: float | None
    clm_blood_ncvrd_chrg_amt: float | None
    clm_prvdr_intrst_pd_amt: float | None
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
    clm_submsn_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
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
    clm_clncl_tril_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_ddctbl_amt: float | None
    clm_mdcr_prfnl_prvdr_asgnmt_sw: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prvdr_acnt_rcvbl_ofst_amt: float | None
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
    # columns from v2_mdcr_clm_lctn_hstry
    clm_audt_trl_stus_cd: Annotated[
        str, {ALIAS: ALIAS_LCTN_HSTRY}, BeforeValidator(transform_null_string)
    ]
    idr_insrt_ts_lctn_hstry: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            ALIAS: ALIAS_LCTN_HSTRY,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_lctn_hstry: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_LCTN_HSTRY,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    # Columns from v2_mdcr_clm_dcmtn
    clm_nrln_ric_cd: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)]

    # Columns from v2_mdcr_prvdr_hstry
    # prvdr_blg_prvdr_npi_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_sk: Annotated[
    #     int | None,
    #     {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_int_to_null),
    # ]
    # prvdr_blg_mdl_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_type_cd: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_txnmy_cmpst_cd: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_oscar_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_1st_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_provider_name),
    # ]
    # prvdr_blg_lgl_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_emplr_id_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_BLG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_blg_last_name: Annotated[
    #     str,
    #     {EXPR: provider_last_name_expr(ALIAS_PRVDR_BLG, "clm_blg_prvdr_last_name")},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_prvdr_npi_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_sk: Annotated[
    #     int | None,
    #     {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_int_to_null),
    # ]
    # prvdr_rfrg_mdl_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_type_cd: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_txnmy_cmpst_cd: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_oscar_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_1st_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_provider_name),
    # ]
    # prvdr_rfrg_lgl_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_emplr_id_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_RFRG},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_rfrg_last_name: Annotated[
    #     str,
    #     {EXPR: provider_last_name_expr(ALIAS_PRVDR_RFRG, "clm_rfrg_prvdr_last_name")},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_prvdr_npi_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_sk: Annotated[
    #     int | None,
    #     {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_int_to_null),
    # ]
    # prvdr_srvc_mdl_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_type_cd: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_txnmy_cmpst_cd: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_oscar_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_1st_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_provider_name),
    # ]
    # prvdr_srvc_lgl_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # prvdr_srvc_emplr_id_num: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]
    # # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    # prvdr_srvc_last_name: Annotated[
    #     str,
    #     {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_SRVC},
    #     BeforeValidator(transform_default_string),
    # ]

    @staticmethod
    def table() -> str:
        return CLAIM_PROFESSIONAL_SS_TABLE

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_PROFESSIONAL_SS_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        sgntr = ALIAS_SGNTR
        prfnl = ALIAS_PRFNL
        dcmtn = ALIAS_DCMTN
        lctn_hstry = ALIAS_LCTN_HSTRY
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
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_prfnl {prfnl} ON
                {clm}.geo_bene_sk = {prfnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {prfnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {prfnl}.clm_type_cd AND
                {clm}.clm_num_sk = {prfnl}.clm_num_sk
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
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PROFESSIONAL_PAC_PARTITIONS
