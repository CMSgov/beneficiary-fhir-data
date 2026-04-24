from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from constants import (
    CLAIM_RX_TABLE,
    DEFAULT_MAX_DATE,
    IDR_CLAIM_DATE_SIGNATURE_TABLE,
    IDR_CLAIM_LINE_RX_TABLE,
    IDR_CLAIM_LINE_TABLE,
    IDR_CLAIM_TABLE,
    IDR_CONTRACT_PBP_NUM_TABLE,
    IDR_PROVIDER_HISTORY_TABLE,
)
from load_partition import LoadPartition
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_LINE,
    ALIAS_PBP_NUM,
    ALIAS_PRVDR_PRSCRBNG,
    ALIAS_PRVDR_SRVC,
    ALIAS_RX_LINE,
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
    ModelType,
    Source,
    base_claim_filter,
    clm_base_query,
    clm_child_query,
    clm_dt_sgntr_query,
    clm_orig_cntl_num_expr,
    clm_query,
    provider_careteam_name_expr,
    provider_last_or_legal_name_expr,
    transform_default_date_to_null,
    transform_default_string,
    transform_null_date_to_min,
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
        BeforeValidator(transform_default_string),
    ]
    clm_orig_cntl_num: Annotated[
        str,
        {
            ALIAS: ALIAS_CLM,
            EXPR: clm_orig_cntl_num_expr(),
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
    prvdr_srvc_id_qlfyr_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_srvc_prvdr_gnrc_id_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    prvdr_prsbng_id_qlfyr_cd: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
    clm_prsbng_prvdr_gnrc_id_num: Annotated[
        str, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_default_string)
    ]
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
        {ALIAS: ALIAS_LINE, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from V2_MDCR_CLM_LINE_RX
    clm_brnd_gnrc_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_cmpnd_cd: Annotated[str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)]
    clm_ctstrphc_cvrg_ind_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_daw_prod_slctn_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_drug_cvrg_stus_cd: Annotated[
        str, {ALIAS: ALIAS_RX_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_line_days_suply_qty: Annotated[int | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_grs_above_thrshld_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_grs_blw_thrshld_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_ingrdnt_cst_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_lis_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_plro_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_rx_fill_num: Annotated[int | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_rx_orgn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_sls_tax_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_srvc_cst_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_troop_tot_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_vccn_admin_fee_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_ltc_dspnsng_mthd_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_phrmcy_srvc_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcng_excptn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_ptnt_rsdnc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_rptd_gap_dscnt_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_rptd_mftr_dscnt_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_rebt_passthru_pos_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_cms_calcd_mftr_dscnt_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_line_grs_cvrd_cst_tot_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    clm_phrmcy_price_dscnt_at_pos_amt: Annotated[float | None, {ALIAS: ALIAS_RX_LINE}]
    idr_insrt_ts_line_rx: Annotated[
        datetime,
        {ALIAS: ALIAS_RX_LINE, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_rx: Annotated[
        datetime,
        {ALIAS: ALIAS_RX_LINE, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_cntrct_pbp_num
    cntrct_pbp_name: Annotated[
        str, {ALIAS: ALIAS_PBP_NUM}, BeforeValidator(transform_default_string)
    ]

    # Columns from v2_mdcr_clm_dt_sgntr
    clm_cms_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_submsn_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
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

    # Columns from v2_mdcr_prvdr_hstry
    prvdr_srvc_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_1st_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_1st_name", ALIAS: ALIAS_PRVDR_SRVC},
        BeforeValidator(transform_default_string),
    ]
    prvdr_srvc_last_or_lgl_name: Annotated[
        str,
        {EXPR: provider_last_or_legal_name_expr(ALIAS_PRVDR_SRVC)},
        BeforeValidator(transform_default_string),
    ]

    prvdr_prscrbng_prvdr_npi_num: Annotated[
        str,
        {COLUMN_MAP: "prvdr_npi_num", ALIAS: ALIAS_PRVDR_PRSCRBNG},
        BeforeValidator(transform_default_string),
    ]
    bfd_prvdr_prscrbng_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_PRSCRBNG, None)},
        BeforeValidator(transform_default_string),
    ]

    @override
    @staticmethod
    def table() -> str:
        return CLAIM_RX_TABLE

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.CLAIM_RX

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        clm = ALIAS_CLM
        line = ALIAS_LINE
        rx_line = ALIAS_RX_LINE
        sgntr = ALIAS_SGNTR
        prvdr_srvc = ALIAS_PRVDR_SRVC
        prvdr_prscrbng = ALIAS_PRVDR_PRSCRBNG
        pbp_num = ALIAS_PBP_NUM
        return f"""
            WITH claim_base AS (
                {clm_base_query(start_time, partition, cls.model_type())}
            ),
            claims AS (
                {clm_query()}
                UNION
                {clm_dt_sgntr_query()}
                UNION
                {clm_child_query(IDR_CLAIM_LINE_TABLE)}
                UNION
                {clm_child_query(IDR_CLAIM_LINE_RX_TABLE)}
            ),
            contracts AS (
                SELECT cntrct_pbp_name, cntrct_num, cntrct_pbp_num,
                RANK() OVER (
                    PARTITION BY cntrct_num, cntrct_pbp_num 
                    ORDER BY cntrct_pbp_sk_obslt_dt DESC
                ) AS contract_version_rank
                FROM {IDR_CONTRACT_PBP_NUM_TABLE}
            )
            SELECT {{COLUMNS}}
            FROM claims c
            JOIN {IDR_CLAIM_TABLE} {clm} ON
                {clm}.geo_bene_sk = c.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = c.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = c.clm_type_cd AND
                {clm}.clm_num_sk = c.clm_num_sk
            JOIN {IDR_CLAIM_DATE_SIGNATURE_TABLE} {sgntr} ON 
                {sgntr}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
            JOIN {IDR_CLAIM_LINE_TABLE} {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            LEFT JOIN {IDR_CLAIM_LINE_RX_TABLE} {rx_line} ON 
                {clm}.geo_bene_sk = {rx_line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {rx_line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {rx_line}.clm_type_cd AND
                {clm}.clm_num_sk = {rx_line}.clm_num_sk
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_srvc}
                ON {prvdr_srvc}.prvdr_npi_num = {clm}.prvdr_srvc_prvdr_npi_num
                AND {prvdr_srvc}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_prscrbng}
                ON {prvdr_prscrbng}.prvdr_npi_num = {clm}.prvdr_prscrbng_prvdr_npi_num
                AND {prvdr_prscrbng}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN contracts {pbp_num}
                ON {pbp_num}.cntrct_num = {clm}.clm_sbmtr_cntrct_num
                AND {pbp_num}.cntrct_pbp_num = {clm}.clm_sbmtr_cntrct_pbp_num
                AND {pbp_num}.contract_version_rank = 1
            {{WHERE_CLAUSE}} AND {base_claim_filter(partition)}
            {{ORDER_BY}}
        """
