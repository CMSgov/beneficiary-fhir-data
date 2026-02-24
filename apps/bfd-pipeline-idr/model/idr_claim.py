from collections.abc import Sequence
from datetime import date, datetime
from typing import Annotated

from pydantic import BeforeValidator

from constants import (
    ALL_CLAIM_PARTITIONS,
    CLAIM_TABLE,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    ALIAS,
    ALIAS_CLM,
    ALIAS_DCMTN,
    BATCH_ID,
    BATCH_TIMESTAMP,
    COLUMN_MAP,
    HISTORICAL_BATCH_TIMESTAMP,
    LAST_UPDATED_TIMESTAMP,
    PRIMARY_KEY,
    UPDATE_TIMESTAMP,
    IdrBaseModel,
    claim_filter,
    transform_default_and_zero_string,
    transform_default_string,
    transform_null_date_to_max,
    transform_null_date_to_min,
    transform_null_float,
    transform_null_int,
)


class IdrClaim(IdrBaseModel):
    clm_uniq_id: Annotated[int, {PRIMARY_KEY: True, BATCH_ID: True, LAST_UPDATED_TIMESTAMP: True}]
    geo_bene_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_dt_sgntr_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_type_cd: Annotated[int, {ALIAS: ALIAS_CLM}]
    clm_num_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    bene_sk: int
    clm_cntl_num: str
    clm_orig_cntl_num: Annotated[str, BeforeValidator(transform_default_string)]
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
    clm_alowd_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_bene_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_cntrctr_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_pd_dt: Annotated[date, BeforeValidator(transform_null_date_to_max)]
    clm_pmt_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_ltst_clm_ind: str
    clm_atndg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_atndg_prvdr_last_name: Annotated[str, BeforeValidator(transform_default_string)]
    clm_oprtg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_oprtg_prvdr_last_name: Annotated[str, BeforeValidator(transform_default_string)]
    clm_othr_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_othr_prvdr_last_name: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rndrg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_rndrg_prvdr_last_name: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_blg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_rfrg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_srvc_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_atndg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_othr_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_rndrng_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_oprtg_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_ric_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_disp_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_sbmt_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_blood_pt_frnsh_qty: Annotated[int, BeforeValidator(transform_null_int)]
    clm_nch_prmry_pyr_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_blg_prvdr_oscar_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_nrln_ric_cd: Annotated[str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)]
    clm_idr_ld_dt: Annotated[date, {HISTORICAL_BATCH_TIMESTAMP: True}]
    clm_srvc_prvdr_gnrc_id_num: Annotated[str, BeforeValidator(transform_default_string)]
    prvdr_prscrbng_prvdr_npi_num: Annotated[str, BeforeValidator(transform_default_and_zero_string)]
    clm_adjstmt_type_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bene_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_blg_prvdr_zip5_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_sbmt_frmt_cd: Annotated[str, BeforeValidator(transform_default_string)]
    clm_sbmtr_cntrct_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_sbmtr_cntrct_pbp_num: Annotated[str, BeforeValidator(transform_default_string)]
    clm_bnft_enhncmt_1_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_2_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_3_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_4_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_bnft_enhncmt_5_cd: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_ngaco_pbpmt_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_ngaco_cptatn_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_ngaco_pdschrg_hcbs_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_ngaco_snf_wvr_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_ngaco_tlhlth_sw: Annotated[
        str, {ALIAS: ALIAS_DCMTN}, BeforeValidator(transform_default_string)
    ]
    clm_blood_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_tot_cntrctl_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_bene_intrst_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_bene_pmt_coinsrnc_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_cob_ptnt_resp_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_prvdr_otaf_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_othr_tp_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_prvdr_rmng_due_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_blood_ncvrd_chrg_amt: Annotated[float, BeforeValidator(transform_null_float)]
    clm_prvdr_intrst_pd_amt: Annotated[float, BeforeValidator(transform_null_float)]
    meta_src_sk: Annotated[int, {ALIAS: ALIAS_CLM}]
    idr_insrt_ts_clm: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_CLM, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_clm: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_CLM, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_insrt_ts_dcmtn: Annotated[
        datetime,
        {BATCH_TIMESTAMP: True, ALIAS: ALIAS_DCMTN, COLUMN_MAP: "idr_insrt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]
    idr_updt_ts_dcmtn: Annotated[
        datetime,
        {UPDATE_TIMESTAMP: True, ALIAS: ALIAS_DCMTN, COLUMN_MAP: "idr_updt_ts"},
        BeforeValidator(transform_null_date_to_min),
    ]

    @staticmethod
    def table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_table() -> str:
        return CLAIM_TABLE

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return ["bfd_claim_updated_ts"]

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
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
            {{WHERE_CLAUSE}} AND {claim_filter(start_time, partition)}
            {{ORDER_BY}}
        """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return ALL_CLAIM_PARTITIONS
