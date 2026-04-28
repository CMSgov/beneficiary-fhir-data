from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from constants import IDR_BENE_ENTITLEMENT_REASON_TABLE
from load_partition import LoadPartition
from loader import LoadMode
from model.base_model import (
    ALIAS_HSTRY,
    BATCH_ID,
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    ModelType,
    deceased_bene_filter,
    transform_default_string,
    transform_null_date_to_min,
)


class IdrBeneficiaryEntitlementReason(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_rsn_cd: str
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @override
    @staticmethod
    def table() -> str:
        return "idr.beneficiary_entitlement_reason"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_a_coverage_updated_ts",
            "bfd_part_b_coverage_updated_ts",
        ]

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.BENEFICIARY

    @override
    @classmethod
    def fetch_query(
        cls, partition: LoadPartition, start_time: datetime, load_mode: LoadMode
    ) -> str:
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM {IDR_BENE_ENTITLEMENT_REASON_TABLE} rsn
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {deceased_bene_filter(hstry, start_time)}
                AND {hstry}.bene_sk = rsn.bene_sk
            )
            {{ORDER_BY}}
        """
