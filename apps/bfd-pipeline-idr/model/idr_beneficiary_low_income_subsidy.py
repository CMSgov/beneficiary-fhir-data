from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    BENEFICIARY_TABLE,
    DEFAULT_MAX_DATE,
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
    transform_null_date_to_min,
)


class IdrBeneficiaryLowIncomeSubsidy(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_rng_bgn_dt: Annotated[datetime, {PRIMARY_KEY: True}]
    bene_rng_end_dt: date
    bene_lis_copmt_lvl_cd: str
    bene_lis_ptd_prm_pct: str
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_low_income_subsidy"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_part_d_coverage_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        hstry = ALIAS_HSTRY
        return f"""
                SELECT {{COLUMNS}}
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_lis bene_lis
                {{WHERE_CLAUSE}}
                AND NOT EXISTS (
                    {deceased_bene_filter(hstry)}
                    AND {hstry}.bene_sk = bene_lis.bene_sk
                )
                AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
                {{ORDER_BY}}
            """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
