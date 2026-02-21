from collections.abc import Sequence
from datetime import datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    CLAIM_TABLE,
    INSTITUTIONAL_PAC_PARTITIONS,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_FISS,
    BATCH_ID,
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    transform_null_date_to_min,
    transform_null_string,
)


class IdrClaimFiss(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    clm_crnt_stus_cd: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_FISS},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_FISS},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_fiss"

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        clm = ALIAS_CLM
        fiss = ALIAS_FISS
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_fiss {fiss} ON
                {clm}.geo_bene_sk = {fiss}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {fiss}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {fiss}.clm_type_cd AND
                {clm}.clm_num_sk = {fiss}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return INSTITUTIONAL_PAC_PARTITIONS
