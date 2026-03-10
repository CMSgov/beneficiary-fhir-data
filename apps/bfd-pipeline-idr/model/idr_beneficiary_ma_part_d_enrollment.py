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
from model.base_model import (
    ALIAS_HSTRY,
    BATCH_ID,
    BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    deceased_bene_filter,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
)


class IdrBeneficiaryMaPartDEnrollment(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    cntrct_pbp_sk: int
    bene_pbp_num: str
    bene_enrlmt_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_enrlmt_end_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    bene_cntrct_num: str
    bene_cvrg_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    bene_enrlmt_pgm_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_enrlmt_emplr_sbsdy_sw: Annotated[str, BeforeValidator(transform_default_string)]
    idr_ltst_trans_flg: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_ma_part_d_enrollment"

    @staticmethod
    def last_updated_date_table() -> str:
        return BENEFICIARY_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return [
            "bfd_part_c_coverage_updated_ts",
            "bfd_part_d_coverage_updated_ts",
        ]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        # There are only a very few instances where non-obsolete records have a
        # bene_enrlmt_pgm_type_cd set to '~' and these are all from the 80s,
        # so it should be safe to filter these.
        hstry = ALIAS_HSTRY
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mapd_enrlmt enrlmt
            {{WHERE_CLAUSE}}
            AND NOT EXISTS (
                {deceased_bene_filter(hstry)}
                AND {hstry}.bene_sk = enrlmt.bene_sk
            )
            AND idr_trans_obslt_ts >= '{DEFAULT_MAX_DATE}'
            AND bene_enrlmt_pgm_type_cd != '~'
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
