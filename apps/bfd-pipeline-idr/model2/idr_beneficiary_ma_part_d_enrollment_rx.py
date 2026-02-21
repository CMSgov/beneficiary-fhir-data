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
from model import (
    ALIAS_HSTRY,
    BATCH_ID,
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    deceased_bene_filter,
    transform_default_string,
    transform_null_date_to_min,
)


class IdrBeneficiaryMaPartDEnrollmentRx(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    cntrct_pbp_sk: int
    bene_cntrct_num: Annotated[str, {PRIMARY_KEY: True}]
    bene_pbp_num: Annotated[str, {PRIMARY_KEY: True}]
    bene_enrlmt_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_pdp_enrlmt_mmbr_id_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_pdp_enrlmt_grp_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_pdp_enrlmt_prcsr_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_pdp_enrlmt_bank_id_num: Annotated[str, BeforeValidator(transform_default_string)]
    bene_enrlmt_pdp_rx_info_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_ma_part_d_enrollment_rx"

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
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mapd_enrlmt_rx enrlmt_rx
                {{WHERE_CLAUSE}}
                AND NOT EXISTS (
                    {deceased_bene_filter(hstry)}
                    AND {hstry}.bene_sk = enrlmt_rx.bene_sk
                )
                AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
                {{ORDER_BY}}
            """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
