from datetime import date, datetime
from typing import Annotated, override

from load_partition import LoadPartition
from loader import LoadMode
from model.base_model import (
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    ModelType,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
)
from pydantic import BeforeValidator


class IdrBeneficiaryMbiId(IdrBaseModel):
    bene_mbi_id: Annotated[str, {PRIMARY_KEY: True, LAST_UPDATED_TIMESTAMP: True}]
    bene_mbi_efctv_dt: date
    bene_mbi_obslt_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_default_string)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @override
    @staticmethod
    def table() -> str:
        return "idr.beneficiary_mbi_id"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_patient_updated_ts"]

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.BENEFICIARY

    @override
    @classmethod
    def fetch_query(
        cls, partition: LoadPartition, start_time: datetime, load_mode: LoadMode
    ) -> str:
        return """
               SELECT {COLUMNS}
               FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id
                   {WHERE_CLAUSE}
                   {ORDER_BY}
               """
