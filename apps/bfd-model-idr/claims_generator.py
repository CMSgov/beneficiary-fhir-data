import csv
import random
import sys
from collections import OrderedDict, defaultdict
from enum import StrEnum, auto
from pathlib import Path
from typing import Any

import click
import tqdm

import field_constants as f
from claims_adj import AdjudicatedGeneratorUtil
from claims_other import OtherGeneratorUtil
from claims_pac import PacGeneratorUtil
from claims_static import INSTITUTIONAL_CLAIM_TYPES, PHARMACY_CLM_TYPE_CDS, PROFESSIONAL_CLAIM_TYPES
from claims_util import four_part_key, match_line_num
from generator_util import (
    BENE_HSTRY,
    CLM,
    CLM_ANSI_SGNTR,
    CLM_DCMTN,
    CLM_DT_SGNTR,
    CLM_FISS,
    CLM_INSTNL,
    CLM_LCTN_HSTRY,
    CLM_LINE,
    CLM_LINE_DCMTN,
    CLM_LINE_INSTNL,
    CLM_LINE_PRFNL,
    CLM_LINE_RX,
    CLM_PRFNL,
    CLM_PROD,
    CLM_RLT_COND_SGNTR_MBR,
    CLM_VAL,
    CNTRCT_PBP_CNTCT,
    CNTRCT_PBP_NUM,
    PRVDR_HSTRY,
    GeneratorUtil,
    RowAdapter,
    adapters_to_dicts,
    as_list,
    load_file_dict,
    partition_rows,
    probability,
    run_command,
)

_INT_TO_STRING_COLS = [
    f.BENE_SK,
    f.CLM_TYPE_CD,
    f.CLM_NUM_SK,
    f.PRVDR_PRSCRBNG_PRVDR_NPI_NUM,
    f.PRVDR_RFRG_PRVDR_NPI_NUM,
    f.PRVDR_BLG_PRVDR_NPI_NUM,
    f.CLM_ATNDG_PRVDR_NPI_NUM,
    f.CLM_OPRTG_PRVDR_NPI_NUM,
    f.CLM_OTHR_PRVDR_NPI_NUM,
    f.CLM_RNDRG_PRVDR_NPI_NUM,
    f.CLM_BLG_PRVDR_NPI_NUM,
    f.CLM_RFRG_PRVDR_PIN_NUM,
    f.PRVDR_ATNDG_PRVDR_NPI_NUM,
    f.PRVDR_SRVC_PRVDR_NPI_NUM,
    f.PRVDR_OTHR_PRVDR_NPI_NUM,
    f.PRVDR_RNDRNG_PRVDR_NPI_NUM,
    f.PRVDR_OPRTG_PRVDR_NPI_NUM,
]
"""Columns you want as string without decimal/nan"""


class GeneratePacDataMode(StrEnum):
    NO = auto()
    IF_NONE = auto()
    ALWAYS = auto()


class BeneSkMode(StrEnum):
    BENE_HSTRY = auto()
    CLM = auto()
    BOTH = auto()


class _ClaimsFile(StrEnum):
    CLM = (
        CLM,
        [
            f.CLM_DT_SGNTR_SK,
            f.CLM_UNIQ_ID,
            f.CLM_RLT_COND_SGNTR_SK,
            f.CLM_TYPE_CD,
            f.CLM_SRC_ID,
            f.META_SRC_SK,
            f.CLM_FROM_DT,
            f.CLM_THRU_DT,
            f.CLM_CNTL_NUM,
            f.CLM_NUM_SK,
            f.CLM_EFCTV_DT,
            f.CLM_IDR_LD_DT,
            f.CLM_OBSLT_DT,
            f.GEO_BENE_SK,
            f.BENE_SK,
            f.CLM_DISP_CD,
            f.CLM_QUERY_CD,
            f.CLM_ADJSTMT_TYPE_CD,
            f.CLM_BILL_FAC_TYPE_CD,
            f.CLM_BILL_CLSFCTN_CD,
            f.CLM_BILL_FREQ_CD,
            f.CLM_CNTRCTR_NUM,
            f.CLM_NCH_PRMRY_PYR_CD,
            f.CLM_FINL_ACTN_IND,
            f.CLM_LTST_CLM_IND,
            f.PRVDR_BLG_PRVDR_NPI_NUM,
            f.CLM_ATNDG_PRVDR_NPI_NUM,
            f.PRVDR_ATNDG_PRVDR_NPI_NUM,
            f.CLM_OPRTG_PRVDR_NPI_NUM,
            f.PRVDR_OPRTG_PRVDR_NPI_NUM,
            f.CLM_OTHR_PRVDR_NPI_NUM,
            f.PRVDR_OTHR_PRVDR_NPI_NUM,
            f.CLM_RNDRG_PRVDR_NPI_NUM,
            f.PRVDR_RNDRNG_PRVDR_NPI_NUM,
            f.CLM_BLG_PRVDR_OSCAR_NUM,
            f.CLM_MDCR_COINSRNC_AMT,
            f.CLM_BLG_PRVDR_ZIP5_CD,
            f.CLM_SBMT_CHRG_AMT,
            f.CLM_PMT_AMT,
            f.CLM_MDCR_DDCTBL_AMT,
            f.CLM_NCVRD_CHRG_AMT,
            f.CLM_BLOOD_LBLTY_AMT,
            f.CLM_BENE_PMT_COINSRNC_AMT,
            f.CLM_BLOOD_CHRG_AMT,
            f.CLM_BLOOD_NCVRD_CHRG_AMT,
            f.CLM_COB_PTNT_RESP_AMT,
            f.CLM_PRVDR_INTRST_PD_AMT,
            f.CLM_PRVDR_OTAF_AMT,
            f.CLM_PRVDR_RMNG_DUE_AMT,
            f.CLM_TOT_CNTRCTL_AMT,
            f.CLM_BLOOD_PT_FRNSH_QTY,
            f.PRVDR_RFRG_PRVDR_NPI_NUM,
            f.CLM_PRVDR_PMT_AMT,
            f.CLM_RIC_CD,
            f.CLM_BLG_PRVDR_NPI_NUM,
            f.CLM_RFRG_PRVDR_PIN_NUM,
            f.CLM_ALOWD_CHRG_AMT,
            f.CLM_BENE_PD_AMT,
            f.CLM_BENE_PMT_AMT,
            f.CLM_BENE_INTRST_PD_AMT,
            f.CLM_ORIG_CNTL_NUM,
            f.CLM_SRVC_PRVDR_GNRC_ID_NUM,
            f.PRVDR_SRVC_PRVDR_NPI_NUM,
            f.CLM_PD_DT,
            f.PRVDR_PRSCRBNG_PRVDR_NPI_NUM,
            f.CLM_SBMT_FRMT_CD,
            f.CLM_SBMTR_CNTRCT_NUM,
            f.CLM_SBMTR_CNTRCT_PBP_NUM,
            f.CLM_OTHR_TP_PD_AMT,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_ANSI_SGNTR = (
        CLM_ANSI_SGNTR,
        [
            f.CLM_ANSI_SGNTR_SK,
            f.CLM_1_REV_CNTR_ANSI_RSN_CD,
            f.CLM_2_REV_CNTR_ANSI_RSN_CD,
            f.CLM_3_REV_CNTR_ANSI_RSN_CD,
            f.CLM_4_REV_CNTR_ANSI_RSN_CD,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_DCMTN = (
        CLM_DCMTN,
        [
            f.CLM_DT_SGNTR_SK,
            f.CLM_NUM_SK,
            f.GEO_BENE_SK,
            f.CLM_TYPE_CD,
            f.CLM_NRLN_RIC_CD,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_DT_SGNTR = (
        CLM_DT_SGNTR,
        [
            f.CLM_DT_SGNTR_SK,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
            f.CLM_ACTV_CARE_FROM_DT,
            f.CLM_DSCHRG_DT,
            f.CLM_MDCR_EXHSTD_DT,
            f.CLM_NCVRD_FROM_DT,
            f.CLM_NCVRD_THRU_DT,
            f.CLM_SUBMSN_DT,
            f.CLM_CMS_PROC_DT,
            f.CLM_NCH_WKLY_PROC_DT,
            f.CLM_ACTV_CARE_THRU_DT,
            f.CLM_QLFY_STAY_FROM_DT,
            f.CLM_QLFY_STAY_THRU_DT,
        ],
    )
    CLM_FISS = (
        CLM_FISS,
        [
            f.CLM_DT_SGNTR_SK,
            f.GEO_BENE_SK,
            f.CLM_NUM_SK,
            f.CLM_TYPE_CD,
            f.CLM_CRNT_STUS_CD,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_INSTNL = (
        CLM_INSTNL,
        [
            f.GEO_BENE_SK,
            f.CLM_DT_SGNTR_SK,
            f.CLM_TYPE_CD,
            f.CLM_NUM_SK,
            f.CLM_FI_ACTN_CD,
            f.CLM_ADMSN_TYPE_CD,
            f.BENE_PTNT_STUS_CD,
            f.CLM_MDCR_INSTNL_MCO_PD_SW,
            f.CLM_ADMSN_SRC_CD,
            f.DGNS_DRG_CD,
            f.DGNS_DRG_OUTLIER_CD,
            f.CLM_INSTNL_CVRD_DAY_CNT,
            f.CLM_MDCR_IP_LRD_USE_CNT,
            f.CLM_INSTNL_PER_DIEM_AMT,
            f.CLM_HIPPS_UNCOMPD_CARE_AMT,
            f.CLM_MDCR_INSTNL_PRMRY_PYR_AMT,
            f.CLM_INSTNL_DRG_OUTLIER_AMT,
            f.CLM_MDCR_IP_PPS_DSPRPRTNT_AMT,
            f.CLM_INSTNL_MDCR_COINS_DAY_CNT,
            f.CLM_INSTNL_NCVRD_DAY_CNT,
            f.CLM_MDCR_IP_PPS_DRG_WT_NUM,
            f.CLM_MDCR_IP_PPS_EXCPTN_AMT,
            f.CLM_MDCR_IP_PPS_CPTL_FSP_AMT,
            f.CLM_MDCR_IP_PPS_CPTL_IME_AMT,
            f.CLM_MDCR_IP_PPS_OUTLIER_AMT,
            f.CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT,
            f.CLM_MDCR_IP_PPS_CPTL_TOT_AMT,
            f.CLM_MDCR_IP_BENE_DDCTBL_AMT,
            f.CLM_PPS_IND_CD,
            f.CLM_MDCR_HOSPC_PRD_CNT,
            f.CLM_MDCR_NPMT_RSN_CD,
            f.CLM_OP_SRVC_TYPE_CD,
            f.CLM_INSTNL_PRFNL_AMT,
            f.CLM_MDCR_INSTNL_BENE_PD_AMT,
            f.CLM_FINL_STDZD_PYMT_AMT,
            f.CLM_HHA_RFRL_CD,
            f.CLM_MDCR_HHA_TOT_VISIT_CNT,
            f.CLM_HIPPS_READMSN_RDCTN_AMT,
            f.CLM_HIPPS_VBP_AMT,
            f.CLM_INSTNL_LOW_VOL_PMT_AMT,
            f.CLM_MDCR_IP_1ST_YR_RATE_AMT,
            f.CLM_MDCR_IP_SCND_YR_RATE_AMT,
            f.CLM_PPS_MD_WVR_STDZD_VAL_AMT,
            f.CLM_HHA_LUP_IND_CD,
            f.CLM_HAC_RDCTN_PYMT_AMT,
            f.CLM_HIPPS_MODEL_BNDLD_PMT_AMT,
            f.CLM_SITE_NTRL_CST_BSD_PYMT_AMT,
            f.CLM_SITE_NTRL_IP_PPS_PYMT_AMT,
            f.CLM_SS_OUTLIER_STD_PYMT_AMT,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_LCTN_HSTRY = (
        CLM_LCTN_HSTRY,
        [
            f.CLM_DT_SGNTR_SK,
            f.GEO_BENE_SK,
            f.CLM_NUM_SK,
            f.CLM_TYPE_CD,
            f.CLM_LCTN_CD_SQNC_NUM,
            f.CLM_AUDT_TRL_STUS_CD,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_LINE_DCMTN = (
        CLM_LINE_DCMTN,
        [
            f.GEO_BENE_SK,
            f.CLM_DT_SGNTR_SK,
            f.CLM_TYPE_CD,
            f.CLM_NUM_SK,
            f.CLM_LINE_PA_UNIQ_TRKNG_NUM,
            f.CLM_LINE_NUM,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_LINE_INSTNL = (
        CLM_LINE_INSTNL,
        [
            f.GEO_BENE_SK,
            f.CLM_DT_SGNTR_SK,
            f.CLM_TYPE_CD,
            f.CLM_NUM_SK,
            f.CLM_LINE_NUM,
            f.CLM_LINE_INSTNL_ADJSTD_AMT,
            f.CLM_LINE_INSTNL_RDCD_AMT,
            f.CLM_DDCTBL_COINSRNC_CD,
            f.CLM_LINE_INSTNL_RATE_AMT,
            f.CLM_LINE_INSTNL_MSP1_PD_AMT,
            f.CLM_LINE_INSTNL_MSP2_PD_AMT,
            f.CLM_LINE_INSTNL_REV_CTR_DT,
            f.CLM_REV_DSCNT_IND_CD,
            f.CLM_OTAF_ONE_IND_CD,
            f.CLM_REV_PACKG_IND_CD,
            f.CLM_REV_PMT_MTHD_CD,
            f.CLM_REV_CNTR_STUS_CD,
            f.CLM_ANSI_SGNTR_SK,
            f.CLM_LINE_ADD_ON_PYMT_AMT,
            f.CLM_LINE_NON_EHR_RDCTN_AMT,
            f.CLM_REV_CNTR_TDAPA_AMT,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_LINE_PRFNL = (
        CLM_LINE_PRFNL,
        [
            f.GEO_BENE_SK,
            f.CLM_DT_SGNTR_SK,
            f.CLM_TYPE_CD,
            f.CLM_NUM_SK,
            f.CLM_LINE_NUM,
            f.CLM_BENE_PRMRY_PYR_PD_AMT,
            f.CLM_SRVC_DDCTBL_SW,
            f.CLM_PRCSG_IND_CD,
            f.CLM_PMT_80_100_CD,
            f.CLM_MTUS_IND_CD,
            f.CLM_LINE_PRFNL_MTUS_CNT,
            f.CLM_PHYSN_ASTNT_CD,
            f.CLM_LINE_CARR_CLNCL_CHRG_AMT,
            f.CLM_LINE_CARR_PSYCH_OT_LMT_AMT,
            f.CLM_LINE_PRFNL_INTRST_AMT,
            f.CLM_MDCR_PRMRY_PYR_ALOWD_AMT,
            f.CLM_LINE_HCT_HGB_TYPE_CD,
            f.CLM_LINE_CARR_CLNCL_LAB_NUM,
            f.CLM_LINE_DMERC_SCRN_SVGS_AMT,
            f.CLM_SUPLR_TYPE_CD,
            f.CLM_LINE_PRFNL_DME_PRICE_AMT,
            f.CLM_LINE_HCT_HGB_RSLT_NUM,  # TODO: not generated yet
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_LINE_RX = (
        CLM_LINE_RX,
        [
            f.CLM_UNIQ_ID,
            f.CLM_DT_SGNTR_SK,
            f.CLM_NUM_SK,
            f.CLM_TYPE_CD,
            f.GEO_BENE_SK,
            f.CLM_LINE_NUM,
            f.CLM_FROM_DT,
            f.CLM_DSPNSNG_STUS_CD,
            f.CLM_LINE_RX_ORGN_CD,
            f.CLM_BRND_GNRC_CD,
            f.CLM_PTNT_RSDNC_CD,
            f.CLM_PHRMCY_SRVC_TYPE_CD,
            f.CLM_LINE_AUTHRZD_FILL_NUM,
            f.CLM_LTC_DSPNSNG_MTHD_CD,
            f.CLM_CMPND_CD,
            f.CLM_LINE_DAYS_SUPLY_QTY,
            f.CLM_LINE_RX_FILL_NUM,
            f.CLM_DAW_PROD_SLCTN_CD,
            f.CLM_DRUG_CVRG_STUS_CD,
            f.CLM_CTSTRPHC_CVRG_IND_CD,
            f.CLM_LINE_GRS_ABOVE_THRSHLD_AMT,
            f.CLM_LINE_GRS_BLW_THRSHLD_AMT,
            f.CLM_LINE_LIS_AMT,
            f.CLM_LINE_TROOP_TOT_AMT,
            f.CLM_LINE_PLRO_AMT,
            f.CLM_RPTD_MFTR_DSCNT_AMT,
            f.CLM_LINE_INGRDNT_CST_AMT,
            f.CLM_LINE_VCCN_ADMIN_FEE_AMT,
            f.CLM_LINE_SRVC_CST_AMT,
            f.CLM_LINE_SLS_TAX_AMT,
            f.CLM_PRCNG_EXCPTN_CD,
            f.CLM_CMS_CALCD_MFTR_DSCNT_AMT,
            f.CLM_LINE_REBT_PASSTHRU_POS_AMT,
            f.CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT,
            f.CLM_LINE_RPTD_GAP_DSCNT_AMT,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_LINE = (
        CLM_LINE,
        [
            f.CLM_UNIQ_ID,
            f.GEO_BENE_SK,
            f.CLM_DT_SGNTR_SK,
            f.CLM_TYPE_CD,
            f.CLM_NUM_SK,
            f.CLM_LINE_NUM,
            f.CLM_FROM_DT,
            f.CLM_LINE_FROM_DT,
            f.CLM_LINE_THRU_DT,
            f.CLM_LINE_HCPCS_CD,
            f.HCPCS_1_MDFR_CD,
            f.HCPCS_2_MDFR_CD,
            f.HCPCS_3_MDFR_CD,
            f.HCPCS_4_MDFR_CD,
            f.HCPCS_5_MDFR_CD,
            f.CLM_LINE_ANSTHSA_UNIT_CNT,  # TODO: generate this
            f.CLM_LINE_SRVC_UNIT_QTY,
            f.CLM_LINE_REV_CTR_CD,
            f.CLM_LINE_BENE_PMT_AMT,
            f.CLM_LINE_BENE_PD_AMT,
            f.CLM_LINE_ALOWD_CHRG_AMT,
            f.CLM_LINE_SBMT_CHRG_AMT,
            f.CLM_LINE_CVRD_PD_AMT,
            f.CLM_LINE_BLOOD_DDCTBL_AMT,
            f.CLM_LINE_MDCR_DDCTBL_AMT,
            f.CLM_LINE_PRVDR_PMT_AMT,
            f.CLM_LINE_NCVRD_CHRG_AMT,
            f.CLM_LINE_FINL_ACTN_IND,
            f.CLM_LINE_LTST_CLM_IND,
            f.CLM_LINE_OTAF_AMT,
            f.CLM_LINE_NDC_CD,
            f.CLM_LINE_NDC_QTY,
            f.CLM_LINE_NDC_QTY_QLFYR_CD,
            f.CLM_RNDRG_PRVDR_TYPE_CD,
            f.CLM_LINE_MDCR_COINSRNC_AMT,
            f.CLM_LINE_DGNS_CD,
            f.CLM_POS_CD,
            f.CLM_RNDRG_PRVDR_PRTCPTG_CD,
            f.CLM_RNDRG_PRVDR_TAX_NUM,
            f.CLM_RNDRG_PRVDR_PIN_NUM,
            f.PRVDR_RNDRNG_PRVDR_NPI_NUM,
            f.CLM_RNDRG_PRVDR_NPI_NUM,
            f.CLM_LINE_PMD_UNIQ_TRKNG_NUM,
            f.CLM_LINE_NCVRD_PD_AMT,
            f.CLM_LINE_RX_NUM,
            f.CLM_LINE_GRS_CVRD_CST_TOT_AMT,
            f.CLM_LINE_OTHR_TP_PD_AMT,
            f.CLM_RNDRNG_PRVDR_NPI_NUM,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_PRFNL = (
        CLM_PRFNL,
        [
            f.CLM_DT_SGNTR_SK,
            f.CLM_NUM_SK,
            f.GEO_BENE_SK,
            f.CLM_TYPE_CD,
            f.CLM_CARR_PMT_DNL_CD,
            f.CLM_MDCR_PRFNL_PRMRY_PYR_AMT,
            f.CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW,
            f.CLM_CLNCL_TRIL_NUM,
            f.CLM_PRVDR_ACNT_RCVBL_OFST_AMT,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_PROD = (
        CLM_PROD,
        [
            f.CLM_PROD_TYPE_CD,
            f.CLM_PRCDR_CD,
            f.CLM_DGNS_PRCDR_ICD_IND,
            f.CLM_PRCDR_PRFRM_DT,
            f.CLM_VAL_SQNC_NUM,
            f.CLM_DT_SGNTR_SK,
            f.CLM_NUM_SK,
            f.GEO_BENE_SK,
            f.CLM_TYPE_CD,
            f.CLM_DGNS_CD,
            f.CLM_POA_IND,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CLM_RLT_COND_SGNTR_MBR = (
        CLM_RLT_COND_SGNTR_MBR,
        [
            f.CLM_RLT_COND_SGNTR_SK,
            f.CLM_RLT_COND_SGNTR_SQNC_NUM,
            f.CLM_RLT_COND_CD,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
            # HACK: See generation function for justification. This is not a real field of this
            # table
            f.CLM_UNIQ_ID,
        ],
    )
    CLM_VAL = (
        CLM_VAL,
        [
            f.CLM_DT_SGNTR_SK,
            f.CLM_NUM_SK,
            f.GEO_BENE_SK,
            f.CLM_TYPE_CD,
            f.CLM_VAL_CD,
            f.CLM_VAL_AMT,
            f.CLM_VAL_SQNC_NUM,
            f.IDR_INSRT_TS,
            f.IDR_UPDT_TS,
        ],
    )
    CNTRCT_PBP_CNTCT = (
        CNTRCT_PBP_CNTCT,
        [
            f.CNTRCT_PBP_SK,
            f.CNTRCT_PLAN_CNTCT_OBSLT_DT,
            f.CNTRCT_PLAN_CNTCT_TYPE_CD,
            f.CNTRCT_PLAN_FREE_EXTNSN_NUM,
            f.CNTRCT_PLAN_CNTCT_FREE_NUM,
            f.CNTRCT_PLAN_CNTCT_EXTNSN_NUM,
            f.CNTRCT_PLAN_CNTCT_TEL_NUM,
            f.CNTRCT_PBP_END_DT,
            f.CNTRCT_PBP_BGN_DT,
            f.CNTRCT_PLAN_CNTCT_ST_1_ADR,
            f.CNTRCT_PLAN_CNTCT_ST_2_ADR,
            f.CNTRCT_PLAN_CNTCT_CITY_NAME,
            f.CNTRCT_PLAN_CNTCT_STATE_CD,
            f.CNTRCT_PLAN_CNTCT_ZIP_CD,
        ],
    )
    CNTRCT_PBP_NUM = (
        CNTRCT_PBP_NUM,
        [
            f.CNTRCT_PBP_SK,
            f.CNTRCT_NUM,
            f.CNTRCT_PBP_NUM,
            f.CNTRCT_PBP_NAME,
            f.CNTRCT_PBP_TYPE_CD,
            f.CNTRCT_DRUG_PLAN_IND_CD,
            f.CNTRCT_PBP_SK_EFCTV_DT,
            f.CNTRCT_PBP_END_DT,
            f.CNTRCT_PBP_SK_OBSLT_DT,
        ],
    )
    PRVDR_HSTRY = (
        PRVDR_HSTRY,
        [
            f.PRVDR_SK,
            f.PRVDR_HSTRY_EFCTV_DT,
            f.PRVDR_HSTRY_OBSLT_DT,
            f.PRVDR_1ST_NAME,
            f.PRVDR_MDL_NAME,
            f.PRVDR_LAST_NAME,
            f.PRVDR_NAME,
            f.PRVDR_LGL_NAME,
            f.PRVDR_NPI_NUM,
            f.PRVDR_EMPLR_ID_NUM,
            f.PRVDR_OSCAR_NUM,
            f.PRVDR_TXNMY_CMPST_CD,
            f.PRVDR_TYPE_CD,
            f.META_SK,
            f.META_LST_UPDT_SK,
        ],
    )

    def __init__(
        self,
        value: str,
        ordered_headers: list[str],
    ) -> None:
        self.ordered_headers = ordered_headers

        self.out_path = Path(f"out/{value}.csv")

    def __new__(
        cls: type["_ClaimsFile"],
        value: str,
        ordered_headers: list[str],
    ) -> "_ClaimsFile":
        obj = str.__new__(cls, value)
        obj._value_ = value
        obj.ordered_headers = ordered_headers
        return obj


def _save_claims_data(files: dict[_ClaimsFile, list[RowAdapter]]):
    Path("out").mkdir(exist_ok=True)

    print("Exporting finished synthetic claims to ./out...")
    with tqdm.tqdm(files.items()) as t:
        for claims_file, data in t:
            t.set_postfix(file=str(claims_file.out_path))  # type: ignore
            _write_claims_file(claims_file=claims_file, data=data)
    print("Finished exporting generated claims")


def _write_claims_file(
    claims_file: _ClaimsFile,
    data: list[RowAdapter],
):
    cleaned_data = _clean_int_columns(adapters_to_dicts(data), _INT_TO_STRING_COLS)

    with Path(claims_file.out_path).open("w") as csv_file:
        writer = csv.DictWriter(
            csv_file, fieldnames=claims_file.ordered_headers, restval="", quoting=csv.QUOTE_MINIMAL
        )
        writer.writeheader()
        writer.writerows(cleaned_data)


def _clean_int_columns(rows: list[dict[str, Any]], cols: list[str]):
    for col in cols:
        for row in rows:
            if col in row:
                row[col] = str(row[col])
    return rows


@click.command
@click.option(
    "--sushi/--no-sushi",
    envvar="SUSHI",
    default=False,
    show_default=True,
    help=(
        "Generate new StructureDefinitions. Use when testing locally if new .fsh files have been "
        "added."
    ),
)
@click.option(
    "--min-claims",
    envvar="MIN_CLAIMS",
    type=int,
    default=5,
    show_default=True,
    help="Minimum number of claims to generate per person",
)
@click.option(
    "--max-claims",
    envvar="MAX_CLAIMS",
    type=int,
    default=5,
    show_default=True,
    help="Maximum number of claims to generate per person",
)
@click.option(
    "--enable-samhsa/--disable-samhsa",
    envvar="ENABLE_SAMHSA",
    type=bool,
    default=True,
    show_default=True,
    help="Enables generation of SAMHSA-related data",
)
@click.option(
    "--pac-gen",
    envvar="PAC_GEN",
    type=click.Choice(GeneratePacDataMode, case_sensitive=False),
    default=GeneratePacDataMode.IF_NONE,
    show_default=True,
    help=(
        "Generate new partially-adjudicated claims data based on choice. 'no' will never generate "
        "pac data, 'if_none' will generate if the input claims data has no pac CLMs, and "
        "'always' will force the generation always"
    ),
)
@click.option(
    "--bene-sk-mode",
    envvar="BENE_SK_MODE",
    type=click.Choice(BeneSkMode, case_sensitive=False),
    default=BeneSkMode.BOTH,
    show_default=True,
    help=(
        "Sets the mode for which input files from which distinct BENE_SKs are read. 'bene_hstry' "
        f"indicates that BENE_SKs are only loaded from {BENE_HSTRY}, 'clm' indicates loading from "
        f"only from {CLM}. 'both' indicates loading from both"
    ),
)
@click.argument("paths", nargs=-1, type=click.Path(exists=True))
def generate(
    sushi: bool,
    min_claims: int,
    max_claims: int,
    enable_samhsa: bool,
    pac_gen: GeneratePacDataMode,
    bene_sk_mode: BeneSkMode,
    paths: tuple[Path, ...],
):
    """Generate synthetic claims data. Provided file PATHS will be updated with new fields."""
    if min_claims > max_claims:
        print(
            f"error: min claims value of {min_claims} is greater than "
            f"max claims value of {max_claims}"
        )
        sys.exit(1)

    gen_utils = GeneratorUtil()

    if sushi:
        print("Running sushi build")
        _, stderr = run_command(["sushi", "build"], cwd="./sushi")
        if stderr:
            print("SUSHI errors:")
            print(stderr)

    files: dict[str, list[RowAdapter]] = {
        BENE_HSTRY: [],
        CLM: [],
        CLM_LINE: [],
        CLM_LINE_DCMTN: [],
        CLM_VAL: [],
        CLM_DT_SGNTR: [],
        CLM_PROD: [],
        CLM_INSTNL: [],
        CLM_LINE_INSTNL: [],
        CLM_DCMTN: [],
        CLM_LCTN_HSTRY: [],
        CLM_FISS: [],
        CLM_PRFNL: [],
        CLM_LINE_PRFNL: [],
        CLM_LINE_RX: [],
        CLM_RLT_COND_SGNTR_MBR: [],
        PRVDR_HSTRY: [],
        CNTRCT_PBP_NUM: [],
        CNTRCT_PBP_CNTCT: [],
    }
    load_file_dict(files=files, paths=list(paths))

    out_tables: dict[str, list[RowAdapter]] = {k: [] for k in files}

    if not files[BENE_HSTRY] and not files[CLM]:
        print(f"{BENE_HSTRY} and/or {CLM} must be provided for claims data generation to proceed")
        sys.exit(1)

    other_util = OtherGeneratorUtil()
    cntrct_pbp_num, cntrct_pbp_cntct = other_util.gen_contract_plan(
        amount=10,
        init_contract_pbp_nums=files[CNTRCT_PBP_NUM],
        init_contract_pbp_contacts=files[CNTRCT_PBP_CNTCT],
    )
    out_tables[CNTRCT_PBP_NUM].extend(cntrct_pbp_num)
    out_tables[CNTRCT_PBP_CNTCT].extend(cntrct_pbp_cntct)

    out_tables[PRVDR_HSTRY].extend(
        other_util.gen_provider_history(amount=14, init_provider_historys=files[PRVDR_HSTRY])
    )

    # This table is special in that its data is mostly static and read from a static file, so we
    # don't need to do anything fancy with it
    out_tables[CLM_ANSI_SGNTR] = other_util.gen_synthetic_clm_ansi_sgntr()

    # An operator could provide a BENE_HSTRY with new beneficiaries that have no corresponding CLMs,
    # so we need to resolve the unique union of BENE_SKs from both files here. Below we will check
    # whether a given BENE_SK has CLMs rows already and either regenerate them or generate new ones
    # correspondingly. Additionally, we need to preserve the order of the bene_sks from the source
    # files, else there will be drift in the order of generated rows
    clm_bene_sks = (
        [int(row[f.BENE_SK]) for row in files[CLM]]
        if bene_sk_mode == BeneSkMode.CLM or bene_sk_mode == BeneSkMode.BOTH
        else []
    )
    bene_hstry_bene_sks = (
        [int(row[f.BENE_SK]) for row in files[BENE_HSTRY]]
        if bene_sk_mode == BeneSkMode.BENE_HSTRY or bene_sk_mode == BeneSkMode.BOTH
        else []
    )
    all_bene_sks = clm_bene_sks + bene_hstry_bene_sks  # We take the order of CLM first
    ordered_bene_sks = list(OrderedDict.fromkeys(x for x in all_bene_sks))

    # Regenerating existing data implies that we need a way to uniquely address a single row/set of
    # rows for each claims table per-CLM. We could do this via list comprehensions/scanning each
    # table during generation, but that would take _far_ too long. Instead, the block below sets up
    # precomputed lookup tables for each claims table so that per-CLM regeneration of claims tables
    # does not take an exceedingly long time versus generation of entirely new data.
    clms_per_bene_sk = partition_rows(llist=files[CLM], part_by=lambda x: int(x[f.BENE_SK]))
    # HACK: See generation function for justification. CLM_UNIQ_ID is not a field of this table
    sgntr_mbr_per_clm_uniq_id = {
        str(row[f.CLM_UNIQ_ID]): row
        for row in files[CLM_RLT_COND_SGNTR_MBR]
        if row.get(f.CLM_UNIQ_ID)
    }
    rx_clm_line_per_clm_uniq_id = {
        str(x[f.CLM_UNIQ_ID]): x for x in files[CLM_LINE] if x.get(f.CLM_LINE_RX_NUM)
    }
    norm_clm_lines_per_clm_uniq_id = partition_rows(
        llist=files[CLM_LINE],
        part_by=lambda x: str(x[f.CLM_UNIQ_ID]),
        filter_by=lambda x: not x.get(f.CLM_LINE_RX_NUM),
    )
    clm_line_rx_per_clm_uniq_id = {str(row[f.CLM_UNIQ_ID]): row for row in files[CLM_LINE_RX]}
    clm_dcmtns_per_fpk = partition_rows(
        llist=files[CLM_DCMTN],
        part_by=lambda x: four_part_key(x),
    )
    dsprtnt_clm_val_per_fpk = {
        four_part_key(x): x for x in files[CLM_VAL] if int(x[f.CLM_VAL_CD]) == 18
    }
    ime_clm_val_per_fpk = {
        four_part_key(x): x for x in files[CLM_VAL] if int(x[f.CLM_VAL_CD]) == 19
    }
    proc_clm_prod_per_fpk = partition_rows(
        llist=files[CLM_PROD],
        part_by=lambda x: four_part_key(x),
        filter_by=lambda x: x[f.CLM_PROD_TYPE_CD] == "S",
    )
    diag_clm_prod_per_fpk = partition_rows(
        llist=files[CLM_PROD],
        part_by=lambda x: four_part_key(x),
        filter_by=lambda x: x[f.CLM_PROD_TYPE_CD] != "S",
    )
    clm_dt_sgntr_per_sk = {str(row[f.CLM_DT_SGNTR_SK]): row for row in files[CLM_DT_SGNTR]}
    clm_instnl_per_fpk = {four_part_key(row): row for row in files[CLM_INSTNL]}
    clm_prfnls_per_fpk = partition_rows(
        llist=files[CLM_PRFNL],
        part_by=lambda x: four_part_key(x),
    )
    clm_line_instnls_per_fpk = partition_rows(
        llist=files[CLM_LINE_INSTNL], part_by=lambda x: four_part_key(x)
    )
    clm_line_prfnls_per_fpk = partition_rows(
        llist=files[CLM_LINE_PRFNL], part_by=lambda x: four_part_key(x)
    )
    clm_fiss_per_fpk = {four_part_key(row): row for row in files[CLM_FISS]}
    clm_lctn_hstry_per_fpk = {four_part_key(row): row for row in files[CLM_LCTN_HSTRY]}
    clm_line_dcmtns_per_clk = partition_rows(
        llist=files[CLM_LINE_DCMTN], part_by=lambda x: four_part_key(x)
    )

    # pac CLM generation is random per-generation iteration, so the number of pac CLMs rows is
    # unstable regardless of whether we provide existing CLMs or not. We need a switch to ensure pac
    # claims data is not generated if it already exists, only regenerated, so this checks if there's
    # any pac CLMs in the provided FILEs or if the --force-pac-claims flag is disable and disables
    # _new_ pac claim data generation if so
    any_pac_clms = any(int(x[f.CLM_TYPE_CD]) >= 1011 for x in files[CLM])
    gen_new_pac_clms = pac_gen == GeneratePacDataMode.ALWAYS or (
        not any_pac_clms and pac_gen == GeneratePacDataMode.IF_NONE
    )

    print("Generating synthetic claims data for provided BENE_SKs...")
    adj_util = AdjudicatedGeneratorUtil(enable_samhsa=enable_samhsa)
    pac_util = PacGeneratorUtil()
    for pt_bene_sk in tqdm.tqdm(ordered_bene_sks):
        existing_clms = clms_per_bene_sk.get(pt_bene_sk, [])
        existing_adj_clms = [x for x in existing_clms if int(x[f.CLM_TYPE_CD]) < 1011]
        existing_pac_clms = [x for x in existing_clms if int(x[f.CLM_TYPE_CD]) >= 1011]
        init_adj_clms = (
            existing_adj_clms
            if existing_adj_clms or existing_pac_clms
            else [RowAdapter({}) for _ in range(random.randint(min_claims, max_claims))]
        )
        # This is dumb, but fresh pac claims are derived/generated from adjudicated claims generated
        # here, so we need a way to provide those claims to the pac-generation logic in the next
        # loop
        all_adj_clms_tbls: list[dict[str, list[RowAdapter]]] = []
        for init_adj_clm in init_adj_clms:
            # Store this iteration's generated claims-related tables (list[RowAdapter]) for use in
            # pac generation
            adj_clms_tbls: dict[str, list[RowAdapter]] = defaultdict(list)

            clm_from_dt_min = "2018-01-01"
            clm = adj_util.gen_clm(
                gen_utils=gen_utils,
                bene_sk=str(pt_bene_sk),
                init_clm=init_adj_clm,
                min_date=clm_from_dt_min,
            )
            adj_clms_tbls[CLM].append(clm)

            clm_rlt_cond_sgntr_mbr = adj_util.gen_clm_rlt_cond_sgntr_mbr(
                clm=clm,
                init_clm_rlt_cond_sgntr_mbr=sgntr_mbr_per_clm_uniq_id.get(clm[f.CLM_UNIQ_ID]),
            )
            adj_clms_tbls[CLM_RLT_COND_SGNTR_MBR].append(clm_rlt_cond_sgntr_mbr)

            clm_type_cd = int(clm[f.CLM_TYPE_CD])
            if clm_type_cd in PHARMACY_CLM_TYPE_CDS:
                pharm_clm_line = adj_util.gen_pharm_clm_line(
                    clm=clm,
                    init_clm_line=rx_clm_line_per_clm_uniq_id.get(clm[f.CLM_UNIQ_ID]),
                )
                pharm_clm_line_rx = adj_util.gen_pharm_clm_line_rx(
                    gen_utils=gen_utils,
                    clm=clm,
                    init_clm_line_rx=clm_line_rx_per_clm_uniq_id.get(clm[f.CLM_UNIQ_ID]),
                )
                adj_clms_tbls[CLM_LINE].append(pharm_clm_line)
                adj_clms_tbls[CLM_LINE_RX].append(pharm_clm_line_rx)

            clm_dcmtns = [
                adj_util.gen_clm_dcmtn(clm=clm, init_clm_dcmtn=x)
                for x in clm_dcmtns_per_fpk.get(four_part_key(clm), [RowAdapter({})])
            ]
            adj_clms_tbls[CLM_DCMTN].extend(clm_dcmtns)

            if clm_type_cd in (20, 40, 60, 61, 62, 63, 64):
                dsprtnt_clm_val = adj_util.gen_dsprtnt_clm_val(
                    clm=clm,
                    init_clm_val=dsprtnt_clm_val_per_fpk.get(four_part_key(clm)),
                )
                adj_clms_tbls[CLM_VAL].append(dsprtnt_clm_val)

                ime_clm_val = adj_util.gen_ime_clm_val(
                    clm=clm,
                    init_clm_val=ime_clm_val_per_fpk.get(four_part_key(clm)),
                )
                adj_clms_tbls[CLM_VAL].append(ime_clm_val)

            if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
                init_procs = proc_clm_prod_per_fpk.get(four_part_key(clm)) or [
                    RowAdapter({}) for _ in range(random.randint(1, 5))
                ]
                for proc_idx, proc in enumerate(init_procs):
                    _proc = adj_util.gen_proc_clm_prod(
                        clm=clm, clm_val_sqnc_num=proc_idx, init_clm_prod=proc
                    )
                    adj_clms_tbls[CLM_PROD].append(_proc)

            diagnoses = adj_util.gen_diag_clm_prod_list(
                clm=clm, init_diagnoses=diag_clm_prod_per_fpk.get(four_part_key(clm))
            )
            adj_clms_tbls[CLM_PROD].extend(diagnoses)

            clm_dt_sgntr = adj_util.gen_clm_dt_sgntr(
                clm=clm, init_clm_dt_sgntr=clm_dt_sgntr_per_sk.get(clm[f.CLM_DT_SGNTR_SK])
            )
            adj_clms_tbls[CLM_DT_SGNTR].append(clm_dt_sgntr)

            clm_instnl = None
            if clm_type_cd in INSTITUTIONAL_CLAIM_TYPES:
                clm_instnl = adj_util.gen_clm_instnl(
                    gen_utils=gen_utils,
                    clm=clm,
                    init_clm_instnl=clm_instnl_per_fpk.get(four_part_key(clm)),
                )
                adj_clms_tbls[CLM_INSTNL].append(clm_instnl)

            if clm_type_cd in PROFESSIONAL_CLAIM_TYPES:
                clm_prfnls = [
                    adj_util.gen_clm_prfnl(
                        gen_utils=gen_utils,
                        clm=clm,
                        init_clm_prfnl=x,
                    )
                    for x in clm_prfnls_per_fpk.get(four_part_key(clm), [RowAdapter({})])
                ]
                adj_clms_tbls[CLM_PRFNL].extend(clm_prfnls)

            init_clm_lines = norm_clm_lines_per_clm_uniq_id.get(clm[f.CLM_UNIQ_ID]) or [
                RowAdapter({}) for _ in range(random.randint(1, 15))
            ]
            for line_num, init_clm_line in enumerate(init_clm_lines, start=1):
                if clm_type_cd in PHARMACY_CLM_TYPE_CDS:
                    continue

                clm_line = adj_util.gen_clm_line(
                    gen_utils=gen_utils,
                    clm=clm,
                    clm_line_num=line_num,
                    diagnoses=diagnoses,
                    init_clm_line=init_clm_line,
                )
                adj_clms_tbls[CLM_LINE].append(clm_line)

                fpk = four_part_key(clm)
                if clm_type_cd >= 10 and clm_type_cd <= 65:
                    clm_line_instnl = adj_util.gen_clm_line_instnl(
                        gen_utils=gen_utils,
                        clm=clm,
                        clm_line_num=line_num,
                        init_clm_line_instnl=match_line_num(
                            clm_lines=clm_line_instnls_per_fpk.get(fpk),
                            clm_line_num=int(clm_line[f.CLM_LINE_NUM]),
                        ),
                    )
                    adj_clms_tbls[CLM_LINE_INSTNL].append(clm_line_instnl)
                elif clm_type_cd >= 71 and clm_type_cd <= 82:
                    clm_line_prfnl = adj_util.gen_clm_line_prfnl(
                        gen_utils=gen_utils,
                        clm=clm,
                        clm_line_num=line_num,
                        init_clm_line_prfnl=match_line_num(
                            clm_lines=clm_line_prfnls_per_fpk.get(fpk),
                            clm_line_num=int(clm_line[f.CLM_LINE_NUM]),
                        ),
                    )
                    adj_clms_tbls[CLM_LINE_PRFNL].append(clm_line_prfnl)

            for k, v in adj_clms_tbls.items():
                out_tables[k].extend(v)

            all_adj_clms_tbls.append(adj_clms_tbls)

        # Looks awful, but this is essentially equivalent to looking up each table's row(s)
        # sequentially in the generation algorithm (as we do for adjudicated claims data above). We
        # "pre-lookup" these row(s) so that a BENE_SK with no pac CLMs can have pac claims data
        # generated for it from the freshly-generated adjudicated claims data above
        # ("all_adj_clms_tbls")
        pac_clms_tbls_from_file = [
            {
                CLM: as_list(file_pac_clm),  # as_list ensures None values return empty list
                CLM_FISS: as_list(clm_fiss_per_fpk.get(four_part_key(file_pac_clm))),
                CLM_LCTN_HSTRY: as_list(clm_lctn_hstry_per_fpk.get(four_part_key(file_pac_clm))),
                CLM_RLT_COND_SGNTR_MBR: as_list(
                    sgntr_mbr_per_clm_uniq_id.get(file_pac_clm[f.CLM_UNIQ_ID])
                ),
                CLM_LINE: [
                    *as_list(rx_clm_line_per_clm_uniq_id.get(file_pac_clm[f.CLM_UNIQ_ID])),
                    *norm_clm_lines_per_clm_uniq_id.get(file_pac_clm[f.CLM_UNIQ_ID], []),
                ],
                CLM_DCMTN: clm_dcmtns_per_fpk.get(four_part_key(file_pac_clm), []),
                CLM_PRFNL: clm_prfnls_per_fpk.get(four_part_key(file_pac_clm), []),
                CLM_VAL: [
                    *as_list(dsprtnt_clm_val_per_fpk.get(four_part_key(file_pac_clm))),
                    *as_list(ime_clm_val_per_fpk.get(four_part_key(file_pac_clm))),
                ],
                CLM_LINE_DCMTN: clm_line_dcmtns_per_clk.get(four_part_key(file_pac_clm), []),
                CLM_LINE_INSTNL: clm_line_instnls_per_fpk.get(four_part_key(file_pac_clm), []),
                CLM_LINE_PRFNL: clm_line_prfnls_per_fpk.get(four_part_key(file_pac_clm), []),
                CLM_INSTNL: as_list(clm_instnl_per_fpk.get(four_part_key(file_pac_clm))),
                CLM_PROD: [
                    *proc_clm_prod_per_fpk.get(four_part_key(file_pac_clm), []),
                    *diag_clm_prod_per_fpk.get(four_part_key(file_pac_clm), []),
                ],
                CLM_DT_SGNTR: as_list(clm_dt_sgntr_per_sk.get(file_pac_clm[f.CLM_DT_SGNTR_SK])),
            }
            for file_pac_clm in existing_pac_clms
        ]
        init_pac_clms_tbls = (
            pac_clms_tbls_from_file
            if pac_clms_tbls_from_file
            else [
                x
                for x in all_adj_clms_tbls
                if probability(0.5)
                and int(x[CLM][0][f.CLM_TYPE_CD])
                not in (1, 2, 3, 4)  # obviously we don't have pac claims for PD claims
            ]
            if gen_new_pac_clms
            else []
        )
        for claims_tbls in init_pac_clms_tbls:
            pac_clm = pac_util.gen_pac_clm(init_clm=claims_tbls[CLM][0])
            out_tables[CLM].append(pac_clm)

            pac_clm_fiss = pac_util.gen_clm_fiss(
                clm=pac_clm, init_clm_fiss=next(iter(claims_tbls[CLM_FISS]), None)
            )
            out_tables[CLM_FISS].append(pac_clm_fiss)

            pac_clm_lctn_hstry = pac_util.gen_clm_lctn_hstry(
                clm=pac_clm, init_clm_lctn_hstry=next(iter(claims_tbls[CLM_LCTN_HSTRY]), None)
            )
            out_tables[CLM_LCTN_HSTRY].append(pac_clm_lctn_hstry)

            pac_clm_lines = [
                pac_util.gen_pac_clm_line(clm=pac_clm, init_clm_line=clm_line)
                for clm_line in claims_tbls[CLM_LINE]
            ]
            out_tables[CLM_LINE].extend(pac_clm_lines)

            if claims_tbls[CLM_VAL]:
                out_tables[CLM_VAL].extend(
                    [
                        pac_util.gen_pac_clm_val(clm=pac_clm, init_clm_val=x)
                        for x in claims_tbls[CLM_VAL]
                    ]
                )

            for clm_line in claims_tbls[CLM_LINE]:
                # CLM_LINE_DCMTN is a bit special in that if we're creating it from an adjudicated
                # CLM_LINE we get the tracking number from the CLM_LINE, but if we already have a
                # CLM_LINE_DCMTN (we're regenerating it) the CLM_LINE does not have that field
                # anymore so we need to get it from the CLM_LINE_DCMTN. Pretty dumb, but it works
                clm_line_dcmtns = claims_tbls.get(CLM_LINE_DCMTN, [])
                clm_line_num = int(clm_line[f.CLM_LINE_NUM])
                init_clm_line_dcmtn = match_line_num(
                    clm_lines=clm_line_dcmtns,
                    clm_line_num=clm_line_num,
                )
                tracking_num = clm_line.get(f.CLM_LINE_PA_UNIQ_TRKNG_NUM) or (
                    init_clm_line_dcmtn[f.CLM_LINE_PA_UNIQ_TRKNG_NUM]
                    if init_clm_line_dcmtn
                    else None
                )
                if tracking_num:
                    out_tables[CLM_LINE_DCMTN].append(
                        pac_util.gen_pac_clm_line_dcmtn(
                            clm=pac_clm,
                            clm_line_num=clm_line_num,
                            tracking_num=tracking_num,
                            init_clm_line_dcmtn=match_line_num(
                                clm_lines=clm_line_dcmtns,
                                clm_line_num=clm_line_num,
                            ),
                        )
                    )

            out_tables[CLM_LINE_INSTNL].extend(
                [
                    pac_util.gen_pac_clm_instnl(clm=pac_clm, init_clm_instnl=clm_line_instnl)
                    for clm_line_instnl in claims_tbls[CLM_LINE_INSTNL]
                ]
            )

            out_tables[CLM_LINE_PRFNL].extend(
                [
                    pac_util.gen_pac_clm_line_prfnl(clm=pac_clm, init_clm_line_prfnl=clm_line_prfnl)
                    for clm_line_prfnl in claims_tbls[CLM_LINE_PRFNL]
                ]
            )

            if claims_tbls[CLM_INSTNL]:
                out_tables[CLM_INSTNL].append(
                    pac_util.gen_pac_clm_instnl(
                        clm=pac_clm, init_clm_instnl=claims_tbls[CLM_INSTNL][0]
                    )
                )

            out_tables[CLM_PROD].extend(
                [
                    pac_util.gen_pac_clm_prod(clm=pac_clm, init_clm_prod=clm_prod)
                    for clm_prod in claims_tbls[CLM_PROD]
                ]
            )

            out_tables[CLM_DT_SGNTR].append(
                pac_util.gen_pac_clm_dt_sgntr(
                    clm=pac_clm, init_clm_dt_sgntr=claims_tbls[CLM_DT_SGNTR][0]
                )
            )

            out_tables[CLM_RLT_COND_SGNTR_MBR].append(
                pac_util.gen_pac_clm_rlt_cond_sgntr_mbr(
                    clm=pac_clm,
                    init_clm_rlt_cond_sgntr_mbr=next(
                        iter(claims_tbls[CLM_RLT_COND_SGNTR_MBR]), RowAdapter({})
                    ),
                )
            )

            out_tables[CLM_DCMTN].extend(
                [
                    adj_util.gen_clm_dcmtn(clm=pac_clm, init_clm_dcmtn=x)
                    for x in claims_tbls[CLM_DCMTN]
                ]
            )

            clm_type_cd = int(pac_clm[f.CLM_TYPE_CD])
            if clm_type_cd in PROFESSIONAL_CLAIM_TYPES:
                clm_prfnls = [
                    adj_util.gen_clm_prfnl(
                        gen_utils=gen_utils,
                        clm=pac_clm,
                        init_clm_prfnl=x,
                    )
                    for x in claims_tbls[CLM_PRFNL]
                ]
                out_tables[CLM_PRFNL].extend(clm_prfnls)

    print("Done generating synthetic claims data for provided BENE_SKs")

    _save_claims_data({_ClaimsFile(k): v for k, v in out_tables.items() if k in _ClaimsFile})


if __name__ == "__main__":
    generate()
