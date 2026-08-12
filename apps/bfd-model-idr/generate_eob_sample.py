import json
import sys
from pathlib import Path
from typing import Any

import click
import pandas as pd


@click.command
@click.option(
    '--clm-uniq-id',
    type=str, 
    required=True, 
    help='Pass the claim unique ID.'
)
def main(clm_uniq_id:str):
    """Generate EOB sample JSON from SYNTHETIC_EOB.csv based on claim unique ID."""
    claim_row = read_clm(clm_uniq_id)
    claim_type = int(claim_row.get("CLM_TYPE_CD"))

    match claim_type:
        case 1 | 2 | 3 | 4:
            result = create_pharmacy(clm_uniq_id,claim_row)
        case _:
            print("Unknown Type")
            sys.exit(1)
    
    with Path(result.output_file).open(mode="w", encoding="utf-8") as f:
        json.dump(result.result_json, f, indent=2)
    
    print(f"Successfully generated sample JSON: {result.output_file}")

class Result: 
    result_json: dict[str, Any]
    output_file: str


def create_pharmacy(clm_uniq_id: Any,claim_row: Any) -> Result:
    
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
        "lastUpdated": extract_col_str(claim_row,"IDR_UPDT_TS"),
        "CLM_FINL_ACTN_IND": extract_col_str(claim_row,"CLM_FINL_ACTN_IND"),
        "CLM_SRC_ID": extract_col_str(claim_row,"CLM_SRC_ID"),
        "BENE_SK": extract_col_str(claim_row,"BENE_SK"),
        "CLM_TYPE_CD": int(extract_col_str(claim_row,"CLM_TYPE_CD")),
        "CLM_UNIQ_ID": extract_col_str(claim_row,"CLM_UNIQ_ID"),
        "CLM_CNTL_NUM": extract_col_str(claim_row,"CLM_CNTL_NUM"),
        "CLM_ORIG_CNTL_NUM": extract_col_str(claim_row,"CLM_ORIG_CNTL_NUM"),
        "CLM_FROM_DT": extract_col_str(claim_row,"CLM_FROM_DT"),
        "CLM_THRU_DT": extract_col_str(claim_row,"CLM_THRU_DT"),
        "CLM_EFCTV_DT": extract_col_str(claim_row,"CLM_EFCTV_DT"),
        "CLM_SRVC_PRVDR_GNRC_ID_NUM": extract_col_str(claim_row,"CLM_SRVC_PRVDR_GNRC_ID_NUM"),
        "CLM_PD_DT": extract_col_str(claim_row,"CLM_PD_DT"),
        "PRVDR_PRSCRBNG_PRVDR_NPI_NUM": provider_npi,
        "CLM_PRSBNG_PRVDR_GNRC_ID_NUM": extract_col_str(claim_row,"CLM_PRSBNG_PRVDR_GNRC_ID_NUM"),
        "PRVDR_PRSBNG_ID_QLFYR_CD": extract_col_str(claim_row,"PRVDR_PRSBNG_ID_QLFYR_CD"),
        "PRVDR_LAST_NAME": extract_col_str(prov_row,"PRVDR_LAST_NAME"),
        "CNTRCT_PBP_NAME": extract_col_str(ctr_pmp_row,"CNTRCT_PBP_NAME"),
        "CLM_BENE_PMT_AMT": extract_col_str(claim_row,"CLM_BENE_PMT_AMT"),
        "CLM_OTHR_TP_PD_AMT": extract_col_str(claim_row,"CLM_OTHR_TP_PD_AMT"),
        "META_SRC_SK": extract_col_str(claim_row,"META_SRC_SK"),
        "PRVDR_SRVC_ID_QLFYR_CD": extract_col_str(claim_row,"PRVDR_SRVC_ID_QLFYR_CD"),
        "supportingInfoComponents": [],
        "lineItemComponents": [
            {
                "CLM_LINE_NUM": extract_col_str(clm_line,"CLM_LINE_NUM"),
                "CLM_LINE_FROM_DT": extract_col_str(clm_line,"CLM_LINE_FROM_DT"),
                "CLM_LINE_NDC_CD": extract_col_str(clm_line,"CLM_LINE_NDC_CD"),
                "CLM_LINE_NDC_QTY": extract_col_str(clm_line,"CLM_LINE_NDC_QTY"),
                "CLM_LINE_NDC_QTY_QLFYR_CD": extract_col_str(
                    clm_line,"CLM_LINE_NDC_QTY_QLFYR_CD"),
                "CLM_LINE_CVRD_PD_AMT": extract_col_str(clm_line,"CLM_LINE_CVRD_PD_AMT"),
                "CLM_LINE_GRS_ABOVE_THRSHLD_AMT": extract_col_str(rx_line,
                    "CLM_LINE_GRS_ABOVE_THRSHLD_AMT"),
                "CLM_LINE_GRS_BLW_THRSHLD_AMT": extract_col_str(
                    rx_line,"CLM_LINE_GRS_BLW_THRSHLD_AMT"),
                "CLM_LINE_LIS_AMT": extract_col_str(rx_line,"CLM_LINE_LIS_AMT"),
                "CLM_LINE_TROOP_TOT_AMT": extract_col_str(rx_line,"CLM_LINE_TROOP_TOT_AMT"),
                "CLM_LINE_PLRO_AMT": extract_col_str(rx_line,"CLM_LINE_PLRO_AMT"),
                "CLM_RPTD_MFTR_DSCNT_AMT": extract_col_str(rx_line,"CLM_RPTD_MFTR_DSCNT_AMT"),
                "CLM_LINE_INGRDNT_CST_AMT": extract_col_str(rx_line,"CLM_LINE_INGRDNT_CST_AMT"),
                "CLM_LINE_SRVC_CST_AMT": extract_col_str(rx_line,"CLM_LINE_SRVC_CST_AMT"),
                "CLM_LINE_SLS_TAX_AMT": extract_col_str(rx_line,"CLM_LINE_SLS_TAX_AMT"),
                "CLM_LINE_VCCN_ADMIN_FEE_AMT": extract_col_str(rx_line,"CLM_LINE_VCCN_ADMIN_FEE_AMT"),
                "CLM_PRCNG_EXCPTN_CD": extract_col_str(rx_line,"CLM_PRCNG_EXCPTN_CD"),
                "CLM_LINE_BENE_PMT_AMT": extract_col_str(clm_line,"CLM_LINE_BENE_PMT_AMT"),
                "CLM_CMS_CALCD_MFTR_DSCNT_AMT": extract_col_str(rx_line,
                    "CLM_CMS_CALCD_MFTR_DSCNT_AMT"),
                "CLM_LINE_GRS_CVRD_CST_TOT_AMT": extract_col_str(rx_line,
                    "CLM_LINE_GRS_CVRD_CST_TOT_AMT"),
                "CLM_LINE_REBT_PASSTHRU_POS_AMT": extract_col_str(rx_line,
                    "CLM_LINE_REBT_PASSTHRU_POS_AMT"),
                "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT": extract_col_str(rx_line,
                    "CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT"),
                "CLM_LINE_OTHR_TP_PD_AMT": extract_col_str(clm_line,
                    "CLM_LINE_OTHR_TP_PD_AMT"),
                "CLM_LINE_NCVRD_PD_AMT": extract_col_str(clm_line,
                    "CLM_LINE_NCVRD_PD_AMT"),
                "isCompound": str(extract_col_str(rx_line,"CLM_CMPND_CD") == "2").lower(),
                "CLM_LINE_RPTD_GAP_DSCNT_AMT": extract_col_str(rx_line,
                    "CLM_LINE_RPTD_GAP_DSCNT_AMT"),    
                "CLM_LINE_AUTHRZD_FILL_NUM": extract_col_str(rx_line,
                    "CLM_LINE_AUTHRZD_FILL_NUM"),     
                "CLM_PHRMCY_SRVC_TYPE_CD": extract_col_str(rx_line,
                    "CLM_PHRMCY_SRVC_TYPE_CD"),  
                "CLM_LINE_RX_ORGN_CD": extract_col_str(rx_line,
                    "CLM_LINE_RX_ORGN_CD"), 
                "CLM_BRND_GNRC_CD": extract_col_str(rx_line,
                    "CLM_BRND_GNRC_CD"), 
                "CLM_PTNT_RSDNC_CD": extract_col_str(rx_line,
                    "CLM_PTNT_RSDNC_CD"), 
                "CLM_LTC_DSPNSNG_MTHD_CD": extract_col_str(rx_line,
                    "CLM_LTC_DSPNSNG_MTHD_CD"), 
                "CLM_CMPND_CD": extract_col_str(rx_line,"CLM_CMPND_CD"), 
                "CLM_LINE_DAYS_SUPLY_QTY": extract_col_str(rx_line,"CLM_LINE_DAYS_SUPLY_QTY"), 
                "CLM_LINE_RX_FILL_NUM": extract_col_str(rx_line,"CLM_LINE_RX_FILL_NUM"), 
                "CLM_DAW_PROD_SLCTN_CD": extract_col_str(rx_line,"CLM_DAW_PROD_SLCTN_CD"), 
                "CLM_DRUG_CVRG_STUS_CD": extract_col_str(rx_line,"CLM_DRUG_CVRG_STUS_CD"), 
                "CLM_CTSTRPHC_CVRG_IND_CD": extract_col_str(rx_line,"CLM_CTSTRPHC_CVRG_IND_CD"), 
                "CLM_LINE_RX_NUM": extract_col_str(clm_line,"CLM_LINE_RX_NUM"),
                "CLM_DSPNSNG_STUS_CD": extract_col_str(rx_line,"CLM_DSPNSNG_STUS_CD"), 
            }
        ],
        "CLM_CMS_PROC_DT": extract_col_str(clm_sig_row,"CLM_CMS_PROC_DT"),
        "CLM_IDR_LD_DT ": extract_col_str(claim_row,"CLM_IDR_LD_DT"),
        "CLM_ADJSTMT_TYPE_CD": extract_col_str(claim_row,"CLM_ADJSTMT_TYPE_CD"),
        "CLM_SBMT_FRMT_CD": extract_col_str(claim_row,"CLM_SBMT_FRMT_CD"),
        "CLM_SBMTR_CNTRCT_NUM": extract_col_str(claim_row,"CLM_SBMTR_CNTRCT_NUM"),
        "CLM_SBMTR_CNTRCT_PBP_NUM": extract_col_str(claim_row,"CLM_SBMTR_CNTRCT_PBP_NUM"),
        "CLM_DT_SGNTR_SK": extract_col_str(clm_sig_row,"CLM_DT_SGNTR_SK"),
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


def extract_col_str(row: Any,name: str) -> str:
    return str(row.get(name, "")).strip()

    
if __name__ == "__main__":
    main()
