import argparse
import json
import os
import sys
from datetime import datetime, timezone
import pandas as pd

#we should only really need this for prior auth, since PA doesn't have it. 
def find_bene_sk(mbi_num: str) -> str:
    bene_history_path = "out/SYNTHETIC_BENE_HSTRY.csv"
    if os.path.exists(bene_history_path):
        df_bene = pd.read_csv(bene_history_path, dtype=str, keep_default_na=False)
        match = df_bene[df_bene["BENE_MBI_ID"] == mbi_num]
        if not match.empty:
            return match.iloc[0].get("BENE_SK", "123456789")

    # Try mapping using BENE_MBI_ID table if history doesn't have it
    mbi_table_path = "out/SYNTHETIC_BENE_MBI_ID.csv"
    if os.path.exists(mbi_table_path):
        df_mbi = pd.read_csv(mbi_table_path, dtype=str, keep_default_na=False)
        match = df_mbi[df_mbi["BENE_MBI_ID"] == mbi_num]
        if not match.empty:
            return match.iloc[0].get("BENE_SK", "123456789")

    return "123456789"


def main():
    parser = argparse.ArgumentParser(
        description="Generate a prior auth sample JSON from SYNTHETIC_PRAUC.csv based on UTN."
    )
    parser.add_argument("--utn", required=True, help="Pass the UTN ")
    args = parser.parse_args()

    prauc_path = "out/SYNTHETIC_PRAUC.csv"
    if not os.path.exists(prauc_path):
        print(f"Run the generator or this will not go well.")
        sys.exit(1)

    df_prauc = pd.read_csv(prauc_path, dtype=str, keep_default_na=False)

    # Find matches for the specified UTN
    utn_matches = df_prauc[df_prauc["UTN"] == args.utn]
    if utn_matches.empty:
        print(f"Error: No records found for UTN: {args.utn}")
        sys.exit(1)

    # Extract target MBI from the first match
    target_mbi = utn_matches.iloc[0]["MBI_NUM"]

    # Filter all rows matching target_mbi and UTN
    matching_rows_df = df_prauc[(df_prauc["UTN"] == args.utn) & (df_prauc["MBI_NUM"] == target_mbi)].copy()

    # Sort matching rows by CURRENT_SEGMENT (cast to int to sort correctly)
    matching_rows_df["CURRENT_SEGMENT"] = matching_rows_df["CURRENT_SEGMENT"].astype(int)
    matching_rows_df = matching_rows_df.sort_values(by="CURRENT_SEGMENT")

    # Get bene_sk
    bene_sk = find_bene_sk(target_mbi)

    # First row for header information
    first_row = matching_rows_df.iloc[0]

    billing_npi = str(first_row.get("NPI", "")).strip()

    prior_auth_items = []
    # Build prior_auth_items list of dicts
    for _, row in matching_rows_df.iterrows():
        current_seg = str(row["CURRENT_SEGMENT"])
        item = {
            "CURRENT_SEGMENT": current_seg,
        }
        for field in [
            "HCPCS_OR_CPT_OR_HIPPS",
            "PRICE_MOD1",
            "PRICE_MOD2",
            "PLACE_OF_SERV",
            "REV_CODE_1",
            "PA_DT_ADDED",
            "PA_DT_UPDATED",
            "PA_DECISION",
            "PA_REQ_SUB_DT",
            "PA_REQ_REC_DT",
            "PA_DECISION_DT",
            "PA_DECISION_EXP_DT",
            "SERVICE_CNTS",
            "SVC_RENDER_ST",
            "MR_COUNT_IND",
            "MR_COUNT_ST_DT",
            "MR_COUNT_END_DT",
            "RRB_EXCL_IND",
        ]:
            val = str(row.get(field, "")).strip()
            if val:
                item[field] = val
        prior_auth_items.append(item)

    # Determine derivedOutcome based on segment decisions
    has_pending = any(str(row.get("PA_DECISION", "")).strip() == "P" for _, row in matching_rows_df.iterrows())
    derived_outcome = "partial" if has_pending else "complete"

    # Now we build our actual sample data.
    output_json = {
        "resourceType": "ExplanationOfBenefit-PriorAuth",
        "id": f"pa-{args.utn.replace('-', '')}",
        "createdDate": str(first_row.get("PA_DT_UPDATED", "")).strip(),
        "derivedOutcome": derived_outcome,
        "beneficiarySk": bene_sk,
        "CLM_TYPE": str(first_row.get("CLM_TYPE", "")).strip(),
        "UTN": args.utn,
        "ICN_DCN": str(first_row.get("ICN_DCN", "")).strip(),
        "UTN_VALID_ST_DT": str(first_row.get("UTN_VALID_ST_DT", "")).strip(),
        "UTN_VALID_EN_DT": str(first_row.get("UTN_VALID_EN_DT", "")).strip(),
        "MAC_ID": str(first_row.get("MAC_ID", "")).strip(),
        "TOB": str(first_row.get("TOB", "")).strip(),
        "NPI": billing_npi,
        "NAME": str(first_row.get("NAME", "")).strip(),
        "CMS_CERT": str(first_row.get("CMS_CERT", "")).strip(),
        
        # Avoid futzing too much with augment_sample_resources
        "PRVDR_BLG_PRVDR_NPI_NUM": billing_npi,
        "PRVDR_ATNDG_PRVDR_NPI_NUM": str(first_row.get("ATT_PHY_NPI", "")).strip(),
        "PRVDR_RFRG_PRVDR_NPI_NUM": str(first_row.get("ORDER_REFER_NPI", "")).strip(),
        "PRVDR_RNDRNG_PRVDR_NPI_NUM": str(first_row.get("RENDER_NPI", "")).strip(),
        "PRVDR_OPRTG_PRVDR_NPI_NUM": str(first_row.get("OPERATE_NPI", "")).strip(),
        "CLM_BLG_PRVDR_OSCAR_NUM": str(first_row.get("CMS_CERT", "")).strip(),

        "priorAuthItem": prior_auth_items
    }

    output_file = "sample-data/EOB-PriorAuth-Sample.json"

    with open(output_file, mode="w", encoding="utf-8") as f:
        json.dump(output_json, f, indent=2)

    print(f"Successfully generated sample JSON: {output_file}")


if __name__ == "__main__":
    main()
