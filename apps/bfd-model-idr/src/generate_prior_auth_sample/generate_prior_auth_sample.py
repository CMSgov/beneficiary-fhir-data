import json
import sys
from pathlib import Path

import pandas as pd


# we should only really need this for prior auth, since PA doesn't have it.
def find_bene_sk(mbi_num: str, source_directory: str) -> str:
    bene_history_path = f"{source_directory}/SYNTHETIC_BENE_HSTRY.csv"
    if Path(bene_history_path).exists():
        df_bene = pd.read_csv(bene_history_path, dtype=str, keep_default_na=False)
        match = df_bene[df_bene["BENE_MBI_ID"] == mbi_num]
        if not match.empty:
            return match.iloc[0].get("BENE_SK", "123456789")

    # Try mapping using BENE_MBI_ID table if history doesn't have it
    mbi_table_path = f"{source_directory}/SYNTHETIC_BENE_MBI_ID.csv"
    if Path(mbi_table_path).exists():
        df_mbi = pd.read_csv(mbi_table_path, dtype=str, keep_default_na=False)
        match = df_mbi[df_mbi["BENE_MBI_ID"] == mbi_num]
        if not match.empty:
            return match.iloc[0].get("BENE_SK", "123456789")

    return "123456789"


def ensure_provider_history_exists(npis: set[str], source_directory: str) -> None:
    # TODO this is feels wrong and needs to be pathable.
    # Not sure what that looks like
    poc_path = "sample-data/PRVDR_HSTRY_POC.csv"
    synth_path = f"{source_directory}/SYNTHETIC_PRVDR_HSTRY.csv"
    if not Path(poc_path).exists() or not Path(synth_path).exists():
        return

    df_poc = pd.read_csv(poc_path, dtype=str, keep_default_na=False)
    existing_npis = set(df_poc["PRVDR_SK"])
    missing_npis = {n for n in npis if n} - existing_npis
    if not missing_npis:
        return

    df_synth = pd.read_csv(synth_path, dtype=str, keep_default_na=False)
    matches = df_synth[df_synth["PRVDR_SK"].isin(list(missing_npis))]
    if not matches.empty:
        new_rows = matches.loc[:, df_poc.columns.to_list()].drop_duplicates(subset="PRVDR_SK")
        pd.concat([df_poc, new_rows], ignore_index=True).to_csv(poc_path, index=False)


def run(utn: str, source_directory: str, output_directory: str):

    prauc_path = f"{source_directory}/SYNTHETIC_PRAUC.csv"
    if not Path(prauc_path).exists:
        print("Run the generator or this will not go well.")
        sys.exit(1)

    df_prauc = pd.read_csv(prauc_path, dtype=str, keep_default_na=False)

    # Find matches for the specified UTN
    utn_matches = df_prauc[df_prauc["UTN"] == utn]
    if utn_matches.empty:
        print(f"Error: No records found for UTN: {utn}")
        sys.exit(1)

    # Extract target MBI from the first match
    target_mbi = utn_matches.iloc[0]["MBI_NUM"]

    # Filter all rows matching target_mbi and UTN
    matching_rows_df = df_prauc[
        (df_prauc["UTN"] == utn) & (df_prauc["MBI_NUM"] == target_mbi)
    ].copy()

    # Sort matching rows by CURRENT_SEGMENT (cast to int to sort correctly)
    matching_rows_df["CURRENT_SEGMENT"] = matching_rows_df["CURRENT_SEGMENT"].astype(int)
    matching_rows_df = matching_rows_df.set_index("CURRENT_SEGMENT", drop=False).sort_index()

    # Collect provider NPIs and ensure they exist in sample-data/PRVDR_HSTRY_POC.csv
    npi_cols = [c for c in matching_rows_df.columns if "NPI" in c]
    npis = set(matching_rows_df[npi_cols].to_numpy().ravel())
    ensure_provider_history_exists(npis, source_directory=source_directory)

    # Get bene_sk
    bene_sk = find_bene_sk(target_mbi, source_directory=source_directory)

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
    has_pending = any(
        str(row.get("PA_DECISION", "")).strip() == "P" for _, row in matching_rows_df.iterrows()
    )
    derived_outcome = "partial" if has_pending else "complete"

    # Now we build our actual sample data.
    output_json = {
        "resourceType": "ExplanationOfBenefit-PriorAuth",
        "id": f"pa-{utn.replace('-', '')}",
        "createdDate": str(first_row.get("PA_DT_UPDATED", "")).strip(),
        "derivedOutcome": derived_outcome,
        "beneficiarySk": bene_sk,
        "CLM_TYPE": str(first_row.get("CLM_TYPE", "")).strip(),
        "UTN": utn,
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
        "priorAuthItem": prior_auth_items,
    }

    output_file = Path(f"{output_directory}/EOB-PriorAuth-Sample.json")
    output_file.parent.mkdir(parents=True, exist_ok=True)

    with output_file.open(mode="w", encoding="utf-8") as f:
        json.dump(output_json, f, indent=2)

    print(f"Successfully generated sample JSON: {output_file}")
