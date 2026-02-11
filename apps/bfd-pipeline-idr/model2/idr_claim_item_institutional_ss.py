from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_INSTITUTIONAL_SS_TABLE,
    INSTITUTIONAL_PAC_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_CLM_GRP,
    ALIAS_LINE,
    ALIAS_LINE_DCMTN,
    ALIAS_LINE_FISS,
    ALIAS_LINE_FISS_BFNT,
    ALIAS_LINE_INSTNL,
    ALIAS_PROCEDURE,
    ALIAS_RLT_COND,
    ALIAS_VAL,
    BATCH_ID,
    BATCH_TIMESTAMP,
    COLUMN_MAP,
    HISTORICAL_BATCH_TIMESTAMP,
    INSERT_EXCLUDE,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    get_min_transaction_date,
    transform_default_date_to_null,
    transform_default_hipps_code,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_string,
)


class IdrClaimItemInstitutionalSs(IdrBaseModel):
    clm_uniq_id: Annotated[
        int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_CLM, LAST_UPDATED_TIMESTAMP: True}
    ]
    bfd_row_id: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_CLM_GRP}]
    # columns from V2_MDCR_CLM_LINE
    clm_line_num: Annotated[int | None, {ALIAS: ALIAS_LINE}]
    clm_line_sbmt_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ncvrd_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ncvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_prvdr_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_bene_pmt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_bene_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_cvrd_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_blood_ddctbl_amt: float | None
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
    clm_line_ndc_qty_qlfyr_cd: Annotated[
        str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_line_srvc_unit_qty: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_alowd_chrg_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_ansthsa_unit_cnt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_rev_ctr_cd: Annotated[
        str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)
    ]
    clm_line_othr_tp_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    clm_line_otaf_amt: Annotated[float | None, {ALIAS: ALIAS_LINE}]
    hcpcs_1_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_2_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_3_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_4_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    hcpcs_5_mdfr_cd: Annotated[str, {ALIAS: ALIAS_LINE}, BeforeValidator(transform_default_string)]
    clm_idr_ld_dt: Annotated[
        date, {INSERT_EXCLUDE: True, ALIAS: ALIAS_LINE, HISTORICAL_BATCH_TIMESTAMP: True}
    ]
    idr_insrt_ts_line: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            ALIAS: ALIAS_LINE,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_LINE,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from V2_MDCR_CLM_PROD
    clm_val_sqnc_num_prod: Annotated[
        int | None, {ALIAS: ALIAS_PROCEDURE, COLUMN_MAP: "clm_val_sqnc_num"}
    ]
    clm_prod_type_cd: Annotated[
        str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_null_string)
    ]
    clm_prcdr_cd: Annotated[
        str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)
    ]
    clm_dgns_prcdr_icd_ind: Annotated[
        str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)
    ]
    clm_dgns_cd: Annotated[str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)]
    clm_poa_ind: Annotated[str, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_string)]
    clm_prcdr_prfrm_dt: Annotated[
        date | None, {ALIAS: ALIAS_PROCEDURE}, BeforeValidator(transform_default_date_to_null)
    ]
    idr_insrt_ts_prod: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            ALIAS: ALIAS_PROCEDURE,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_prod: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_PROCEDURE,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from V2_MDCR_CLM_VAL
    clm_val_sqnc_num_val: Annotated[int | None, {ALIAS: ALIAS_VAL, COLUMN_MAP: "clm_val_sqnc_num"}]
    clm_val_cd: Annotated[str, {ALIAS: ALIAS_VAL}, BeforeValidator(transform_default_string)]
    clm_val_amt: float | None
    idr_insrt_ts_val: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_VAL, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_val: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_VAL, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from V2_MDCR_CLM_RLT_COND_SGNTR_MBR
    clm_rlt_cond_cd: Annotated[
        str, {ALIAS: ALIAS_RLT_COND}, BeforeValidator(transform_default_string)
    ]
    clm_rlt_cond_sgntr_sqnc_num: Annotated[int | None, {ALIAS: ALIAS_RLT_COND}]
    idr_insrt_ts_rlt_cond: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            ALIAS: ALIAS_RLT_COND,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_rlt_cond: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_RLT_COND,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    # Columns from v2_mdcr_clm_line_instnl
    clm_rev_apc_hipps_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_hipps_code)
    ]
    clm_otaf_one_ind_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_rev_dscnt_ind_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_rev_packg_ind_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_rev_cntr_stus_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_rev_pmt_mthd_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_ddctbl_coinsrnc_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_INSTNL}, BeforeValidator(transform_default_string)
    ]
    clm_line_instnl_rate_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_instnl_adjstd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_instnl_rdcd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_instnl_msp1_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_instnl_msp2_pd_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_instnl_rev_ctr_dt: Annotated[date | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    clm_rev_cntr_tdapa_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_add_on_pymt_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    clm_line_non_ehr_rdctn_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_INSTNL}]
    idr_insrt_ts_line_instnl: Annotated[
        datetime,
        {
            BATCH_TIMESTAMP: True,
            ALIAS: ALIAS_LINE_INSTNL,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_insrt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_instnl: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_LINE_INSTNL,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from v2_mdcr_clm_line_dcmtn
    clm_line_pa_uniq_trkng_num: Annotated[
        str, {ALIAS: ALIAS_LINE_DCMTN}, BeforeValidator(transform_null_string)
    ]
    idr_insrt_ts_line_dcmtn: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_DCMTN, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_dcmtn: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_LINE_DCMTN,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from v2_mdcr_clm_line_fiss
    clm_line_msp_coinsrnc_amt: Annotated[float | None, {ALIAS: ALIAS_LINE_FISS}]
    idr_insrt_ts_line_fiss: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_FISS, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_fiss: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_LINE_FISS,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from v2_mdcr_clm_line_fiss_bnft_svg
    clm_bnft_svg_ansi_grp_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_FISS_BFNT}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_svg_ansi_rsn_cd: Annotated[
        str, {ALIAS: ALIAS_LINE_FISS_BFNT}, BeforeValidator(transform_default_string)
    ]
    idr_insrt_ts_line_fiss_bnft: Annotated[
        datetime,
        {ALIAS: ALIAS_LINE_FISS_BFNT, INSERT_EXCLUDE: True, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_line_fiss_bnft: Annotated[
        datetime,
        {
            UPDATE_TIMESTAMP: True,
            ALIAS: ALIAS_LINE_FISS_BFNT,
            INSERT_EXCLUDE: True,
            COLUMN_MAP: "idr_updt_ts",
        },
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr_new.claim_item_institutional_ss"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_INSTITUTIONAL_SS_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:
        clm = ALIAS_CLM
        clm_grp = ALIAS_CLM_GRP
        prod = ALIAS_PROCEDURE
        line = ALIAS_LINE
        line_instnl = ALIAS_LINE_INSTNL
        val = ALIAS_VAL
        rlt_cond = ALIAS_RLT_COND
        line_dcmtn = ALIAS_LINE_DCMTN
        line_fiss = ALIAS_LINE_FISS
        line_fiss_bnft = ALIAS_LINE_FISS_BFNT
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
                claim_vals AS {not_materialized} (
                    SELECT
                        {clm}.clm_uniq_id,
                        {val}.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY {clm}.clm_uniq_id
                            ORDER BY {val}.clm_val_sqnc_num
                        ) AS bfd_row_id
                    FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_val {val}
                    JOIN claims {clm}
                        ON {val}.geo_bene_sk = {clm}.geo_bene_sk
                        AND {val}.clm_type_cd = {clm}.clm_type_cd
                        AND {val}.clm_num_sk = {clm}.clm_num_sk
                        AND {val}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                ),
                claim_related_conditions AS {not_materialized} (
                    SELECT
                        {clm}.clm_uniq_id,
                        {rlt_cond}.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY {clm}.clm_uniq_id 
                            ORDER BY {rlt_cond}.clm_rlt_cond_cd,
                                {rlt_cond}.clm_rlt_cond_sgntr_sqnc_num
                        ) AS bfd_row_id
                    FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_rlt_cond_sgntr_mbr {rlt_cond}
                    JOIN claims {clm}
                        ON {rlt_cond}.clm_rlt_cond_sgntr_sk = {clm}.clm_rlt_cond_sgntr_sk
                    WHERE 
                        {clm}.clm_rlt_cond_sgntr_sk != 0
                        AND {clm}.clm_rlt_cond_sgntr_sk != 1
                ),
                claim_groups AS (
                    SELECT clm_uniq_id, bfd_row_id
                    FROM claim_lines
                    UNION
                    SELECT clm_uniq_id, bfd_row_id
                    FROM claim_procedures
                    UNION
                    SELECT clm_uniq_id, bfd_row_id
                    FROM claim_vals
                    UNION
                    SELECT clm_uniq_id, bfd_row_id
                    FROM claim_related_conditions
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
                LEFT JOIN claim_vals {val}
                    ON {val}.geo_bene_sk = {clm}.geo_bene_sk
                    AND {val}.clm_type_cd = {clm}.clm_type_cd
                    AND {val}.clm_num_sk = {clm}.clm_num_sk
                    AND {val}.clm_dt_sgntr_sk = {clm}.clm_dt_sgntr_sk
                    AND {val}.bfd_row_id = {clm_grp}.bfd_row_id
                LEFT JOIN claim_related_conditions {rlt_cond}
                    ON {rlt_cond}.clm_uniq_id = {clm}.clm_uniq_id
                    AND {rlt_cond}.bfd_row_id = {clm_grp}.bfd_row_id
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_instnl {line_instnl}
                    ON {line_instnl}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_instnl}.clm_type_cd = {line}.clm_type_cd
                    AND {line_instnl}.clm_num_sk = {line}.clm_num_sk
                    AND {line_instnl}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_instnl}.clm_line_num = {line}.clm_line_num
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_dcmtn {line_dcmtn}
                    ON {line_dcmtn}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_dcmtn}.clm_type_cd = {line}.clm_type_cd
                    AND {line_dcmtn}.clm_num_sk = {line}.clm_num_sk
                    AND {line_dcmtn}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_dcmtn}.clm_line_num = {line}.clm_line_num
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_fiss {line_fiss}
                    ON {line_fiss}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_fiss}.clm_type_cd = {line}.clm_type_cd
                    AND {line_fiss}.clm_num_sk = {line}.clm_num_sk
                    AND {line_fiss}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_fiss}.clm_line_num = {line}.clm_line_num
                LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_fiss_bnft_svg {line_fiss_bnft}
                    ON {line_fiss_bnft}.geo_bene_sk = {line}.geo_bene_sk
                    AND {line_fiss_bnft}.clm_type_cd = {line}.clm_type_cd
                    AND {line_fiss_bnft}.clm_num_sk = {line}.clm_num_sk
                    AND {line_fiss_bnft}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk
                    AND {line_fiss_bnft}.clm_line_num = {line}.clm_line_num
                {{WHERE_CLAUSE}}
                {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_PAC_PARTITIONS
