import csv
import random
import copy
import string
import datetime
import pandas as pd
from dateutil.parser import parse
from faker import Faker
from pathlib import Path


class PatientGeneratorUtil:
    def __init__(self):
        self.fake = Faker()
        self.used_bene_sk = []
        self.used_mbi = []
        self.bene_table = []
        self.bene_hstry_table = []
        self.mbi_table = {}
        self.address_options = []
        self.load_addresses()

    def load_addresses(self):
        with open("beneficiary-components/addresses.csv", "r") as file:
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
        if mbi in self.mbi_table.keys():
            return self.gen_mbi()
        return mbi

    def gen_bene_sk(self):
        bene_sk = random.randint(1000, 1000000000)
        if bene_sk in self.used_bene_sk:
            return self.gen_bene_sk()
        return bene_sk

    def gen_address(self):
        return self.address_options[random.randint(0, len(self.address_options) - 1)]

    def set_timestamps(self, patient, min_date):
        max_date = datetime.datetime.now() - datetime.timedelta(days=1)
        efctv_ts = self.fake.date_time_between_dates(min_date, max_date)
        patient["IDR_TRANS_EFCTV_TS"] = str(efctv_ts)
        patient["IDR_UPDT_TS"] = str(
            self.fake.date_time_between_dates(efctv_ts, max_date)
        )
        patient["IDR_TRANS_OBSLT_TS"] = "9999-12-31 00:00:00.000000"

    def create_base_patient(self):
        patient = {}
        self.set_timestamps(patient, datetime.date(year=2017, month=5, day=20))
        patient["CNTCT_LANG_CD"] = random.choice(["~", "ENG", "SPA"])
        address = self.gen_address()
        for component in address:
            patient[component] = address[component]
        return patient

    def handle_mbis(self, patient, num_mbis, custom_first_mbi=None):
        for mbi_idx in range(0, num_mbis):
            mbi_obj = {}
            mbi = self.gen_mbi()
            self.mbi_table[mbi] = {}

            if mbi_idx == 0:
                efctv_dt = self.fake.date_between_dates(
                    datetime.date(year=2017, month=5, day=20),
                    datetime.date(year=2021, month=1, day=1),
                )
                self.set_timestamps(patient, efctv_dt)

                if custom_first_mbi is not None:
                    patient["BENE_MBI_ID"] = custom_first_mbi
                else:
                    patient["BENE_MBI_ID"] = mbi
            else:
                efctv_dt = self.fake.date_between_dates(
                    datetime.date(year=2021, month=1, day=2),
                    datetime.date(year=2025 - num_mbis + mbi_idx, month=1, day=1),
                )

            mbi_obj["BENE_MBI_EFCTV_DT"] = str(efctv_dt)
            if mbi_idx != num_mbis - 1:
                mbi_obj["BENE_MBI_OBSLT_DT"] = self.fake.date_between_dates(
                    parse(mbi_obj["BENE_MBI_EFCTV_DT"]),
                    datetime.date(year=2025 - num_mbis + mbi_idx, month=1, day=1),
                ).strftime("%Y-%m-%d")
                historical_patient = copy.deepcopy(patient)
                historical_patient["BENE_MBI_ID"] = mbi
                self.set_timestamps(historical_patient, efctv_dt)
                self.bene_hstry_table.append(historical_patient)

            self.set_timestamps(mbi_obj, efctv_dt)
            self.mbi_table[mbi] = mbi_obj

    def save_output_files(self):
        Path("out").mkdir(exist_ok=True)

        df = pd.json_normalize(self.bene_table)
        df = df[
            [
                "BENE_SK",
                "BENE_XREF_EFCTV_SK",
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
                "IDR_TRANS_EFCTV_TS",
                "IDR_UPDT_TS",
                "IDR_TRANS_OBSLT_TS",
            ]
        ]
        df.to_csv("out/SYNTHETIC_BENE.csv", index=False)

        df = pd.json_normalize(self.bene_hstry_table)
        df = df[
            [
                "BENE_SK",
                "BENE_XREF_EFCTV_SK",
                "BENE_MBI_ID",
                "IDR_TRANS_EFCTV_TS",
                "IDR_UPDT_TS",
                "IDR_TRANS_OBSLT_TS",
            ]
        ]
        df.to_csv("out/SYNTHETIC_BENE_HSTRY.csv", index=False)

        arr = [{"BENE_MBI_ID": mbi, **self.mbi_table[mbi]} for mbi in self.mbi_table]
        df = pd.json_normalize(arr)
        df = df[
            [
                "BENE_MBI_ID",
                "BENE_MBI_EFCTV_DT",
                "BENE_MBI_OBSLT_DT",
                "IDR_TRANS_EFCTV_TS",
                "IDR_UPDT_TS",
                "IDR_TRANS_OBSLT_TS",
            ]
        ]
        df.to_csv("out/SYNTHETIC_BENE_MBI_ID.csv", index=False)
