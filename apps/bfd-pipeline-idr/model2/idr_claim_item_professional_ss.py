from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_PROFESSIONAL_SS_TABLE,
    DEFAULT_MAX_DATE,
    PROFESSIONAL_PAC_PARTITIONS,
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
    ALIAS_PROCEDURE,
    ALIAS_PRVDR_RNDRNG,
    BATCH_ID,
    COLUMN_MAP,
    HISTORICAL_BATCH_TIMESTAMP,
    INSERT_EXCLUDE,
    INSERT_FIELD,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_FIELD,
    IdrBaseModel,
    claim_filter,
    get_min_transaction_date,
    transform_default_date_to_null,
    transform_default_int_to_null,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_string,
    transform_provider_name,
)


class IdrClaimItemProfessionalSs(IdrBaseModel):
    clm_uniq_id: Annotated[
        int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_CLM, LAST_UPDATED_TIMESTAMP: True}
    ]
    bfd_row_id: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_CLM_GRP}]
    # columns from V2_MDCR_CLM_LINE
    clm_line_num: Annotated[int | None, {ALIAS: ALIAS_LINE}]
    clm_line_sbmt_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_alowd_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_prvdr_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_bene_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_cvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_dgns_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_null_string)]
    clm_line_from_dt: Annotated[
        date | None, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_line_thru_dt: Annotated[
        date | None, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_date_to_null)
    ]
    clm_line_mdcr_ddctbl_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_hcpcs_cd: Annotated[
        str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_line_ndc_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_line_ndc_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ndc_qty_qlfyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_srvc_unit_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_rx_num: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_pos_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_rndrg_prvdr_type_cd: Annotated[
        str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_rndrg_prvdr_prtcptg_cd: Annotated[
        str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_rndrg_prvdr_tax_num: Annotated[
        str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)
    ]
    hcpcs_1_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_2_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_3_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_4_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_5_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_line_otaf_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_idr_ld_dt: Annotated[
        date, {INSERT_EXCLUDE: True, ALIAS: ALIAS_CLM, HISTORICAL_BATCH_TIMESTAMP: True}
    ]
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
    # columns from V2_MDCR_CLM_PROD
    clm_val_sqnc_num_prod: Annotated[
        int | None, {ALIAS: ALIAS_PROCEDURE, COLUMN_MAP: "clm_val_sqnc_num"}
    ]
    clm_prod_type_cd: Annotated[
        str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_null_string)
    ]
    clm_dgns_prcdr_icd_ind: Annotated[
        str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)
    ]
    clm_dgns_cd: Annotated[str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)]
    clm_poa_ind: Annotated[str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)]
    idr_insrt_ts_prod: Annotated[
        datetime,
        {ALIAS: ALIAS_PROCEDURE, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_prod: Annotated[
        datetime,
        {ALIAS: ALIAS_PROCEDURE, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_line_prfnl
    clm_fed_type_srvc_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_PRFNL}, BeforeValidator(transform_default_string)
    ]
    clm_line_prfnl_dme_price_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_PRFNL}]
    clm_pmt_80_100_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_PRFNL}, BeforeValidator(transform_default_string)
    ]
    clm_prvdr_spclty_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_PRFNL}, BeforeValidator(transform_default_string)
    ]
    clm_srvc_ddctbl_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_PRFNL}, BeforeValidator(transform_default_string)
    ]
    clm_suplr_type_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_PRFNL}, BeforeValidator(transform_default_string)
    ]
    clm_line_carr_psych_ot_lmt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_PRFNL}]
    idr_insrt_ts_line_prfnl: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_PRFNL, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_prfnl: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_PRFNL, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # columns from v2_mdcr_clm_line_dcmtn
    clm_line_bnft_enhncmt_1_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_bnft_enhncmt_2_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_bnft_enhncmt_3_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_bnft_enhncmt_4_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_bnft_enhncmt_5_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_ngaco_cptatn_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_ngaco_pdschrg_hcbs_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_ngaco_snf_wvr_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_ngaco_tlhlth_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_aco_care_mgmt_hcbs_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_ngaco_pbpmt_sw: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_line_pa_uniq_trkng_num: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_null_string)
    ]
    idr_insrt_ts_line_dcmtn: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_DCMTN, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_dcmtn: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_DCMTN, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    # Columns from v2_mdcr_clm_line_mcs
    clm_line_rbndlg_crtfctn_num: Annotated[
        str, {ALIAS: ALIAS_LINE_MCS}, BeforeValidator(transform_default_string)
    ]
    clm_line_hct_lvl_num: Annotated[int | None, {ALIAS: ALIAS_LINE_MCS}]
    clm_line_hgb_lvl_num: Annotated[int | None, {ALIAS: ALIAS_LINE_MCS}]
    idr_insrt_ts_line_mcs: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_MCS, **INSERT_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_mcs: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_MCS, **UPDATE_FIELD},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_prvdr_hstry
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
    prvdr_rndrng_last_name: Annotated[
        str,
        {COLUMN_MAP: "prvdr_last_name", ALIAS: ALIAS_PRVDR_RNDRNG},
        BeforeValidator(transform_default_string),
    ]

    @staticmethod
    def table() -> str:
        return "idr_new.claim_item_professional_ss"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_PROFESSIONAL_SS_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PROFESSIONAL_PAC_PARTITIONS

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:
        clm = ALIAS_CLM
        clm_grp = ALIAS_CLM_GRP
        prod = ALIAS_PROCEDURE
        line = ALIAS_LINE
        line_dcmtn = ALIAS_LINE_DCMTN
        line_mcs = ALIAS_LINE_MCS
        line_prfnl = ALIAS_LINE_PRFNL
        prvdr_rndrng = ALIAS_PRVDR_RNDRNG
        # This query is taking all the values for CLM_PROD, CLM_LINE, and CLM_VAL and storing
        # them in a unified table. This is necessary because each of these tables have a different
        # number of rows for each claim. If we don't combine these values, we would either have to
        # do three separate database queries to load these in the server, or we have to join on each
        # table in the same query and deal with the fact that the result is a cartesian product of
        # clm_line + clm_prod + clm_val which can generate many thousands of rows for large claims.
        # Performing this normalization means we can perform a single query to generate the rows
        # equal to max(len(clm_prod), len(clm_line), len(clm_val)). The number of rows here will
        # usually only be a few dozen, maybe a few hundred at worst.

        # We need to eagerly filter by the claim date in the first CTE at the top to prevent the
        # query from pulling back all of the claims and only filtering at the end. This has a
        # massive impact on the query performance.

        # There's a few steps here:
        #   1. Figure out how many rows we need for each claim.
        #      We do this by taking the UNION of the rows for each table. The end result will be
        #      a list of rows equal to max(len(clm_prod), len(clm_line), len(clm_val)).
        #   2. clm_prod is special because its sequence numbers depend on the values in the table
        #      itself and are not monotonically increasing.
        #      We perform an intermediary step to create our own line number for clm_prod values.
        #   3. Take our list of claim_uniq_id + line number and left join each table against it to
        #      get the final result.

        # In Postgres, we need to tell the query planner to NOT materialize the CTEs because
        # it will behave extremely poorly when trying to join against these large sets of
        # non-indexed data in memory. This is fine in Snowflake because it's fundamentally
        # different, but we need to force this behavior for local testing.
        not_materialized = "" if load_mode == LoadMode.PRODUCTION else "NOT MATERIALIZED"

        return f"""
                WITH claims AS {not_materialized} (
                    SELECT 
                        {clm}.clm_uniq_id, 
                        {clm}.geo_bene_sk, 
                        {clm}.clm_type_cd, 
                        {clm}.clm_num_sk, 
                        {clm}.clm_dt_sgntr_sk,
                        {clm}.clm_rlt_cond_sgntr_sk,
                        {clm}.clm_idr_ld_dt
                    FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
                    WHERE
                        {claim_filter(start_time, partition)} AND
                        {clm}.clm_idr_ld_dt >= '{get_min_transaction_date()}'
                ),
                claim_lines AS {not_materialized} (
                    SELECT
                        {line}.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY {clm}.clm_uniq_id
                            ORDER BY {line}.clm_line_num
                        ) AS bfd_row_id
                    FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_line {line}
                    JOIN claims {clm}
                        ON {line}.geo_bene_sk = {clm}.geo_bene_sk
                        AND {line}.clm_type_cd = {clm}.clm_type_cd
                        AND {line}.clm_num_sk = {clm}.clm_num_sk
                        AND {line}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                ),
                claim_procedures AS {not_materialized} (
                    SELECT
                        {clm}.clm_uniq_id,
                        {prod}.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY {clm}.clm_uniq_id
                            ORDER BY {prod}.clm_prod_type_cd,
                                {prod}.clm_val_sqnc_num
                        ) AS bfd_row_id
                    FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_prod {prod}
                    JOIN claims {clm}
                        ON {prod}.geo_bene_sk = {clm}.geo_bene_sk
                        AND {prod}.clm_type_cd = {clm}.clm_type_cd
                        AND {prod}.clm_num_sk = {clm}.clm_num_sk
                        AND {prod}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                ),
                claim_groups AS (
                    SELECT clm_uniq_id, bfd_row_id
                    FROM claim_lines
                    UNION
                    SELECT clm_uniq_id, bfd_row_id
                    FROM claim_procedures
                )
                SELECT {{COLUMNS}}
                FROM claims {clm}
                JOIN claim_groups {clm_grp}
                    ON {clm_grp}.clm_uniq_id = {clm}.clm_uniq_id
                LEFT JOIN claim_lines {line}
                    ON {line}.geo_bene_sk = {clm}.geo_bene_sk
                    AND {line}.clm_type_cd = {clm}.clm_type_cd
                    AND {line}.clm_num_sk = {clm}.clm_num_sk
                    AND {line}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                    AND {line}.bfd_row_id = {clm_grp}.bfd_row_id
                LEFT JOIN claim_procedures {prod}
                    ON {prod}.geo_bene_sk = {clm}.geo_bene_sk
                    AND {prod}.clm_type_cd = {clm}.clm_type_cd
                    AND {prod}.clm_num_sk = {clm}.clm_num_sk
                    AND {prod}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                    AND {prod}.bfd_row_id = {clm_grp}.bfd_row_id
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_prfnl {line_prfnl}
                    ON {line_prfnl}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_prfnl}.clm_type_cd = {line}.clm_type_cd
                    AND {line_prfnl}.clm_num_sk = {line}.clm_num_sk
                    AND {line_prfnl}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_prfnl}.clm_line_num = {line}.clm_line_num
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_mcs {line_mcs}
                    ON {line_mcs}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_mcs}.clm_type_cd = {line}.clm_type_cd
                    AND {line_mcs}.clm_num_sk = {line}.clm_num_sk
                    AND {line_mcs}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_mcs}.clm_line_num = {line}.clm_line_num
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_dcmtn {line_dcmtn}
                    ON {line_dcmtn}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_dcmtn}.clm_type_cd = {line}.clm_type_cd
                    AND {line_dcmtn}.clm_num_sk = {line}.clm_num_sk
                    AND {line_dcmtn}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_dcmtn}.clm_line_num = {line}.clm_line_num
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry {prvdr_rndrng}
                    ON {prvdr_rndrng}.prvdr_npi_num = {line}.prvdr_rndrng_prvdr_npi_num
                    AND {prvdr_rndrng}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
                {{WHERE_CLAUSE}}
                {{ORDER_BY}}
        """
