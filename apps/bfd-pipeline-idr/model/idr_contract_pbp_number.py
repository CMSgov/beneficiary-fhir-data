from datetime import datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from load_partition import LoadPartition
from loader import LoadMode
from model.base_model import (
    ALIAS,
    ALIAS_CNTRCT_SGMT,
    ALIAS_PBP_NUM,
    BATCH_ID,
    DERIVED,
    PRIMARY_KEY,
    IdrBaseModel,
    ModelType,
    transform_default_string,
)


class IdrContractPbpNumber(IdrBaseModel):
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, ALIAS: ALIAS_PBP_NUM}]
    cntrct_drug_plan_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_name: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_num: Annotated[str, BeforeValidator(transform_default_string)]
    cntrct_pbp_sgmt_num: Annotated[
        str, ALIAS:ALIAS_CNTRCT_SGMT, BeforeValidator(transform_default_string)
    ]
    bfd_contract_version_rank: Annotated[int, {DERIVED: True}]

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
    def fetch_query(
        cls, partition: LoadPartition, start_time: datetime, load_mode: LoadMode
    ) -> str:
        pbp_num = ALIAS_PBP_NUM
        # We need to include obsolete records since some bene_mapd records are tied to
        # obsolete pbp_sks.
        # Additionally, some contracts are marked obsolete and no non-obsolete record
        # is created, so we have to use RANK to get the latest version of each contract.
        # Then, these can be queries by searching for rows where
        # bfd_contract_version_rank = 1
        return f"""
            WITH sgmt as (
                SELECT
                    cntrct_pbp_sk,
                    cntrct_pbp_sgmt_num
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_sgmt
                GROUP BY cntrct_pbp_sk, cntrct_pbp_sgmt_num
                HAVING COUNT(*) = 1
            )
            SELECT 
                {{COLUMNS}},
                ROW_NUMBER() OVER (
                    PARTITION BY cntrct_num, cntrct_pbp_num 
                    ORDER BY cntrct_pbp_sk_obslt_dt DESC) AS bfd_contract_version_rank
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num {pbp_num}
            LEFT JOIN sgmt
                    ON {pbp_num}.cntrct_pbp_sk = sgmt.cntrct_pbp_sk
            WHERE {pbp_num}.cntrct_pbp_sk != 0
            """
