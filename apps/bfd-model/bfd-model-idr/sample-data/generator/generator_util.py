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
        self.bene_hstry_table = []
        self.bene_xref_table = []
        self.mbi_table = {}
        self.address_options = []
        self.mdcr_stus = []
        self.mdcr_entlmt = []
        self.mdcr_tp = []
        self.mdcr_rsn = []
        self.bene_cmbnd_dual_mdcr = []
        self.bene_lis = []
        self.bene_mapd_enrlmt_rx = []
        self.bene_mapd_enrlmt = []
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
    
    def generate_bene_xref(self, new_bene_sk, old_bene_sk):

        bene_hicn_num = str(random.randint(1000, 100000000)) + random.choice(string.ascii_letters)

        #10% chance for invalid xref.
        kill_cred_cd = 1 if random.randint(1, 10) == 1 else 2
        
        efctv_ts = self.fake.date_time_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.datetime.now() - datetime.timedelta(days=1)
        )
        src_rec_ctre_ts = self.fake.date_time_between_dates(efctv_ts, datetime.datetime.now() - datetime.timedelta(days=1))
        insrt_ts = self.fake.date_time_between_dates(efctv_ts, datetime.datetime.now() - datetime.timedelta(days=1))
        updt_ts = self.fake.date_time_between_dates(insrt_ts, datetime.datetime.now() - datetime.timedelta(days=1))
        
        xref_row = {
            "BENE_SK": str(new_bene_sk),
            "BENE_XREF_SK": str(old_bene_sk),
            "BENE_HICN_NUM": bene_hicn_num,
            "BENE_KILL_CRED_CD": str(kill_cred_cd),
            "SRC_REC_CRTE_TS": str(src_rec_ctre_ts),
            "IDR_TRANS_EFCTV_TS": str(efctv_ts),
            "IDR_INSRT_TS": str(insrt_ts),
            "IDR_UPDT_TS": str(updt_ts),
            "IDR_TRANS_OBSLT_TS": "9999-12-31T00:00:00.000000+0000"
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

    def create_base_patient(self):
        patient = {}
        self.set_timestamps(patient, datetime.date(year=2017, month=5, day=20))
        patient["CNTCT_LANG_CD"] = random.choice(["~", "ENG", "SPA"])
        patient["IDR_LTST_TRANS_FLG"] = "Y"
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
                
                if previous_mbi and previous_mbi != current_mbi:
                    historical_patient = copy.deepcopy(patient)
                    historical_patient["BENE_MBI_ID"] = previous_mbi
                    historical_patient["IDR_LTST_TRANS_FLG"] = "N"  
                    
                    self.set_timestamps(historical_patient, obslt_dt)
                    historical_patient["IDR_TRANS_OBSLT_TS"] = str(obslt_dt) + "T00:00:00.000000+0000"
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

        # Generate dual coverage data 50% of the time
        if random.choice([True, False]):
            # Generate dual eligibility dates
            dual_start_date = self.fake.date_between_dates(
                datetime.date(year=2017, month=5, day=20),
                datetime.date(year=2021, month=1, day=1)
            )
            dual_end_date = '9999-12-31'
            dual_status_cd = random.choice(self.code_systems['BENE_DUAL_STUS_CD'])
            dual_type_cd = random.choice(self.code_systems['BENE_DUAL_TYPE_CD'])
            
            state_codes = ['AL', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY', 'DC']
            medicaid_state_cd = random.choice(state_codes)
            
            dual_row = {
                "BENE_SK": patient['BENE_SK'],
                'IDR_LTST_TRANS_FLG': 'Y',
                'BENE_DUAL_STUS_CD': dual_status_cd,
                'BENE_DUAL_TYPE_CD': dual_type_cd,
                'MEDICAID_STATE_CD': medicaid_state_cd,
                'BENE_MDCD_ELGBLTY_BGN_DT': str(dual_start_date),
                'BENE_MDCD_ELGBLTY_END_DT': dual_end_date,
                "IDR_TRANS_EFCTV_TS": str(dual_start_date) + "T00:00:00.000000+0000",
                "IDR_INSRT_TS": str(dual_start_date) + "T00:00:00.000000+0000",
                "IDR_UPDT_TS": str(dual_start_date) + "T00:00:00.000000+0000",
                'IDR_TRANS_OBSLT_TS': '9999-12-31T00:00:00.000000+0000'
            }
            self.bene_cmbnd_dual_mdcr.append(dual_row)

    def generate_bene_lis(self, patient):
        if random.choice([True,False]):
            lis_start_date = self.fake.date_between_dates(
                datetime.date(year=2017, month=5, day=20),
                datetime.date(year=2021, month=1, day=1)
            )
            lis_end_date = '9999-12-31'
            lis_efctv_cd = random.choice(self.code_systems['BENE_LIS_EFCTV_CD'])
            copmt_lvl_cd = random.choice(self.code_systems['BENE_LIS_COPMT_LVL_CD'])
            ptd_prm_pct = random.choice(['025','050','075','100'])
            
            lis_row = {
                "BENE_SK": patient['BENE_SK'],
                'IDR_LTST_TRANS_FLG': 'Y',
                'BENE_LIS_EFCTV_CD': lis_efctv_cd,
                'BENE_LIS_COPMT_LVL_CD': copmt_lvl_cd,
                'BENE_LIS_PTD_PRM_PCT': str(ptd_prm_pct),
                'BENE_RNG_BGN_DT': str(lis_start_date),
                'BENE_RNG_END_DT': lis_end_date,
                "IDR_TRANS_EFCTV_TS": str(lis_start_date) + "T00:00:00.000000+0000",
                "IDR_INSRT_TS": str(lis_start_date) + "T00:00:00.000000+0000",
                "IDR_UPDT_TS": str(lis_start_date) + "T00:00:00.000000+0000",
                'IDR_TRANS_OBSLT_TS': '9999-12-31T00:00:00.000000+0000'
            }
            self.bene_lis.append(lis_row)

    def generate_bene_mapd_enrlmt_rx(self, patient,contract_info = None):
        rx_start_date = self.fake.date_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.date(year=2021, month=1, day=1)
        )
        member_id_num = str(random.randint(100000000, 999999999))
        group_num = str(random.randint(100, 999))
        prcsr_num = str(random.randint(100000, 999999))
        bank_id_num = str(random.randint(100000, 999999))
            
        rx_row = {
            "BENE_SK": patient['BENE_SK'],
            'IDR_LTST_TRANS_FLG': 'Y',
            'BENE_PDP_ENRLMT_MMBR_ID_NUM': member_id_num,
            'BENE_PDP_ENRLMT_GRP_NUM': group_num,
            'BENE_PDP_ENRLMT_PRCSR_NUM': prcsr_num,
            'BENE_PDP_ENRLMT_BANK_ID_NUM': bank_id_num,
            'BENE_CNTRCT_NUM': contract_info['contract_num'],
            'BENE_PBP_NUM': contract_info['pbp_num'],
            "IDR_TRANS_EFCTV_TS": str(rx_start_date) + "T00:00:00.000000+0000",
            "IDR_INSRT_TS": str(rx_start_date) + "T00:00:00.000000+0000",
            "IDR_UPDT_TS": str(rx_start_date) + "T00:00:00.000000+0000",
            'IDR_TRANS_OBSLT_TS': '9999-12-31T00:00:00.000000+0000'
        }
        self.bene_mapd_enrlmt_rx.append(rx_row)


    def generate_bene_mapd_enrlmt(self, patient, pdp_only=False):
        enrollment_start_date = self.fake.date_between_dates(
            datetime.date(year=2017, month=5, day=20),
            datetime.date(year=2021, month=1, day=1)
        )
        enrollment_end_date = '9999-12-31'

        if(pdp_only):
            cntrct_num = 'S0001'
        else:
            cntrct_num = random.choice(['H1234','G1234'])
        pbp_num = "001"
        cvrg_type_cd = '11' if pdp_only else '3'
            
        enrollment_row = {
            "BENE_SK": patient['BENE_SK'],
            'IDR_LTST_TRANS_FLG': 'Y',
            'BENE_CNTRCT_NUM': cntrct_num,
            'BENE_PBP_NUM': pbp_num,
            'BENE_CVRG_TYPE_CD': cvrg_type_cd,
            'BENE_ENRLMT_BGN_DT': str(enrollment_start_date),
            'BENE_ENRLMT_END_DT': enrollment_end_date,
            "IDR_TRANS_EFCTV_TS": str(enrollment_start_date) + "T00:00:00.000000+0000",
            "IDR_INSRT_TS": str(enrollment_start_date) + "T00:00:00.000000+0000",
            "IDR_UPDT_TS": str(enrollment_start_date) + "T00:00:00.000000+0000",
            'IDR_TRANS_OBSLT_TS': '9999-12-31T00:00:00.000000+0000'
        }
        self.bene_mapd_enrlmt.append(enrollment_row)
        return {"contract_num":cntrct_num,"pbp_num":pbp_num}

    def save_output_files(self):
        Path("out").mkdir(exist_ok=True)

        df = pd.json_normalize(self.bene_hstry_table)
        if(df.size>0):
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
                    "IDR_LTST_TRANS_FLG",
                    "IDR_TRANS_EFCTV_TS",
                    "IDR_INSRT_TS",
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
                "IDR_INSRT_TS",
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
        
        df = pd.json_normalize(self.bene_cmbnd_dual_mdcr)
        if(df.size>0):
            df.to_csv("out/SYNTHETIC_BENE_CMBND_DUAL_MDCR.csv", index=False)
            
        df = pd.json_normalize(self.bene_xref_table)
        if(df.size>0):
            df.to_csv("out/SYNTHETIC_BENE_XREF.csv", index=False)
            
        # Save new synthetic data tables
        df = pd.json_normalize(self.bene_lis)
        if(df.size>0):
            df.to_csv("out/SYNTHETIC_BENE_LIS.csv", index=False)
            
        df = pd.json_normalize(self.bene_mapd_enrlmt_rx)
        if(df.size>0):
            df.to_csv("out/SYNTHETIC_BENE_MAPD_ENRLMT_RX.csv", index=False)
            
        df = pd.json_normalize(self.bene_mapd_enrlmt)
        if(df.size>0):
            df.to_csv("out/SYNTHETIC_BENE_MAPD_ENRLMT.csv", index=False)