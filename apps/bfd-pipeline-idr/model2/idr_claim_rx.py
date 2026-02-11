from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_RX_TABLE,
    DEFAULT_MAX_DATE,
    PART_D_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_LINE,
    ALIAS_PBP_NUM,
    ALIAS_PRVDR_PRSCRBNG,
    ALIAS_PRVDR_SRVC,
    ALIAS_RX_LINE,
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
    provider_last_name_expr,
    transform_default_date_to_null,
    transform_default_int_to_null,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_string,
    transform_provider_name,
)


class IdrClaimRx(IdrBaseModel):
    # Columns from v2_mdcr_clm
    clm_uniq_id: Annotated[
        int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_CLM, LAST_UPDATED_TIMESTAMP: True}
    ]
    clm_type_cd: Annotated[int, {ALIAS: ALIAS_CLM}]
    bene_sk: Annotated[int, ALIAS:ALIAS_CLM]
    clm_cntl_num: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_prnt_cntl_num: Annotated[
        str,
        {
            ALIAS: ALIAS_CLM,
            EXPR: f"""CASE 
                WHEN {ALIAS_CLM}.clm_cntl_num = {ALIAS_CLM}.clm_prnt_cntl_num 
                THEN '' 
                ELSE {ALIAS_CLM}.clm_prnt_cntl_num
                END""",
        },
        BeforeValidator(transform_null_string),
    ]
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
    clm_pd_dt: Annotated[
        date | None, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_ltst_clm_ind: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_adjstmt_type_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_sbmt_frmt_cd: Annotated[str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)]
    clm_sbmtr_cntrct_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_sbmtr_cntrct_pbp_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_othr_tp_pd_amt: Annotated[float | None, {ALIAS: ALIAS_CLM}]
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

    # Columns from v2_mdcr_clm_line
    clm_line_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_cvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_from_dt: Annotated[date, {ALIAS: ALIAS_LINE}]
    clm_line_thru_dt: Annotated[date, {ALIAS: ALIAS_LINE}]
    clm_line_ndc_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_line_ndc_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ndc_qty_qlfyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_srvc_unit_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_rx_num: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_line_othr_tp_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ncvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    idr_insrt_ts_line: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_LINE,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_LINE,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from V2_MDCR_CLM_LINE_RX
    clm_brnd_gnrc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_cmpnd_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_ctstrphc_cvrg_ind_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_daw_prod_slctn_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_drug_cvrg_stus_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_dspnsng_stus_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_authrzd_fill_num: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_days_suply_qty: int | None
    clm_line_grs_above_thrshld_amt: float | None
    clm_line_grs_blw_thrshld_amt: float | None
    clm_line_ingrdnt_cst_amt: float | None
    clm_line_lis_amt: float | None
    clm_line_plro_amt: float | None
    clm_line_rx_fill_num: int | None
    clm_line_rx_orgn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_sls_tax_amt: float | None
    clm_line_srvc_cst_amt: float | None
    clm_line_troop_tot_amt: float | None
    clm_line_vccn_admin_fee_amt: float | None
    clm_ltc_dspnsng_mthd_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_phrmcy_srvc_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcng_excptn_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_ptnt_rsdnc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_rptd_gap_dscnt_amt: float | None
    clm_rptd_mftr_dscnt_amt: float | None
    clm_line_rebt_passthru_pos_amt: float | None
    clm_cms_calcd_mftr_dscnt_amt: float | None
    clm_line_grs_cvrd_cst_tot_amt: float | None
    clm_phrmcy_price_dscnt_at_pos_amt: float | None
    idr_insrt_ts_line_rx: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_RX_LINE,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_rx: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            INSERT_EXCLUDE: True,
            ALIAS: ALIAS_RX_LINE,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_dt_sgntr
    clm_cms_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
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

    # Columns from v2_mdcr_prvdr_hstry
    prvdr_prscrbng_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_prscrbng_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_prscrbng_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    prvdr_prscrbng_last_name: Annotated[
        str,
        {EXPR: provider_last_name_expr(ALIAS_PRVDR_PRSCRBNG, "clm_prscrbng_prvdr_last_name")},
        BeforeValidator(transform_default_string),
    ]

    prvdr_srvc_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_sk: Annotated[
        int | None,
        {COLUMN_MAP: "prvdr_sk", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_int_to_null),
    ]
    prvdr_srvc_mdl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_mdl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_type_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_type_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_txnmy_cmpst_cd: Annotated[
        str,
        {COLUMN_MAP: "prvdr_txnmy_cmpst_cd", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_oscar_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_oscar_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_provider_name),
    ]
    prvdr_srvc_lgl_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_lgl_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_emplr_id_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_emplr_id_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    # There doesn't seem to be an version of "clm_srvc_prvdr_last_name" in the claim table
    prvdr_srvc_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    cntrct_pbp_name: Annotated[str, BeforeValidator(transform_null_string)]

    @staticmethod
    def table() -> str:
        return CLAIM_RX_TABLE

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_RX_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        line = ALIAS_LINE
        rx_line = ALIAS_RX_LINE
        sgntr = ALIAS_SGNTR
        prvdr_srvc = ALIAS_PRVDR_SRVC
        prvdr_prscrbng = ALIAS_PRVDR_PRSCRBNG
        pbp_num = ALIAS_PBP_NUM
        return f"""
            WITH contracts AS (
                SELECT cntrct_pbp_name, cntrct_num, cntrct_pbp_num,
                RANK() OVER (
                    PARTITION BY cntrct_num, cntrct_pbp_num 
                    ORDER BY cntrct_pbp_sk_obslt_dt DESC
                ) AS contract_version_rank
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num
            )
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dt_sgntr {sgntr} ON 
                {sgntr}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_rx {rx_line} ON 
                {clm}.geo_bene_sk = {rx_line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {rx_line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {rx_line}.clm_type_cd AND
                {clm}.clm_num_sk = {rx_line}.clm_num_sk
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_srvc}
                ON {prvdr_srvc}.prvdr_npi_num = {clm}.prvdr_srvc_prvdr_npi_num
                AND {prvdr_srvc}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_prscrbng}
                ON {prvdr_prscrbng}.prvdr_npi_num = {clm}.prvdr_prscrbng_prvdr_npi_num
                AND {prvdr_prscrbng}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN contracts {pbp_num}
                ON {pbp_num}.cntrct_num = {clm}.clm_sbmtr_cntrct_num
                AND {pbp_num}.cntrct_pbp_num = {clm}.clm_sbmtr_cntrct_pbp_num
                AND {pbp_num}.contract_version_rank = 1
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PART_D_PARTITIONS
