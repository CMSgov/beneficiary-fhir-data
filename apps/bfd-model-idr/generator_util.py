import copy
import csv
import datetime
import json
import random
import string
import subprocess
import sys
from collections.abc import Iterable
from pathlib import Path
from typing import Any

import pandas as pd
from dateutil.parser import parse
from faker import Faker

BENE_HSTRY = "SYNTHETIC_BENE_HSTRY"
BENE_MBI_ID = "SYNTHETIC_BENE_MBI_ID"
BENE_STUS = "SYNTHETIC_BENE_MDCR_STUS"
BENE_ENTLMT_RSN = "SYNTHETIC_BENE_MDCR_ENTLMT_RSN"
BENE_ENTLMT = "SYNTHETIC_BENE_MDCR_ENTLMT"
BENE_TP = "SYNTHETIC_BENE_TP"
BENE_XREF = "SYNTHETIC_BENE_XREF"
BENE_DUAL = "SYNTHETIC_BENE_CMBND_DUAL_MDCR"
BENE_MAPD_ENRLMT = "SYNTHETIC_BENE_MAPD_ENRLMT"
BENE_MAPD_ENRLMT_RX = "SYNTHETIC_BENE_MAPD_ENRLMT_RX"
BENE_LIS = "SYNTHETIC_BENE_LIS"
CLM = "SYNTHETIC_CLM"
CLM_LINE = "SYNTHETIC_CLM_LINE"
CLM_VAL = "SYNTHETIC_CLM_VAL"
CLM_DT_SGNTR = "SYNTHETIC_CLM_DT_SGNTR"
CLM_PROD = "SYNTHETIC_CLM_PROD"
CLM_INSTNL = "SYNTHETIC_CLM_INSTNL"
CLM_LINE_INSTNL = "SYNTHETIC_CLM_LINE_INSTNL"
CLM_DCMTN = "SYNTHETIC_CLM_DCMTN"
CLM_LCTN_HSTRY = "SYNTHETIC_CLM_LCTN_HSTRY"
CLM_FISS = "SYNTHETIC_CLM_FISS"
CLM_PRFNL = "SYNTHETIC_CLM_PRFNL"
CLM_LINE_PRFNL = "SYNTHETIC_CLM_LINE_PRFNL"
CLM_LINE_RX = "SYNTHETIC_CLM_LINE_RX"
CLM_RLT_COND_SGNTR_MBR = "SYNTHETIC_CLM_RLT_COND_SGNTR_MBR"
PRVDR_HSTRY = "SYNTHETIC_PRVDR_HSTRY"
CNTRCT_PBP_NUM = "SYNTHETIC_CNTRCT_PBP_NUM"
CNTRCT_PBP_CNTCT = "SYNTHETIC_CNTRCT_PBP_CNTCT"

# Lazily computed output table by filename (see above constants) to mappings of the file's rows by
# BENE_SK. Dramatically speeds up generation with large static inputs versus just doing typical list
# scanning via list comprehensions. Outermost dict is keyed by original filename, inner dict is
# keyed by the patient's bene_sk, and innermost dict is the full row itself
_tables_by_bene_sk: dict[str, dict[str, dict[str, Any]]] = {}


def load_file_dict(
    files: dict[str, list["RowAdapter"]], file_paths: list[str], exclude_empty: bool = False
):
    for file_path, file_name in (
        (Path(file_path), file_name)
        for file_path in file_paths
        for file_name in files
        if f"{file_name}.csv" in file_path
    ):
        csv_data = pd.read_csv(  # type: ignore
            file_path,
            dtype=str,
            na_filter=exclude_empty,
        )
        file_as_dictlist = csv_data.to_dict(orient="records")  # type: ignore
        if not exclude_empty:
            files[file_name] = load_file(file_as_dictlist)  # type: ignore
            continue

        files[file_name] = load_file([
            {str(k): v for k, v in x.items() if pd.notna(v)} for x in file_as_dictlist
        ])


def load_file(file: Iterable[dict[str, Any]]):
    return [RowAdapter(kv=row, loaded_from_file=True) for row in file]


def probability(frac: float) -> bool:
    return random.random() < (frac)


def adapters_to_dicts(adapters: list["RowAdapter"]) -> list[dict[str, Any]]:
    return [x.kv for x in adapters]


def output_table_contains_by_bene_sk(
    table: list[dict[str, Any]], for_file: str, bene_sk: str
) -> bool:
    if for_file not in _tables_by_bene_sk:
        _tables_by_bene_sk[for_file] = {str(row["BENE_SK"]): row for row in table}

    return bene_sk in _tables_by_bene_sk[for_file]


def convert_tilde_str(val: str) -> str:
    if val == "~":
        return ""
    return val


class RowAdapter:
    def __init__(self, kv: dict[str, Any], loaded_from_file: bool = False):
        self.kv = kv
        self.loaded_from_file = loaded_from_file

    def __getitem__(self, key: str):
        return self.kv[key]

    def __setitem__(self, key: str, new_value: Any):
        if key not in self.kv:
            self.kv[key] = new_value

    def __contains__(self, key: str) -> bool:
        return key in self.kv

    def get(self, key: str, default: Any | None = None) -> Any:
        return self.kv.get(key, default)


class GeneratorUtil:
    USE_COLS = "use_cols"
    NO_COLS = "no_cols"

    def __init__(self):
        self.fake = Faker()
        self.used_bene_sk: list[int] = []
        self.used_mbi: list[str] = []
        self.bene_hstry_table: list[dict[str, Any]] = []
        self.bene_xref_table: list[dict[str, Any]] = []
        self.mbi_table: dict[str, dict[str, Any]] = {}
        self.address_options: list[dict[str, Any]] = []
        self.mdcr_stus: list[dict[str, Any]] = []
        self.mdcr_entlmt: list[dict[str, Any]] = []
        self.mdcr_tp: list[dict[str, Any]] = []
        self.mdcr_rsn: list[dict[str, Any]] = []
        self.bene_cmbnd_dual_mdcr: list[dict[str, Any]] = []
        self.bene_lis: list[dict[str, Any]] = []
        self.bene_mapd_enrlmt_rx: list[dict[str, Any]] = []
        self.bene_mapd_enrlmt: list[dict[str, Any]] = []
        self.code_systems = {}

        self.load_addresses()
        self.load_code_systems()

    def load_code_systems(self):
        code_systems = {}
        sushi_dir = "./sushi"
        relative_path = f"{sushi_dir}/fsh-generated/resources"

        # Check if the resources directory exists, if not run sushi build
        if not Path(relative_path).exists():
            print("Running sushi build")
            try:
                sushi_dir = "./sushi"
                result = subprocess.run(
                    ["sushi", "build"], check=True, cwd=sushi_dir, capture_output=True, text=True
                )
                if result.returncode == 0:
                    print("Sushi build completed successfully")
                else:
                    print(f"Sushi build failed with error: {result.stderr}")
                    sys.exit(1)
            except Exception as e:
                print(f"Error running sushi build: {e}")
                sys.exit(1)

        try:
            for path in Path(relative_path).iterdir():
                file = path.name
                if ".json" not in file or "CodeSystem" not in file:
                    continue
                full_path = relative_path + "/" + file
                try:
                    with Path(full_path).open() as file:
                        data = json.load(file)
                        concepts = [i["code"] for i in data["concept"]]
                        code_systems[data["name"]] = concepts
                except FileNotFoundError:
                    print(f"Error: File not found at path: {full_path}")
                except json.JSONDecodeError:
                    print(f"Error: Invalid JSON format in file: {full_path}")
        except FileNotFoundError:
            print(f"Error: Resources directory not found at path: {relative_path}")
            sys.exit(1)

        self.code_systems: dict[str, list[str]] = code_systems

    def load_addresses(self):
        with Path("beneficiary-components/addresses.csv").open() as file:
            csvreader = csv.reader(file)
            header = next(csvreader)
            for row in csvreader:
                cur_row: dict[str, Any] = {}
                for col in range(len(row)):
                    cur_row[header[col]] = row[col]
                self.address_options.append(cur_row)

    def gen_mbi(self) -> str:
        mbi: list[str] = []
        set_1 = set(string.ascii_uppercase) - set(["S", "L", "O", "I", "B", "Z"])
        set_2 = set(list(set_1) + list(string.digits))
        mbi.append(random.choice(["1", "2", "3", "4", "5", "6", "7", "8", "9"]))
        mbi.append(random.choice(["L", "O", "I", "B", "Z"]))
        mbi.append(random.choice(list(set_2)))
        mbi.append(random.choice(string.digits))
        mbi.append(random.choice(list(set_1)))
        mbi.append(random.choice(list(set_2)))
        mbi.append(random.choice(string.digits))
        mbi.append(random.choice(list(set_1)))
        mbi.append(random.choice(list(set_1)))
        mbi.append(random.choice(string.digits))
        mbi.append(random.choice(string.digits))

        full_mbi = "".join(mbi)
        if full_mbi in self.mbi_table:
            return self.gen_mbi()
        return full_mbi

    def gen_bene_sk(self) -> int:
        bene_sk = random.randint(-1000000000, -1000)
        if bene_sk in self.used_bene_sk:
            return self.gen_bene_sk()
        return bene_sk

    def generate_bene_xref(self, bene_xref: RowAdapter, new_bene_sk: str, old_bene_sk: int):
        bene_hicn_num = str(random.randint(1000, 100000000)) + random.choice(string.ascii_letters)

        # 10% chance for invalid xref.
        kill_cred_cd = 1 if random.randint(1, 10) == 1 else 2

        efctv_ts = self.fake.date_time_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.datetime.now() - datetime.timedelta(days=1),
        )
        src_rec_ctre_ts = self.fake.date_time_between_dates(
            efctv_ts, datetime.datetime.now() - datetime.timedelta(days=1)
        )
        src_rec_updt_ts = self.fake.date_time_between_dates(
            src_rec_ctre_ts, datetime.datetime.now() - datetime.timedelta(days=1)
        )
        insrt_ts = self.fake.date_time_between_dates(
            efctv_ts, datetime.datetime.now() - datetime.timedelta(days=1)
        )
        updt_ts = self.fake.date_time_between_dates(
            insrt_ts, datetime.datetime.now() - datetime.timedelta(days=1)
        )

        bene_xref["BENE_SK"] = str(new_bene_sk)
        bene_xref["BENE_XREF_SK"] = str(old_bene_sk)
        bene_xref["BENE_HICN_NUM"] = bene_hicn_num
        bene_xref["BENE_KILL_CRED_CD"] = str(kill_cred_cd)
        bene_xref["SRC_REC_CRTE_TS"] = str(src_rec_ctre_ts)
        bene_xref["SRC_REC_UPDT_TS"] = str(src_rec_updt_ts)
        bene_xref["IDR_TRANS_EFCTV_TS"] = str(efctv_ts)
        bene_xref["IDR_INSRT_TS"] = str(insrt_ts)
        bene_xref["IDR_UPDT_TS"] = str(updt_ts)
        bene_xref["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

        self.bene_xref_table.append(bene_xref.kv)

    def gen_address(self):
        return self.address_options[random.randint(0, len(self.address_options) - 1)]

    def set_timestamps(self, patient: RowAdapter, min_date: datetime.date):
        max_date = datetime.datetime.now() - datetime.timedelta(days=1)
        efctv_ts = self.fake.date_time_between_dates(min_date, max_date)
        insrt_ts = self.fake.date_time_between_dates(efctv_ts, max_date)
        updt_ts = self.fake.date_time_between_dates(insrt_ts, max_date)
        patient["IDR_TRANS_EFCTV_TS"] = str(efctv_ts)
        patient["IDR_INSRT_TS"] = str(insrt_ts)
        patient["IDR_UPDT_TS"] = str(updt_ts)
        patient["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

    def create_base_patient(self, patient: RowAdapter):
        self.set_timestamps(patient, datetime.date(year=2017, month=5, day=20))
        patient["CNTCT_LANG_CD"] = random.choice(["~", "ENG", "SPA"])
        patient["IDR_LTST_TRANS_FLG"] = "Y"
        address = self.gen_address()
        for component in address:
            patient[component] = address[component]

    def gen_mbis_for_patient(
        self, patient: RowAdapter, num_mbis: int, initial_mbi_obj: RowAdapter | None = None
    ):
        previous_obslt_dt = None
        previous_mbi = None

        for mbi_idx in range(num_mbis):
            # This is a bit of a hack to support regeneration of existing BENE_MBI_ID rows without
            # touching too much of the remaining generation code here. Basically, we know that
            # regeneration will only ever call this function with "num_mbis" set to 1, so we can set
            # the mbi_obj here immediately knowing that the mbi_idx = 0 and num_mbis = 1 case does
            # not mutate the output BENE_HSTRY table
            mbi_obj = RowAdapter({}) if num_mbis > 1 or not initial_mbi_obj else initial_mbi_obj

            if mbi_idx == 0:
                efctv_dt = self.fake.date_between_dates(
                    datetime.date(year=2017, month=5, day=20),
                    datetime.date(year=2021, month=1, day=1),
                )
                self.set_timestamps(patient, efctv_dt)

                current_mbi = str(mbi_obj.get("BENE_MBI_ID")) or self.gen_mbi()
                patient["BENE_MBI_ID"] = current_mbi

            else:
                # If we have a previous obsolescence date, start the new MBI the next day
                if previous_obslt_dt:
                    efctv_dt = previous_obslt_dt + datetime.timedelta(days=1)
                else:
                    # Fallback to random date if no previous obsolescence date
                    efctv_dt = self.fake.date_between_dates(
                        datetime.date(year=2021, month=1, day=2),
                        datetime.date(year=2025 - num_mbis + mbi_idx, month=1, day=1),
                    )
                current_mbi = self.gen_mbi()

            # Create the MBI object with all required fields
            mbi_obj["BENE_MBI_EFCTV_DT"] = str(efctv_dt)
            mbi_obj["IDR_LTST_TRANS_FLG"] = "Y"
            if mbi_idx != num_mbis - 1:
                # Calculate obsolescence date
                obslt_dt = self.fake.date_between_dates(
                    efctv_dt,
                    datetime.date(year=2025 - num_mbis + mbi_idx, month=1, day=1),
                )
                mbi_obj["BENE_MBI_OBSLT_DT"] = obslt_dt.strftime("%Y-%m-%d")

                if previous_mbi and previous_mbi != current_mbi:
                    historical_patient = copy.deepcopy(patient)
                    historical_patient["BENE_MBI_ID"] = previous_mbi
                    historical_patient["IDR_LTST_TRANS_FLG"] = "N"

                    self.set_timestamps(historical_patient, obslt_dt)
                    historical_patient["IDR_TRANS_OBSLT_TS"] = (
                        str(obslt_dt) + "T00:00:00.000000+0000"
                    )
                    self.bene_hstry_table.append(historical_patient.kv)

                previous_obslt_dt = obslt_dt  # Store for next iteration
            else:
                # For the last MBI, no obsolescence date
                mbi_obj["BENE_MBI_OBSLT_DT"] = None

            self.set_timestamps(mbi_obj, efctv_dt)
            self.mbi_table[current_mbi] = mbi_obj.kv

            # Update patient with current MBI and store previous for next iteration
            previous_mbi = patient["BENE_MBI_ID"]
            patient.kv["BENE_MBI_ID"] = current_mbi

    def generate_coverages(self, patient: RowAdapter, force_ztm: bool = False):
        parts = random.choices([["A"], ["B"], ["A", "B"], []], weights=[0.2, 0.2, 0.5, 0.1])[0]
        include_tp = random.random() > 0.2
        expired = random.random() < 0.2
        future = random.random() < 0.2
        self._generate_coverages(
            patient=patient,
            coverage_parts=parts,
            include_tp=include_tp,
            expired=expired,
            future=future,
            force_ztm=force_ztm,
        )

    def _generate_coverages(
        self,
        patient: RowAdapter,
        coverage_parts: list[str],
        include_tp: bool,
        expired: bool,
        future: bool,
        force_ztm: bool,
    ):
        now = datetime.date.today()
        if expired:
            medicare_start_date = now - datetime.timedelta(days=730)
            medicare_end_date = now - datetime.timedelta(days=365)
        elif future:
            medicare_start_date = now + datetime.timedelta(days=365)
            medicare_end_date = now + datetime.timedelta(days=730)
        else:
            medicare_start_date = parse(
                self.mbi_table[patient["BENE_MBI_ID"]]["BENE_MBI_EFCTV_DT"]
            ).date()
            medicare_end_date = datetime.date(9999, 12, 31)
        mdcr_stus_cd = "~"
        while mdcr_stus_cd in ("0", "~", "00"):
            mdcr_stus_cd = random.choice(self.code_systems["BENE_MDCR_STUS_CD"])

        # Gross, but Python will pass this by reference to everything that uses it. We want
        # RowAdapters for empty rows to include the patient's BENE_SK as a foreign key, so
        # everywhere this is used we copy it first to ensure the same inner RowAdapter dict isn't
        # being mutated across different tables
        initial_kv_template = {"BENE_SK": patient["BENE_SK"]}

        if not output_table_contains_by_bene_sk(
            table=self.mdcr_stus, for_file=BENE_STUS, bene_sk=patient["BENE_SK"]
        ):
            self.generate_bene_stus(
                stus_row=RowAdapter(initial_kv_template.copy()),
                medicare_start_date=medicare_start_date,
                medicare_end_date=medicare_end_date,
                mdcr_stus_cd=mdcr_stus_cd,
            )

        buy_in_cd = random.choice(self.code_systems["BENE_BUYIN_CD"])

        if not output_table_contains_by_bene_sk(
            table=self.mdcr_rsn, for_file=BENE_ENTLMT_RSN, bene_sk=patient["BENE_SK"]
        ):
            self.generate_bene_entlmnt_rsn(
                rsn_row=RowAdapter(initial_kv_template.copy()),
                medicare_start_date=medicare_start_date,
                medicare_end_date=medicare_end_date,
            )

        for coverage_type in coverage_parts:
            # We need to check if the patient was loaded from file because there's a 10%
            # chance for any given patient to not have a BENE_ENTLMT row generated for them so
            # subsequent generations can introduce more data for patients that previously had none.
            # "force_ztm" overrides this check so that those patients may have new rows generated if
            # "coverage_parts" is not empty when it was in a previous run
            if (not patient.loaded_from_file or force_ztm) and not output_table_contains_by_bene_sk(
                table=self.mdcr_entlmt, for_file=BENE_ENTLMT, bene_sk=patient["BENE_SK"]
            ):
                self.generate_bene_entlmt(
                    entlmt_row=RowAdapter(initial_kv_template.copy()),
                    medicare_start_date=medicare_start_date,
                    medicare_end_date=medicare_end_date,
                    coverage_type=coverage_type,
                )

            if (
                include_tp
                and (not patient.loaded_from_file or force_ztm)
                and not output_table_contains_by_bene_sk(
                    table=self.mdcr_tp, for_file=BENE_TP, bene_sk=patient["BENE_SK"]
                )
            ):
                self.generate_bene_tp(
                    tp_row=RowAdapter(initial_kv_template.copy()),
                    medicare_start_date=medicare_start_date,
                    medicare_end_date=medicare_end_date,
                    buy_in_cd=buy_in_cd,
                    coverage_type=coverage_type,
                )

        # Generate dual coverage data 50% of the time
        if (
            (not patient.loaded_from_file or force_ztm)
            and not output_table_contains_by_bene_sk(
                table=self.bene_cmbnd_dual_mdcr, for_file=BENE_DUAL, bene_sk=patient["BENE_SK"]
            )
            and probability(0.5)
        ):
            # Generate dual eligibility dates
            dual_start_date = str(
                self.fake.date_between_dates(
                    datetime.date(year=2017, month=5, day=20),
                    datetime.date(year=2021, month=1, day=1),
                )
            )
            dual_end_date = "9999-12-31"
            dual_status_cd = random.choice(self.code_systems["BENE_DUAL_STUS_CD"])
            dual_type_cd = random.choice(self.code_systems["BENE_DUAL_TYPE_CD"])

            state_codes = [
                "AL",
                "TN",
                "TX",
                "UT",
                "VT",
                "VA",
                "WA",
                "WV",
                "WI",
                "WY",
                "DC",
            ]
            medicaid_state_cd = random.choice(state_codes)

            if not patient.loaded_from_file or force_ztm:
                self.generate_bene_dual(
                    dual_row=RowAdapter(initial_kv_template.copy()),
                    dual_start_date=dual_start_date,
                    dual_end_date=dual_end_date,
                    dual_status_cd=dual_status_cd,
                    dual_type_cd=dual_type_cd,
                    medicaid_state_cd=medicaid_state_cd,
                )

    def generate_bene_dual(
        self,
        dual_row: RowAdapter,
        dual_start_date: str,
        dual_end_date: str,
        dual_status_cd: str,
        dual_type_cd: str,
        medicaid_state_cd: str,
    ):
        dual_row["IDR_LTST_TRANS_FLG"] = "Y"
        dual_row["BENE_DUAL_STUS_CD"] = dual_status_cd
        dual_row["BENE_DUAL_TYPE_CD"] = dual_type_cd
        dual_row["GEO_USPS_STATE_CD"] = medicaid_state_cd
        dual_row["BENE_MDCD_ELGBLTY_BGN_DT"] = dual_start_date
        dual_row["BENE_MDCD_ELGBLTY_END_DT"] = dual_end_date
        dual_row["IDR_TRANS_EFCTV_TS"] = str(dual_start_date) + "T00:00:00.000000+0000"
        dual_row["IDR_INSRT_TS"] = str(dual_start_date) + "T00:00:00.000000+0000"
        dual_row["IDR_UPDT_TS"] = str(dual_start_date) + "T00:00:00.000000+0000"
        dual_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        self.bene_cmbnd_dual_mdcr.append(dual_row.kv)

    def generate_bene_tp(
        self,
        tp_row: RowAdapter,
        medicare_start_date: datetime.date,
        medicare_end_date: datetime.date,
        buy_in_cd: str,
        coverage_type: str,
    ):
        tp_row["IDR_LTST_TRANS_FLG"] = "Y"
        tp_row["BENE_TP_TYPE_CD"] = coverage_type
        tp_row["IDR_TRANS_EFCTV_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        tp_row["IDR_INSRT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        tp_row["IDR_UPDT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        tp_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        tp_row["BENE_RNG_BGN_DT"] = medicare_start_date
        tp_row["BENE_RNG_END_DT"] = medicare_end_date
        tp_row["BENE_BUYIN_CD"] = buy_in_cd
        self.mdcr_tp.append(tp_row.kv)

    def generate_bene_entlmt(
        self,
        entlmt_row: RowAdapter,
        medicare_start_date: datetime.date,
        medicare_end_date: datetime.date,
        coverage_type: str,
    ):
        entlmt_row["IDR_LTST_TRANS_FLG"] = "Y"
        entlmt_row["BENE_MDCR_ENTLMT_TYPE_CD"] = coverage_type
        entlmt_row["BENE_MDCR_ENRLMT_RSN_CD"] = random.choice(
            self.code_systems["BENE_ENRLMT_RSN_CD"]
        )
        entlmt_row["BENE_MDCR_ENTLMT_STUS_CD"] = "Y"
        entlmt_row["IDR_TRANS_EFCTV_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        entlmt_row["IDR_INSRT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        entlmt_row["IDR_UPDT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        entlmt_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        entlmt_row["BENE_RNG_BGN_DT"] = medicare_start_date
        entlmt_row["BENE_RNG_END_DT"] = medicare_end_date
        self.mdcr_entlmt.append(entlmt_row.kv)

    def generate_bene_entlmnt_rsn(
        self,
        rsn_row: RowAdapter,
        medicare_start_date: datetime.date,
        medicare_end_date: datetime.date,
    ):
        entitlement_reason = random.choice(self.code_systems["BENE_MDCR_ENTLMT_RSN_CD"])
        rsn_row["IDR_LTST_TRANS_FLG"] = "Y"
        rsn_row["IDR_TRANS_EFCTV_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        rsn_row["IDR_INSRT_TS"] = (str(medicare_start_date) + "T00:00:00.000000+0000",)
        rsn_row["IDR_UPDT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        rsn_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        rsn_row["BENE_MDCR_ENTLMT_RSN_CD"] = entitlement_reason
        rsn_row["BENE_RNG_BGN_DT"] = medicare_start_date
        rsn_row["BENE_RNG_END_DT"] = medicare_end_date
        self.mdcr_rsn.append(rsn_row.kv)

    def generate_bene_stus(
        self,
        stus_row: RowAdapter,
        medicare_start_date: datetime.date,
        medicare_end_date: datetime.date,
        mdcr_stus_cd: str,
    ):
        stus_row["IDR_LTST_TRANS_FLG"] = "Y"
        stus_row["BENE_MDCR_STUS_CD"] = mdcr_stus_cd
        stus_row["MDCR_STUS_BGN_DT"] = medicare_start_date
        stus_row["MDCR_STUS_END_DT"] = medicare_end_date
        stus_row["IDR_TRANS_EFCTV_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        stus_row["IDR_INSRT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        stus_row["IDR_UPDT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        stus_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        self.mdcr_stus.append(stus_row.kv)

    def generate_bene_lis(self, lis_row: RowAdapter):
        lis_start_date = self.fake.date_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.date(year=2021, month=1, day=1),
        )
        lis_end_date = "9999-12-31"
        lis_efctv_cd = random.choice(self.code_systems["BENE_LIS_EFCTV_CD"])
        copmt_lvl_cd = random.choice(self.code_systems["BENE_LIS_COPMT_LVL_CD"])
        ptd_prm_pct = random.choice(["025", "050", "075", "100"])

        lis_row["IDR_LTST_TRANS_FLG"] = "Y"
        lis_row["BENE_LIS_EFCTV_CD"] = lis_efctv_cd
        lis_row["BENE_LIS_COPMT_LVL_CD"] = copmt_lvl_cd
        lis_row["BENE_LIS_PTD_PRM_PCT"] = str(ptd_prm_pct)
        lis_row["BENE_RNG_BGN_DT"] = str(lis_start_date)
        lis_row["BENE_RNG_END_DT"] = lis_end_date
        lis_row["IDR_TRANS_EFCTV_TS"] = str(lis_start_date) + "T00:00:00.000000+0000"
        lis_row["IDR_INSRT_TS"] = str(lis_start_date) + "T00:00:00.000000+0000"
        lis_row["IDR_UPDT_TS"] = str(lis_start_date) + "T00:00:00.000000+0000"
        lis_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

        self.bene_lis.append(lis_row.kv)

    def generate_bene_mapd_enrlmt_rx(self, rx_row: RowAdapter, contract_num: str, pbp_num: str):
        enrollment_start_date = self.fake.date_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.date(year=2021, month=1, day=1),
        )
        rx_start_date = self.fake.date_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.date(year=2021, month=1, day=1),
        )
        member_id_num = str(random.randint(100000000, 999999999))
        group_num = str(random.randint(100, 999))
        prcsr_num = str(random.randint(100000, 999999))
        bank_id_num = str(random.randint(100000, 999999))

        rx_row["CNTRCT_PBP_SK"] = "".join(random.choices(string.digits, k=12))
        rx_row["BENE_ENRLMT_BGN_DT"] = str(enrollment_start_date)
        rx_row["BENE_ENRLMT_PDP_RX_INFO_BGN_DT"] = str(rx_start_date)
        rx_row["IDR_LTST_TRANS_FLG"] = "Y"
        rx_row["BENE_PDP_ENRLMT_MMBR_ID_NUM"] = member_id_num
        rx_row["BENE_PDP_ENRLMT_GRP_NUM"] = group_num
        rx_row["BENE_PDP_ENRLMT_PRCSR_NUM"] = prcsr_num
        rx_row["BENE_PDP_ENRLMT_BANK_ID_NUM"] = bank_id_num
        rx_row["BENE_CNTRCT_NUM"] = contract_num
        rx_row["BENE_PBP_NUM"] = pbp_num
        rx_row["IDR_TRANS_EFCTV_TS"] = str(rx_start_date) + "T00:00:00.000000+0000"
        rx_row["IDR_INSRT_TS"] = str(rx_start_date) + "T00:00:00.000000+0000"
        rx_row["IDR_UPDT_TS"] = str(rx_start_date) + "T00:00:00.000000+0000"
        rx_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

        self.bene_mapd_enrlmt_rx.append(rx_row.kv)

    def generate_bene_mapd_enrlmt(self, enrollment_row: RowAdapter, pdp_only: bool = False):
        enrollment_start_date = self.fake.date_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.date(year=2021, month=1, day=1),
        )
        enrollment_end_date = "9999-12-31"
        avail_pbp_nums = ["001", "002", "003"]

        cntrct_num = "S0001" if pdp_only else random.choice(["H1234", "G1234"])
        pbp_num = random.choice(avail_pbp_nums)
        cvrg_type_cd = "11" if pdp_only else "3"
        bene_enrlmt_pgm_type_cd = random.choice(["1", "2", "3"])
        bene_enrlmt_emplr_sbsdy_sw = random.choice(["Y", "~", "1"])

        enrollment_row["CNTRCT_PBP_SK"] = "".join(random.choices(string.digits, k=12))
        enrollment_row["IDR_LTST_TRANS_FLG"] = "Y"
        enrollment_row["BENE_CNTRCT_NUM"] = cntrct_num
        enrollment_row["BENE_PBP_NUM"] = pbp_num
        enrollment_row["BENE_CVRG_TYPE_CD"] = cvrg_type_cd
        enrollment_row["BENE_ENRLMT_PGM_TYPE_CD"] = bene_enrlmt_pgm_type_cd
        enrollment_row["BENE_ENRLMT_EMPLR_SBSDY_SW"] = bene_enrlmt_emplr_sbsdy_sw
        enrollment_row["BENE_ENRLMT_BGN_DT"] = str(enrollment_start_date)
        enrollment_row["BENE_ENRLMT_END_DT"] = enrollment_end_date
        enrollment_row["IDR_TRANS_EFCTV_TS"] = str(enrollment_start_date) + "T00:00:00.000000+0000"
        enrollment_row["IDR_INSRT_TS"] = str(enrollment_start_date) + "T00:00:00.000000+0000"
        enrollment_row["IDR_UPDT_TS"] = str(enrollment_start_date) + "T00:00:00.000000+0000"
        enrollment_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

        self.bene_mapd_enrlmt.append(enrollment_row.kv)
        return (cntrct_num, pbp_num)

    def save_output_files(self):
        Path("out").mkdir(exist_ok=True)

        BENE_HSTRY_COLS = [
            "BENE_SK",
            "BENE_XREF_EFCTV_SK",
            "BENE_XREF_SK",
            "BENE_MBI_ID",
            "BENE_LAST_NAME",
            "BENE_1ST_NAME",
            "BENE_MIDL_NAME",
            "BENE_BRTH_DT",
            "BENE_DEATH_DT",
            "BENE_VRFY_DEATH_DAY_SW",
            "BENE_SEX_CD",
            "BENE_RACE_CD",
            "BENE_LINE_1_ADR",
            "BENE_LINE_2_ADR",
            "BENE_LINE_3_ADR",
            "BENE_LINE_4_ADR",
            "BENE_LINE_5_ADR",
            "BENE_LINE_6_ADR",
            "GEO_ZIP_PLC_NAME",
            "GEO_ZIP5_CD",
            "GEO_USPS_STATE_CD",
            "CNTCT_LANG_CD",
            "IDR_LTST_TRANS_FLG",
            "IDR_TRANS_EFCTV_TS",
            "IDR_INSRT_TS",
            "IDR_UPDT_TS",
            "IDR_TRANS_OBSLT_TS",
        ]

        mbi_arr = [{"BENE_MBI_ID": mbi, **self.mbi_table[mbi]} for mbi in self.mbi_table]
        BENE_MBI_COLS = [
            "BENE_MBI_ID",
            "BENE_MBI_EFCTV_DT",
            "BENE_MBI_OBSLT_DT",
            "IDR_LTST_TRANS_FLG",
            "IDR_TRANS_EFCTV_TS",
            "IDR_INSRT_TS",
            "IDR_UPDT_TS",
            "IDR_TRANS_OBSLT_TS",
        ]

        beneficiary_exports = [
            (self.bene_hstry_table, f"out/{BENE_HSTRY}.csv", BENE_HSTRY_COLS),
            (mbi_arr, f"out/{BENE_MBI_ID}.csv", BENE_MBI_COLS),
            (self.mdcr_stus, f"out/{BENE_STUS}.csv", GeneratorUtil.NO_COLS),
            (self.mdcr_entlmt, f"out/{BENE_ENTLMT}.csv", GeneratorUtil.NO_COLS),
            (self.mdcr_tp, f"out/{BENE_TP}.csv", GeneratorUtil.NO_COLS),
            (self.mdcr_rsn, f"out/{BENE_ENTLMT_RSN}.csv", GeneratorUtil.NO_COLS),
            (self.bene_xref_table, f"out/{BENE_XREF}.csv", GeneratorUtil.NO_COLS),
            (
                self.bene_cmbnd_dual_mdcr,
                f"out/{BENE_DUAL}.csv",
                GeneratorUtil.NO_COLS,
            ),
            (self.bene_lis, f"out/{BENE_LIS}.csv", GeneratorUtil.NO_COLS),
            (
                self.bene_mapd_enrlmt_rx,
                f"out/{BENE_MAPD_ENRLMT_RX}.csv",
                GeneratorUtil.NO_COLS,
            ),
            (self.bene_mapd_enrlmt, f"out/{BENE_MAPD_ENRLMT}.csv", GeneratorUtil.NO_COLS),
        ]

        for data, path, cols in beneficiary_exports:
            self.export_df(data, path, cols)

    @staticmethod
    def export_df(data: list[dict[str, Any]], out_path: str, cols: list[str] | str = NO_COLS):
        df = pd.json_normalize(data)  # type: ignore
        if cols != GeneratorUtil.NO_COLS:
            df = df[cols]
        df.to_csv(out_path, index=False)
