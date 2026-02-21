from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    BENEFICIARY_TABLE,
    NON_CLAIM_PARTITION,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    ALIAS,
    ALIAS_HSTRY,
    ALIAS_XREF,
    BATCH_ID,
    BATCH_TIMESTAMP,
    COLUMN_MAP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    deceased_bene_filter,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
    transform_null_string,
)


class IdrBeneficiary(IdrBaseModel):
    # columns from V2_MDCR_BENE_HSTRY
    bene_sk: Annotated[
        int,
        {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_HSTRY, LAST_UPDATED_TIMESTAMP: True},
    ]
    bene_xref_efctv_sk: int
    bene_mbi_id: str
    bene_1st_name: str
    bene_midl_name: Annotated[str, BeforeValidator(transform_null_string)]
    bene_last_name: str
    bene_brth_dt: date
    bene_death_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    bene_vrfy_death_day_sw: Annotated[str, BeforeValidator(transform_default_string)]
    bene_sex_cd: str
    bene_race_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_usps_state_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_zip5_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_zip_plc_name: Annotated[str, BeforeValidator(transform_default_string)]
    bene_line_1_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_2_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_3_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_4_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_5_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_6_adr: Annotated[str, BeforeValidator(transform_null_string)]
    cntct_lang_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts_bene: Annotated[
        datetime, {BATCH_TIMESTAMP: True, ALIAS: ALIAS_HSTRY, COLUMN_MAP: "idr_insrt_ts"}
    ]
    idr_updt_ts_bene: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_HSTRY, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    # columns from V2_MDCR_BENE_XREF
    bene_kill_cred_cd: Annotated[str, BeforeValidator(transform_default_string)]
    src_rec_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date_to_min)]
    idr_insrt_ts_xref: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_XREF, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_xref: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_XREF, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def computed_keys() -> list[str]:
        return ["bene_xref_efctv_sk_computed"]

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_patient_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        xref = ALIAS_XREF
        # There can be multiple xref records for the same bene_sk/bene_ref_sk combo
        # so we need to find the most recent one based on src_rec_updt_ts.

        # Unlike idr_updt_ts, src_rec_updt_ts will be set to the created timestamp
        # if no update has been applied. Therefore, we can just check the updated timestamp
        # without caring about the created timestamp.

        # There can also be duplicate values with the same idr_insrt_ts, so we have to rely on
        # src_rec_insrt_ts/src_rec_updt_ts for this.
        return f"""
            WITH ordered_xref AS (
                SELECT bene_sk,
                    bene_xref_sk,
                    bene_hicn_num,
                    src_rec_crte_ts,
                    ROW_NUMBER() OVER (
                        PARTITION BY bene_sk, bene_xref_sk
                        ORDER BY src_rec_updt_ts DESC
                    ) AS row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref
            ),
            current_xref AS (
                SELECT
                    ox.bene_sk,
                    ox.bene_xref_sk,
                    bx.bene_kill_cred_cd,
                    bx.src_rec_updt_ts,
                    bx.idr_insrt_ts,
                    bx.idr_updt_ts
                FROM ordered_xref ox
                JOIN cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref bx
                    ON bx.bene_sk = ox.bene_sk
                    AND bx.bene_xref_sk = ox.bene_xref_sk
                    AND bx.bene_hicn_num = ox.bene_hicn_num
                    AND bx.src_rec_crte_ts = ox.src_rec_crte_ts
                WHERE ox.row_order = 1
            ),
            deceased_benes AS (
                {deceased_bene_filter(hstry)}
            )
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry {hstry}
            -- NOTE: the join condition is intentionally inverted here
            -- In the xref table, the bene_sk and bene_xref_sk fields are mirrored
            LEFT JOIN current_xref {xref}
                ON {xref}.bene_sk = {hstry}.bene_xref_sk
                AND {xref}.bene_xref_sk = {hstry}.bene_sk
            {{WHERE_CLAUSE}}
            AND {hstry}.bene_mbi_id IS NOT NULL
            AND NOT EXISTS (SELECT 1 FROM deceased_benes db WHERE db.bene_sk = {hstry}.bene_sk)
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
