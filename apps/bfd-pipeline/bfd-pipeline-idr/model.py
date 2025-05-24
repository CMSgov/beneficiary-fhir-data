from datetime import date, datetime
from typing import Annotated, ClassVar, Iterable, Literal, TypeVar
from pydantic import BaseModel, BeforeValidator, Field


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


class IdrBaseModel(BaseModel):
    @classmethod
    def _format_column_alias(cls, key: str) -> str:
        metadata = cls.model_fields[key].metadata

        if (
            len(metadata) > 0
            and isinstance(metadata[0], Iterable)
            and "alias" in metadata[0]
        ):
            return f"{metadata[0]['alias']}.{key}"
        else:
            return key

    @classmethod
    def column_aliases(cls) -> list[str]:
        return [cls._format_column_alias(key) for key in cls.model_fields.keys()]


T = TypeVar("T", bound=IdrBaseModel)


class IdrBeneficiary(IdrBaseModel):
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


class IdrBeneficiaryHistory(IdrBaseModel):
    bene_sk: int
    bene_xref_efctv_sk: int
    bene_mbi_id: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryMbi(IdrBaseModel):
    bene_mbi_id: str
    bene_mbi_efctv_dt: date
    bene_mbi_obslt_dt: Annotated[date, BeforeValidator(transform_null_date)]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryThirdParty(IdrBaseModel):
    bene_sk: int
    bene_buyin_cd: str
    bene_tp_type_cd: str
    bene_rng_bgn_dt: date
    bene_rng_end_dt: date
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryStatus(IdrBaseModel):
    bene_sk: int
    bene_mdcr_stus_cd: str
    mdcr_stus_bgn_dt: date
    mdcr_stus_end_dt: date
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryEntitlement(IdrBaseModel):
    bene_sk: int
    bene_rng_bgn_dt: date
    bene_rng_end_dt: date
    bene_mdcr_entlmt_type_cd: str
    bene_mdcr_entlmt_stus_cd: str
    bene_mdcr_enrlmt_rsn_cd: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrBeneficiaryEntitlementReason(IdrBaseModel):
    bene_sk: int
    bene_rng_bgn_dt: date
    bene_rng_end_dt: date
    bene_mdcr_entlmt_rsn_cd: str
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[datetime, BeforeValidator(transform_null_date)]


class IdrElectionPeriodUsage(IdrBaseModel):
    bene_sk: int
    cntrct_pbp_sk: int
    bene_cntrct_num: str
    bene_pbp_num: str
    bene_elctn_enrlmt_disenrlmt_cd: str
    bene_elctn_aplctn_dt: date
    bene_enrlmt_efctv_dt: date
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime


class IdrContractPbpNumber(IdrBaseModel):
    cntrct_pbp_sk: int
    cntrct_drug_plan_ind_cd: str
    cntrct_pbp_type_cd: str


ALIAS_CLM = "clm"
ALIAS_DCMTN = "dcmtn"


class IdrClaim(IdrBaseModel):
    clm_uniq_id: int
    geo_bene_sk: Annotated[int, {"alias": ALIAS_CLM}]
    clm_dt_sgntr_sk: Annotated[int, {"alias": ALIAS_CLM}]
    clm_type_cd: Annotated[int, {"alias": ALIAS_CLM}]
    clm_num_sk: Annotated[int, {"alias": ALIAS_CLM}]
    bene_sk: int
    clm_cntl_num: str
    clm_orig_cntl_num: str
    clm_from_dt: date
    clm_thru_dt: date
    clm_efctv_dt: date
    clm_finl_actn_ind: str
    clm_src_id: str
    clm_query_cd: str
    clm_mdcr_coinsrnc_amt: int
    clm_blood_lblty_amt: int
    clm_ncvrd_chrg_amt: int
    clm_mdcr_ddctbl_amt: int
    clm_cntrctr_num: str
    clm_pmt_amt: int
    clm_ltst_clm_ind: str
    clm_atndg_prvdr_npi_num: str
    clm_oprtg_prvdr_npi_num: str
    clm_othr_prvdr_npi_num: str
    clm_rndrg_prvdr_npi_num: str
    prvdr_blg_prvdr_npi_num: str
    clm_disp_cd: str
    clm_sbmt_chrg_amt: int
    clm_blood_pt_frnsh_qty: int
    clm_nch_prmry_pyr_cd: str
    clm_blg_prvdr_oscar_num: str
    clm_mdcr_coinsrnc_amt: int
    clm_idr_ld_dt: date
    clm_nrln_ric_cd: Annotated[str, {"alias": ALIAS_DCMTN}]


class IdrClaimDateSignature(IdrBaseModel):
    clm_dt_sgntr_sk: int
    clm_cms_proc_dt: date
    clm_actv_care_from_dt: date
    clm_dschrg_dt: date
    clm_submsn_dt: date
    clm_ncvrd_from_dt: date
    clm_ncvrd_thru_dt: date
    clm_actv_care_thru_dt: date
    clm_mdcr_exhstd_dt: date


class IdrClaimInstitutional(IdrBaseModel):
    clm_uniq_id: int
    clm_admsn_type_cd: str
    bene_ptnt_stus_cd: str
    dgns_drg_cd: int
    clm_mdcr_instnl_mco_pd_sw: str
    clm_admsn_src_cd: str
    clm_bill_fac_type_cd: str
    clm_bill_clsfctn_cd: str
    clm_bill_freq_cd: str
    clm_fi_actn_cd: str
    clm_mdcr_ip_lrd_use_cnt: int
    clm_hipps_uncompd_care_amt: int
    clm_instnl_mdcr_coins_day_cnt: int
    clm_instnl_ncvrd_day_cnt: int
    clm_instnl_per_diem_amt: int
    clm_mdcr_npmt_rsn_cd: str
    clm_mdcr_ip_pps_drg_wt_num: int
    clm_mdcr_ip_pps_dsprprtnt_amt: int
    clm_mdcr_ip_pps_excptn_amt: int
    clm_mdcr_ip_pps_cptl_fsp_amt: int
    clm_mdcr_ip_pps_cptl_ime_amt: int
    clm_mdcr_ip_pps_outlier_amt: int
    clm_mdcr_ip_pps_cptl_hrmls_amt: int
    clm_pps_ind_cd: str
    clm_mdcr_ip_pps_cptl_tot_amt: int
    clm_instnl_cvrd_day_cnt: int
    clm_mdcr_instnl_prmry_pyr_amt: int
    clm_instnl_prfnl_amt: int
    clm_mdcr_ip_bene_ddctbl_amt: int
    clm_instnl_drg_outlier_amt: int


class LoadProgress(IdrBaseModel):
    table_name: str
    last_ts: datetime
    batch_completion_ts: datetime
