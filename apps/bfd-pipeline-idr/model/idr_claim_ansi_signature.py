from collections.abc import Sequence
from datetime import datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    COMBINED_CLAIM_PARTITION,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    BATCH_ID,
    BATCH_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    transform_default_string,
    transform_null_date_to_min,
)


class IdrClaimAnsiSignature(IdrBaseModel):
    clm_ansi_sgntr_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True}]
    clm_1_rev_cntr_ansi_grp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_2_rev_cntr_ansi_grp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_3_rev_cntr_ansi_grp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_4_rev_cntr_ansi_grp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_1_rev_cntr_ansi_rsn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_2_rev_cntr_ansi_rsn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_3_rev_cntr_ansi_rsn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_4_rev_cntr_ansi_rsn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts: Annotated[
        datetime, {BATCH_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_ansi_signature"

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS_NO_ALIAS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_ansi_sgntr
            {WHERE_CLAUSE}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [COMBINED_CLAIM_PARTITION]
