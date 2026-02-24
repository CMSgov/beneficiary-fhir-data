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
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_DCMTN,
    ALIAS_PRFNL,
    ALIAS_PRVDR_BLG,
    ALIAS_PRVDR_RFRG,
    ALIAS_PRVDR_SRVC,
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
    provider_careteam_name_expr,
    provider_last_or_legal_name_expr,
    transform_default_date_to_null,
    transform_default_string,
    transform_null_date_to_min,
)


class IdrClaimProfessionalNch(IdrBaseModel):
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
        BeforeValidator(transform_default_string),
    ]
    clm_from_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_thru_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_efctv_dt: Annotated[date, {ALIAS: ALIAS_CLM}]
    clm_obslt_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_finl_actn_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
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
    clm_blg_prvdr_tax_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_blood_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
    meta_src_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_src_id: Annotated[str, {ALIAS: ALIAS_CLM}]
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
    clm_nch_wkly_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
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
    # Columns from v2_mdcr_clm_dcmtn
    clm_nrln_ric_cd: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)]

    # Columns from v2_mdcr_prvdr_hstry
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

    prvdr_rfrg_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_RFRG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_rfrg_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_RFRG, None)},
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

    @staticmethod
    def table() -> str:
        return CLAIM_PROFESSIONAL_NCH_TABLE

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_PROFESSIONAL_NCH_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        sgntr = ALIAS_SGNTR
        prfnl = ALIAS_PRFNL
        dcmtn = ALIAS_DCMTN
        prvdr_blg = ALIAS_PRVDR_BLG
        prvdr_rfrg = ALIAS_PRVDR_RFRG
        prvdr_srvc = ALIAS_PRVDR_SRVC
        return f"""
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
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_blg}
                ON {prvdr_blg}.prvdr_npi_num = {clm}.prvdr_blg_prvdr_npi_num
                AND {prvdr_blg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_rfrg}
                ON {prvdr_rfrg}.prvdr_npi_num = {clm}.prvdr_rfrg_prvdr_npi_num
                AND {prvdr_rfrg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_srvc}
                ON {prvdr_srvc}.prvdr_npi_num = {clm}.prvdr_srvc_prvdr_npi_num
                AND {prvdr_srvc}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PROFESSIONAL_ADJUDICATED_PARTITIONS
