import json
import sys
from pathlib import Path

import pandas as pd


class SampleGenerator:
    def __init__(self, source_directory: str, output_directory: str):
        self.source_directory = source_directory
        self.output_directory = output_directory

    def run(self, bene_sk: str) -> None:
        """Generate Bene samp sample JSON from given source directory."""
        print(f"Generating for bene_sk: {bene_sk}")
        print(f"Source Directory: {self.source_directory}")
        print(f"Target Directory: {self.output_directory}")

        if not Path(self.source_directory).exists():
            print("Source directory not found. Run the generator or this will not go well.")
            sys.exit(1)

        bene_line = self.read_bene_hist(bene_sk=bene_sk)

        bene_xref_efctv_sk = extract_col_str(bene_line, "BENE_XREF_EFCTV_SK")

        mbi_lines = [
            {
                "BENE_MBI_ID": extract_col_str(mbi_line, "BENE_MBI_ID"),
                "BENE_MBI_EFCTV_DT": extract_col_str(mbi_line, "BENE_MBI_EFCTV_DT"),
                "BENE_MBI_OBSLT_DT": extract_col_str(mbi_line, "BENE_MBI_OBSLT_DT"),
            }
            for mbi_line in self.read_mbi_lines(bene_hist_line=bene_line)
        ]

        result_json = {
            "resourceType": "Beneficiary",
            "BENE_SK": bene_sk,
            "BENE_1ST_NAME": extract_col_str(bene_line, "BENE_1ST_NAME"),
            "BENE_LAST_NAME": extract_col_str(bene_line, "BENE_LAST_NAME"),
            "BENE_MIDL_NAME": extract_col_str(bene_line, "BENE_MIDL_NAME"),
            "BENE_BRTH_DT": extract_col_str(bene_line, "BENE_BRTH_DT"),
            "BENE_DEATH_DT": extract_col_str(bene_line, "BENE_DEATH_DT"),
            "BENE_VRFY_DEATH_DAY_SW": extract_col_bool(bene_line, "BENE_VRFY_DEATH_DAY_SW"),
            "mbi": mbi_lines,
            "BENE_XREF_EFCTV_SK": bene_xref_efctv_sk,
            "ALL_BENE_SKs": [bene_sk]
            if bene_sk == bene_xref_efctv_sk
            else [bene_sk, bene_xref_efctv_sk],
            "GEO_USPS_STATE_CD": extract_col_str(bene_line, "GEO_USPS_STATE_CD"),
            "GEO_ZIP5_CD": extract_col_str(bene_line, "GEO_ZIP5_CD"),
            "BENE_LINE_1_ADR": extract_col_str(bene_line, "BENE_LINE_1_ADR"),
            "BENE_LINE_2_ADR": extract_col_str(bene_line, "BENE_LINE_2_ADR"),
            "BENE_LINE_3_ADR": extract_col_str(bene_line, "BENE_LINE_3_ADR"),
            "BENE_LINE_4_ADR": extract_col_str(bene_line, "BENE_LINE_4_ADR"),
            "BENE_LINE_5_ADR": extract_col_str(bene_line, "BENE_LINE_5_ADR"),
            "BENE_LINE_6_ADR": extract_col_str(bene_line, "BENE_LINE_6_ADR"),
            "GEO_ZIP_PLC_NAME": extract_col_str(bene_line, "GEO_ZIP_PLC_NAME"),
            "CNTCT_LANG_CD": extract_col_str(bene_line, "CNTCT_LANG_CD"),
            "BENE_SEX_CD": extract_col_str(bene_line, "BENE_SEX_CD"),
            "BENE_RACE_CD": extract_col_str(bene_line, "BENE_RACE_CD"),
        }

        output_file_name = "Beneficiary-Sample.json"
        output_path = Path(f"{self.output_directory}/{output_file_name}")
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with output_path.open(mode="w", encoding="utf-8") as f:
            json.dump(result_json, f, indent=2)

        print(f"Successfully generated sample JSON: {output_file_name}")

    def read_bene_hist(self, bene_sk: str) -> dict[str, str]:
        file_path = f"{self.source_directory}/SYNTHETIC_BENE_HSTRY.csv"
        if not Path(file_path).exists():
            print(
                "SYNTHETIC_BENE_HSTRY file not found. Run the generator or this will not go well."
            )
            sys.exit(1)

        records_found = pd.read_csv(file_path, dtype=str, keep_default_na=False)

        record_matches = records_found[records_found["BENE_SK"] == bene_sk]

        if record_matches.empty:
            print(f"No claims found for claim unique ID: {bene_sk}")
            sys.exit(1)

        return record_matches.iloc[0]

    def read_mbi_lines(self, bene_hist_line: dict[str, str]) -> list[dict[str, str]]:
        file_path = f"{self.source_directory}/SYNTHETIC_BENE_MBI_ID.csv"

        if not Path(file_path).exists():
            return []

        records_found = pd.read_csv(file_path, dtype=str, keep_default_na=False)

        return records_found[
            (records_found["BENE_MBI_ID"] == bene_hist_line["BENE_MBI_ID"])
        ].to_dict("records")


def extract_col_str(row: dict[str, str], name: str) -> str | None:
    return str(row.get(name, "")).strip() or None


def extract_col_bool(row: dict[str, str], name: str) -> str:
    val = row.get(name, "").strip().upper()

    if val in ("TRUE", "1", "YES", "Y"):
        return "true"

    return "false"
