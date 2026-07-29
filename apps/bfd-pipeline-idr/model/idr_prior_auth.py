from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from constants import DEFAULT_MAX_DATE, IDR_PRIOR_AUTH_TABLE, IDR_PROVIDER_HISTORY_TABLE
from load_partition import LoadPartition
from model.base_model import (
    ALIAS_PRIOR_AUTH,
    ALIAS_PRVDR_ATT_PHY,
    ALIAS_PRVDR_ORDER_REFER,
    ALIAS_PRVDR_RENDER,
    BATCH_TIMESTAMP,
    EXPR,
    INSERT_EXCLUDE,
    PRIMARY_KEY_ORDER,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    ModelType,
    Source,
    provider_careteam_name_expr,
    provider_npi_type_expr,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
)
from settings import MIN_PRIOR_AUTH_LOAD_DATE


class IdrPriorAuth(IdrBaseModel):
    mbi_num: Annotated[str, {PRIMARY_KEY_ORDER: 0}]
    utn: Annotated[str, {PRIMARY_KEY_ORDER: 1}]
    utn_valid_st_dt: Annotated[date, BeforeValidator(transform_null_date_to_min)]
    utn_valid_en_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_type: Annotated[str, BeforeValidator(transform_default_string)]
    mac_id: str
    order_refer_npi: Annotated[str, BeforeValidator(transform_default_string)]
    bfd_order_refer_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_ORDER_REFER, None)},
        BeforeValidator(transform_default_string),
    ]
    bfd_order_refer_npi_type: Annotated[
        int | None, {EXPR: provider_npi_type_expr(ALIAS_PRVDR_ORDER_REFER)}
    ]
    render_npi: Annotated[str, BeforeValidator(transform_default_string)]
    bfd_render_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_RENDER, None)},
        BeforeValidator(transform_default_string),
    ]
    bfd_render_npi_type: Annotated[int | None, {EXPR: provider_npi_type_expr(ALIAS_PRVDR_RENDER)}]
    icn_dcn: Annotated[str, BeforeValidator(transform_default_string)]
    cms_cert: Annotated[str, BeforeValidator(transform_default_string)]
    npi: Annotated[str, BeforeValidator(transform_default_string)]
    name: str
    tob: Annotated[str, BeforeValidator(transform_default_string)]
    att_phy_npi: Annotated[str, BeforeValidator(transform_default_string)]
    bfd_att_phy_careteam_name: Annotated[
        str,
        {EXPR: provider_careteam_name_expr(ALIAS_PRVDR_ATT_PHY, None)},
        BeforeValidator(transform_default_string),
    ]
    bfd_att_phy_npi_type: Annotated[int | None, {EXPR: provider_npi_type_expr(ALIAS_PRVDR_ATT_PHY)}]
    # TBD: might have to change the insert & update ts once IDR adds those
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True, INSERT_EXCLUDE: True}]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, INSERT_EXCLUDE: True},
        BeforeValidator(transform_null_date_to_min),
    ]

    @override
    @staticmethod
    def table() -> str:
        return "idr.prior_auth"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.PRIOR_AUTH

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        # Prior auth data older than the lookback period should be filtered
        prior_auth = ALIAS_PRIOR_AUTH
        prvdr_att_phy = ALIAS_PRVDR_ATT_PHY
        prvdr_order_refer = ALIAS_PRVDR_ORDER_REFER
        prvdr_render = ALIAS_PRVDR_RENDER
        return f"""
            WITH distinct_prior_auths AS (
                SELECT *, ROW_NUMBER() 
                    OVER (PARTITION BY mbi_num, utn ORDER BY current_segment) as row_order
                FROM {IDR_PRIOR_AUTH_TABLE}
                WHERE pa_req_rec_dt > '{MIN_PRIOR_AUTH_LOAD_DATE}'
            ) 
            SELECT {{COLUMNS}} FROM distinct_prior_auths {prior_auth}
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_att_phy}
                ON {prvdr_att_phy}.prvdr_npi_num = {prior_auth}.att_phy_npi
                AND {prvdr_att_phy}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_order_refer}
                ON {prvdr_order_refer}.prvdr_npi_num = {prior_auth}.order_refer_npi
                AND {prvdr_order_refer}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            LEFT JOIN {IDR_PROVIDER_HISTORY_TABLE} {prvdr_render}
                ON {prvdr_render}.prvdr_npi_num = {prior_auth}.render_npi
                AND {prvdr_render}.prvdr_hstry_obslt_dt >= '{DEFAULT_MAX_DATE}'
            {{WHERE_CLAUSE}} AND row_order = 1;
            """
