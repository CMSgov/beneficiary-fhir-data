import copy
import csv
import datetime
import json
import random
import string
import subprocess
import sys
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


def probability(frac: float) -> bool:
    return random.random() < (frac)


def contains_bene_sk(file, bene_sk):
    return len([row for row in file if row["BENE_SK"] == bene_sk]) > 0


def find_bene_sk(file, bene_sk):
    res = [row for row in file if row["BENE_SK"] == bene_sk]
    return res[0] if res else RowAdapter({"BENE_SK": bene_sk})


class RowAdapter:
    def __init__(self, kv: dict[str, Any], loaded_from_file: bool = False):
        self.kv = kv
        self.loaded_from_file = loaded_from_file

    def __getitem__(self, key: str):
        return self.kv[key]

    def __setitem__(self, key: str, new_value: Any):
        if key not in self.kv or not self.kv[key]:
            self.kv[key] = new_value


class GeneratorUtil:
    USE_COLS = "use_cols"
    NO_COLS = "no_cols"

    def __init__(self):
        self.fake = Faker()
        self.used_bene_sk: list[int] = []
        self.used_mbi = []
        self.bene_hstry_table = []
        self.bene_xref_table = []
        self.mbi_table: dict[str, dict[str, Any]] = {}
        self.address_options = []
        self.mdcr_stus = []
        self.mdcr_entlmt = []
        self.mdcr_tp = []
        self.mdcr_rsn = []
        self.bene_cmbnd_dual_mdcr = []
        self.bene_lis = []
        self.bene_mapd_enrlmt_rx = []
        self.bene_mapd_enrlmt = []
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

        self.code_systems = code_systems

    def load_addresses(self):
        with Path("beneficiary-components/addresses.csv").open() as file:
            csvreader = csv.reader(file)
            header = next(csvreader)
            for row in csvreader:
                cur_row = {}
                for col in range(len(row)):
                    cur_row[header[col]] = row[col]
                self.address_options.append(cur_row)

    def gen_mbi(self):
        mbi = []
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
        mbi = "".join(mbi)
        if mbi in self.mbi_table:
            return self.gen_mbi()
        return mbi

    def gen_bene_sk(self) -> int:
        bene_sk = random.randint(-1000000000, -1000)
        if bene_sk in self.used_bene_sk:
            return self.gen_bene_sk()
        return bene_sk

    def generate_bene_xref(self, new_bene_sk, old_bene_sk):
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

        xref_row = {
            "BENE_SK": str(new_bene_sk),
            "BENE_XREF_SK": str(old_bene_sk),
            "BENE_HICN_NUM": bene_hicn_num,
            "BENE_KILL_CRED_CD": str(kill_cred_cd),
            "SRC_REC_CRTE_TS": str(src_rec_ctre_ts),
            "SRC_REC_UPDT_TS": str(src_rec_updt_ts),
            "IDR_TRANS_EFCTV_TS": str(efctv_ts),
            "IDR_INSRT_TS": str(insrt_ts),
            "IDR_UPDT_TS": str(updt_ts),
            "IDR_TRANS_OBSLT_TS": "9999-12-31T00:00:00.000000+0000",
        }

        self.bene_xref_table.append(xref_row)

    def gen_address(self):
        return self.address_options[random.randint(0, len(self.address_options) - 1)]

    def set_timestamps(self, patient, min_date):
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

    def gen_mbis_for_patient(self, patient, num_mbis):
        previous_obslt_dt = None
        previous_mbi = None

        for mbi_idx in range(num_mbis):
            mbi_obj = {}

            if mbi_idx == 0:
                efctv_dt = self.fake.date_between_dates(
                    datetime.date(year=2017, month=5, day=20),
                    datetime.date(year=2021, month=1, day=1),
                )
                self.set_timestamps(patient, efctv_dt)

                current_mbi = self.gen_mbi()
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
                    historical_patient = copy.deepcopy(patient).kv
                    historical_patient["BENE_MBI_ID"] = previous_mbi
                    historical_patient["IDR_LTST_TRANS_FLG"] = "N"

                    self.set_timestamps(historical_patient, obslt_dt)
                    historical_patient["IDR_TRANS_OBSLT_TS"] = (
                        str(obslt_dt) + "T00:00:00.000000+0000"
                    )
                    self.bene_hstry_table.append(historical_patient)

                previous_obslt_dt = obslt_dt  # Store for next iteration
            else:
                # For the last MBI, no obsolescence date
                mbi_obj["BENE_MBI_OBSLT_DT"] = None

            self.set_timestamps(mbi_obj, efctv_dt)
            self.mbi_table[current_mbi] = mbi_obj

            # Update patient with current MBI and store previous for next iteration
            previous_mbi = patient["BENE_MBI_ID"]
            patient.kv["BENE_MBI_ID"] = current_mbi

    def generate_coverages(self, patient, files):
        parts = random.choices([["A"], ["B"], ["A", "B"], []], weights=[0.2, 0.2, 0.5, 0.1])[0]
        include_tp = random.random() > 0.2
        expired = random.random() < 0.2
        future = random.random() < 0.2
        self._generate_coverages(
            patient,
            files,
            coverage_parts=parts,
            include_tp=include_tp,
            expired=expired,
            future=future,
        )

    def _generate_coverages(self, patient, files, coverage_parts, include_tp, expired, future):
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

        stus_row = find_bene_sk(files[BENE_STUS], patient["BENE_SK"])
        stus_row["IDR_LTST_TRANS_FLG"] = "Y"
        stus_row["BENE_MDCR_STUS_CD"] = mdcr_stus_cd
        stus_row["MDCR_STUS_BGN_DT"] = medicare_start_date
        stus_row["MDCR_STUS_END_DT"] = medicare_end_date
        stus_row["IDR_TRANS_EFCTV_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        stus_row["IDR_INSRT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        stus_row["IDR_UPDT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        stus_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        self.mdcr_stus.append(stus_row.kv)

        buy_in_cd = random.choice(self.code_systems["BENE_BUYIN_CD"])

        entitlement_reason = random.choice(self.code_systems["BENE_MDCR_ENTLMT_RSN_CD"])
        rsn_row = find_bene_sk(files[BENE_ENTLMT_RSN], patient["BENE_SK"])
        rsn_row["IDR_LTST_TRANS_FLG"] = "Y"
        rsn_row["IDR_TRANS_EFCTV_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        rsn_row["IDR_INSRT_TS"] = (str(medicare_start_date) + "T00:00:00.000000+0000",)
        rsn_row["IDR_UPDT_TS"] = str(medicare_start_date) + "T00:00:00.000000+0000"
        rsn_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
        rsn_row["BENE_MDCR_ENTLMT_RSN_CD"] = entitlement_reason
        rsn_row["BENE_RNG_BGN_DT"] = medicare_start_date
        rsn_row["BENE_RNG_END_DT"] = medicare_end_date
        self.mdcr_rsn.append(rsn_row.kv)

        for coverage_type in coverage_parts:
            # ENTLMT
            entlmt_row = find_bene_sk(files[BENE_ENTLMT], patient["BENE_SK"])
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
            # TP
            if include_tp or contains_bene_sk(files[BENE_TP], patient["BENE_SK"]):
                tp_row = find_bene_sk(files[BENE_TP], patient["BENE_SK"])
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

        # Generate dual coverage data 50% of the time
        if probability(0.5):
            # Generate dual eligibility dates
            dual_start_date = self.fake.date_between_dates(
                datetime.date(year=2017, month=5, day=20),
                datetime.date(year=2021, month=1, day=1),
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

            dual_row = find_bene_sk(files[BENE_DUAL], patient["BENE_SK"])
            dual_row["IDR_LTST_TRANS_FLG"] = "Y"
            dual_row["BENE_DUAL_STUS_CD"] = dual_status_cd
            dual_row["BENE_DUAL_TYPE_CD"] = dual_type_cd
            dual_row["GEO_USPS_STATE_CD"] = medicaid_state_cd
            dual_row["BENE_MDCD_ELGBLTY_BGN_DT"] = str(dual_start_date)
            dual_row["BENE_MDCD_ELGBLTY_END_DT"] = dual_end_date
            dual_row["IDR_TRANS_EFCTV_TS"] = str(dual_start_date) + "T00:00:00.000000+0000"
            dual_row["IDR_INSRT_TS"] = str(dual_start_date) + "T00:00:00.000000+0000"
            dual_row["IDR_UPDT_TS"] = str(dual_start_date) + "T00:00:00.000000+0000"
            dual_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"
            self.bene_cmbnd_dual_mdcr.append(dual_row.kv)

    def generate_bene_lis(self, patient, files):
        if probability(0.5) or contains_bene_sk(files[BENE_LIS], patient["BENE_SK"]):
            lis_start_date = self.fake.date_between_dates(
                datetime.date(year=2017, month=5, day=20),
                datetime.date(year=2021, month=1, day=1),
            )
            lis_end_date = "9999-12-31"
            lis_efctv_cd = random.choice(self.code_systems["BENE_LIS_EFCTV_CD"])
            copmt_lvl_cd = random.choice(self.code_systems["BENE_LIS_COPMT_LVL_CD"])
            ptd_prm_pct = random.choice(["025", "050", "075", "100"])

            lis_row = find_bene_sk(files[BENE_LIS], patient["BENE_SK"])
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

    def generate_bene_mapd_enrlmt_rx(self, patient, files, contract_info):
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

        rx_row = find_bene_sk(files[BENE_MAPD_ENRLMT_RX], patient["BENE_SK"])
        rx_row["CNTRCT_PBP_SK"]= "".join(random.choices(string.digits, k=12))
        rx_row["BENE_ENRLMT_BGN_DT"]= str(enrollment_start_date)
        rx_row["BENE_ENRLMT_PDP_RX_INFO_BGN_DT"]=str(rx_start_date)
        rx_row["IDR_LTST_TRANS_FLG"] = "Y"
        rx_row["BENE_PDP_ENRLMT_MMBR_ID_NUM"] = member_id_num
        rx_row["BENE_PDP_ENRLMT_GRP_NUM"] = group_num
        rx_row["BENE_PDP_ENRLMT_PRCSR_NUM"] = prcsr_num
        rx_row["BENE_PDP_ENRLMT_BANK_ID_NUM"] = bank_id_num
        rx_row["BENE_CNTRCT_NUM"] = contract_info["contract_num"]
        rx_row["BENE_PBP_NUM"] = contract_info["pbp_num"]
        rx_row["IDR_TRANS_EFCTV_TS"] = str(rx_start_date) + "T00:00:00.000000+0000"
        rx_row["IDR_INSRT_TS"] = str(rx_start_date) + "T00:00:00.000000+0000"
        rx_row["IDR_UPDT_TS"] = str(rx_start_date) + "T00:00:00.000000+0000"
        rx_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

        self.bene_mapd_enrlmt_rx.append(rx_row.kv)

    def generate_bene_mapd_enrlmt(self, patient, files, pdp_only=False):
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

        enrollment_row = find_bene_sk(files[BENE_MAPD_ENRLMT], patient["BENE_SK"])
        enrollment_row["CNTRCT_PBP_SK"]= "".join(random.choices(string.digits, k=12))
        enrollment_row["IDR_LTST_TRANS_FLG"] = "Y"
        enrollment_row["BENE_CNTRCT_NUM"] = cntrct_num
        enrollment_row["BENE_PBP_NUM"] = pbp_num
        enrollment_row["BENE_CVRG_TYPE_CD"] = cvrg_type_cd
        enrollment_row["BENE_ENRLMT_PGM_TYPE_CD"]= bene_enrlmt_pgm_type_cd
        enrollment_row["BENE_ENRLMT_EMPLR_SBSDY_SW"]= bene_enrlmt_emplr_sbsdy_sw
        enrollment_row["BENE_ENRLMT_BGN_DT"] = str(enrollment_start_date)
        enrollment_row["BENE_ENRLMT_END_DT"] = enrollment_end_date
        enrollment_row["IDR_TRANS_EFCTV_TS"] = str(enrollment_start_date) + "T00:00:00.000000+0000"
        enrollment_row["IDR_INSRT_TS"] = str(enrollment_start_date) + "T00:00:00.000000+0000"
        enrollment_row["IDR_UPDT_TS"] = str(enrollment_start_date) + "T00:00:00.000000+0000"
        enrollment_row["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

        self.bene_mapd_enrlmt.append(enrollment_row)
        return {"contract_num": cntrct_num, "pbp_num": pbp_num}

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
    def export_df(data, out_path, cols=NO_COLS):
        df = pd.json_normalize(data)
        if cols != GeneratorUtil.NO_COLS:
            df = df[cols]
        df.to_csv(out_path, index=False)
