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
    ALIAS_HSTRY,
    BATCH_ID,
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    deceased_bene_filter,
    transform_default_string,
    transform_null_date_to_min,
)


class IdrBeneficiaryThirdParty(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_buyin_cd: Annotated[str, BeforeValidator(transform_default_string)]
    bene_tp_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_third_party"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_a_coverage_updated_ts",
            "bfd_part_b_coverage_updated_ts",
        ]

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp tp
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = tp.bene_sk
            )
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
