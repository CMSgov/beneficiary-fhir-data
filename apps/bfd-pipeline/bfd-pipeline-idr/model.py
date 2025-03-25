from datetime import date, datetime
from pydantic import BaseModel


class IdrBeneficiary(BaseModel):
    bene_sk: int
    bene_xref_efctv_sk: int
    bene_mbi_id: str
    bene_ssn_num: str
    bene_1st_name: str
    bene_midl_name: str
    bene_last_name: str
    bene_brth_dt: date
    bene_sex_cd: str
    bene_race_cd: str
    geo_usps_state_cd: str
    geo_zip5_cd: str
    geo_zip4_cd: str
    geo_zip_plc_name: str
    bene_line_1_adr: str
    bene_line_2_adr: str
    bene_line_3_adr: str
    bene_line_4_adr: str
    bene_line_5_adr: str
    bene_line_6_adr: str
    cntct_lang_cd: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime


class IdrBeneficiaryHistory(BaseModel):
    bene_sk: int
    bene_xref_efctv_sk: int
    bene_mbi_id: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime


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
