from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from constants import (
    CLAIM_PROFESSIONAL_NCH_TABLE,
    DEFAULT_MAX_DATE,
    IDR_CLAIM_DATE_SIGNATURE_TABLE,
    IDR_CLAIM_DOCUMENTATION_TABLE,
    IDR_CLAIM_PROFESSIONAL_TABLE,
    IDR_CLAIM_TABLE,
    IDR_PROVIDER_HISTORY_TABLE,
)
from load_partition import LoadPartition
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_DCMTN,
    ALIAS_OCRNC_SGNTR_DERIVED_DATES,
    ALIAS_PRFNL,
    ALIAS_PRVDR_BLG,
    ALIAS_PRVDR_RFRG,
    ALIAS_PRVDR_SRVC,
    ALIAS_RLT_OCRNC_SGNTR_DERIVED_DATES,
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
    claim_occurrence_cte,
    claim_related_occurrences_cte,
    clm_base_query,
    clm_child_query,
    clm_dt_sgntr_query,
    clm_ocrnc_sgntr_query,
    clm_orig_cntl_num_expr,
    clm_query,
    clm_rlt_ocrnc_clause,
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
    idr_insrt_ts: Annotated[
        datetime,
        {ALIAS: ALIAS_CLM, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {ALIAS: ALIAS_CLM, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    clm_idr_ld_dt: Annotated[date, {HISTORICAL_BATCH_TIMESTAMP: True, ALIAS: ALIAS_CLM}]

    # Columns from v2_mdcr_clm_dt_sngtr
    clm_submsn_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
    clm_nch_wkly_proc_dt: Annotated[date | None, BeforeValidator(transform_default_date_to_null)]
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

    # Columns from v2_mdcr_clm_prfnl
    clm_carr_pmt_dnl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_clncl_tril_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_prfnl_prmry_pyr_amt: float | None
    clm_mdcr_ddctbl_amt: float | None
    clm_mdcr_prfnl_prvdr_asgnmt_sw: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts_prfnl: Annotated[
        datetime,
        {ALIAS: ALIAS_PRFNL, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_prfnl: Annotated[
        datetime,
        {ALIAS: ALIAS_PRFNL, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    # Columns from v2_mdcr_clm_dcmtn
    clm_nrln_ric_cd: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)]
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

    # columns derived from v2_mdcr_clm_ocrnc_sgntr_mbr
    bfd_clm_ncvrd_from_dt: Annotated[
        date | None,
        {ALIAS: ALIAS_OCRNC_SGNTR_DERIVED_DATES},
        BeforeValidator(transform_default_date_to_null),
    ]
    bfd_clm_ncvrd_thru_dt: Annotated[
        date | None,
        {ALIAS: ALIAS_OCRNC_SGNTR_DERIVED_DATES},
        BeforeValidator(transform_default_date_to_null),
    ]
    bfd_clm_qlfy_stay_from_dt: Annotated[
        date | None,
        {ALIAS: ALIAS_OCRNC_SGNTR_DERIVED_DATES},
        BeforeValidator(transform_default_date_to_null),
    ]
    bfd_clm_qlfy_stay_thru_dt: Annotated[
        date | None,
        {ALIAS: ALIAS_OCRNC_SGNTR_DERIVED_DATES},
        BeforeValidator(transform_default_date_to_null),
    ]
    idr_insrt_ts_ocrnc_sgntr: Annotated[
        datetime,
        {ALIAS: ALIAS_OCRNC_SGNTR_DERIVED_DATES, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # columns derived from v2_clm_rlt_ocrnc_sgntr_mbr
    bfd_clm_mdcr_exhstd_dt: Annotated[
        date | None,
        {ALIAS: ALIAS_RLT_OCRNC_SGNTR_DERIVED_DATES},
        BeforeValidator(transform_default_date_to_null),
    ]
    bfd_clm_actv_care_thru_dt: Annotated[
        date | None,
        {ALIAS: ALIAS_RLT_OCRNC_SGNTR_DERIVED_DATES},
        BeforeValidator(transform_default_date_to_null),
    ]
    idr_insrt_ts_rlt_ocrnc_sgntr: Annotated[
        datetime,
        {ALIAS: ALIAS_RLT_OCRNC_SGNTR_DERIVED_DATES, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    @override
    @staticmethod
    def table() -> str:
        return CLAIM_PROFESSIONAL_NCH_TABLE

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.CLAIM_PROFESSIONAL_NCH

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        clm = ALIAS_CLM
        sgntr = ALIAS_SGNTR
        prfnl = ALIAS_PRFNL
        dcmtn = ALIAS_DCMTN
        prvdr_blg = ALIAS_PRVDR_BLG
        prvdr_rfrg = ALIAS_PRVDR_RFRG
        prvdr_srvc = ALIAS_PRVDR_SRVC
        ocrnc_sgntr_dd = ALIAS_OCRNC_SGNTR_DERIVED_DATES
        rlt_ocrnc_sgntr_dd = ALIAS_RLT_OCRNC_SGNTR_DERIVED_DATES
        not_materialized = "" if source == Source.SNOWFLAKE else "NOT MATERIALIZED"
        return f"""
            WITH claim_base AS (
                {clm_base_query(start_time, partition, cls.model_type())}
            ),
            claims AS (
                {clm_query()}
                UNION
                {clm_dt_sgntr_query()}
                UNION
                {clm_child_query(IDR_CLAIM_PROFESSIONAL_TABLE)}
                UNION
                {clm_child_query(IDR_CLAIM_DOCUMENTATION_TABLE)}
                UNION
                {clm_ocrnc_sgntr_query()}
                UNION
                {clm_rlt_ocrnc_clause()}
            ),
            claim_occurrence_spans_dates AS {not_materialized} 
                ({claim_occurrence_cte()}),
            claim_related_occurrences_dates AS {not_materialized} 
                ({claim_related_occurrences_cte()})
            SELECT {{COLUMNS}}
            FROM claims c
            JOIN {IDR_CLAIM_TABLE} {clm} ON
                {clm}.geo_bene_sk = c.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = c.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = c.clm_type_cd AND
                {clm}.clm_num_sk = c.clm_num_sk
            JOIN {IDR_CLAIM_DATE_SIGNATURE_TABLE} {sgntr} ON 
                {sgntr}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
            LEFT JOIN {IDR_CLAIM_PROFESSIONAL_TABLE} {prfnl} ON
                {clm}.geo_bene_sk = {prfnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {prfnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {prfnl}.clm_type_cd AND
                {clm}.clm_num_sk = {prfnl}.clm_num_sk
            LEFT JOIN {IDR_CLAIM_DOCUMENTATION_TABLE} {dcmtn} ON
                {clm}.geo_bene_sk = {dcmtn}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {dcmtn}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {dcmtn}.clm_type_cd AND
                {clm}.clm_num_sk = {dcmtn}.clm_num_sk
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_blg}
                ON {prvdr_blg}.prvdr_npi_num = {clm}.prvdr_blg_prvdr_npi_num
                AND {prvdr_blg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_rfrg}
                ON {prvdr_rfrg}.prvdr_npi_num = {clm}.prvdr_rfrg_prvdr_npi_num
                AND {prvdr_rfrg}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_srvc}
                ON {prvdr_srvc}.prvdr_npi_num = {clm}.prvdr_srvc_prvdr_npi_num
                AND {prvdr_srvc}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN claim_occurrence_spans_dates {ocrnc_sgntr_dd} 
                ON {ocrnc_sgntr_dd}.clm_ocrnc_sgntr_sk = {clm}.clm_ocrnc_sgntr_sk
            LEFT JOIN claim_related_occurrences_dates {rlt_ocrnc_sgntr_dd}
                ON {rlt_ocrnc_sgntr_dd}.clm_rlt_ocrnc_sgntr_sk = {clm}.clm_rlt_ocrnc_sgntr_sk
            {{WHERE_CLAUSE}} AND {base_claim_filter(partition)}
            {{ORDER_BY}}
        """
