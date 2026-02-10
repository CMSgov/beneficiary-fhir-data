from collections.abc import Sequence
from datetime import datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_TABLE,
    PROFESSIONAL_ADJUDICATED_PARTITIONS,
    PROFESSIONAL_PAC_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_LCTN_HSTRY,
    ALIAS_PRFNL,
    BATCH_ID,
    BATCH_TIMESTAMP,
    COLUMN_MAP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    transform_default_string,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_string,
)


class IdrClaimProfessional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_carr_pmt_dnl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_clncl_tril_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_prfnl_prmry_pyr_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_prfnl_prvdr_asgnmt_sw: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prvdr_acnt_rcvbl_ofst_amt: Annotated[float, BeforeValidator(transform_null_float)]
    idr_insrt_ts_clm_prfnl: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_PRFNL, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_clm_prfnl: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_PRFNL, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    # column from v2_mdcr_clm_lctn_hstry
    clm_audt_trl_stus_cd: Annotated[
        str, {ALIAS: ALIAS_LCTN_HSTRY}, BeforeValidator(transform_null_string)
    ]
    idr_insrt_ts_lctn_hstry: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_LCTN_HSTRY, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_lctn_hstry: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_LCTN_HSTRY, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_professional"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        prfnl = ALIAS_PRFNL
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
                FROM claims {clm}
                JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_prfnl {prfnl} ON
                    {clm}.geo_bene_sk = {prfnl}.geo_bene_sk AND
                    {clm}.clm_type_cd = {prfnl}.clm_type_cd AND
                    {clm}.clm_dt_sgntr_sk = {prfnl}.clm_dt_sgntr_sk AND
                    {clm}.clm_num_sk = {prfnl}.clm_num_sk
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
                {{WHERE_CLAUSE}}
                {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return PROFESSIONAL_ADJUDICATED_PARTITIONS + PROFESSIONAL_PAC_PARTITIONS
