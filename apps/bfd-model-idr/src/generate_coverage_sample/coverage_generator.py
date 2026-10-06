import json
import sys
from datetime import date, datetime
from enum import Enum
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
    def __init__(self, value: int, code: str):
        self._value_ = value
        self.code = code

    A = (1, "A")
    B = (2, "B")
    C = (3, "C")
    D = (4, "D")
    DUAL = (5, "DUAL")
    ALL = (6, "")


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

        bene_entlmt = None
        bene_entlmt_rsn = None
        bene_map_d = None
        bene_map_d_rx = None
        bene_dual = None
        cntrct_pbp_num = None
        cntrct_pbp_cntct = None
        cntrct_pmp_sk = None
        cntrct_pbp_sgmt = None

        match part:
            case MedicarePart.A | MedicarePart.B:
                bene_entlmt = self.read_bene_entlmt(part)

                if not bene_entlmt:
                    print(f"Coverage not found for Medicare Part {part.name}.")
                    return

                bene_entlmt_rsn = self.read_bene_entlmt_rsn()
            case MedicarePart.C | MedicarePart.D:
                bene_map_d = self.read_bene_map_d(part)
                if not bene_map_d:
                    print(f"Coverage not found for Medicare Part {part.name}.")
                    return
                cntrct_pmp_sk = extract_col_str(bene_map_d, "CNTRCT_PBP_SK")
                if part == MedicarePart.D:
                    bene_map_d_rx = self.read_bene_map_d_rx(bene_map_d=bene_map_d)
            case MedicarePart.DUAL:
                bene_dual = self.read_bene_cmbnd_dual()
                if not bene_dual:
                    print(f"Coverage not found for Medicare Part {part.name}.")
                    return

        bene_tp = self.read_bene_tp()

        bene_status = self.read_bene_status()
        bene_lis = self.read_bene_cmbnd()

        if cntrct_pmp_sk:
            cntrct_pbp_num = self.read_cntrct_pbp_num(cntrct_pmp_sk=cntrct_pmp_sk)
            cntrct_pbp_cntct = self.read_cntrct_pbp_cntct(cntrct_pmp_sk=cntrct_pmp_sk)

        json_output = {
            "resourceType": "CoverageBase",
            "coveragePart": part.name,
            "BENE_MBI_ID": extract_col_str(bene_hist, "BENE_MBI_ID"),
            "XREF_EFCTV_BENE_SK": extract_col_str(bene_hist, "BENE_SK"),
            "lastUpdated": extract_col_str(bene_hist, "IDR_UPDT_TS"),
            # This is literally right now. Progam has an override for allowing dynamic date settings
            "currentDate": str(self.current_date),
            "BENE_BUYIN_CD": extract_col_str(bene_tp, "BENE_BUYIN_CD"),
            "BENE_MDCR_STUS_CD": extract_col_str(bene_status, "BENE_MDCR_STUS_CD"),
            "BENE_RNG_BGN_DT": extract_col_str(bene_entlmt, "BENE_RNG_BGN_DT"),
            "BENE_RNG_END_DT": extract_col_str(bene_entlmt, "BENE_RNG_END_DT"),
            "BENE_MDCR_ENTLMT_STUS_CD": extract_col_str(bene_entlmt, "BENE_MDCR_ENTLMT_STUS_CD"),
            "BENE_MDCR_ENRLMT_RSN_CD": extract_col_str(bene_entlmt, "BENE_MDCR_ENRLMT_RSN_CD"),
            "BENE_MDCR_ENTLMT_RSN_CD": extract_col_str(bene_entlmt_rsn, "BENE_MDCR_ENTLMT_RSN_CD"),
            "BENE_ENRLMT_BGN_DT": extract_col_str(bene_map_d, "BENE_ENRLMT_BGN_DT"),
            "BENE_ENRLMT_END_DT": extract_col_str(bene_map_d, "BENE_ENRLMT_END_DT"),
            "BENE_CNTRCT_NUM": extract_col_str(bene_map_d, "BENE_CNTRCT_NUM"),
            "BENE_PBP_NUM": extract_col_str(bene_map_d, "BENE_PBP_NUM"),
            "BENE_CVRG_TYPE_CD": extract_col_str(bene_map_d, "BENE_CVRG_TYPE_CD"),
            "BENE_ENRLMT_EMPLR_SBSDY_SW": extract_col_str(bene_map_d, "BENE_ENRLMT_EMPLR_SBSDY_SW"),
            "CNTRCT_PBP_NAME": extract_col_str(cntrct_pbp_num, "CNTRCT_PBP_NAME"),
            "CNTRCT_PLAN_CNTCT_TEL_NUM": extract_col_str(
                cntrct_pbp_cntct, "CNTRCT_PLAN_CNTCT_TEL_NUM"
            ),
            "BENE_PDP_ENRLMT_MMBR_ID_NUM": extract_col_str(
                bene_map_d_rx, "BENE_PDP_ENRLMT_MMBR_ID_NUM"
            ),
            "BENE_PDP_ENRLMT_GRP_NUM": extract_col_str(bene_map_d_rx, "BENE_PDP_ENRLMT_GRP_NUM"),
            "BENE_PDP_ENRLMT_PRCSR_NUM": extract_col_str(
                bene_map_d_rx, "BENE_PDP_ENRLMT_PRCSR_NUM"
            ),
            "BENE_PDP_ENRLMT_BANK_ID_NUM": extract_col_str(
                bene_map_d_rx, "BENE_PDP_ENRLMT_BANK_ID_NUM"
            ),
            "BENE_CMBND_DEEMD_IND": extract_col_str(bene_lis, "BENE_CMBND_DEEMD_IND"),
            "BENE_CMBND_DEEMD_COPMT_LVL_ID": extract_col_str(
                bene_lis, "BENE_CMBND_DEEMD_COPMT_LVL_ID"
            ),
            "BENE_CMBND_DEEMD_PRM_PCT": extract_col_str(bene_lis, "BENE_CMBND_DEEMD_PRM_PCT"),
            "BENE_MDCD_ELGBLTY_BGN_DT": extract_col_str(bene_dual, "BENE_MDCD_ELGBLTY_BGN_DT"),
            "BENE_MDCD_ELGBLTY_END_DT": extract_col_str(bene_dual, "BENE_MDCD_ELGBLTY_END_DT"),
            "BENE_DUAL_STUS_CD": extract_col_str(bene_dual, "BENE_DUAL_STUS_CD"),
            "BENE_DUAL_TYPE_CD": extract_col_str(bene_dual, "BENE_DUAL_TYPE_CD"),
            "CNTRCT_PBP_SGMT_NUM": extract_col_str(cntrct_pbp_sgmt, "CNTRCT_PBP_SGMT_NUM"),
            "GEO_USPS_STATE_CD": extract_col_str(bene_hist, "GEO_USPS_STATE_CD"),
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

    def read_latest(self, file_name: str, additional_params: list[Param]) -> dict[str, str] | None:
        params = [Param("BENE_SK", self.bene_sk), Param("IDR_LTST_TRANS_FLG", "Y")]
        if additional_params:
            params += additional_params
        records = self.read_multi_line_file(
            file_name,
            False,
            params,
        )
        return_record = None
        return_record_date = None
        for record in records:
            record_date = parse_date(record["BENE_RNG_BGN_DT"])
            if record_date <= self.current_date and (
                not return_record or (return_record_date and return_record_date < record_date)
            ):
                return_record = record
                return_record_date = record_date

        return return_record

    def read_bene_entlmt(self, part: MedicarePart) -> dict[str, str] | None:
        return self.read_latest(
            "SYNTHETIC_BENE_MDCR_ENTLMT", [Param("BENE_MDCR_ENTLMT_TYPE_CD", part.code)]
        )

    def read_bene_entlmt_rsn(self) -> dict[str, str] | None:
        return self.read_latest("SYNTHETIC_BENE_MDCR_ENTLMT_RSN", [])

    def read_bene_tp(self) -> dict[str, str] | None:
        return self.read_latest("SYNTHETIC_BENE_TP", [])

    def read_bene_status(self) -> dict[str, str]:
        return self.read_single_line_file(
            "SYNTHETIC_BENE_MDCR_STUS",
            False,
            [Param("BENE_SK", self.bene_sk), Param("IDR_LTST_TRANS_FLG", "Y")],
        )

    def read_bene_cmbnd(self) -> dict[str, str]:
        return self.read_single_line_file(
            "SYNTHETIC_BENE_LIS_CMBND",
            False,
            [Param("BENE_SK", self.bene_sk), Param("IDR_LTST_TRANS_FLG", "Y")],
        )

    def read_bene_map_d(self, part: MedicarePart) -> dict[str, str] | None:
        params = [Param("BENE_SK", self.bene_sk), Param("IDR_LTST_TRANS_FLG", "Y")]
        match part:
            case MedicarePart.C:
                records = self.read_multi_line_file(
                    "SYNTHETIC_BENE_MAPD_ENRLMT",
                    False,
                    params,
                )
                records = [
                    record
                    for record in records
                    if record["BENE_ENRLMT_PGM_TYPE_CD"] == "1"
                    or record["BENE_ENRLMT_PGM_TYPE_CD"] == "3"
                ]
                return {} if not records else records[0]
            case MedicarePart.D:
                records = self.read_multi_line_file(
                    "SYNTHETIC_BENE_MAPD_ENRLMT",
                    False,
                    params,
                )
                records = [
                    record
                    for record in records
                    if record["BENE_ENRLMT_PGM_TYPE_CD"] == "2"
                    or record["BENE_ENRLMT_PGM_TYPE_CD"] == "3"
                ]
                return {} if not records else records[0]
            case _:
                return None

    def read_bene_map_d_rx(self, bene_map_d: dict[str, str]) -> dict[str, str] | None:
        records = self.read_multi_line_file(
            "SYNTHETIC_BENE_MAPD_ENRLMT_RX",
            False,
            [
                Param("BENE_SK", bene_map_d["BENE_SK"]),
                Param("BENE_ENRLMT_BGN_DT", bene_map_d["BENE_ENRLMT_BGN_DT"]),
                Param("CNTRCT_PBP_SK", bene_map_d["CNTRCT_PBP_SK"]),
            ],
        )
        records = [
            record
            for record in records
            if parse_date(record["BENE_ENRLMT_BGN_DT"])
            == parse_date(bene_map_d["BENE_ENRLMT_BGN_DT"])
        ]
        return {} if not records else records[0]

    def read_cntrct_pbp_num(self, cntrct_pmp_sk: str) -> dict[str, str]:
        return self.read_single_line_file(
            "SYNTHETIC_CNTRCT_PBP_NUM",
            False,
            [Param("CNTRCT_PBP_SK", cntrct_pmp_sk)],
        )

    def read_cntrct_pbp_cntct(self, cntrct_pmp_sk: str) -> dict[str, str]:
        return self.read_single_line_file(
            "SYNTHETIC_CNTRCT_PBP_CNTCT",
            False,
            [Param("CNTRCT_PBP_SK", cntrct_pmp_sk)],
        )

    def read_cntrct_pbp_sgmt(self, cntrct_pmp_sk: str) -> dict[str, str]:
        return self.read_single_line_file(
            "SYNTHETIC_CNTRCT_PBP_SGMT",
            False,
            [Param("CNTRCT_PBP_SK", cntrct_pmp_sk)],
        )

    def read_bene_cmbnd_dual(self) -> dict[str, str] | None:
        records = self.read_multi_line_file(
            "SYNTHETIC_BENE_CMBND_DUAL_MDCR",
            False,
            [Param("BENE_SK", self.bene_sk), Param("IDR_LTST_TRANS_FLG", "Y")],
        )
        return_record = None
        return_record_date = None
        for record in records:
            record_date = parse_date(record["BENE_MDCD_ELGBLTY_BGN_DT"])
            if record_date <= self.current_date and (
                not return_record or (return_record_date and return_record_date < record_date)
            ):
                return_record = record
                return_record_date = record_date

        return return_record


def extract_col_str(row: dict[str, str] | None, name: str) -> str | None:
    if not row:
        return None
    return str(row.get(name, "")).strip() or None


def parse_date(date_str: str) -> date:
    return datetime.strptime(date_str, "%Y-%m-%d").date()
