from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    DEFAULT_MAX_DATE,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    BATCH_ID,
    BATCH_TIMESTAMP,
    DERIVED,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    idr_dates_from_meta_sk,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
    transform_null_string,
)


class IdrProviderHistory(IdrBaseModel):
    prvdr_npi_num: Annotated[str, {PRIMARY_KEY: True, BATCH_ID: True}]
    prvdr_sk: int
    prvdr_hstry_efctv_dt: datetime
    prvdr_mdl_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_txnmy_cmpst_cd: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_oscar_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_1st_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_hstry_obslt_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    prvdr_lgl_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_emplr_id_num: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_last_name: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, DERIVED: True},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, DERIVED: True},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.provider_history"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return f"""
            SELECT
            {idr_dates_from_meta_sk()},
            {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_prvdr_hstry
            WHERE prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return []
