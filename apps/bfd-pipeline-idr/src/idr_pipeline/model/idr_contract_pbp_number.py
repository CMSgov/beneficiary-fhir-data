from datetime import datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from ..load_partition import LoadPartition
from ..model.base_model import (
    ALIAS,
    ALIAS_CNTRCT_SGMT,
    ALIAS_PBP_NUM,
    BATCH_ID,
    PRIMARY_KEY_ORDER,
    IdrBaseModel,
    ModelType,
    Source,
    transform_default_string,
)
from ..settings import SETTINGS


class IdrContractPbpNumber(IdrBaseModel):
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY_ORDER: 0, BATCH_ID: True, ALIAS: ALIAS_PBP_NUM}]
    cntrct_drug_plan_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_name: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_sgmt_num: Annotated[
        str, ALIAS:ALIAS_CNTRCT_SGMT, BeforeValidator(transform_default_string)
    ]

    @override
    @staticmethod
    def table() -> str:
        return "idr.contract_pbp_number"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.BENEFICIARY

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        pbp_num = ALIAS_PBP_NUM
        # We need to include obsolete records since some bene_mapd records are tied to
        # obsolete pbp_sks.
        # Include all contracts but don't join on the segment table when >1 segment exists
        # since we can't map those yet.
        return f"""
            WITH sgmt_count AS (
                SELECT
                    {pbp_num}.cntrct_pbp_sk,
                    COUNT(*) AS cntrct_count
                FROM {SETTINGS.idr_contract_pbp_num_table} {pbp_num}
                LEFT JOIN {SETTINGS.idr_contract_pbp_segment_table} sgmt 
                    ON {pbp_num}.cntrct_pbp_sk = sgmt.cntrct_pbp_sk
                GROUP BY {pbp_num}.cntrct_pbp_sk
            )
            SELECT
                {{COLUMNS}}
            FROM {SETTINGS.idr_contract_pbp_num_table} {pbp_num} {{TABLESAMPLE}}
            JOIN sgmt_count ON {pbp_num}.cntrct_pbp_sk = sgmt_count.cntrct_pbp_sk
            LEFT JOIN {SETTINGS.idr_contract_pbp_segment_table} sgmt
                ON sgmt.cntrct_pbp_sk = {pbp_num}.cntrct_pbp_sk AND sgmt_count.cntrct_count = 1
            WHERE {pbp_num}.cntrct_pbp_sk != 0
            {{LIMIT}}
            """
