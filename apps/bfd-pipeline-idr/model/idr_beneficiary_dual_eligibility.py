from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

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


class IdrBeneficiaryDualEligibility(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_mdcd_elgblty_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcd_elgblty_end_dt: date
    bene_dual_stus_cd: str
    bene_dual_type_cd: str
    geo_usps_state_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @override
    @staticmethod
    def table() -> str:
        return "idr.beneficiary_dual_eligibility"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_dual_coverage_updated_ts",
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
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_cmbnd_dual_mdcr dual
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {deceased_bene_filter(hstry, start_time)}
                AND {hstry}.bene_sk = dual.bene_sk
            )
            {{ORDER_BY}}
        """
