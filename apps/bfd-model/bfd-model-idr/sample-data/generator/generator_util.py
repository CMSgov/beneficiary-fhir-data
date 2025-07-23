import csv
import random
import copy
import string
import datetime
import pandas as pd
import os
import json
import subprocess
import sys
from dateutil.parser import parse
from faker import Faker
from pathlib import Path


class GeneratorUtil():
    def __init__(self):
        self.fake = Faker()
        self.used_bene_sk = []
        self.used_mbi = []
        self.bene_table = []
        self.bene_hstry_table = []
        self.mbi_table = {}
        self.address_options = []
        self.mdcr_stus = []
        self.mdcr_entlmt = []
        self.mdcr_tp = []
        self.mdcr_rsn = []
        self.code_systems  = {}

        self.load_addresses()
        self.load_code_systems()

    def load_code_systems(self):
        code_systems = {}
        relative_path = "../../sushi/fsh-generated/resources"
        
        # Check if the resources directory exists, if not run sushi build
        if not os.path.exists(relative_path):
            print("Running sushi build")
            try:
                sushi_dir = "../../sushi"
                result = subprocess.run(['sushi', 'build'], cwd=sushi_dir, capture_output=True, text=True)
                if result.returncode == 0:
                    print("Sushi build completed successfully")
                else:
                    print(f"Sushi build failed with error: {result.stderr}")
                    sys.exit(1)
            except Exception as e:
                print(f"Error running sushi build: {e}")
                sys.exit(1)
        
        try:
            for file in os.listdir(relative_path):
                if('.json' not in file or 'CodeSystem' not in file):
                    continue
                full_path = relative_path+"/"+file
                try:
                    with open(full_path, 'r') as file:
                        data = json.load(file)
                        concepts = []
                        for i in data['concept']:
                            #print(i['code'])
                            concepts.append(i['code'])
                        code_systems[data['name']] = concepts
                except FileNotFoundError:
                    print(f"Error: File not found at path: {full_path}")
                except json.JSONDecodeError:
                    print(f"Error: Invalid JSON format in file: {full_path}") 
        except FileNotFoundError:
            print(f"Error: Resources directory not found at path: {relative_path}")
            sys.exit(1)
        
        self.code_systems = code_systems 

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
        patient["IDR_TRANS_OBSLT_TS"] = "9999-12-31T00:00:00.000000+0000"

    def create_base_patient(self):
        patient = {}
        self.set_timestamps(patient, datetime.date(year=2017, month=5, day=20))
        patient["CNTCT_LANG_CD"] = random.choice(["~", "ENG", "SPA"])
        address = self.gen_address()
        for component in address:
            patient[component] = address[component]
        return patient

    def handle_mbis(self, patient, num_mbis, custom_first_mbi=None):
        previous_obslt_dt = None
        previous_mbi = None
        
        for mbi_idx in range(0, num_mbis):
            mbi_obj = {}
            
            if mbi_idx == 0:
                efctv_dt = self.fake.date_between_dates(
                    datetime.date(year=2017, month=5, day=20),
                    datetime.date(year=2021, month=1, day=1),
                )
                self.set_timestamps(patient, efctv_dt)

                if custom_first_mbi is not None:
                    patient["BENE_MBI_ID"] = custom_first_mbi
                    current_mbi = custom_first_mbi
                else:
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
            if mbi_idx != num_mbis - 1:
                # Calculate obsolescence date
                obslt_dt = self.fake.date_between_dates(
                    efctv_dt,
                    datetime.date(year=2025 - num_mbis + mbi_idx, month=1, day=1),
                )
                mbi_obj["BENE_MBI_OBSLT_DT"] = obslt_dt.strftime("%Y-%m-%d")
                
                # Create historical entry for the OLD MBI (not the new one)
                historical_patient = copy.deepcopy(patient)
                historical_patient["BENE_MBI_ID"] = previous_mbi if previous_mbi else patient["BENE_MBI_ID"]
                # Set the obsolescence timestamp for the historical entry
                self.set_timestamps(historical_patient, obslt_dt)
                self.bene_hstry_table.append(historical_patient)
                
                previous_obslt_dt = obslt_dt  # Store for next iteration
            else:
                # For the last MBI, no obsolescence date
                mbi_obj["BENE_MBI_OBSLT_DT"] = None

            self.set_timestamps(mbi_obj, efctv_dt)
            self.mbi_table[current_mbi] = mbi_obj
            
            # Update patient with current MBI and store previous for next iteration
            previous_mbi = patient["BENE_MBI_ID"]
            patient["BENE_MBI_ID"] = current_mbi

    def generate_coverages(self, patient):
        mdcr_stus_cd = '~'
        while(mdcr_stus_cd in ('0','~','00')):
            mdcr_stus_cd = random.choice(self.code_systems['BENE_MDCR_STUS_CD'])

        medicare_start_date = self.mbi_table[patient['BENE_MBI_ID']]['BENE_MBI_EFCTV_DT']
        medicare_end_date = '9999-12-31'
        if(random.randint(0,10)>8):
            end_dt=datetime.date.today().strftime("%Y-%m-%d")
        #STUS
        stus_row = {"BENE_SK":patient['BENE_SK'],
                    'IDR_LTST_TRANS_FLG':'Y',
                    'BENE_MDCR_STUS_CD': mdcr_stus_cd,
                    'MDCR_STUS_BGN_DT': medicare_start_date,
                    'MDCR_STUS_END_DT': medicare_end_date,
                    "IDR_TRANS_EFCTV_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                    "IDR_INSRT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                    "IDR_UPDT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                    'IDR_TRANS_OBSLT_TS':'9999-12-31T00:00:00.000000+0000',
                    }
        self.mdcr_stus.append(stus_row)    
        
        buy_in_cd = random.choice(self.code_systems['BENE_BUYIN_CD'])

        entitlement_reason = random.choice(self.code_systems['BENE_MDCR_ENTLMT_RSN_CD'])
        rsn_row = {"BENE_SK":patient['BENE_SK'],
                    'IDR_LTST_TRANS_FLG':'Y',
                    "IDR_TRANS_EFCTV_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                    "IDR_INSRT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",                        
                    "IDR_UPDT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                    'IDR_TRANS_OBSLT_TS':'9999-12-31T00:00:00.000000+0000',
                    'BENE_MDCR_ENTLMT_RSN_CD': entitlement_reason,
                    'BENE_RNG_BGN_DT': medicare_start_date,
                    'BENE_RNG_END_DT': medicare_end_date
                }
        self.mdcr_rsn.append(rsn_row)
        for coverage_type in ['A','B']:
            
            #ENTLMT
            entlmt_row = {"BENE_SK":patient['BENE_SK'],
                        'IDR_LTST_TRANS_FLG':'Y',
                        'BENE_MDCR_ENTLMT_TYPE_CD': coverage_type,
                        'BENE_MDCR_ENRLMT_RSN_CD': random.choice(self.code_systems['BENE_ENRLMT_RSN_CD']),
                        'BENE_MDCR_ENTLMT_STUS_CD': 'Y',
                        "IDR_TRANS_EFCTV_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                        "IDR_INSRT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                        "IDR_UPDT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                        'IDR_TRANS_OBSLT_TS':'9999-12-31T00:00:00.000000+0000',
                        'BENE_RNG_BGN_DT': medicare_start_date,
                        'BENE_RNG_END_DT': medicare_end_date
            }
            self.mdcr_entlmt.append(entlmt_row)
            #TP
            if(random.randint(0,10)==10):
                tp_row = {"BENE_SK":patient['BENE_SK'],
                            'IDR_LTST_TRANS_FLG':'Y',
                            'BENE_TP_TYPE_CD': coverage_type,
                            "IDR_TRANS_EFCTV_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                            "IDR_INSRT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                            "IDR_UPDT_TS": str(medicare_start_date) + "T00:00:00.000000+0000",
                            'IDR_TRANS_OBSLT_TS':'9999-12-31T00:00:00.000000+0000',
                            'BENE_RNG_BGN_DT':medicare_start_date,
                            'BENE_RNG_END_DT': medicare_end_date,
                            'BENE_BUYIN_CD': buy_in_cd
                }
                self.mdcr_tp.append(tp_row)
            

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
        if(df.size>0):
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

        df = pd.json_normalize(self.mdcr_stus)
        df.to_csv("out/SYNTHETIC_BENE_MDCR_STUS.csv", index=False)
        df = pd.json_normalize(self.mdcr_entlmt)
        df.to_csv("out/SYNTHETIC_BENE_MDCR_ENTLMT.csv", index=False)
        df = pd.json_normalize(self.mdcr_tp)
        df.to_csv("out/SYNTHETIC_BENE_TP.csv", index=False)
        
        df = pd.json_normalize(self.mdcr_rsn)
        df.to_csv("out/SYNTHETIC_BENE_MDCR_ENTLMT_RSN.csv", index=False)
        
