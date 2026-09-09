import json
import sys
from pathlib import Path
from typing import Any

import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from claims_static import INSTITUTIONAL_CLAIM_TYPES  # noqa: E402


class Result:

    def __init__(self, result_json: str, output_file:str):
        self.result_json = result_json
        self.output_file = output_file

    result_json: dict[str, Any]
    output_file: str


class SampleGenerator:
    def __init__(self, source_directory: str):
        self.source_directory = source_directory

    def run(self, clm_uniq_id: str) -> None:
        """Generate EOB sample JSON from SYNTHETIC_EOB.csv based on claim unique ID."""
        print(f"Generating EOB sample for claim unique ID: {clm_uniq_id}")
        print(f"Source Directory: {self.source_directory}")
        if not Path(self.source_directory).exists():
            print("Source directory not found. Run the generator or this will not go well.")
            sys.exit(1)

        claim_row = self.read_clm(clm_uniq_id)
        claim_type = int(claim_row.get("CLM_TYPE_CD"))

        match claim_type:
            # pharmacy claim type
            case 1 | 2 | 3 | 4:
                result = self.create_pharmacy(clm_uniq_id, claim_row)
            case _:
                # this a default fallback for institutional claim types at the moment
                # as this work continues we may add more specific handling
                # for different institutional claim types
                if claim_type in INSTITUTIONAL_CLAIM_TYPES:
                    result = self.create_base(clm_uniq_id, claim_row)
                else:
                    print("Unknown Type")
                    sys.exit(1)

        with Path(result.output_file).open(mode="w", encoding="utf-8") as f:
            json.dump(result.result_json, f, indent=2)

        print(f"Successfully generated sample JSON: {result.output_file}")

    def create_base(self, clm_uniq_id: str, claim_row: dict[str, str]) -> Result:
        prod_lines = self.read_prod_lines(claim_row)

        diagnoses_lines = [
            {
                "CLM_VAL_SQNC_NUM": extract_col_str(prod_line, "CLM_VAL_SQNC_NUM"),
                "CLM_DGNS_CD": extract_col_str(prod_line, "CLM_DGNS_CD"),
                "CLM_DGNS_PRCDR_ICD_IND": extract_col_str(prod_line, "CLM_DGNS_PRCDR_ICD_IND"),
                "CLM_PROD_TYPE_CD": extract_col_str(prod_line, "CLM_PROD_TYPE_CD"),
                "CLM_POA_IND": extract_col_str(prod_line, "CLM_POA_IND"),
            }
            for prod_line in (prod_lines or [])
        ]

        procedure_lines = [
            {
                "CLM_VAL_SQNC_NUM": extract_col_str(prod_line, "CLM_VAL_SQNC_NUM"),
                "CLM_PRCDR_PRFRM_DT": extract_col_str(prod_line, "CLM_PRCDR_PRFRM_DT"),
                "CLM_PRCDR_CD": extract_col_str(prod_line, "CLM_PRCDR_CD"),
                "CLM_DGNS_PRCDR_ICD_IND": extract_col_str(prod_line, "CLM_DGNS_PRCDR_ICD_IND"),
            }
            for prod_line in (prod_lines or [])
        ]

        clm_lines = self.read_line(claim_row)

        instnl_lines = self.read_instnl_lines(claim_row)

        line_item_components = [
            {
                "PRVDR_RNDRNG_PRVDR_NPI_NUM": extract_col_str(
                    clm_line, "PRVDR_RNDRNG_PRVDR_NPI_NUM"
                ),
                "CLM_LINE_ADD_ON_PYMT_AMT": find_field_in_line_by_num(
                    instnl_lines, clm_line, "CLM_LINE_ADD_ON_PYMT_AMT"
                ),
                "CLM_LINE_NON_EHR_RDCTN_AMT": find_field_in_line_by_num(
                    instnl_lines, clm_line, "CLM_LINE_NON_EHR_RDCTN_AMT"
                ),
                "CLM_LINE_ALOWD_CHRG_AMT": extract_col_str(clm_line, "CLM_LINE_ALOWD_CHRG_AMT"),
                "CLM_REV_CNTR_TDAPA_AMT": extract_col_str(clm_line, "CLM_REV_CNTR_TDAPA_AMT"),
                "CLM_LINE_NUM": extract_col_str(clm_line, "CLM_LINE_NUM"),
                "CLM_LINE_HCPCS_CD": extract_col_str(clm_line, "CLM_LINE_HCPCS_CD"),
                # This is hard coded in all the samples I cannot find it anywhere else in the code
                "CLM_REV_APC_HIPPS_CD": "",
                "CLM_LINE_NDC_CD": extract_col_str(clm_line, "CLM_LINE_NDC_CD"),
                "CLM_LINE_NDC_QTY": extract_col_str(clm_line, "CLM_LINE_NDC_QTY"),
                "CLM_LINE_SRVC_UNIT_QTY": extract_col_str(clm_line, "CLM_LINE_SRVC_UNIT_QTY"),
                "CLM_LINE_REV_CTR_CD": extract_col_str(clm_line, "CLM_LINE_REV_CTR_CD"),
                "CLM_DDCTBL_COINSRNC_CD": find_field_in_line_by_num(
                    instnl_lines, clm_line, "CLM_DDCTBL_COINSRNC_CD"
                ),
                # This is hard coded in all the samples I cannot find it anywhere else in the code
                "GEO_FAC_SSA_STATE_CD": "01",
                "CLM_LINE_PRVDR_PMT_AMT": extract_col_str(clm_line, "CLM_LINE_PRVDR_PMT_AMT"),
                "CLM_LINE_SBMT_CHRG_AMT": extract_col_str(clm_line, "CLM_LINE_SBMT_CHRG_AMT"),
                "HCPCS_1_MDFR_CD": extract_col_str(clm_line, "HCPCS_1_MDFR_CD"),
                "CLM_LINE_NCVRD_CHRG_AMT": extract_col_str(clm_line, "CLM_LINE_NCVRD_CHRG_AMT"),
                "CLM_LINE_BENE_PMT_AMT": extract_col_str(clm_line, "CLM_LINE_BENE_PMT_AMT"),
                "CLM_LINE_BENE_PD_AMT": extract_col_str(clm_line, "CLM_LINE_BENE_PD_AMT"),
                "CLM_LINE_CVRD_PD_AMT": extract_col_str(clm_line, "CLM_LINE_CVRD_PD_AMT"),
                "CLM_LINE_BLOOD_DDCTBL_AMT": extract_col_str(clm_line, "CLM_LINE_BLOOD_DDCTBL_AMT"),
                "CLM_LINE_MDCR_DDCTBL_AMT": extract_col_str(clm_line, "CLM_LINE_MDCR_DDCTBL_AMT"),
                "CLM_LINE_INSTNL_REV_CTR_DT": extract_col_str(
                    clm_line, "CLM_LINE_INSTNL_REV_CTR_DT"
                ),
                "CLM_LINE_INSTNL_RATE_AMT": extract_col_str(clm_line, "CLM_LINE_INSTNL_RATE_AMT"),
                "CLM_LINE_INSTNL_MSP1_PD_AMT": extract_col_str(
                    clm_line, "CLM_LINE_INSTNL_MSP1_PD_AMT"
                ),
                "CLM_LINE_INSTNL_MSP2_PD_AMT": extract_col_str(
                    clm_line, "CLM_LINE_INSTNL_MSP2_PD_AMT"
                ),
                "CLM_LINE_INSTNL_RDCD_AMT": extract_col_str(clm_line, "CLM_LINE_INSTNL_RDCD_AMT"),
                "CLM_LINE_INSTNL_ADJSTD_AMT": extract_col_str(
                    clm_line, "CLM_LINE_INSTNL_ADJSTD_AMT"
                ),
                "CLM_LINE_NDC_QTY_QLFYR_CD": extract_col_str(clm_line, "CLM_LINE_NDC_QTY_QLFYR_CD"),
            }
            for clm_line in clm_lines
        ]

        instnl = self.read_instnl(clm_row=claim_row)

        claim_values = [
            {
                "CLM_VAL_CD": extract_col_str(val_line, "CLM_VAL_CD"),
                "CLM_VAL_AMT": extract_col_str(val_line, "CLM_VAL_AMT"),
            }
            for val_line in self.read_clm_val(clm_line=claim_row)
        ]

        clm_sig_row = self.read_sig_line(str(claim_row.get("CLM_DT_SGNTR_SK", "")).strip())

        output_json = {
            "resourceType": "ExplanationOfBenefitBase",
            "id": str(clm_uniq_id.replace("-", "")).strip(),
            "lastUpdated": extract_col_str(claim_row, "IDR_UPDT_TS"),
            "BENE_SK": extract_col_str(claim_row, "BENE_SK"),
            "CLM_TYPE_CD": int(extract_col_str(claim_row, "CLM_TYPE_CD")),
            "META_SRC_SK": extract_col_str(claim_row, "META_SRC_SK"),
            "CLM_UNIQ_ID": extract_col_str(claim_row, "CLM_UNIQ_ID"),
            "CLM_CNTL_NUM": extract_col_str(claim_row, "CLM_CNTL_NUM"),
            "CLM_FROM_DT": extract_col_str(claim_row, "CLM_FROM_DT"),
            "CLM_THRU_DT": extract_col_str(claim_row, "CLM_THRU_DT"),
            "CLM_EFCTV_DT": extract_col_str(claim_row, "CLM_EFCTV_DT"),
            "CLM_SRC_ID": extract_col_str(claim_row, "CLM_SRC_ID"),
            "PRVDR_BLG_PRVDR_NPI_NUM": extract_col_str(claim_row, "PRVDR_BLG_PRVDR_NPI_NUM"),
            "CLM_BLG_PRVDR_ZIP5_CD": extract_col_str(claim_row, "CLM_BLG_PRVDR_ZIP5_CD"),
            "GEO_BLG_SSA_STATE_CD": extract_col_str(claim_row, "GEO_BLG_SSA_STATE_CD"),
            "diagnoses": diagnoses_lines,
            "procedures": procedure_lines,
            "supportingInfoComponents": [],
            "lineItemComponents": line_item_components,
            "CLM_MDCR_DDCTBL_AMT": extract_col_str(claim_row, "CLM_MDCR_DDCTBL_AMT"),
            "CLM_NRLN_RIC_CD": extract_col_str(claim_row, "CLM_NRLN_RIC_CD"),
            "CLM_PMT_AMT": extract_col_str(claim_row, "CLM_PMT_AMT"),
            "CLM_SBMT_CHRG_AMT": extract_col_str(claim_row, "CLM_SBMT_CHRG_AMT"),
            "CLM_MDCR_COINSRNC_AMT": extract_col_str(claim_row, "CLM_MDCR_COINSRNC_AMT"),
            "CLM_NCVRD_CHRG_AMT": extract_col_str(claim_row, "CLM_NCVRD_CHRG_AMT"),
            "CLM_BLOOD_LBLTY_AMT": extract_col_str(claim_row, "CLM_BLOOD_LBLTY_AMT"),
            "CLM_BLG_PRVDR_TAX_NUM": extract_col_str(claim_row, "CLM_BLG_PRVDR_TAX_NUM"),
            "institutionalComponents": {
                # This is in the sample data but I cannot find it anywhere else
                # "CLM_HIPPS_MODEL_BNDLD_PYMT_AMT": "44.11",
                "CLM_MDCR_HHA_TOT_VISIT_CNT": extract_col_str(instnl, "CLM_MDCR_HHA_TOT_VISIT_CNT"),
                "CLM_MDCR_HOSPC_PRD_CNT": extract_col_str(instnl, "CLM_MDCR_HOSPC_PRD_CNT"),
                "CLM_MDCR_INSTNL_PRMRY_PYR_AMT": extract_col_str(
                    instnl, "CLM_MDCR_INSTNL_PRMRY_PYR_AMT"
                ),
                "CLM_MDCR_IP_LRD_USE_CNT": extract_col_str(instnl, "CLM_MDCR_IP_LRD_USE_CNT"),
                "CLM_INSTNL_PER_DIEM_AMT": extract_col_str(instnl, "CLM_INSTNL_PER_DIEM_AMT"),
                "CLM_INSTNL_CVRD_DAY_CNT": extract_col_str(instnl, "CLM_INSTNL_CVRD_DAY_CNT"),
                "CLM_HIPPS_UNCOMPD_CARE_AMT": extract_col_str(instnl, "CLM_HIPPS_UNCOMPD_CARE_AMT"),
                "CLM_MDCR_IP_PPS_DSPRPRTNT_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_PPS_DSPRPRTNT_AMT"
                ),
                "CLM_MDCR_IP_PPS_DRG_WT_NUM": extract_col_str(instnl, "CLM_MDCR_IP_PPS_DRG_WT_NUM"),
                "CLM_INSTNL_MDCR_COINS_DAY_CNT": extract_col_str(
                    instnl, "CLM_INSTNL_MDCR_COINS_DAY_CNT"
                ),
                "CLM_INSTNL_NCVRD_DAY_CNT": extract_col_str(instnl, "CLM_INSTNL_NCVRD_DAY_CNT"),
                "CLM_MDCR_IP_PPS_EXCPTN_AMT": extract_col_str(instnl, "CLM_MDCR_IP_PPS_EXCPTN_AMT"),
                "CLM_MDCR_IP_PPS_CPTL_FSP_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_PPS_CPTL_FSP_AMT"
                ),
                "CLM_MDCR_IP_PPS_CPTL_IME_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_PPS_CPTL_IME_AMT"
                ),
                "CLM_MDCR_IP_PPS_OUTLIER_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_PPS_OUTLIER_AMT"
                ),
                "CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT"
                ),
                "CLM_MDCR_IP_PPS_CPTL_TOT_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_PPS_CPTL_TOT_AMT"
                ),
                "CLM_INSTNL_DRG_OUTLIER_AMT": extract_col_str(
                    instnl, "CLM_INSTNL_DRG_OUTLIER_AMT"
                ),
                "CLM_INSTNL_PRFNL_AMT": extract_col_str(instnl, "CLM_INSTNL_PRFNL_AMT"),
                "CLM_FINL_STDZD_PYMT_AMT": extract_col_str(instnl, "CLM_FINL_STDZD_PYMT_AMT"),
                "CLM_HAC_RDCTN_PYMT_AMT": extract_col_str(instnl, "CLM_HAC_RDCTN_PYMT_AMT"),
                "CLM_HIPPS_MODEL_BNDLD_PMT_AMT": extract_col_str(
                    instnl, "CLM_HIPPS_MODEL_BNDLD_PMT_AMT"
                ),
                "CLM_HIPPS_READMSN_RDCTN_AMT": extract_col_str(
                    instnl, "CLM_HIPPS_READMSN_RDCTN_AMT"
                ),
                "CLM_HIPPS_VBP_AMT": extract_col_str(instnl, "CLM_HIPPS_VBP_AMT"),
                "CLM_MDCR_IP_1ST_YR_RATE_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_1ST_YR_RATE_AMT"
                ),
                "CLM_MDCR_IP_SCND_YR_RATE_AMT": extract_col_str(
                    instnl, "CLM_MDCR_IP_SCND_YR_RATE_AMT"
                ),
                "CLM_PPS_MD_WVR_STDZD_VAL_AMT": extract_col_str(
                    instnl, "CLM_PPS_MD_WVR_STDZD_VAL_AMT"
                ),
                "CLM_SITE_NTRL_CST_BSD_PYMT_AMT": extract_col_str(
                    instnl, "CLM_SITE_NTRL_CST_BSD_PYMT_AMT"
                ),
                "CLM_SITE_NTRL_IP_PPS_PYMT_AMT": extract_col_str(
                    instnl, "CLM_SITE_NTRL_IP_PPS_PYMT_AMT"
                ),
                "CLM_SS_OUTLIER_STD_PYMT_AMT": extract_col_str(
                    instnl, "CLM_SS_OUTLIER_STD_PYMT_AMT"
                ),
                "DGNS_DRG_CD": extract_col_str(instnl, "DGNS_DRG_CD"),
                "CLM_ADMSN_TYPE_CD": extract_col_str(instnl, "CLM_ADMSN_TYPE_CD"),
                "BENE_PTNT_STUS_CD": extract_col_str(instnl, "BENE_PTNT_STUS_CD"),
                "CLM_MDCR_INSTNL_MCO_PD_SW": extract_col_str(instnl, "CLM_MDCR_INSTNL_MCO_PD_SW"),
                "CLM_ADMSN_SRC_CD": extract_col_str(instnl, "CLM_ADMSN_SRC_CD"),
                "CLM_PPS_IND_CD": extract_col_str(instnl, "CLM_PPS_IND_CD"),
                "CLM_HHA_LUP_IND_CD": extract_col_str(instnl, "CLM_HHA_LUP_IND_CD"),
                "CLM_HHA_RFRL_CD": extract_col_str(instnl, "CLM_HHA_RFRL_CD"),
                "DGNS_DRG_OUTLIER_CD": extract_col_str(instnl, "DGNS_DRG_OUTLIER_CD"),
                "CLM_MDCR_NPMT_RSN_CD": extract_col_str(instnl, "CLM_MDCR_NPMT_RSN_CD"),
                "CLM_FI_ACTN_CD": extract_col_str(instnl, "CLM_FI_ACTN_CD"),
            },
            "claimValues": claim_values,
            "PRVDR_ATNDG_PRVDR_NPI_NUM": extract_col_str(claim_row, "PRVDR_ATNDG_PRVDR_NPI_NUM"),
            "PRVDR_OPRTG_PRVDR_NPI_NUM": extract_col_str(claim_row, "PRVDR_OPRTG_PRVDR_NPI_NUM"),
            "PRVDR_OTHR_PRVDR_NPI_NUM": extract_col_str(claim_row, "PRVDR_OTHR_PRVDR_NPI_NUM"),
            "PRVDR_RNDRNG_PRVDR_NPI_NUM": extract_col_str(claim_row, "PRVDR_RNDRNG_PRVDR_NPI_NUM"),
            "CLM_BLG_PRVDR_OSCAR_NUM": extract_col_str(claim_row, "CLM_BLG_PRVDR_OSCAR_NUM"),
            "CLM_BENE_PMT_COINSRNC_AMT": extract_col_str(claim_row, "CLM_BENE_PMT_COINSRNC_AMT"),
            "CLM_BLOOD_CHRG_AMT": extract_col_str(claim_row, "CLM_BLOOD_CHRG_AMT"),
            "CLM_BLOOD_NCVRD_CHRG_AMT": extract_col_str(claim_row, "CLM_BLOOD_NCVRD_CHRG_AMT"),
            "CLM_COB_PTNT_RESP_AMT": extract_col_str(claim_row, "CLM_COB_PTNT_RESP_AMT"),
            "CLM_OTHR_TP_PD_AMT": extract_col_str(claim_row, "CLM_OTHR_TP_PD_AMT"),
            "CLM_PRVDR_INTRST_PD_AMT": extract_col_str(claim_row, "CLM_PRVDR_INTRST_PD_AMT"),
            "CLM_PRVDR_OTAF_AMT": extract_col_str(claim_row, "CLM_PRVDR_OTAF_AMT"),
            "CLM_PRVDR_RMNG_DUE_AMT": extract_col_str(claim_row, "CLM_PRVDR_RMNG_DUE_AMT"),
            "CLM_BNFT_ENHNCMT_1_CD": extract_col_str(claim_row, "CLM_BNFT_ENHNCMT_1_CD"),
            "CLM_BNFT_ENHNCMT_2_CD": extract_col_str(claim_row, "CLM_BNFT_ENHNCMT_2_CD"),
            "CLM_BNFT_ENHNCMT_3_CD": extract_col_str(claim_row, "CLM_BNFT_ENHNCMT_3_CD"),
            "CLM_BNFT_ENHNCMT_4_CD": extract_col_str(claim_row, "CLM_BNFT_ENHNCMT_4_CD"),
            "CLM_BNFT_ENHNCMT_5_CD": extract_col_str(claim_row, "CLM_BNFT_ENHNCMT_5_CD"),
            "CLM_ACO_CARE_MGMT_HCBS_SW": extract_col_str(claim_row, "CLM_ACO_CARE_MGMT_HCBS_SW"),
            "CLM_TOT_CNTRCTL_AMT": extract_col_str(claim_row, "CLM_TOT_CNTRCTL_AMT"),
            "CLM_ATNDG_FED_PRVDR_SPCLTY_CD": extract_col_str(
                claim_row, "CLM_ATNDG_FED_PRVDR_SPCLTY_CD"
            ),
            "CLM_BILL_FAC_TYPE_CD": extract_col_str(claim_row, "CLM_BILL_FAC_TYPE_CD"),
            "CLM_BILL_CLSFCTN_CD": extract_col_str(claim_row, "CLM_BILL_CLSFCTN_CD"),
            "CLM_BILL_FREQ_CD": extract_col_str(claim_row, "CLM_BILL_FREQ_CD"),
            "CLM_RLT_COND_SGNTR_SK": extract_col_str(claim_row, "CLM_RLT_COND_SGNTR_SK"),
            "CLM_ACTV_CARE_FROM_DT": extract_col_str(clm_sig_row, "CLM_ACTV_CARE_FROM_DT"),
            "CLM_DSCHRG_DT": extract_col_str(clm_sig_row, "CLM_DSCHRG_DT"),
            "CLM_MDCR_EXHSTD_DT": extract_col_str(clm_sig_row, "CLM_MDCR_EXHSTD_DT"),
            "CLM_SUBMSN_DT": extract_col_str(clm_sig_row, "CLM_MDCR_EXHSTD_DT"),
            "CLM_NCH_WKLY_PROC_DT": extract_col_str(clm_sig_row, "CLM_NCH_WKLY_PROC_DT"),
            "CLM_NCVRD_FROM_DT": extract_col_str(clm_sig_row, "CLM_NCVRD_FROM_DT"),
            "CLM_NCVRD_THRU_DT": extract_col_str(clm_sig_row, "CLM_NCVRD_THRU_DT"),
            "CLM_ACTV_CARE_THRU_DT": extract_col_str(clm_sig_row, "CLM_ACTV_CARE_THRU_DT"),
            "CLM_QLFY_STAY_FROM_DT": extract_col_str(clm_sig_row, "CLM_QLFY_STAY_FROM_DT"),
            "CLM_QLFY_STAY_THRU_DT": extract_col_str(clm_sig_row, "CLM_QLFY_STAY_THRU_DT"),
            "CLM_CMS_PROC_DT": extract_col_str(clm_sig_row, "CLM_CMS_PROC_DT"),
            "CLM_NCH_PRMRY_PYR_CD": extract_col_str(claim_row, "CLM_NCH_PRMRY_PYR_CD"),
            "CLM_QUERY_CD": extract_col_str(claim_row, "CLM_QUERY_CD"),
            "CLM_IDR_LD_DT": extract_col_str(claim_row, "CLM_IDR_LD_DT"),
            "CLM_CNTRCTR_NUM": extract_col_str(claim_row, "CLM_CNTRCTR_NUM"),
            "CLM_ADJSTMT_TYPE_CD": extract_col_str(claim_row, "CLM_ADJSTMT_TYPE_CD"),
            "CLM_DISP_CD": extract_col_str(claim_row, "CLM_DISP_CD"),
        }

        return Result(result_json=output_json, output_file="sample-data/EOB-Base-Sample.json")

    def create_pharmacy(self, clm_uniq_id: str, claim_row: dict[str, str]) -> Result:

        provider_npi = str(claim_row.get("PRVDR_PRSCRBNG_PRVDR_NPI_NUM", "")).strip()

        prov_row = self.read_provider(provider_npi)

        clm_sig_row = self.read_sig_line(str(claim_row.get("CLM_DT_SGNTR_SK", "")).strip())

        clm_sbmtr_cntrct_num = str(claim_row.get("CLM_SBMTR_CNTRCT_NUM", "")).strip()
        clm_sbmtr_cntrct_pbp_num = str(claim_row.get("CLM_SBMTR_CNTRCT_PBP_NUM", "")).strip()

        ctr_pmp_row = self.read_pmp(clm_sbmtr_cntrct_num, clm_sbmtr_cntrct_pbp_num)

        clm_lines = self.read_line(claim_row)

        clm_line = {} if not clm_lines else clm_lines[0]

        rx_line = self.read_rx_line(claim_row)

        output_json = {
            "resourceType": "ExplanationOfBenefit-Pharmacy",
            "id": str(clm_uniq_id.replace("-", "")).strip(),
            "lastUpdated": extract_col_str(claim_row, "IDR_UPDT_TS"),
            "CLM_FINL_ACTN_IND": extract_col_str(claim_row, "CLM_FINL_ACTN_IND"),
            "CLM_SRC_ID": extract_col_str(claim_row, "CLM_SRC_ID"),
            "BENE_SK": extract_col_str(claim_row, "BENE_SK"),
            "CLM_TYPE_CD": int(extract_col_str(claim_row, "CLM_TYPE_CD")),
            "CLM_UNIQ_ID": extract_col_str(claim_row, "CLM_UNIQ_ID"),
            "CLM_CNTL_NUM": extract_col_str(claim_row, "CLM_CNTL_NUM"),
            "CLM_ORIG_CNTL_NUM": extract_col_str(claim_row, "CLM_ORIG_CNTL_NUM"),
            "CLM_FROM_DT": extract_col_str(claim_row, "CLM_FROM_DT"),
            "CLM_THRU_DT": extract_col_str(claim_row, "CLM_THRU_DT"),
            "CLM_EFCTV_DT": extract_col_str(claim_row, "CLM_EFCTV_DT"),
            "CLM_SRVC_PRVDR_GNRC_ID_NUM": extract_col_str(claim_row, "CLM_SRVC_PRVDR_GNRC_ID_NUM"),
            "CLM_PD_DT": extract_col_str(claim_row, "CLM_PD_DT"),
            "PRVDR_PRSCRBNG_PRVDR_NPI_NUM": provider_npi,
            "CLM_PRSBNG_PRVDR_GNRC_ID_NUM": extract_col_str(
                claim_row, "CLM_PRSBNG_PRVDR_GNRC_ID_NUM"
            ),
            "PRVDR_PRSBNG_ID_QLFYR_CD": extract_col_str(claim_row, "PRVDR_PRSBNG_ID_QLFYR_CD"),
            "PRVDR_LAST_NAME": extract_col_str(prov_row, "PRVDR_LAST_NAME"),
            "CNTRCT_PBP_NAME": extract_col_str(ctr_pmp_row, "CNTRCT_PBP_NAME"),
            "CLM_BENE_PMT_AMT": extract_col_str(claim_row, "CLM_BENE_PMT_AMT"),
            "CLM_OTHR_TP_PD_AMT": extract_col_str(claim_row, "CLM_OTHR_TP_PD_AMT"),
            "META_SRC_SK": extract_col_str(claim_row, "META_SRC_SK"),
            "PRVDR_SRVC_ID_QLFYR_CD": extract_col_str(claim_row, "PRVDR_SRVC_ID_QLFYR_CD"),
            "supportingInfoComponents": [],
            "lineItemComponents": [
                {
                    "CLM_LINE_NUM": extract_col_str(clm_line, "CLM_LINE_NUM"),
                    "CLM_LINE_FROM_DT": extract_col_str(clm_line, "CLM_LINE_FROM_DT"),
                    "CLM_LINE_NDC_CD": extract_col_str(clm_line, "CLM_LINE_NDC_CD"),
                    "CLM_LINE_NDC_QTY": extract_col_str(clm_line, "CLM_LINE_NDC_QTY"),
                    "CLM_LINE_NDC_QTY_QLFYR_CD": extract_col_str(
                        clm_line, "CLM_LINE_NDC_QTY_QLFYR_CD"
                    ),
                    "CLM_LINE_CVRD_PD_AMT": extract_col_str(clm_line, "CLM_LINE_CVRD_PD_AMT"),
                    "CLM_LINE_GRS_ABOVE_THRSHLD_AMT": extract_col_str(
                        rx_line, "CLM_LINE_GRS_ABOVE_THRSHLD_AMT"
                    ),
                    "CLM_LINE_GRS_BLW_THRSHLD_AMT": extract_col_str(
                        rx_line, "CLM_LINE_GRS_BLW_THRSHLD_AMT"
                    ),
                    "CLM_LINE_LIS_AMT": extract_col_str(rx_line, "CLM_LINE_LIS_AMT"),
                    "CLM_LINE_TROOP_TOT_AMT": extract_col_str(rx_line, "CLM_LINE_TROOP_TOT_AMT"),
                    "CLM_LINE_PLRO_AMT": extract_col_str(rx_line, "CLM_LINE_PLRO_AMT"),
                    "CLM_RPTD_MFTR_DSCNT_AMT": extract_col_str(rx_line, "CLM_RPTD_MFTR_DSCNT_AMT"),
                    "CLM_LINE_INGRDNT_CST_AMT": extract_col_str(
                        rx_line, "CLM_LINE_INGRDNT_CST_AMT"
                    ),
                    "CLM_LINE_SRVC_CST_AMT": extract_col_str(rx_line, "CLM_LINE_SRVC_CST_AMT"),
                    "CLM_LINE_SLS_TAX_AMT": extract_col_str(rx_line, "CLM_LINE_SLS_TAX_AMT"),
                    "CLM_LINE_VCCN_ADMIN_FEE_AMT": extract_col_str(
                        rx_line, "CLM_LINE_VCCN_ADMIN_FEE_AMT"
                    ),
                    "CLM_PRCNG_EXCPTN_CD": extract_col_str(rx_line, "CLM_PRCNG_EXCPTN_CD"),
                    "CLM_LINE_BENE_PMT_AMT": extract_col_str(clm_line, "CLM_LINE_BENE_PMT_AMT"),
                    "CLM_CMS_CALCD_MFTR_DSCNT_AMT": extract_col_str(
                        rx_line, "CLM_CMS_CALCD_MFTR_DSCNT_AMT"
                    ),
                    "CLM_LINE_GRS_CVRD_CST_TOT_AMT": extract_col_str(
                        rx_line, "CLM_LINE_GRS_CVRD_CST_TOT_AMT"
                    ),
                    "CLM_LINE_REBT_PASSTHRU_POS_AMT": extract_col_str(
                        rx_line, "CLM_LINE_REBT_PASSTHRU_POS_AMT"
                    ),
                    "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT": extract_col_str(
                        rx_line, "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT"
                    ),
                    "CLM_LINE_OTHR_TP_PD_AMT": extract_col_str(clm_line, "CLM_LINE_OTHR_TP_PD_AMT"),
                    "CLM_LINE_NCVRD_PD_AMT": extract_col_str(clm_line, "CLM_LINE_NCVRD_PD_AMT"),
                    "isCompound": str(extract_col_str(rx_line, "CLM_CMPND_CD") == "2").lower(),
                    "CLM_LINE_RPTD_GAP_DSCNT_AMT": extract_col_str(
                        rx_line, "CLM_LINE_RPTD_GAP_DSCNT_AMT"
                    ),
                    "CLM_LINE_AUTHRZD_FILL_NUM": extract_col_str(
                        rx_line, "CLM_LINE_AUTHRZD_FILL_NUM"
                    ),
                    "CLM_PHRMCY_SRVC_TYPE_CD": extract_col_str(rx_line, "CLM_PHRMCY_SRVC_TYPE_CD"),
                    "CLM_LINE_RX_ORGN_CD": extract_col_str(rx_line, "CLM_LINE_RX_ORGN_CD"),
                    "CLM_BRND_GNRC_CD": extract_col_str(rx_line, "CLM_BRND_GNRC_CD"),
                    "CLM_PTNT_RSDNC_CD": extract_col_str(rx_line, "CLM_PTNT_RSDNC_CD"),
                    "CLM_LTC_DSPNSNG_MTHD_CD": extract_col_str(rx_line, "CLM_LTC_DSPNSNG_MTHD_CD"),
                    "CLM_CMPND_CD": extract_col_str(rx_line, "CLM_CMPND_CD"),
                    "CLM_LINE_DAYS_SUPLY_QTY": extract_col_str(rx_line, "CLM_LINE_DAYS_SUPLY_QTY"),
                    "CLM_LINE_RX_FILL_NUM": extract_col_str(rx_line, "CLM_LINE_RX_FILL_NUM"),
                    "CLM_DAW_PROD_SLCTN_CD": extract_col_str(rx_line, "CLM_DAW_PROD_SLCTN_CD"),
                    "CLM_DRUG_CVRG_STUS_CD": extract_col_str(rx_line, "CLM_DRUG_CVRG_STUS_CD"),
                    "CLM_CTSTRPHC_CVRG_IND_CD": extract_col_str(
                        rx_line, "CLM_CTSTRPHC_CVRG_IND_CD"
                    ),
                    "CLM_LINE_RX_NUM": extract_col_str(clm_line, "CLM_LINE_RX_NUM"),
                    "CLM_DSPNSNG_STUS_CD": extract_col_str(rx_line, "CLM_DSPNSNG_STUS_CD"),
                }
            ],
            "CLM_CMS_PROC_DT": extract_col_str(clm_sig_row, "CLM_CMS_PROC_DT"),
            "CLM_IDR_LD_DT ": extract_col_str(claim_row, "CLM_IDR_LD_DT"),
            "CLM_ADJSTMT_TYPE_CD": extract_col_str(claim_row, "CLM_ADJSTMT_TYPE_CD"),
            "CLM_SBMT_FRMT_CD": extract_col_str(claim_row, "CLM_SBMT_FRMT_CD"),
            "CLM_SBMTR_CNTRCT_NUM": extract_col_str(claim_row, "CLM_SBMTR_CNTRCT_NUM"),
            "CLM_SBMTR_CNTRCT_PBP_NUM": extract_col_str(claim_row, "CLM_SBMTR_CNTRCT_PBP_NUM"),
            "CLM_DT_SGNTR_SK": extract_col_str(clm_sig_row, "CLM_DT_SGNTR_SK"),
        }

        return Result(result_json=output_json, output_file="sample-data/EOB-Pharmacy-Sample.json")

    def read_clm(self, clm_uniq_id: str) -> dict[str, str]:
        eob_path = f"{self.source_directory}/SYNTHETIC_CLM.csv"
        if not Path(eob_path).exists():
            print("EOB file not found. Run the generator or this will not go well.")
            sys.exit(1)

        claims_found = pd.read_csv(eob_path, dtype=str, keep_default_na=False)

        clm_matches = claims_found[claims_found["CLM_UNIQ_ID"] == clm_uniq_id]

        if clm_matches.empty:
            print(f"No claims found for claim unique ID: {clm_uniq_id}")
            sys.exit(1)

        return clm_matches.iloc[0]

    def read_provider(self, provider_npi: str) -> dict[str, str]:
        provider_path = f"{self.source_directory}/SYNTHETIC_PRVDR_HSTRY.csv"

        if not Path(provider_path).exists():
            print("Provider file not found. Run the generator or this will not go well.")
            sys.exit(1)

        provs_found = pd.read_csv(provider_path, dtype=str, keep_default_na=False)

        prov_matches = provs_found[provs_found["PRVDR_NPI_NUM"] == provider_npi]

        return {} if prov_matches.empty else prov_matches.iloc[0]

    def read_sig_line(self, clm_dt_sgntr_sk: str) -> dict[str, str]:
        clm_sig_path = f"{self.source_directory}/SYNTHETIC_CLM_DT_SGNTR.csv"

        if not Path(clm_sig_path).exists():
            return {}

        clm_sig_found = pd.read_csv(clm_sig_path, dtype=str, keep_default_na=False)

        clm_sig_matches = clm_sig_found[clm_sig_found["CLM_DT_SGNTR_SK"] == clm_dt_sgntr_sk]

        return {} if clm_sig_matches.empty else clm_sig_matches.iloc[0]

    def read_pmp(self, clm_sbmtr_cntrct_num: str, clm_sbmtr_cntrct_pbp_num: str) -> dict[str, str]:
        ctr_pmp_path = f"{self.source_directory}/SYNTHETIC_CNTRCT_PBP_NUM.csv"

        if not Path(ctr_pmp_path).exists():
            print("Contract PBP file not found. Run the generator or this will not go well.")
            sys.exit(1)

        ctr_pmp_found = pd.read_csv(ctr_pmp_path, dtype=str, keep_default_na=False)

        ctr_pmp_matches = ctr_pmp_found[
            (ctr_pmp_found["CNTRCT_NUM"] == clm_sbmtr_cntrct_num)
            & (ctr_pmp_found["CNTRCT_PBP_NUM"] == clm_sbmtr_cntrct_pbp_num)
        ]

        return {} if ctr_pmp_matches.empty else ctr_pmp_matches.iloc[0]

    def read_instnl(self, clm_row: dict[str, str]) -> dict[str, str]:
        clm_instnl = f"{self.source_directory}/SYNTHETIC_CLM_INSTNL.csv"

        if not Path(clm_instnl).exists():
            return {}

        clm_instnl_found = pd.read_csv(clm_instnl, dtype=str, keep_default_na=False)

        clm_instnl_matches = clm_instnl_found[
            (clm_instnl_found["GEO_BENE_SK"] == clm_row["GEO_BENE_SK"])
            & (clm_instnl_found["CLM_DT_SGNTR_SK"] == clm_row["CLM_DT_SGNTR_SK"])
            & (clm_instnl_found["CLM_TYPE_CD"] == clm_row["CLM_TYPE_CD"])
            & (clm_instnl_found["CLM_NUM_SK"] == clm_row["CLM_NUM_SK"])
        ]

        return {} if clm_instnl_matches.empty else clm_instnl_matches.iloc[0]

    def read_line(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        line_path = f"{self.source_directory}/SYNTHETIC_CLM_LINE.csv"

        if not Path(line_path).exists():
            print("Claim Line file not found. Run the generator or this will not go well.")
            sys.exit(1)

        line_found = pd.read_csv(line_path, dtype=str, keep_default_na=False)

        return line_found[
            (line_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
            & (line_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
            & (line_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
            & (line_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
        ].to_dict(orient="records")

    def read_rx_line(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        line_rx_path = f"{self.source_directory}/SYNTHETIC_CLM_LINE_RX.csv"

        if not Path(line_rx_path).exists():
            print("Claim RX Line file not found. Run the generator or this will not go well.")
            sys.exit(1)

        line_rx_found = pd.read_csv(line_rx_path, dtype=str, keep_default_na=False)

        line_rx_matches = line_rx_found[
            (line_rx_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
            & (line_rx_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
            & (line_rx_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
            & (line_rx_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
        ].to_dict(orient="records")

        return {} if not line_rx_matches else line_rx_matches[0]

    def read_prod_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        line_prod_path = f"{self.source_directory}/SYNTHETIC_CLM_PROD.csv"

        if not Path(line_prod_path).exists():
            return []

        line_prod_found = pd.read_csv(line_prod_path, dtype=str, keep_default_na=False)

        return line_prod_found[
            (line_prod_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
            & (line_prod_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
            & (line_prod_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
            & (line_prod_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
        ].to_dict(orient="records")

    def read_instnl_lines(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        line_instnl_path = f"{self.source_directory}/SYNTHETIC_CLM_LINE_INSTNL.csv"

        if not Path(line_instnl_path).exists():
            return []

        line_instnl_found = pd.read_csv(line_instnl_path, dtype=str, keep_default_na=False)

        return line_instnl_found[
            (line_instnl_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
            & (line_instnl_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
            & (line_instnl_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
            & (line_instnl_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
        ].to_dict(orient="records")

    def read_clm_val(self, clm_line: dict[str, str]) -> list[dict[str, str]]:
        line_val_path = f"{self.source_directory}/SYNTHETIC_CLM_VAL.csv"

        if not Path(line_val_path).exists():
            return []

        line_val_found = pd.read_csv(line_val_path, dtype=str, keep_default_na=False)

        return line_val_found[
            (line_val_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
            & (line_val_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
            & (line_val_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
            & (line_val_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
        ].to_dict(orient="records")

    # leaving here ended up not needing but future work probably will
    #def read_rlt_line(self, clm_rlt_cond_sgntr_sk: str) -> dict[str, str]:
    #        clm_rlt_path = f"{self.source_directory}/SYNTHETIC_CLM_RLT_COND_SGNTR_MBR.csv"
    #
    #        if not Path(clm_rlt_path).exists():
    #            return {}
    #
    #        clm_rlt_found = pd.read_csv(clm_rlt_path, dtype=str, keep_default_na=False)
    #
    #        clm_rlt_matches = clm_rlt_found[
    #            clm_rlt_found["CLM_RLT_COND_SGNTR_SK"] == clm_rlt_cond_sgntr_sk
    #        ]
    #
    #        return {} if clm_rlt_matches.empty else clm_rlt_matches.iloc[0]


def find_field_in_line_by_num(
    lines: list[dict[str, str]],
    clm_line: dict[str, str],
    field_name: str,
) -> str:
    if not lines:
        return ""

    clm_line_num = extract_col_str(clm_line, "CLM_LINE_NUM")

    line_matches = [
        line for line in lines
        if extract_col_str(line, "CLM_LINE_NUM") == clm_line_num
    ]

    if not line_matches:
        return ""

    return extract_col_str(line_matches[0], field_name)


def extract_col_str(row: dict[str, str], name: str) -> str:
    return str(row.get(name, "")).strip()
