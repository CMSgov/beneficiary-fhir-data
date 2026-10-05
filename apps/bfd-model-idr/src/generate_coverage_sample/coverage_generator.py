import json
import sys
from datetime import date
from enum import Enum, auto
from pathlib import Path
from typing import Any

import pandas as pd


class Result:
    def __init__(self, result_json: dict[str, Any], output_file: str):
        self.result_json = result_json
        self.output_file = output_file

    result_json: dict[str, Any]
    output_file: str


class Param:
    def __init__(self, field: str, value: str):
        self.field = field
        self.value = value

    field: str
    value: str


class MedicarePart(Enum):
    A = auto()
    B = auto()
    C = auto()
    D = auto()
    ALL = auto()


class SampleGenerator:
    def __init__(
        self,
        bene_sk: str,
        source_directory: str,
        output_directory: str,
        parts: list[MedicarePart],
        current_date: date,
    ):
        self.bene_sk = bene_sk
        self.source_directory = source_directory
        self.output_directory = output_directory
        self.parts = parts
        self.current_date = current_date

    def run(self) -> None:
        """Generate Coverage sample JSON from the given input directory."""
        if MedicarePart.ALL in self.parts:
            self.parts = [part for part in MedicarePart]
            self.parts.remove(MedicarePart.ALL)
        print(f"Source CSV Directory: {self.source_directory}")
        print(f"Output Directory: {self.output_directory}")
        print(f"Bene_SK: {self.bene_sk}")
        print(f"Medicare Parts: {', '.join(str(part.name) for part in self.parts)}")

        bene_hist = self.read_bene()

        for part in self.parts:
            match part:
                case MedicarePart.A:
                    self.create_coverage(
                        bene_hist=bene_hist, part=part, output_file_name="Coverage-FFS-Sample.json"
                    )
                case MedicarePart.B:
                    self.create_coverage(
                        bene_hist=bene_hist,
                        part=part,
                        output_file_name="Coverage-FFS-Sample-PartB.json",
                    )
                case MedicarePart.C:
                    self.create_coverage(
                        bene_hist=bene_hist,
                        part=part,
                        output_file_name="Coverage-PartC-Sample.json",
                    )
                case MedicarePart.D:
                    self.create_coverage(
                        bene_hist=bene_hist,
                        part=part,
                        output_file_name="Coverage-PartD-Sample.json",
                    )
                case MedicarePart.DUAL:
                    self.create_coverage(
                        bene_hist=bene_hist, part=part, output_file_name="Coverage-Dual-Sample.json"
                    )

    def create_coverage(
        self, bene_hist: dict[str, str], part: MedicarePart, output_file_name: str
    ) -> None:

        json_output = {
            "resourceType": "CoverageBase",
            "coveragePart": part.name,
            "BENE_MBI_ID": extract_col_str(bene_hist, "BENE_MBI_ID"),
            "XREF_EFCTV_BENE_SK": extract_col_str(bene_hist, "BENE_SK"),
            "lastUpdated": extract_col_str(bene_hist, "IDR_UPDT_TS"),
            # This is literally right now. Progam has an override for allowing dynamic date settings
            "currentDate": str(self.current_date),
            "BENE_ENRLMT_BGN_DT": "2018-04-11",
            "BENE_ENRLMT_END_DT": "9999-12-31",
            "BENE_CNTRCT_NUM": "H1234",
            "BENE_PBP_NUM": "001",
            "BENE_CVRG_TYPE_CD": "3",
            "CNTRCT_PBP_NAME": "Sample Medicare Advantage Plan",
            "CNTRCT_PLAN_CNTCT_TEL_NUM": "1-800-555-1234",
            "CNTRCT_PBP_SGMT_NUM": "000",
            "BENE_PDP_ENRLMT_MMBR_ID_NUM": "M123456789",
            "BENE_PDP_ENRLMT_GRP_NUM": "G987654321",
            "BENE_PDP_ENRLMT_PRCSR_NUM": "P123456",
            "BENE_PDP_ENRLMT_BANK_ID_NUM": "BIN123456",
            "BENE_CMBND_DEEMD_IND": "Y",
            "BENE_CMBND_DEEMD_COPMT_LVL_ID": "1",
            "BENE_CMBND_DEEMD_PRM_PCT": "100.0",
            "BENE_ENRLMT_EMPLR_SBSDY_SW": "Y",
            "BENE_MDCR_STUS_CD": "31",
            "BENE_BUYIN_CD": "A",
            "BENE_RNG_BGN_DT": "2018-04-11",
            "BENE_RNG_END_DT": "9999-12-31",
            "BENE_MDCR_ENTLMT_STUS_CD": "E",
            "BENE_MDCR_ENRLMT_RSN_CD": "I",
            "BENE_MDCR_ENTLMT_RSN_CD": "2",
            "BENE_MDCD_ELGBLTY_BGN_DT": "2018-04-11",
            "BENE_MDCD_ELGBLTY_END_DT": "9999-12-31",
            "BENE_DUAL_STUS_CD": "01",
            "BENE_DUAL_TYPE_CD": "P",
            "GEO_USPS_STATE_CD": "TX",
        }

        self.write_file(Result(result_json=json_output, output_file=output_file_name))

    def write_file(self, result: Result) -> None:
        output_path = Path(f"{self.output_directory}/{result.output_file}")
        output_path.parent.mkdir(parents=True, exist_ok=True)

        print(f"Writing to {self.output_directory}/{result.output_file}")
        with output_path.open(mode="w", encoding="utf-8") as f:
            json.dump(result.result_json, f, indent=2)

        print(f"Successfully generated sample JSON: {result.output_file}")

    def read_multi_line_file(
        self, file_name: str, not_found_fail: bool, params: list[Param]
    ) -> list[dict[str, str]]:
        file_path = f"{self.source_directory}/{file_name}.csv"
        if not Path(file_path).exists():
            print(f"File {file_name} not found. Run the generator may have issues.")
            if not_found_fail:
                sys.exit(1)
            else:
                return []

        records_found = pd.read_csv(file_path, dtype=str, keep_default_na=False)

        param_matches = [records_found[param.field] == param.value for param in params]
        record_matches = (
            records_found[pd.concat(param_matches, axis=1).all(axis=1)]
            if param_matches
            else records_found
        )

        if not_found_fail and record_matches.empty:
            print(f"No record found for file name: {file_name}")
            sys.exit(1)

        return [] if record_matches.empty else record_matches.to_dict(orient="records")  # type: ignore[reportCallIssue]

    def read_single_line_file(
        self, file_name: str, not_found_fail: bool, params: list[Param]
    ) -> dict[str, str]:
        records = self.read_multi_line_file(file_name, not_found_fail, params)
        return {} if not records else records[0]

    def read_bene(self) -> dict[str, str]:
        return self.read_single_line_file(
            "SYNTHETIC_BENE_HSTRY",
            True,
            [Param("BENE_SK", self.bene_sk), Param("IDR_LTST_TRANS_FLG", "Y")],
        )


def extract_col_str(row: dict[str, str], name: str) -> str | None:
    return str(row.get(name, "")).strip() or None


def extract_col_bool(row: dict[str, str], name: str) -> str:
    val = row.get(name, "").strip().upper()

    if val in ("TRUE", "1", "YES", "Y"):
        return "true"

    return "false"
