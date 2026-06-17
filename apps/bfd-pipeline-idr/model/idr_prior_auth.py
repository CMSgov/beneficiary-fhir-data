from datetime import date, datetime
from typing import Annotated, override

from dateutil.relativedelta import relativedelta
from pydantic import BeforeValidator

from constants import (
    IDR_PRIOR_AUTH_TABLE,
)
from load_partition import LoadPartition
from model.base_model import (
    BATCH_ID,
    PRIMARY_KEY_ORDER,
    IdrBaseModel,
    ModelType,
    Source,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
)


class IdrPriorAuth(IdrBaseModel):
    mbi_num: Annotated[str, {PRIMARY_KEY_ORDER: 0, BATCH_ID: True}]
    utn: Annotated[str, {PRIMARY_KEY_ORDER: 1}]
    current_segment: Annotated[int, {PRIMARY_KEY_ORDER: 2}]
    utn_valid_st_dt: Annotated[date, BeforeValidator(transform_null_date_to_min)]
    utn_valid_en_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_type: Annotated[str, BeforeValidator(transform_default_string)]
    hcpcs_or_cpt_or_hipps: str
    mac_id: str
    pa_dt_added: date
    pa_dt_updated: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    service_cnts: int
    pa_decision: str
    pa_req_sub_dt: date
    pa_req_rec_dt: date
    pa_decision_dt: Annotated[date, BeforeValidator(transform_null_date_to_min)]
    pa_decision_exp_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    order_refer_npi: Annotated[str, BeforeValidator(transform_default_string)]
    render_npi: Annotated[str, BeforeValidator(transform_default_string)]
    svc_render_st: str
    place_of_serv: Annotated[str, BeforeValidator(transform_default_string)]
    price_mod1: Annotated[str, BeforeValidator(transform_default_string)]
    price_mod2: Annotated[str, BeforeValidator(transform_default_string)]
    icn_dcn: Annotated[str, BeforeValidator(transform_default_string)]
    cms_cert: Annotated[str, BeforeValidator(transform_default_string)]
    npi: Annotated[str, BeforeValidator(transform_default_string)]
    name: str
    rev_code_1: Annotated[str, BeforeValidator(transform_default_string)]
    tob: Annotated[str, BeforeValidator(transform_default_string)]
    mr_count_ind: int
    mr_count_st_dt: Annotated[date, BeforeValidator(transform_null_date_to_min)]
    mr_count_end_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    att_phy_npi: Annotated[str, BeforeValidator(transform_default_string)]
    rrb_excl_ind: Annotated[str, BeforeValidator(transform_default_string)]

    @override
    @staticmethod
    def table() -> str:
        return "idr.prior_auth"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []  # TBD once IDR adds ts

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.PRIOR_AUTH

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        # Prior auth data older than 2 years should be filtered
        prior_auth_cutoff_date = start_time - relativedelta(years=2)
        start_time_sql = prior_auth_cutoff_date.strftime("'%Y-%m-%d %H:%M:%S'")

        return f"""
            WITH distinct_prior_auths AS (
                SELECT *, ROW_NUMBER() 
                    OVER (PARTITION BY mbi_num, utn, current_segment ORDER BY mbi_num) as row_order
                FROM {IDR_PRIOR_AUTH_TABLE}
                WHERE pa_req_rec_dt > {start_time_sql}
            ) SELECT {{COLUMNS}} FROM distinct_prior_auths;
            """
