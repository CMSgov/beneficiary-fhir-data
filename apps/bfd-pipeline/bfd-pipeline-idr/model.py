import os
from collections.abc import Iterable
from datetime import UTC, date, datetime
from typing import Annotated, TypeVar

from pydantic import BaseModel, BeforeValidator

from constants import CLAIM_TYPE_CODES, DEFAULT_MAX_DATE, DEFAULT_MIN_DATE


def transform_null_date_to_max(value: date | None) -> date:
    if value is None:
        return date.fromisoformat(DEFAULT_MAX_DATE)
    return value


def transform_null_date_to_min(value: date | None) -> date:
    if value is None:
        return date.fromisoformat(DEFAULT_MIN_DATE)
    return value


def transform_null_string(value: str | None) -> str:
    if value is None:
        return ""
    return value


def transform_default_string(value: str | None) -> str:
    if value is None or value == "~":
        return ""
    return value


def transform_empty_string(value: str | None) -> str:
    if value is None:
        return ""
    return value.strip()


def transform_null_float(value: float | None) -> float:
    if value is None:
        return 0.0
    return value


def transform_null_int(value: int | None) -> float:
    if value is None:
        return 0
    return value


PRIMARY_KEY = "primary_key"
BATCH_TIMESTAMP = "batch_timestamp"
HISTORICAL_BATCH_TIMESTAMP = "historical_batch_timestamp"
UPDATE_TIMESTAMP = "update_timestamp"
ALIAS = "alias"
INSERT_EXCLUDE = "insert_exclude"
DERIVED = "derived"


class IdrBaseModel(BaseModel):
    @staticmethod
    def table() -> str: ...

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str: ...

    @classmethod
    def _historical_fetch_query(cls, start_time: datetime) -> str:
        return cls._current_fetch_query(start_time)

    @classmethod
    def fetch_query(cls, is_historical: bool, start_time: datetime) -> str:
        if is_historical:
            return cls._historical_fetch_query(start_time)
        return cls._current_fetch_query(start_time)

    @staticmethod
    def computed_keys() -> list[str]:
        return []

    @classmethod
    def unique_key(cls) -> list[str]:
        return [key for key in cls.model_fields if cls._extract_meta(key, PRIMARY_KEY)]

    @classmethod
    def batch_timestamp_col(cls, is_historical: bool) -> str | None:
        if is_historical:
            historical_col = cls._get_timestamp_col(HISTORICAL_BATCH_TIMESTAMP)
            if historical_col:
                return historical_col
        return cls._get_timestamp_col(BATCH_TIMESTAMP)

    @classmethod
    def batch_timestamp_col_alias(cls, is_historical: bool) -> str | None:
        col = cls.batch_timestamp_col(is_historical)
        if col:
            return cls._format_column_alias(col)
        return None

    @classmethod
    def update_timestamp_col(cls) -> str | None:
        return cls._get_timestamp_col(UPDATE_TIMESTAMP)

    @classmethod
    def update_timestamp_col_alias(cls) -> str | None:
        col = cls.update_timestamp_col()
        if col:
            return cls._format_column_alias(col)
        return None

    @classmethod
    def _get_timestamp_col(cls, meta_key: str) -> str | None:
        keys = [key for key in cls.model_fields if cls._extract_meta(key, meta_key)]

        if len(keys) > 1:
            raise ValueError(f"More than one {meta_key} key supplied for {cls.table()}")
        if len(keys) == 0:
            return None

        return keys[0]

    @classmethod
    def _extract_meta(cls, key: str, meta_key: str) -> object | None:
        metadata = cls.model_fields[key].metadata

        if len(metadata) > 0 and isinstance(metadata[0], Iterable) and meta_key in metadata[0]:
            return metadata[0][meta_key]  # type: ignore
        return None

    @classmethod
    def _format_column_alias(cls, key: str) -> str:
        metadata = cls.model_fields[key].metadata
        alias = cls._extract_meta(key, ALIAS)
        if alias is not None:
            return f"{metadata[0][ALIAS]}.{key}"
        return key

    @classmethod
    def column_aliases(cls) -> list[str]:
        return [cls._format_column_alias(key) for key in cls.columns_raw() if key]

    @classmethod
    def columns_raw(cls) -> list[str]:
        return [key for key in cls.model_fields if not cls._extract_meta(key, DERIVED)]

    @classmethod
    def insert_keys(cls) -> list[str]:
        return [key for key in cls.model_fields if not cls._extract_meta(key, INSERT_EXCLUDE)]


T = TypeVar("T", bound=IdrBaseModel)


class IdrBeneficiary(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_xref_efctv_sk: int
    bene_mbi_id: str
    bene_1st_name: str
    bene_midl_name: Annotated[str, BeforeValidator(transform_null_string)]
    bene_last_name: str
    bene_brth_dt: date
    bene_death_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
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
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary"

    @staticmethod
    def computed_keys() -> list[str]:
        return ["bene_xref_efctv_sk_computed"]

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrBeneficiaryMbiId(IdrBaseModel):
    bene_mbi_id: Annotated[str, {PRIMARY_KEY: True}]
    bene_mbi_efctv_dt: date
    bene_mbi_obslt_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_mbi_id"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mbi_id
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrBeneficiaryThirdParty(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_buyin_cd: str
    bene_tp_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_third_party"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_tp
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrBeneficiaryStatus(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_mdcr_stus_cd: str
    mdcr_stus_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    mdcr_stus_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: datetime
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_status"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_stus
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrBeneficiaryEntitlement(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_stus_cd: str
    bene_mdcr_enrlmt_rsn_cd: str
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_entitlement"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrBeneficiaryEntitlementReason(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_rng_bgn_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_rng_end_dt: Annotated[date, {PRIMARY_KEY: True}]
    bene_mdcr_entlmt_rsn_cd: str
    idr_ltst_trans_flg: Annotated[str, BeforeValidator(transform_null_string)]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: Annotated[datetime, {PRIMARY_KEY: True}]
    idr_trans_obslt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_entitlement_reason"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_mdcr_entlmt_rsn
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrBeneficiaryXref(IdrBaseModel):
    bene_hicn_num: Annotated[str, {PRIMARY_KEY: True}]
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_xref_sk: int
    bene_kill_cred_cd: Annotated[str, BeforeValidator(transform_default_string)]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime
    idr_insrt_ts: datetime
    idr_updt_ts: Annotated[
        datetime, {UPDATE_TIMESTAMP: True}, BeforeValidator(transform_null_date_to_min)
    ]
    src_rec_crte_ts: Annotated[datetime, {PRIMARY_KEY: True, BATCH_TIMESTAMP: True}]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_xref"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref
            {WHERE_CLAUSE}
            {ORDER_BY}
        """


class IdrElectionPeriodUsage(IdrBaseModel):
    bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY: True}]
    bene_cntrct_num: str
    bene_pbp_num: str
    bene_elctn_enrlmt_disenrlmt_cd: str
    bene_elctn_aplctn_dt: date
    bene_enrlmt_efctv_dt: Annotated[date, {PRIMARY_KEY: True}]
    idr_insrt_ts: Annotated[datetime, {BATCH_TIMESTAMP: True}]
    idr_trans_efctv_ts: datetime
    idr_trans_obslt_ts: datetime

    @staticmethod
    def table() -> str:
        return "idr.election_period_usage"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        # equivalent to "select distinct on", but Snowflake has different syntax for that,
        # so it's unfortunately not portable
        return """
            WITH dupes as (
                SELECT {COLUMNS}, ROW_NUMBER() OVER (
                    PARTITION BY bene_sk, cntrct_pbp_sk, bene_enrlmt_efctv_dt 
                {ORDER_BY} DESC) as row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_elctn_prd_usg
                {WHERE_CLAUSE}
                {ORDER_BY}
            )
            SELECT {COLUMNS} FROM dupes WHERE row_order = 1
            """


class IdrContractPbpNumber(IdrBaseModel):
    cntrct_pbp_sk: Annotated[int, {PRIMARY_KEY: True}]
    cntrct_drug_plan_ind_cd: str
    cntrct_pbp_type_cd: str

    @staticmethod
    def table() -> str:
        return "idr.contract_pbp_number"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return f"""
        SELECT {{COLUMNS}}
        FROM cms_vdm_view_mdcr_prd.v2_mdcr_cntrct_pbp_num
        WHERE cntrct_pbp_sk_obslt_dt >= '{DEFAULT_MAX_DATE}'
        """


ALIAS_CLM = "clm"
ALIAS_DCMTN = "dcmtn"
ALIAS_SGNTR = "sgntr"
ALIAS_LINE = "line"
ALIAS_INSTNL = "instnl"
ALIAS_VAL = "val"


def claim_type_clause(start_time: datetime) -> str:  # noqa: ARG001
    latest_claims_env = "IDR_LATEST_CLAIMS"
    if latest_claims_env in os.environ and os.environ[latest_claims_env] in ("1", "true"):
        return f"""
            {ALIAS_CLM}.clm_type_cd IN (61,62,63,64)
            AND {ALIAS_CLM}.clm_src_id = '20000' 
            AND {ALIAS_CLM}.clm_finl_actn_ind = 'Y'
            AND {ALIAS_CLM}.clm_ltst_clm_ind = 'Y'
            """
    return f"""
        {ALIAS_CLM}.clm_type_cd IN ({",".join([str(c) for c in CLAIM_TYPE_CODES])})
    """


class IdrClaim(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True}]
    geo_bene_sk: Annotated[int, {ALIAS: ALIAS_CLM}, BeforeValidator(transform_null_int)]
    clm_dt_sgntr_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_type_cd: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_num_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    bene_sk: int
    clm_cntl_num: str
    clm_orig_cntl_num: Annotated[str, BeforeValidator(transform_null_string)]
    clm_from_dt: date
    clm_thru_dt: date
    clm_efctv_dt: date
    clm_obslt_dt: date
    clm_bill_clsfctn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bill_fac_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bill_freq_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_finl_actn_ind: str
    clm_src_id: Annotated[str, {ALIAS: ALIAS_CLM}]
    clm_query_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_coinsrnc_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_blood_lblty_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_ncvrd_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ddctbl_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_prvdr_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_alowd_chrg_amt: float
    clm_bene_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_cntrctr_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pd_dt: date
    clm_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_ltst_clm_ind: str
    clm_atndg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_atndg_prvdr_last_name: Annotated[str, BeforeValidator(transform_null_string)]
    clm_oprtg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_oprtg_prvdr_last_name: Annotated[str, BeforeValidator(transform_null_string)]
    clm_othr_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_othr_prvdr_last_name: Annotated[str, BeforeValidator(transform_null_string)]
    clm_rndrg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rndrg_prvdr_last_name: Annotated[str, BeforeValidator(transform_null_string)]
    prvdr_blg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_rfrg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_ric_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_disp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_sbmt_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_blood_pt_frnsh_qty: Annotated[int, BeforeValidator(transform_null_int)]
    clm_nch_prmry_pyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_blg_prvdr_oscar_num: Annotated[str, BeforeValidator(transform_null_string)]
    clm_nrln_ric_cd: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_null_string)]
    clm_idr_ld_dt: Annotated[date, {HISTORICAL_BATCH_TIMESTAMP: True}]
    clm_ric_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_srvc_prvdr_gnrc_id_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_prscrbng_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_CLM},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_CLM},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        dcmtn = ALIAS_DCMTN
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            LEFT JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dcmtn {dcmtn} ON
                {clm}.geo_bene_sk = {dcmtn}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {dcmtn}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {dcmtn}.clm_type_cd AND
                {clm}.clm_num_sk = {dcmtn}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
            {{ORDER_BY}}
        """


class IdrClaimDateSignature(IdrBaseModel):
    clm_dt_sgntr_sk: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_SGNTR}]
    clm_cms_proc_dt: date
    clm_actv_care_from_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_dschrg_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_submsn_dt: date
    clm_ncvrd_from_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_ncvrd_thru_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_actv_care_thru_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_mdcr_exhstd_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_nch_wkly_proc_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_qlfy_stay_from_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_qlfy_stay_thru_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_SGNTR},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_SGNTR},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_date_signature"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        sgntr = ALIAS_SGNTR
        return f"""
            WITH dupes as (
                SELECT {{COLUMNS}}, ROW_NUMBER() OVER (
                    PARTITION BY {sgntr}.clm_dt_sgntr_sk 
                    {{ORDER_BY}} DESC) 
                AS row_order
                FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
                JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_dt_sgntr {sgntr}
                ON {clm}.clm_dt_sgntr_sk = {sgntr}.clm_dt_sgntr_sk
                {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
                {{ORDER_BY}}
            )
            SELECT {{COLUMNS_NO_ALIAS}} FROM dupes WHERE row_order = 1
        """


class IdrClaimInstitutional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True}]
    clm_admsn_type_cd: str
    bene_ptnt_stus_cd: Annotated[str, BeforeValidator(transform_default_string)]
    dgns_drg_cd: int
    clm_mdcr_instnl_mco_pd_sw: str
    clm_admsn_src_cd: str
    clm_fi_actn_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_ip_lrd_use_cnt: int
    clm_hipps_uncompd_care_amt: float
    clm_hha_rfrl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_hha_lup_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_hha_tot_visit_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_mdcr_coins_day_cnt: int
    clm_instnl_ncvrd_day_cnt: float
    clm_instnl_per_diem_amt: float
    clm_mdcr_instnl_bene_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_hospc_prd_cnt: Annotated[int, BeforeValidator(transform_null_int)]
    clm_mdcr_npmt_rsn_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_mdcr_ip_pps_drg_wt_num: float
    clm_mdcr_ip_pps_dsprprtnt_amt: float
    clm_mdcr_ip_pps_excptn_amt: float
    clm_mdcr_ip_pps_cptl_fsp_amt: float
    clm_mdcr_ip_pps_cptl_ime_amt: float
    clm_mdcr_ip_pps_outlier_amt: float
    clm_mdcr_ip_pps_cptl_hrmls_amt: float
    clm_pps_ind_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_mdcr_ip_pps_cptl_tot_amt: float
    clm_instnl_cvrd_day_cnt: float
    clm_mdcr_instnl_prmry_pyr_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_prfnl_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_ip_bene_ddctbl_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_instnl_drg_outlier_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_INSTNL},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_INSTNL},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_institutional"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        instnl = ALIAS_INSTNL
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_instnl {instnl} ON
                {clm}.geo_bene_sk = {instnl}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {instnl}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {instnl}.clm_type_cd AND
                {clm}.clm_num_sk = {instnl}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
            {{ORDER_BY}}
        """


class IdrClaimValue(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True}]
    clm_val_sqnc_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_val_cd: str
    clm_val_amt: float
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_VAL},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_VAL},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_value"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        val = ALIAS_VAL
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_val {val} ON
                {clm}.geo_bene_sk = {val}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {val}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {val}.clm_type_cd AND
                {clm}.clm_num_sk = {val}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
            {{ORDER_BY}}
        """


class IdrClaimLine(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_LINE}]
    clm_line_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_line_ansthsa_unit_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_sbmt_chrg_amt: float
    clm_line_alowd_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_ncvrd_chrg_amt: float
    clm_line_prvdr_pmt_amt: float
    clm_line_bene_pmt_amt: float
    clm_line_bene_pd_amt: float
    clm_line_cvrd_pd_amt: float
    clm_line_dgns_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_blood_ddctbl_amt: float
    clm_line_mdcr_ddctbl_amt: float
    clm_line_hcpcs_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_ndc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_mdcr_coinsrnc_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_ndc_qty: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_ndc_qty_qlfyr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_srvc_unit_qty: float
    clm_line_rev_ctr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_rx_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pos_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rndrg_prvdr_prtcptg_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rndrg_prvdr_tax_num: Annotated[str, BeforeValidator(transform_default_string)]
    hcpcs_1_mdfr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    hcpcs_2_mdfr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    hcpcs_3_mdfr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    hcpcs_4_mdfr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    hcpcs_5_mdfr_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_line_from_dt: date
    clm_line_thru_dt: date
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_line"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        line = ALIAS_LINE
        # Note: joining on clm_uniq_id isn't strictly necessary,
        # but it did seem to improve performance a bit
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk AND
                {clm}.clm_from_dt = {line}.clm_from_dt AND
                {clm}.clm_uniq_id = {line}.clm_uniq_id
            {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
            {{ORDER_BY}}
        """


def transform_default_hipps_code(value: str | None) -> str:
    if value is None or value == "00000":
        return ""
    return value


class IdrClaimLineInstitutional(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True}]
    clm_line_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_rev_apc_hipps_cd: Annotated[str, BeforeValidator(transform_default_hipps_code)]
    clm_otaf_one_ind_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_rev_dscnt_ind_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_rev_packg_ind_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_rev_cntr_stus_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_rev_pmt_mthd_cd: Annotated[str, BeforeValidator(transform_null_string)]
    clm_ansi_sgntr_sk: Annotated[int, BeforeValidator(transform_null_int)]
    clm_ddctbl_coinsrnc_cd: str
    clm_line_instnl_rate_amt: float
    clm_line_instnl_adjstd_amt: float
    clm_line_instnl_rdcd_amt: float
    clm_line_instnl_msp1_pd_amt: float
    clm_line_instnl_msp2_pd_amt: float
    clm_line_instnl_rev_ctr_dt: date
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_line_institutional"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        line = ALIAS_LINE
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_instnl {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
            {{ORDER_BY}}
        """


class IdrClaimAnsiSignature(IdrBaseModel):
    clm_ansi_sgntr_sk: Annotated[int, {PRIMARY_KEY: True}]
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
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return """
            SELECT {COLUMNS_NO_ALIAS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_ansi_sgntr
            {WHERE_CLAUSE}
        """

    @classmethod
    def _historical_fetch_query(cls, start_time: datetime) -> str:  # noqa: ARG003
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_ansi_sgntr
        """


class IdrClaimProcedure(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True}]
    clm_val_sqnc_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_prod_type_cd: Annotated[str, {PRIMARY_KEY: True}]
    clm_prcdr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_dgns_prcdr_icd_ind: Annotated[str, BeforeValidator(transform_empty_string)]
    clm_dgns_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_poa_ind: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcdr_prfrm_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_idr_ld_dt: Annotated[date, {INSERT_EXCLUDE: True, HISTORICAL_BATCH_TIMESTAMP: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_LINE},
        BeforeValidator(transform_null_date_to_min),
    ]
    bfd_row_num: Annotated[int, {DERIVED: True}]

    @staticmethod
    def table() -> str:
        return "idr.claim_procedure"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:
        clm = ALIAS_CLM
        line = ALIAS_LINE
        return f"""
            SELECT {{COLUMNS}}, ROW_NUMBER() OVER (
                PARTITION BY clm_uniq_id ORDER BY clm_uniq_id
            ) AS bfd_row_num
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_prod {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_type_clause(start_time)}
            {{ORDER_BY}}
        """

class IdrClaimProfessional(IdrBaseModel):
    geo_bene_sk: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_CLM}]
    clm_type_cd: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_CLM}]
    clm_num_sk: Annotated[int, {PRIMARY_KEY: True, ALIAS: ALIAS_CLM}]
    clm_carr_pmt_dnl_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_clncl_tril_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_mdcr_prfnl_prmry_pyr_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mdcr_prfnl_prvdr_asgnmt_sw: Annotated[str, BeforeValidator(transform_default_string)]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_CLM},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_CLM},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_professional"

    @staticmethod
    def _current_fetch_query() -> str:
        clm = ALIAS_CLM
        line = ALIAS_LINE
        return f"""
            SELECT {{COLUMNS}}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm {clm}
            JOIN cms_vdm_view_mdcr_prd.v2_mdcr_clm_prfnl {line} ON
                {clm}.geo_bene_sk = {line}.geo_bene_sk AND
                {clm}.clm_type_cd = {line}.clm_type_cd AND
                {clm}.clm_dt_sgntr_sk = {line}.clm_dt_sgntr_sk AND
                {clm}.clm_num_sk = {line}.clm_num_sk
            {{WHERE_CLAUSE}} AND {claim_type_clause()}
            {{ORDER_BY}}
        """

class IdrClaimLineProfessional(IdrBaseModel):
    clm_bene_prmry_pyr_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_fed_type_srvc_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_carr_clncl_lab_num: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_carr_hpsa_scrcty_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_dmerc_scrn_svgs_amt: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_hct_hgb_rslt_num: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_hct_hgb_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_line_prfnl_dme_price_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_line_prfnl_mtus_cnt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_mtus_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_physn_astnt_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pmt_80_100_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcng_lclty_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prcsg_ind_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prmry_pyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_prvdr_spclty_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_srvc_ddctbl_sw: Annotated[str, BeforeValidator(transform_default_string)]
    clm_suplr_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    geo_bene_sk: Annotated[int, {PRIMARY_KEY: True}]
    clm_line_num: Annotated[int, {PRIMARY_KEY: True}]
    clm_num_sk: Annotated[int, {PRIMARY_KEY: True}]
    clm_type_cd: Annotated[int, {PRIMARY_KEY: True}]
    idr_insrt_ts: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return "idr.claim_line_professional"

    @staticmethod
    def _current_fetch_query() -> str:
        return """
            SELECT {COLUMNS}
            FROM cms_vdm_view_mdcr_prd.v2_mdcr_clm_line_prfnl
            {WHERE_CLAUSE}
        """

class LoadProgress(IdrBaseModel):
    table_name: str
    last_ts: datetime
    batch_start_ts: datetime
    batch_complete_ts: datetime

    @staticmethod
    def query_placeholder() -> str:
        return "table_name"

    @staticmethod
    def table() -> str:
        return "idr.load_progress"

    @staticmethod
    def _current_fetch_query(start_time: datetime) -> str:  # noqa: ARG004
        return f"""
        SELECT table_name, last_ts, batch_start_ts, batch_complete_ts 
        FROM idr.load_progress
        WHERE table_name = %({LoadProgress.query_placeholder()})s
        """

    def is_historical(self) -> bool:
        # 2021-4-18 is the most recent date where idr_insrt_ts could be null in claims data
        return self.last_ts <= datetime(2021, 4, 19, tzinfo=UTC)
