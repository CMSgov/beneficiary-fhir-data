from datetime import date, datetime
from typing import Annotated
from pydantic import BaseModel, BeforeValidator


def transform_null_date(value: date | None) -> date:
    if value is None:
        return date.fromisoformat("9999-12-31")
    else:
        return value


def transform_null_string(value: str | None) -> str:
    if value is None:
        return ""
    return value


def transform_default_string(value: str | None) -> str:
    if value is None or value == "~":
        return ""
    return value


class IdrBeneficiary(BaseModel):
    bene_sk: int
    bene_xref_efctv_sk: int
    bene_mbi_id: str
    bene_1st_name: str
    bene_midl_name: Annotated[str, BeforeValidator(transform_null_string)]
    bene_last_name: str
    bene_brth_dt: date
    bene_death_dt: Annotated[date, BeforeValidator(transform_null_date)]
    bene_vrfy_death_day_sw: Annotated[str, BeforeValidator(transform_default_string)]
    bene_sex_cd: str
    bene_race_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_usps_state_cd: str
    geo_zip5_cd: str
    geo_zip_plc_name: str
    bene_line_1_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_2_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_3_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_4_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_5_adr: Annotated[str, BeforeValidator(transform_null_string)]
    bene_line_6_adr: Annotated[str, BeforeValidator(transform_null_string)]
    cntct_lang_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryHistory(BaseModel):
    bene_sk: int
    bene_xref_efctv_sk: int
    bene_mbi_id: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryMbi(BaseModel):
    bene_mbi_id: str
    bene_mbi_efctv_dt: date
    bene_mbi_obslt_dt: Annotated[date, BeforeValidator(transform_null_date)]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrElectionPeriodUsage(BaseModel):
    bene_sk: int
    cntrct_pbp_sk: int
    bene_cntrct_num: str
    bene_pbp_num: str
    bene_elctn_enrlmt_disenrlmt_cd: str
    bene_elctn_aplctn_dt: date
    bene_enrlmt_efctv_dt: date
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime


class IdrContractPbpNumber(BaseModel):
    cntrct_pbp_sk: int
    cntrct_drug_plan_ind_cd: str
    cntrct_pbp_type_cd: str


class LoadProgress(BaseModel):
    table_name: str
    last_id: str
    last_timestamp: datetime
