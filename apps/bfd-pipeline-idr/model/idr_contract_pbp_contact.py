from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    DEFAULT_MAX_DATE,
    NON_CLAIM_PARTITION,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    BATCH_ID,
    PRIMARY_KEY,
    IdrBaseModel,
    transform_default_string,
)


class IdrContractPbpContact(IdrBaseModel):
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True}]
    cntrct_plan_cntct_obslt_dt: date
    cntrct_plan_cntct_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_free_extnsn_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_free_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_extnsn_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_tel_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_end_dt: date
    cntrct_pbp_bgn_dt: date
    cntrct_plan_cntct_st_1_adr: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_st_2_adr: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_city_name: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_state_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_plan_cntct_zip_cd: Annotated[str, BeforeValidator(transform_default_string)]

    @staticmethod
    def table() -> str:
        return "idr.contract_pbp_contact"

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return f"""
            WITH contract_contacts as (
                SELECT {{COLUMNS}}, ROW_NUMBER() OVER (
                    PARTITION BY cntrct_pbp_sk
                ORDER BY cntrct_pbp_bgn_dt,
                CASE
                    WHEN cntrct_plan_cntct_type_cd = '62' THEN 1
                    WHEN cntrct_plan_cntct_type_cd = '30' THEN 2
                    ELSE 3
                END) as row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_cntct cntct
                WHERE cntrct_plan_cntct_obslt_dt >= '{DEFAULT_MAX_DATE}'
                AND cntrct_pbp_bgn_dt >= DATE_TRUNC('MONTH', CURRENT_DATE)
                AND cntrct_pbp_bgn_dt < cntrct_pbp_end_dt
            )
            SELECT {{COLUMNS}} FROM contract_contacts WHERE row_order = 1
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
