import argparse
import json
import sys
from pathlib import Path
from typing import Any

import pandas as pd


def main():
    parser = argparse.ArgumentParser(
            description="Generate EOB sample JSON from SYNTHETIC_EOB.csv based on claim unique ID."
        )
    parser.add_argument("--clm_uniq_id", required=True, help="Pass the claim unique ID ")
    parser.add_argument("--eob_type", required=True, help="EOB Type")
    args = parser.parse_args()

    if args.eob_type == "Pharmacy":
        result = create_pharmacy(args.clm_uniq_id)
    else:
        print("Unknown Type")
        sys.exit(1)
    
    with Path(result.output_file).open(mode="w", encoding="utf-8") as f:
        json.dump(result.result_json, f, indent=2)
    
    print(f"Successfully generated sample JSON: {result.output_file}")

class Result: 
    result_json: dict[str, Any]
    output_file: str


def create_pharmacy(clm_uniq_id: Any) -> Result:
    claim_row = read_clm(clm_uniq_id)
    
    provider_npi = str(claim_row.get("PRVDR_PRSCRBNG_PRVDR_NPI_NUM", "")).strip()

    prov_row = read_provider(provider_npi)

    clm_sig_row = read_sig_line(str(claim_row.get("CLM_DT_SGNTR_SK", "")).strip())

    clm_sbmtr_cntrct_num = str(claim_row.get("CLM_SBMTR_CNTRCT_NUM", "")).strip()
    clm_sbmtr_cntrct_pbp_num = str(claim_row.get("CLM_SBMTR_CNTRCT_PBP_NUM", "")).strip()

    ctr_pmp_row = read_pmp(clm_sbmtr_cntrct_num,clm_sbmtr_cntrct_pbp_num)

    clm_lines = read_line(claim_row)

    clm_line = {} if clm_lines.empty else clm_lines.iloc[0]

    rx_line = read_rx_line(claim_row)

    output_json = {
        "resourceType": "ExplanationOfBenefit-Pharmacy",
        "id": str(clm_uniq_id.replace('-', '')).strip(),
        "lastUpdated": str(claim_row.get("IDR_UPDT_TS", "")).strip(),
        "CLM_FINL_ACTN_IND": str(claim_row.get("CLM_FINL_ACTN_IND", "")).strip(),
        "CLM_SRC_ID": str(claim_row.get("CLM_SRC_ID", "")).strip(),
        "BENE_SK": str(claim_row.get("BENE_SK", "")).strip(),
        "CLM_TYPE_CD": int(str(claim_row.get("CLM_TYPE_CD", "")).strip()),
        "CLM_UNIQ_ID": str(claim_row.get("CLM_UNIQ_ID", "")).strip(),
        "CLM_CNTL_NUM": str(claim_row.get("CLM_CNTL_NUM", "")).strip(),
        "CLM_ORIG_CNTL_NUM": str(claim_row.get("CLM_ORIG_CNTL_NUM", "")).strip(),
        "CLM_FROM_DT": str(claim_row.get("CLM_FROM_DT", "")).strip(),
        "CLM_THRU_DT": str(claim_row.get("CLM_THRU_DT", "")).strip(),
        "CLM_EFCTV_DT": str(claim_row.get("CLM_EFCTV_DT", "")).strip(),
        "CLM_SRVC_PRVDR_GNRC_ID_NUM": str(claim_row.get("CLM_SRVC_PRVDR_GNRC_ID_NUM", "")).strip(),
        "CLM_PD_DT": str(claim_row.get("CLM_PD_DT", "")).strip(),
        "PRVDR_PRSCRBNG_PRVDR_NPI_NUM": provider_npi,
        "CLM_PRSBNG_PRVDR_GNRC_ID_NUM": str(claim_row.get("CLM_PRSBNG_PRVDR_GNRC_ID_NUM", ""))
            .strip(),
        "PRVDR_PRSBNG_ID_QLFYR_CD": str(claim_row.get("PRVDR_PRSBNG_ID_QLFYR_CD", "")).strip(),
        "PRVDR_LAST_NAME": str(prov_row.get("PRVDR_LAST_NAME", "")).strip(),
        "CNTRCT_PBP_NAME": str(ctr_pmp_row.get("CNTRCT_PBP_NAME", "")).strip(),
        "CLM_BENE_PMT_AMT": str(claim_row.get("CLM_BENE_PMT_AMT", "")).strip(),
        "CLM_OTHR_TP_PD_AMT": str(claim_row.get("CLM_OTHR_TP_PD_AMT", "")).strip(),
        "META_SRC_SK": str(claim_row.get("META_SRC_SK", "")).strip(),
        "PRVDR_SRVC_ID_QLFYR_CD": str(claim_row.get("PRVDR_SRVC_ID_QLFYR_CD", "")).strip(),
        "supportingInfoComponents": [],
        "lineItemComponents": [
            {
                "CLM_LINE_NUM": str(clm_line.get("CLM_LINE_NUM", "")).strip(),
                "CLM_LINE_FROM_DT": str(clm_line.get("CLM_LINE_FROM_DT", "")).strip(),
                "CLM_LINE_NDC_CD": str(clm_line.get("CLM_LINE_NDC_CD", "")).strip(),
                "CLM_LINE_NDC_QTY": str(clm_line.get("CLM_LINE_NDC_QTY", "")).strip(),
                "CLM_LINE_NDC_QTY_QLFYR_CD": str(clm_line.get("CLM_LINE_NDC_QTY_QLFYR_CD", ""))
                    .strip(),
                "CLM_LINE_CVRD_PD_AMT": str(clm_line.get("CLM_LINE_CVRD_PD_AMT", "")).strip(),
                "CLM_LINE_GRS_ABOVE_THRSHLD_AMT": str(rx_line.get(
                    "CLM_LINE_GRS_ABOVE_THRSHLD_AMT", "")).strip(),
                "CLM_LINE_GRS_BLW_THRSHLD_AMT": str(rx_line.get(
                    "CLM_LINE_GRS_BLW_THRSHLD_AMT", "")).strip(),
                "CLM_LINE_LIS_AMT": str(rx_line.get("CLM_LINE_LIS_AMT", "")).strip(),
                "CLM_LINE_TROOP_TOT_AMT": str(rx_line.get("CLM_LINE_TROOP_TOT_AMT", "")).strip(),
                "CLM_LINE_PLRO_AMT": str(rx_line.get("CLM_LINE_PLRO_AMT", "")).strip(),
                "CLM_RPTD_MFTR_DSCNT_AMT": str(rx_line.get("CLM_RPTD_MFTR_DSCNT_AMT", "")).strip(),
                "CLM_LINE_INGRDNT_CST_AMT": str(rx_line.get("CLM_LINE_INGRDNT_CST_AMT", "")).strip(),
                "CLM_LINE_SRVC_CST_AMT": str(rx_line.get("CLM_LINE_SRVC_CST_AMT", "")).strip(),
                "CLM_LINE_SLS_TAX_AMT": str(rx_line.get("CLM_LINE_SLS_TAX_AMT", "")).strip(),
                "CLM_LINE_VCCN_ADMIN_FEE_AMT": str(rx_line.get("CLM_LINE_VCCN_ADMIN_FEE_AMT", ""))
                    .strip(),
                "CLM_PRCNG_EXCPTN_CD": str(rx_line.get("CLM_PRCNG_EXCPTN_CD", "")).strip(),
                "CLM_LINE_BENE_PMT_AMT": str(clm_line.get("CLM_LINE_BENE_PMT_AMT", "")).strip(),
                "CLM_CMS_CALCD_MFTR_DSCNT_AMT": str(rx_line.get(
                    "CLM_CMS_CALCD_MFTR_DSCNT_AMT", "")).strip(),
                "CLM_LINE_GRS_CVRD_CST_TOT_AMT": str(rx_line.get(
                    "CLM_LINE_GRS_CVRD_CST_TOT_AMT", "")).strip(),
                "CLM_LINE_REBT_PASSTHRU_POS_AMT": str(rx_line.get(
                    "CLM_LINE_REBT_PASSTHRU_POS_AMT", "")).strip(),
                "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT": str(rx_line.get(
                    "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT", "")).strip(),
                "CLM_LINE_OTHR_TP_PD_AMT": str(clm_line.get(
                    "CLM_LINE_OTHR_TP_PD_AMT", "")).strip(),
                "CLM_LINE_NCVRD_PD_AMT": str(clm_line.get(
                    "CLM_LINE_NCVRD_PD_AMT", "")).strip(),
                "CLM_LINE_RPTD_GAP_DSCNT_AMT": str(rx_line.get(
                    "CLM_LINE_RPTD_GAP_DSCNT_AMT", "")).strip(),    
                "CLM_LINE_AUTHRZD_FILL_NUM": str(rx_line.get(
                    "CLM_LINE_AUTHRZD_FILL_NUM", "")).strip(),     
                "CLM_PHRMCY_SRVC_TYPE_CD": str(rx_line.get(
                    "CLM_PHRMCY_SRVC_TYPE_CD", "")).strip(),  
                "CLM_LINE_RX_ORGN_CD": str(rx_line.get(
                    "CLM_LINE_RX_ORGN_CD", "")).strip(), 
                "CLM_BRND_GNRC_CD": str(rx_line.get(
                    "CLM_BRND_GNRC_CD", "")).strip(), 
                "CLM_PTNT_RSDNC_CD": str(rx_line.get(
                    "CLM_PTNT_RSDNC_CD", "")).strip(), 
                "CLM_LTC_DSPNSNG_MTHD_CD": str(rx_line.get(
                    "CLM_LTC_DSPNSNG_MTHD_CD", "")).strip(), 
                "CLM_CMPND_CD": str(rx_line.get("CLM_CMPND_CD", "")).strip(), 
                "CLM_LINE_DAYS_SUPLY_QTY": str(rx_line.get("CLM_LINE_DAYS_SUPLY_QTY", "")).strip(), 
                "CLM_LINE_RX_FILL_NUM": str(rx_line.get("CLM_LINE_RX_FILL_NUM", "")).strip(), 
                "CLM_DAW_PROD_SLCTN_CD": str(rx_line.get("CLM_DAW_PROD_SLCTN_CD", "")).strip(), 
                "CLM_DRUG_CVRG_STUS_CD": str(rx_line.get("CLM_DRUG_CVRG_STUS_CD", "")).strip(), 
                "CLM_CTSTRPHC_CVRG_IND_CD": str(rx_line.get("CLM_CTSTRPHC_CVRG_IND_CD", ""))
                    .strip(), 
                "CLM_LINE_RX_NUM": str(clm_line.get(
                    "CLM_LINE_RX_NUM", "")).strip(),
                "CLM_DSPNSNG_STUS_CD": str(rx_line.get("CLM_DSPNSNG_STUS_CD", "")).strip(), 
            }
        ],
        "CLM_CMS_PROC_DT": str(clm_sig_row.get("CLM_CMS_PROC_DT", "")).strip(),
        "CLM_IDR_LD_DT ": str(claim_row.get("CLM_IDR_LD_DT", "")).strip(),
        "CLM_ADJSTMT_TYPE_CD": str(claim_row.get("CLM_ADJSTMT_TYPE_CD", "")).strip(),
        "CLM_SBMT_FRMT_CD": str(claim_row.get("CLM_SBMT_FRMT_CD", "")).strip(),
        "CLM_SBMTR_CNTRCT_NUM": str(claim_row.get("CLM_SBMTR_CNTRCT_NUM", "")).strip(),
        "CLM_SBMTR_CNTRCT_PBP_NUM": str(claim_row.get("CLM_SBMTR_CNTRCT_PBP_NUM", "")).strip(),
        "CLM_DT_SGNTR_SK": str(clm_sig_row.get("CLM_DT_SGNTR_SK", "")).strip(),
    }

    result = Result()
    result.result_json = output_json
    result.output_file = "sample-data/EOB-Pharmacy-Sample.json"
    return result


def read_clm(clm_uniq_id: Any) -> Any:
    eob_path = "out/SYNTHETIC_CLM.csv"
    if not Path(eob_path).exists():
        print("EOB file not found. Run the generator or this will not go well.")
        sys.exit(1)

    claims_found = pd.read_csv(eob_path, dtype=str, keep_default_na=False)

    clm_matches = claims_found[claims_found["CLM_UNIQ_ID"] == clm_uniq_id]

    if clm_matches.empty:
        print(f"No claims found for claim unique ID: {clm_uniq_id}")
        sys.exit(1)

    return clm_matches.iloc[0]

def read_provider(provider_npi: Any) -> Any:
    provider_path = "out/SYNTHETIC_PRVDR_HSTRY.csv"
    
    if not Path(provider_path).exists():
        print("Provider file not found. Run the generator or this will not go well.")
        sys.exit(1)

    provs_found = pd.read_csv(provider_path, dtype=str, keep_default_na=False)

    prov_matches = provs_found[provs_found["PRVDR_NPI_NUM"] == provider_npi]

    return {} if prov_matches.empty else prov_matches.iloc[0]

def read_sig_line(clm_dt_sgntr_sk: Any) -> Any:
    clm_sig_path = "out/SYNTHETIC_CLM_DT_SGNTR.csv"
    
    if not Path(clm_sig_path).exists():
        print("Claim signature file not found. Run the generator or this will not go well.")
        sys.exit(1)

    clm_sig_found = pd.read_csv(clm_sig_path, dtype=str, keep_default_na=False)

    clm_sig_matches = clm_sig_found[clm_sig_found["CLM_DT_SGNTR_SK"] == clm_dt_sgntr_sk]

    return {} if clm_sig_matches.empty else clm_sig_matches.iloc[0]

def read_pmp(clm_sbmtr_cntrct_num:Any,clm_sbmtr_cntrct_pbp_num:Any) -> Any:
    ctr_pmp_path = "out/SYNTHETIC_CNTRCT_PBP_NUM.csv"
    
    if not Path(ctr_pmp_path).exists():
        print("Provider file not found. Run the generator or this will not go well.")
        sys.exit(1)

    ctr_pmp_found = pd.read_csv(ctr_pmp_path, dtype=str, keep_default_na=False)
    
    ctr_pmp_matches = ctr_pmp_found[
        (ctr_pmp_found["CNTRCT_NUM"] == clm_sbmtr_cntrct_num)
        & (ctr_pmp_found["CNTRCT_PBP_NUM"] == clm_sbmtr_cntrct_pbp_num)
    ]

    return {} if ctr_pmp_matches.empty else ctr_pmp_matches.iloc[0]

def read_line(clm_line:Any) -> Any:
    line_path = "out/SYNTHETIC_CLM_LINE.csv"
        
    if not Path(line_path).exists():
        print("Claim Line file not found. Run the generator or this will not go well.")
        sys.exit(1)

    line_found = pd.read_csv(line_path, dtype=str, keep_default_na=False)
    
    return line_found[
        (line_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
        & (line_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
        & (line_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
        & (line_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
    ]

def read_rx_line(clm_line:Any) -> Any:
    line_rx_path = "out/SYNTHETIC_CLM_LINE_RX.csv"
        
    if not Path(line_rx_path).exists():
        print("Claim RX Line file not found. Run the generator or this will not go well.")
        sys.exit(1)

    line_rx_found = pd.read_csv(line_rx_path, dtype=str, keep_default_na=False)
    
    line_rx_matches = line_rx_found[
        (line_rx_found["GEO_BENE_SK"] == clm_line["GEO_BENE_SK"])
        & (line_rx_found["CLM_DT_SGNTR_SK"] == clm_line["CLM_DT_SGNTR_SK"])
        & (line_rx_found["CLM_TYPE_CD"] == clm_line["CLM_TYPE_CD"])
        & (line_rx_found["CLM_NUM_SK"] == clm_line["CLM_NUM_SK"])
    ]

    return {} if line_rx_matches.empty else line_rx_matches.iloc[0]


    
if __name__ == "__main__":
    main()
