from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from constants import (
    DEFAULT_MAX_DATE,
    IDR_BENE_LOW_INCOME_SUBSIDY_CMBND_TABLE,
)
from load_partition import LoadPartition
from model.base_model import (
    ALIAS_HSTRY,
    BATCH_ID,
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY_ORDER,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    ModelType,
    Source,
    deceased_bene_filter,
    transform_null_date_to_min,
)


class IdrBeneficiaryLowIncomeSubsidyCmbnd(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY_ORDER: 0, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_cmbnd_deemd_efctv_dt: Annotated[datetime, {PRIMARY_KEY_ORDER: 1}]
    bene_cmbnd_deemd_trmntn_dt: date
    bene_cmbnd_deemd_copmt_lvl_id: str
    bene_cmbnd_deemd_prm_pct: str
    bene_cmbnd_deemd_ind: str
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @override
    @staticmethod
    def table() -> str:
        return "idr.beneficiary_low_income_subsidy_cmbnd"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_part_d_coverage_updated_ts"]

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.BENEFICIARY

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        hstry = ALIAS_HSTRY
        return f"""
                SELECT {{COLUMNS}}
                FROM {IDR_BENE_LOW_INCOME_SUBSIDY_CMBND_TABLE} bene_lis
                {{WHERE_CLAUSE}}
                AND NOT EXISTS (
                    {deceased_bene_filter(hstry, start_time)}
                    AND {hstry}.bene_sk = bene_lis.bene_sk
                )
                AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
                {{ORDER_BY}}
            """
