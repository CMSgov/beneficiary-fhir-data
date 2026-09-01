from datetime import date, datetime
from typing import Annotated, override

from pydantic import BeforeValidator

from ..load_partition import LoadPartition
from ..model.base_model import (
    ALIAS_PRIOR_AUTH,
    PRIMARY_KEY_ORDER,
    IdrBaseModel,
    ModelType,
    Source,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
)
from ..settings import SETTINGS


class IdrPriorAuthItem(IdrBaseModel):
    mbi_num: Annotated[str, {PRIMARY_KEY_ORDER: 0}]
    utn: Annotated[str, {PRIMARY_KEY_ORDER: 1}]
    current_segment: Annotated[int, {PRIMARY_KEY_ORDER: 2}]
    hcpcs_or_cpt_or_hipps: str
    price_mod1: Annotated[str, BeforeValidator(transform_default_string)]
    price_mod2: Annotated[str, BeforeValidator(transform_default_string)]
    place_of_serv: Annotated[str, BeforeValidator(transform_default_string)]
    rev_code_1: Annotated[str, BeforeValidator(transform_default_string)]
    pa_dt_added: date
    pa_dt_updated: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    pa_decision: str
    pa_req_sub_dt: date
    pa_req_rec_dt: date
    pa_decision_dt: Annotated[date, BeforeValidator(transform_null_date_to_min)]
    pa_decision_exp_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    service_cnts: int
    svc_render_st: str
    mr_count_ind: int
    mr_count_st_dt: Annotated[date, BeforeValidator(transform_null_date_to_min)]
    mr_count_end_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    rrb_excl_ind: Annotated[str, BeforeValidator(transform_default_string)]

    @override
    @staticmethod
    def table() -> str:
        return "idr.prior_auth_item"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.PRIOR_AUTH

    @override
    @staticmethod
    def should_delete_missing() -> bool:
        return True

    @override
    @classmethod
    def is_immutable(cls) -> bool:
        return False

    @override
    @staticmethod
    def synthetic_data_filter() -> str:
        return "utn NOT LIKE '-%'"

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        # Prior auth data older than the lookback period should be filtered
        prior_auth = ALIAS_PRIOR_AUTH
        return f"""
            WITH distinct_prior_auths AS (
                SELECT *, ROW_NUMBER()
                    OVER (PARTITION BY mbi_num, utn, current_segment ORDER BY mbi_num) as row_order
                FROM {SETTINGS.idr_prior_auth_table}
                WHERE pa_req_rec_dt > {{MIN_TS}}
            )
            SELECT {{COLUMNS}} FROM distinct_prior_auths {prior_auth}
            WHERE row_order = 1;
            """
