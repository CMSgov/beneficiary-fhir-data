import argparse
import copy
import os
import random
import shutil
import string
import subprocess
import sys
from datetime import date, datetime, timedelta
from pathlib import Path

import pandas as pd
import yaml
from faker import Faker
from generator_util import GeneratorUtil
from pydantic import BaseModel, ConfigDict, Field, TypeAdapter


class SecurityLabelModel(BaseModel):
    model_config = ConfigDict(coerce_numbers_to_str=True)

    system: str
    code: str
    start_date: datetime = Field(validation_alias="startDate")
    end_date: datetime = Field(validation_alias="endDate")

    @property
    def normalized_code(self) -> str:
        return self.code.replace(".", "")


SECURITY_LABELS_ICD10_PROCEDURE_SYSTEMS = ["http://www.cms.gov/Medicare/Coding/ICD10"]
SECURITY_LABELS_ICD10_DIAGNOSIS_SYSTEMS = ["http://hl7.org/fhir/sid/icd-10-cm"]
SECURITY_LABELS_HCPCS_SYSTEMS = ["https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets"]
SECURITY_LABELS_CPT_SYSTEMS = ["http://www.ama-assn.org/go/cpt"]
SECURITY_LABELS_DRG_SYSTEMS = [
    "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software"
]
SECURITY_LABELS_YML = Path(os.path.realpath(__file__)).parent.joinpath("security_labels.yml")
SECURITY_LABELS = TypeAdapter(list[SecurityLabelModel]).validate_python(
    yaml.safe_load(SECURITY_LABELS_YML.read_text()), by_alias=True
)

generator = GeneratorUtil()
faker = Faker()


def save_output_files(
    clm,
    clm_line,
    clm_val,
    clm_dt_sgntr,
    clm_prod,
    clm_instnl,
    clm_line_instnl,
    clm_dcmtn,
    clm_fiss,
    clm_lctn_hstry,
    clm_prfnl,
    clm_line_prfnl,
    clm_line_rx,
):
    Path("out").mkdir(exist_ok=True)

    df = pd.json_normalize(clm)
    df["CLM_BLOOD_PT_FRNSH_QTY"] = df["CLM_BLOOD_PT_FRNSH_QTY"].astype("Int64")
    df["CLM_BLG_PRVDR_OSCAR_NUM"] = df["CLM_BLG_PRVDR_OSCAR_NUM"].astype("string")
    # Columns you want as string without decimal/nan
    int_to_string_cols = [
        "CLM_TYPE_CD",
        "CLM_NUM_SK",
        "PRVDR_PRSCRBNG_PRVDR_NPI_NUM",
        "PRVDR_RFRG_PRVDR_NPI_NUM",
        "PRVDR_BLG_PRVDR_NPI_NUM",
        "CLM_ATNDG_PRVDR_NPI_NUM",
        "CLM_OPRTG_PRVDR_NPI_NUM",
        "CLM_OTHR_PRVDR_NPI_NUM",
        "CLM_RNDRG_PRVDR_NPI_NUM",
    ]

    for col in int_to_string_cols:
        df[col] = (
            df[col]
            .astype("Int64")  # Handle floats like 1234.0 → 1234
            .astype("string")  # Pandas nullable string type
            .fillna("")  # Replace <NA> with empty string
        )
    df.to_csv("out/SYNTHETIC_CLM.csv", index=False)

    df = pd.json_normalize(clm_line)
    df["CLM_LINE_NUM"] = df["CLM_LINE_NUM"].astype("str")
    df.to_csv("out/SYNTHETIC_CLM_LINE.csv", index=False)
    df = pd.json_normalize(clm_val)
    df.to_csv("out/SYNTHETIC_CLM_VAL.csv", index=False)
    df = pd.json_normalize(clm_dt_sgntr)
    df.to_csv("out/SYNTHETIC_CLM_DT_SGNTR.csv", index=False)
    df = pd.json_normalize(clm_prod)
    df.to_csv("out/SYNTHETIC_CLM_PROD.csv", index=False)
    df = pd.json_normalize(clm_instnl)
    df.to_csv("out/SYNTHETIC_CLM_INSTNL.csv", index=False)
    df = pd.DataFrame(clm_line_instnl)
    df["CLM_LINE_NUM"] = df["CLM_LINE_NUM"].astype("str")
    df.to_csv("out/SYNTHETIC_CLM_LINE_INSTNL.csv", index=False)
    df = pd.json_normalize(clm_dcmtn)
    df.to_csv("out/SYNTHETIC_CLM_DCMTN.csv", index=False)
    df = pd.json_normalize(clm_lctn_hstry)
    df.to_csv("out/SYNTHETIC_CLM_LCTN_HSTRY.csv", index=False)
    df = pd.json_normalize(clm_fiss)
    df.to_csv("out/SYNTHETIC_CLM_FISS.csv", index=False)
    df = pd.json_normalize(clm_prfnl)
    df.to_csv("out/SYNTHETIC_CLM_PRFNL.csv", index=False)
    df = pd.json_normalize(clm_line_prfnl)
    df["CLM_LINE_NUM"] = df["CLM_LINE_NUM"].astype("str")
    df.to_csv("out/SYNTHETIC_CLM_LINE_PRFNL.csv", index=False)
    df = pd.json_normalize(clm_line_rx)
    df.to_csv("out/SYNTHETIC_CLM_LINE_RX.csv", index=False)
    # these are mostly static
    shutil.copy("sample-data/SYNTHETIC_CLM_ANSI_SGNTR.csv", "out/SYNTHETIC_CLM_ANSI_SGNTR.csv")


fiss_clm_type_cds = [
    1011,
    1041,
    1012,
    1013,
    1014,
    1022,
    1023,
    1034,
    1071,
    1072,
    1073,
    1074,
    1075,
    1076,
    1077,
    1083,
    1085,
    1087,
    1089,
    1032,
    1033,
    1081,
    1082,
    1021,
    1018,
    2011,
    2041,
    2012,
    2013,
    2014,
    2022,
    2023,
    2034,
    2071,
    2072,
    2073,
    2074,
    2075,
    2076,
    2077,
    2083,
    2085,
    2087,
    2089,
    2032,
    2033,
    2081,
    2082,
    2021,
    2018,
]
mcs_clm_type_cds = [1700, 2700]
vms_cds = [1800, 2800]

institutional_claim_types = [10, 20, 30, 40, 50, 60, 61, 62, 63, 64, *fiss_clm_type_cds]

type_1_npis = [
    1942945159,
    1437702123,
    1972944437,
    1447692959,
    1558719914,
    1730548868,
    1023051596,
    1003488552,
    1720749690,
]
type_2_npis = [
    1093792350,
    1548226988,
    1477643690,
    1104867175,
    1669572467,
    1508565987,
    1649041195,
]
avail_oscar_codes_institutional = [
    "39T14",
    "000000",
    "001500",
    "001502",
    "001503",
    "001504",
    "001505",
    "001509",
    "001510",
]

code_systems = {}

available_samhsa_icd_10_dgns_codes = [
    x.normalized_code  # IDR has codes without the dot
    for x in SECURITY_LABELS
    if x.system in SECURITY_LABELS_ICD10_DIAGNOSIS_SYSTEMS
]
available_non_samhsa_icd_10_dgns_codes = [
    "W6162",
    "V972",
    "V970",
    "W5922XA",
    "Z631",
    "W5541XA",
    "Y92311",
    "E1169",
    "R465",
    "V9733",
    "Y931",
    "R461",
    "E0170",
    "E0290",
    "W5529",
    "W213",
    "W5813XD",
    "W303XXA",
]
available_samhsa_icd_10_prcdr_codes = [
    x.normalized_code
    for x in SECURITY_LABELS
    if x.system in SECURITY_LABELS_ICD10_PROCEDURE_SYSTEMS
]
available_non_samhsa_icd_10_prcdr_codes = [
    "02HV33Z",
    "5A1D70Z",
    "30233N1",
    "B2111ZZ",
    "0BH17EZ",
    "4A023N7",
    "5A09357",
    "5A1955Z",
    "5A1945Z",
]
proc_codes_samhsa_cpt_hcpcs = [
    x.normalized_code
    for x in SECURITY_LABELS
    if x.system in SECURITY_LABELS_HCPCS_SYSTEMS or x.system in SECURITY_LABELS_CPT_SYSTEMS
]
proc_codes_non_samhsa_cpt_hcpcs = ["99213", "99453", "J2270"]
hcpcs_mods = ["1P", "22", "23", "28", "32", "U6", "US", "PC", "PD"]
available_ndc = [
    "00338004904",
    "00264180032",
    "00338011704",
    "00264180031",
    "00264780020",
]
clm_poa_ind_choices = ["N", "1", "U", "X", "W", "0", "~", "Z", "Y", ""]
avail_pbp_nums = ["001", "002", "003", "004", "005", "006", "007", "008", "009", "010"]
avail_clm_rlt_cond_sk = ["193064687", "117814", "193065597", "117853", "193074307"]

non_samhsa_dgns_drg_cds = list(range(43))
samhsa_dgns_drg_cds = [
    int(x.normalized_code) for x in SECURITY_LABELS if x.system in SECURITY_LABELS_DRG_SYSTEMS
]


# Choose SAMHSA codes 1% of the time
def get_icd_10_dgns_codes() -> list[str]:
    return random.choices(
        population=[available_samhsa_icd_10_dgns_codes, available_non_samhsa_icd_10_dgns_codes],
        weights=(1, 99),
        k=1,
    )[0]


def get_icd_10_prcdr_codes() -> list[str]:
    return random.choices(
        population=[available_samhsa_icd_10_prcdr_codes, available_non_samhsa_icd_10_prcdr_codes],
        weights=(1, 99),
        k=1,
    )[0]


def get_hcpcs_proc_codes() -> list[str]:
    return random.choices(
        population=[proc_codes_samhsa_cpt_hcpcs, proc_codes_non_samhsa_cpt_hcpcs],
        weights=(1, 99),
        k=1,
    )[0]


def get_drg_dgns_codes() -> list[int]:
    return random.choices(
        population=[samhsa_dgns_drg_cds, non_samhsa_dgns_drg_cds],
        weights=(1, 99),
        k=1,
    )[0]


def run_command(cmd, cwd=None):
    try:
        result = subprocess.run(
            cmd, cwd=cwd, shell=True, check=True, text=True, capture_output=True
        )
        return result.stdout, result.stderr
    except subprocess.CalledProcessError as e:
        print("Error running command:", cmd)
        if e.stderr:
            print("Error output:", e.stderr)
        else:
            print("Error info (not necessarily stderr):", e)
        sys.exit(1)


def random_date(start_date, end_date):
    start_formatted = date.fromisoformat(start_date).toordinal()
    end_formatted = date.fromisoformat(end_date).toordinal()
    rand_date = random.randint(start_formatted, end_formatted)
    return date.fromordinal(rand_date).isoformat()


def gen_thru_dt(frm_dt, max_days=30):
    from_date = date.fromisoformat(frm_dt)
    days_to_add = random.randint(0, max_days)
    return (from_date + timedelta(days=days_to_add)).isoformat()


def add_days(input_dt, days_to_add=0):
    return (date.fromisoformat(input_dt) + timedelta(days=days_to_add)).isoformat()


def add_diagnoses(clm_type_cd=-1):
    diagnosis_list = []
    num_diagnoses = 0
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        # inpatient uses concepts of principal, admitting, other, external
        principal_diagnosis = {
            "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "P",
            "CLM_POA_IND": "~",
        }
        first_diagnosis = {
            "CLM_DGNS_CD": principal_diagnosis["CLM_DGNS_CD"],
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "D",
            "CLM_POA_IND": random.choice(clm_poa_ind_choices),
        }
        admitting_diagnosis = {
            "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "A",
            "CLM_POA_IND": "~",
        }
        external_1 = {
            "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "E",
            "CLM_POA_IND": random.choice(clm_poa_ind_choices),
        }
        first_external = {
            "CLM_DGNS_CD": external_1["CLM_DGNS_CD"],
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "1",
            "CLM_POA_IND": "~",
        }
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        diagnosis_list.append(admitting_diagnosis)
        diagnosis_list.append(external_1)
        diagnosis_list.append(first_external)
        num_diagnoses = random.randint(2, 15)
    elif clm_type_cd == 40:
        # outpatient uses principal, other, external cause of injury, patient reason for visit
        principal_diagnosis = {
            "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "P",
            "CLM_POA_IND": "~",
        }
        first_diagnosis = {
            "CLM_DGNS_CD": principal_diagnosis["CLM_DGNS_CD"],
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "D",
            "CLM_POA_IND": "~",
        }
        rfv_diag = {
            "CLM_DGNS_CD": principal_diagnosis["CLM_DGNS_CD"],
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "1",
            "CLM_POA_IND": "~",
        }
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        diagnosis_list.append(rfv_diag)
        num_diagnoses = random.randint(2, 15)
    elif clm_type_cd in (71, 72, 81, 82):
        # professional claims use principal diagnosis and other diagnoses
        principal_diagnosis = {
            "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "P",
            "CLM_POA_IND": "~",
        }
        first_diagnosis = {
            "CLM_DGNS_CD": principal_diagnosis["CLM_DGNS_CD"],
            "CLM_VAL_SQNC_NUM": "1",
            "CLM_DGNS_PRCDR_ICD_IND": "0",
            "CLM_PROD_TYPE_CD": "D",
            "CLM_POA_IND": "~",
        }
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        num_diagnoses = random.randint(2, 8)  # Professional claims typically have fewer diagnoses

    if num_diagnoses > 1 and clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        for diagnosis_sqnc in range(2, num_diagnoses):
            diagnosis = {
                "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
                "CLM_VAL_SQNC_NUM": diagnosis_sqnc,
                "CLM_DGNS_PRCDR_ICD_IND": "0",
                "CLM_PROD_TYPE_CD": "D",
                "CLM_POA_IND": random.choice(clm_poa_ind_choices),
            }
            diagnosis_list.append(diagnosis)
    elif clm_type_cd == 40:
        for diagnosis_sqnc in range(2, num_diagnoses):
            diagnosis = {
                "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
                "CLM_VAL_SQNC_NUM": diagnosis_sqnc,
                "CLM_DGNS_PRCDR_ICD_IND": "0",
                "CLM_PROD_TYPE_CD": "D",
            }
            diagnosis_list.append(diagnosis)
    elif clm_type_cd in (71, 72, 81, 82):
        for diagnosis_sqnc in range(2, num_diagnoses):
            diagnosis = {
                "CLM_DGNS_CD": random.choice(get_icd_10_dgns_codes()),
                "CLM_VAL_SQNC_NUM": diagnosis_sqnc,
                "CLM_DGNS_PRCDR_ICD_IND": "0",
                "CLM_PROD_TYPE_CD": "D",
                "CLM_POA_IND": "~",
            }
            diagnosis_list.append(diagnosis)

    return diagnosis_list


def gen_procedure_icd10pcs():
    procedure = {}
    procedure["CLM_PROD_TYPE_CD"] = "S"
    procedure["CLM_PRCDR_CD"] = random.choice(get_icd_10_prcdr_codes())
    procedure["CLM_DGNS_PRCDR_ICD_IND"] = "0"
    return procedure


now = date.today()


def gen_claim(bene_sk="-1", min_date="2018-01-01", max_date=str(now)):
    claim = {
        "CLM": {},
        "CLM_LINE": [],
        "CLM_DT_SGNTR": {},
        "CLM_LINE_INSTNL": [],
        "CLM_DCMTN": {},
        "CLM_PRFNL": {},
        "CLM_LINE_PRFNL": [],
        "CLM_LINE_RX": [],
    }
    clm_dt_sgntr = {}
    clm_dt_sgntr["CLM_DT_SGNTR_SK"] = "".join(random.choices(string.digits, k=12))
    claim["CLM"]["CLM_DT_SGNTR_SK"] = clm_dt_sgntr["CLM_DT_SGNTR_SK"]
    claim["CLM"]["CLM_UNIQ_ID"] = "-" + "".join(random.choices(string.digits, k=13))
    # clm_type_cd = 60
    clm_type_cd = random.choice([1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 71, 72, 81, 82])
    claim["CLM"]["CLM_TYPE_CD"] = clm_type_cd

    clm_src_id = -1
    clm_src_id = 20000
    claim["CLM"]["CLM_SRC_ID"] = clm_src_id
    claim["CLM"]["CLM_FROM_DT"] = random_date(min_date, max_date)
    claim["CLM"]["CLM_THRU_DT"] = gen_thru_dt(claim["CLM"]["CLM_FROM_DT"])

    # NON-PDE
    claim["CLM"]["CLM_CNTL_NUM"] = "".join(random.choices(string.digits, k=14)) + "".join(
        random.choices(string.ascii_uppercase, k=3)
    )
    # PDE -> diff Claim control number process.
    if clm_type_cd in (1, 2, 3, 4):
        claim["CLM"]["CLM_ORIG_CNTL_NUM"] = "".join(random.choices(string.digits, k=14)) + "".join(
            random.choices(string.ascii_uppercase, k=3)
        )
        claim["CLM"]["CLM_RLT_COND_SGNTR_SK"] = "-1"

    if clm_type_cd in (20, 30, 40, 60, 61, 62, 63, 71, 72):
        claim["CLM"]["CLM_BLOOD_PT_FRNSH_QTY"] = random.randint(0, 20)

    claim["CLM"]["CLM_NUM_SK"] = 1
    claim["CLM"]["CLM_EFCTV_DT"] = str(date.today())
    claim["CLM"]["CLM_IDR_LD_DT"] = random_date(claim["CLM"]["CLM_FROM_DT"], max_date)
    claim["CLM"]["CLM_OBSLT_DT"] = "9999-12-31"
    claim["CLM"]["GEO_BENE_SK"] = "".join(random.choices(string.digits, k=5))
    claim["CLM"]["BENE_SK"] = bene_sk
    claim["CLM"]["CLM_DISP_CD"] = random.choice(generator.code_systems["CLM_DISP_CD"])
    claim["CLM"]["CLM_QUERY_CD"] = random.choice(generator.code_systems["CLM_QUERY_CD"])
    claim["CLM"]["CLM_ADJSTMT_TYPE_CD"] = random.choice(
        generator.code_systems["CLM_ADJSTMT_TYPE_CD"]
    )

    if clm_type_cd in (1, 2, 3, 4):
        claim["CLM"]["CLM_SRVC_PRVDR_GNRC_ID_NUM"] = random.choice(type_2_npis)
        claim["CLM"]["CLM_PD_DT"] = random_date(
            claim["CLM"]["CLM_FROM_DT"], claim["CLM"]["CLM_THRU_DT"]
        )
        claim["CLM"]["PRVDR_PRSCRBNG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
        claim["CLM"]["CLM_SBMT_CHRG_AMT"] = round(random.uniform(1, 1000000), 2)
        claim["CLM"]["CLM_SBMT_FRMT_CD"] = random.choice(generator.code_systems["CLM_SBMT_FRMT_CD"])
        claim["CLM"]["CLM_SBMTR_CNTRCT_NUM"] = "Z0001"
        claim["CLM"]["CLM_SBMTR_CNTRCT_PBP_NUM"] = random.choice(avail_pbp_nums)
        claim_line = {}
        claim_line["CLM_UNIQ_ID"] = claim["CLM"]["CLM_UNIQ_ID"]
        claim_line["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
        claim_line["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
        claim_line["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
        claim_line["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
        claim_line["CLM_LINE_CVRD_PD_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line["CLM_LINE_NCVRD_PD_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line["CLM_LINE_NCVRD_CHRG_AMT"] = round(random.uniform(0, 1500), 2)
        claim_line["CLM_LINE_NDC_CD"] = random.choice(available_ndc)
        claim_line["CLM_LINE_SRVC_UNIT_QTY"] = random.randint(1, 10)
        claim_line["CLM_LINE_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
        claim_line["CLM_LINE_THRU_DT"] = claim["CLM"]["CLM_THRU_DT"]
        claim_line["CLM_LINE_NDC_QTY"] = random.randint(1, 10)
        claim_line["CLM_LINE_NDC_QTY_QLFYR_CD"] = "ML"
        claim_line["CLM_LINE_BENE_PD_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line["CLM_LINE_PRVDR_PMT_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line["CLM_LINE_SBMT_CHRG_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_BENE_PMT_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_BLOOD_DDCTBL_AMT"] = round(random.uniform(0, 15), 2)
        claim_line["CLM_LINE_MDCR_DDCTBL_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_NUM"] = "1"
        claim_line["CLM_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
        claim_line["CLM_LINE_RX_NUM"] = round(random.uniform(0, 100000), 2)

        claim_line_rx = {}
        claim_line_rx["CLM_UNIQ_ID"] = claim["CLM"]["CLM_UNIQ_ID"]
        claim_line_rx["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
        claim_line_rx["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
        claim_line_rx["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
        claim_line_rx["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
        claim_line_rx["CLM_LINE_NUM"] = "1"
        claim_line_rx["CLM_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
        claim_line_rx["CLM_DSPNSNG_STUS_CD"] = random.choice(["P", "C"])
        claim_line_rx["CLM_LINE_RX_ORGN_CD"] = random.choice(
            generator.code_systems["CLM_LINE_RX_ORGN_CD"]
        )
        claim_line_rx["CLM_BRND_GNRC_CD"] = random.choice(
            generator.code_systems["CLM_BRND_GNRC_CD"]
        )
        claim_line_rx["CLM_PTNT_RSDNC_CD"] = random.choice(
            generator.code_systems["CLM_PTNT_RSDNC_CD"]
        )
        claim_line_rx["CLM_PHRMCY_SRVC_TYPE_CD"] = random.choice(
            generator.code_systems["CLM_PHRMCY_SRVC_TYPE_CD"]
        )
        claim_line_rx["CLM_LINE_AUTHRZD_FILL_NUM"] = (
            "0"  # for whatever reason, this is always zero in the IDR
        )
        claim_line_rx["CLM_LTC_DSPNSNG_MTHD_CD"] = random.choice(
            generator.code_systems["CLM_LTC_DSPNSNG_MTHD_CD"]
        )
        claim_line_rx["CLM_CMPND_CD"] = random.choice(generator.code_systems["CLM_CMPND_CD"])
        claim_line_rx["CLM_LINE_DAYS_SUPLY_QTY"] = random.randint(1, 10)
        claim_line_rx["CLM_LINE_RX_FILL_NUM"] = random.randint(1, 10)
        claim_line_rx["CLM_DAW_PROD_SLCTN_CD"] = random.choice([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])
        claim_line_rx["CLM_DRUG_CVRG_STUS_CD"] = random.choice(
            generator.code_systems["CLM_DRUG_CVRG_STUS_CD"]
        )
        claim_line_rx["CLM_CTSTRPHC_CVRG_IND_CD"] = random.choice(
            generator.code_systems["CLM_CTSTRPHC_CVRG_IND_CD"]
        )
        claim_line_rx["CLM_LINE_GRS_ABOVE_THRSHLD_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_GRS_BLW_THRSHLD_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_LIS_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_TROOP_TOT_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_PLRO_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_RPTD_MFTR_DSCNT_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_INGRDNT_CST_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_VCCN_ADMIN_FEE_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_SRVC_CST_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_LINE_SLS_TAX_AMT"] = round(random.uniform(1, 1000000), 2)
        claim_line_rx["CLM_PRCNG_EXCPTN_CD"] = random.choice(["", "O", "M"])

        claim["CLM_LINE"].append(claim_line)
        claim["CLM_LINE_RX"].append(claim_line_rx)

    tob_code = random.choice(generator.code_systems["CLM_BILL_FREQ_CD"])
    claim["CLM"]["CLM_BILL_FAC_TYPE_CD"] = tob_code[0]
    claim["CLM"]["CLM_BILL_CLSFCTN_CD"] = tob_code[1]
    claim["CLM"]["CLM_BILL_FREQ_CD"] = tob_code[2]

    claim["CLM"]["CLM_CNTRCTR_NUM"] = random.choice(generator.code_systems["CLM_CNTRCTR_NUM"])
    claim["CLM"]["CLM_NCH_PRMRY_PYR_CD"] = random.choice(
        generator.code_systems["CLM_NCH_PRMRY_PYR_CD"]
    )

    add_meta_timestamps(claim["CLM"], claim["CLM"], max_date)
    add_meta_timestamps(clm_dt_sgntr, claim["CLM"], max_date)

    clm_finl_actn_ind = "N"
    if clm_type_cd in (1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 61, 62, 63, 71, 72, 81, 82):
        clm_finl_actn_ind = "Y"
    elif clm_type_cd >= 2000 and clm_type_cd < 2800:
        clm_finl_actn_ind = random.choice(["Y", "N"])
    claim["CLM"]["CLM_FINL_ACTN_IND"] = clm_finl_actn_ind

    clm_ltst_clm_ind = "N"
    if clm_type_cd in (1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 61, 62, 63, 71, 72, 81, 82):
        clm_ltst_clm_ind = random.choice(["Y", "N"])
    claim["CLM"]["CLM_LTST_CLM_IND"] = clm_ltst_clm_ind

    claim["CLM_DCMTN"]["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
    claim["CLM_DCMTN"]["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
    claim["CLM_DCMTN"]["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
    claim["CLM_DCMTN"]["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]

    # CLM_RIC_CDs are generally tied to the claim type code.
    if clm_type_cd in (20, 30, 50, 60, 61, 62, 63, 64):
        # part A!
        claim["CLM_DCMTN"]["CLM_NRLN_RIC_CD"] = "V"  # inpatient
    elif clm_type_cd == 40:
        # outpatient
        claim["CLM_DCMTN"]["CLM_NRLN_RIC_CD"] = "W"  # outpatient
    elif clm_type_cd == 10:
        claim["CLM_DCMTN"]["CLM_NRLN_RIC_CD"] = random.choice(["U", "V", "W"])
    elif clm_type_cd in (71, 72):
        claim["CLM_DCMTN"]["CLM_NRLN_RIC_CD"] = "O"
    elif clm_type_cd in (81, 82):
        claim["CLM_DCMTN"]["CLM_NRLN_RIC_CD"] = "M"

    add_meta_timestamps(claim["CLM_DCMTN"], claim["CLM"], max_date)

    # provider elements:
    if (clm_type_cd < 65 and clm_type_cd >= 10) or clm_type_cd in fiss_clm_type_cds:
        claim["CLM"]["PRVDR_BLG_PRVDR_NPI_NUM"] = random.choice(type_2_npis)
        claim["CLM"]["CLM_ATNDG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
        claim["CLM"]["CLM_OPRTG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
        claim["CLM"]["CLM_OTHR_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
        claim["CLM"]["CLM_RNDRG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
        claim["CLM"]["CLM_BLG_PRVDR_OSCAR_NUM"] = random.choice(avail_oscar_codes_institutional)
        claim["CLM"]["CLM_MDCR_COINSRNC_AMT"] = round(random.uniform(0, 25), 2)
        claim["CLM"]["CLM_BLG_PRVDR_ZIP5_CD"] = random.choice(["75205", "77550", "77005"])
        claim["CLM"]["CLM_RLT_COND_SGNTR_SK"] = random.choice(avail_clm_rlt_cond_sk)

    if clm_type_cd == 40 or (clm_type_cd > 70 and clm_type_cd <= 82):
        claim["CLM"]["PRVDR_RFRG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
    if clm_type_cd > 70 and clm_type_cd <= 82:
        claim["CLM"]["CLM_BLG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
        claim["CLM"]["CLM_RLT_COND_SGNTR_SK"] = "0"
        if random.choice([0, 1]):
            claim["CLM"]["CLM_BLG_PRVDR_NPI_NUM"] = random.choice(type_2_npis)

    # generate claim header financial elements here
    claim["CLM"]["CLM_SBMT_CHRG_AMT"] = round(random.uniform(1, 1000000), 2)
    if clm_type_cd == 71 or clm_type_cd == 72:
        claim["CLM"]["CLM_RFRG_PRVDR_PIN_NUM"] = random.choice(
            [
                9181272397,
                9181272391,
                918127239123,
            ]
        )
    if clm_type_cd > 70 and clm_type_cd <= 82:
        claim["CLM"]["CLM_ALOWD_CHRG_AMT"] = round(random.uniform(1, 1000000), 2)
        claim["CLM"]["CLM_BENE_PD_AMT"] = round(random.uniform(1, 1000000), 2)
        claim["CLM"]["CLM_BENE_PMT_AMT"] = round(random.uniform(1, 1000000), 2)
        claim["CLM"]["CLM_PRVDR_PMT_AMT"] = round(random.uniform(1, 1000000), 2)
    claim["CLM"]["CLM_PMT_AMT"] = round(random.uniform(1, claim["CLM"]["CLM_SBMT_CHRG_AMT"]), 2)
    claim["CLM"]["CLM_MDCR_DDCTBL_AMT"] = round(random.uniform(1, 1676), 2)
    claim["CLM"]["CLM_NCVRD_CHRG_AMT"] = round(
        claim["CLM"]["CLM_SBMT_CHRG_AMT"] - claim["CLM"]["CLM_PMT_AMT"], 2
    )
    claim["CLM"]["CLM_BLOOD_LBLTY_AMT"] = round(random.uniform(0, 25), 2)

    if clm_type_cd in (40, 71, 72, 81, 82):
        # be sure to check that DME claims meet the above.
        claim["CLM"]["CLM_PRVDR_PMT_AMT"] = round(random.uniform(0, 25), 2)

    claim["CLM_VAL"] = []
    # CLM_OPRTNL_DSPRTNT_AMT + CLM_OPRTNL_IME_AMT
    if clm_type_cd in (20, 40, 60, 61, 62, 63, 64):
        # Note, this is a table we'll use sparsely, it appears. I've replaced the 5 key unique
        # identifier with CLM_UNIQ_ID.
        clm_val_dsprtnt = {
            "CLM_DT_SGNTR_SK": claim["CLM"]["CLM_DT_SGNTR_SK"],
            "CLM_NUM_SK": claim["CLM"]["CLM_NUM_SK"],
            "GEO_BENE_SK": claim["CLM"]["GEO_BENE_SK"],
            "CLM_TYPE_CD": claim["CLM"]["CLM_TYPE_CD"],
            "CLM_VAL_CD": 18,
            "CLM_VAL_AMT": round(random.uniform(1, 15000), 2),
            "CLM_VAL_SQNC_NUM": 14,
        }
        claim["CLM_VAL"].append(clm_val_dsprtnt)
        clm_val_ime = {
            "CLM_DT_SGNTR_SK": claim["CLM"]["CLM_DT_SGNTR_SK"],
            "CLM_NUM_SK": claim["CLM"]["CLM_NUM_SK"],
            "GEO_BENE_SK": claim["CLM"]["GEO_BENE_SK"],
            "CLM_TYPE_CD": claim["CLM"]["CLM_TYPE_CD"],
            "CLM_VAL_CD": 19,
            "CLM_VAL_AMT": round(random.uniform(1, 15000), 2),
            "CLM_VAL_SQNC_NUM": 3,
        }
        claim["CLM_VAL"].append(clm_val_ime)
        for clm_val in claim["CLM_VAL"]:
            add_meta_timestamps(clm_val, claim["CLM"], max_date)

    # Add procedures
    claim["CLM_PROD"] = []
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        num_procedures_to_add = random.randint(1, 5)
        for proc in range(1, num_procedures_to_add):
            procedure = gen_procedure_icd10pcs()
            procedure["CLM_PRCDR_PRFRM_DT"] = random_date(
                claim["CLM"]["CLM_FROM_DT"], claim["CLM"]["CLM_THRU_DT"]
            )
            procedure["CLM_VAL_SQNC_NUM"] = proc
            procedure["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
            procedure["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
            procedure["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
            procedure["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
            add_meta_timestamps(procedure, claim["CLM"], max_date)
            claim["CLM_PROD"].append(procedure)

    # add diagnoses
    diagnoses = add_diagnoses(clm_type_cd=clm_type_cd)
    for diagnosis in diagnoses:
        diagnosis["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
        diagnosis["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
        diagnosis["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
        diagnosis["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
        claim["CLM_PROD"].append(diagnosis)
        add_meta_timestamps(diagnosis, claim["CLM"], max_date)

    # clm_dt_sgntr info
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        clm_dt_sgntr["CLM_ACTV_CARE_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
        clm_dt_sgntr["CLM_DSCHRG_DT"] = claim["CLM"]["CLM_THRU_DT"]
        if clm_type_cd in (20, 30):
            if random.choice([0, 1]):
                clm_dt_sgntr["CLM_QLFY_STAY_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
                clm_dt_sgntr["CLM_QLFY_STAY_THRU_DT"] = claim["CLM"]["CLM_THRU_DT"]
            else:
                clm_dt_sgntr["CLM_QLFY_STAY_FROM_DT"] = "1000-01-01"
                clm_dt_sgntr["CLM_QLFY_STAY_THRU_DT"] = "1000-01-01"

        if clm_type_cd in (50, 60, 61, 62, 63, 64):
            clm_dt_sgntr["CLM_MDCR_EXHSTD_DT"] = claim["CLM"]["CLM_THRU_DT"]
            if random.choice([0, 1]):
                clm_dt_sgntr["CLM_NCVRD_FROM_DT"] = claim["CLM"]["CLM_THRU_DT"]
                clm_dt_sgntr["CLM_NCVRD_THRU_DT"] = claim["CLM"]["CLM_THRU_DT"]
            else:
                clm_dt_sgntr["CLM_NCVRD_FROM_DT"] = "1000-01-01"
                clm_dt_sgntr["CLM_NCVRD_THRU_DT"] = "1000-01-01"
            if clm_type_cd >= 60:
                clm_dt_sgntr["CLM_ACTV_CARE_THRU_DT"] = claim["CLM"]["CLM_THRU_DT"]

    clm_dt_sgntr["CLM_SUBMSN_DT"] = claim["CLM"][
        "CLM_THRU_DT"
    ]  # This synthetic hospital is really on top of it!

    # clm_dt_sgntr['CLM_MDCR_NCH_PTNT_STUS_IND_CD'] =
    # random.choice(code_systems['CLM_MDCR_NCH_PTNT_STUS_IND_CD'])
    clm_dt_sgntr["CLM_CMS_PROC_DT"] = claim["CLM"]["CLM_THRU_DT"]
    clm_dt_sgntr["CLM_NCH_WKLY_PROC_DT"] = claim["CLM"]["CLM_THRU_DT"]
    claim["CLM_DT_SGNTR"] = clm_dt_sgntr

    if clm_type_cd in institutional_claim_types:
        institutional_parts = {}
        institutional_parts["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
        institutional_parts["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
        institutional_parts["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
        institutional_parts["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
        if clm_type_cd == 40:
            institutional_parts["CLM_OP_SRVC_TYPE_CD"] = random.choice(
                generator.code_systems["CLM_OP_SRVC_TYPE_CD"]
            )
        institutional_parts["CLM_FI_ACTN_CD"] = random.choice(
            generator.code_systems["CLM_FI_ACTN_CD"]
        )
        institutional_parts["CLM_ADMSN_TYPE_CD"] = random.choice(
            generator.code_systems["CLM_ADMSN_TYPE_CD"]
        )
        institutional_parts["BENE_PTNT_STUS_CD"] = random.choice(
            generator.code_systems["BENE_PTNT_STUS_CD"]
        )
        institutional_parts["CLM_MDCR_INSTNL_MCO_PD_SW"] = random.choice(
            generator.code_systems["CLM_MDCR_INSTNL_MCO_PD_SW"]
        )
        institutional_parts["CLM_ADMSN_SRC_CD"] = random.choice(
            generator.code_systems["CLM_ADMSN_SRC_CD"]
        )
        institutional_parts["DGNS_DRG_CD"] = random.choice(get_drg_dgns_codes())
        institutional_parts["DGNS_DRG_OUTLIER_CD"] = random.choice(
            generator.code_systems["DGNS_DRG_OUTLIER_CD"]
        )
        institutional_parts["CLM_INSTNL_CVRD_DAY_CNT"] = random.randint(0, 10)
        institutional_parts["CLM_MDCR_IP_LRD_USE_CNT"] = random.randint(0, 10)
        institutional_parts["CLM_INSTNL_PER_DIEM_AMT"] = round(random.uniform(0, 350), 2)
        institutional_parts["CLM_HIPPS_UNCOMPD_CARE_AMT"] = round(random.uniform(0, 350), 2)
        institutional_parts["CLM_MDCR_INSTNL_PRMRY_PYR_AMT"] = round(random.uniform(0, 3500), 2)
        institutional_parts["CLM_INSTNL_DRG_OUTLIER_AMT"] = round(random.uniform(0, 3500), 2)
        institutional_parts["CLM_MDCR_IP_PPS_DSPRPRTNT_AMT"] = round(random.uniform(0, 3500), 2)
        institutional_parts["CLM_INSTNL_MDCR_COINS_DAY_CNT"] = random.randint(0, 5)
        institutional_parts["CLM_INSTNL_NCVRD_DAY_CNT"] = random.randint(0, 5)
        institutional_parts["CLM_MDCR_IP_PPS_DRG_WT_NUM"] = round(random.uniform(0.5, 1.5), 2)
        institutional_parts["CLM_MDCR_IP_PPS_EXCPTN_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_MDCR_IP_PPS_CPTL_FSP_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_MDCR_IP_PPS_CPTL_IME_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_MDCR_IP_PPS_OUTLIER_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_MDCR_IP_PPS_CPTL_TOT_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_MDCR_IP_BENE_DDCTBL_AMT"] = round(random.uniform(0, 25), 2)
        institutional_parts["CLM_PPS_IND_CD"] = random.choice(["", "2"])
        add_meta_timestamps(institutional_parts, claim["CLM"], max_date)

        if clm_type_cd == 10:
            if random.choice([0, 1]):
                institutional_parts["CLM_HHA_LUP_IND_CD"] = "L"
            institutional_parts["CLM_HHA_RFRL_CD"] = random.choice(
                generator.code_systems["CLM_HHA_RFRL_CD"]
            )
            institutional_parts["CLM_MDCR_HHA_TOT_VISIT_CNT"] = round(random.uniform(0, 25), 2)

        if clm_type_cd == 40:
            institutional_parts["CLM_MDCR_INSTNL_BENE_PD_AMT"] = round(random.uniform(0, 25), 2)

        if clm_type_cd == 50:
            institutional_parts["CLM_MDCR_HOSPC_PRD_CNT"] = random.choice(["1", "2", "3"])
        # We'll throw in a non-payment code on occasion
        if random.choice([0, 10]) > 1:
            institutional_parts["CLM_MDCR_NPMT_RSN_CD"] = random.choice(
                generator.code_systems["CLM_MDCR_NPMT_RSN_CD"]
            )
        claim["CLM_INSTNL"] = institutional_parts

    # professional "stuff"
    claim["CLM_PRFNL"]["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
    claim["CLM_PRFNL"]["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
    claim["CLM_PRFNL"]["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
    claim["CLM_PRFNL"]["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
    claim["CLM_PRFNL"]["CLM_CARR_PMT_DNL_CD"] = random.choice(
        generator.code_systems["CLM_CARR_PMT_DNL_CD"]
    )
    claim["CLM_PRFNL"]["CLM_MDCR_PRFNL_PRMRY_PYR_AMT"] = round(random.uniform(10, 1000), 2)
    claim["CLM_PRFNL"]["CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW"] = random.choice(
        generator.code_systems["CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW"]
    )
    claim["CLM_PRFNL"]["CLM_CLNCL_TRIL_NUM"] = str(random.randint(0, 10000))
    add_meta_timestamps(claim["CLM_PRFNL"], claim["CLM"], max_date)

    num_clm_lines = random.randint(1, 15)
    for line_num in range(1, num_clm_lines + 1):
        if clm_type_cd in (1, 2, 3, 4):
            # handled above
            continue
        claim_line = {}
        claim_line_inst = {}
        claim_line_prfnl = {}
        claim_line["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
        claim_line["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
        claim_line["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
        claim_line["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]
        claim_line["CLM_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
        claim_line["CLM_LINE_FROM_DT"] = claim["CLM"]["CLM_FROM_DT"]
        claim_line["CLM_LINE_THRU_DT"] = claim["CLM"]["CLM_THRU_DT"]
        if clm_type_cd >= 10 and clm_type_cd <= 64:
            claim_line_inst["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
            claim_line_inst["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
            claim_line_inst["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
            claim_line_inst["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]

        if clm_type_cd >= 71 and clm_type_cd <= 82:
            claim_line["CLM_RNDRG_PRVDR_TYPE_CD"] = random.choice(
                generator.code_systems["CLM_PRVDR_TYPE_CD"]
            )
            claim_line_prfnl["GEO_BENE_SK"] = claim["CLM"]["GEO_BENE_SK"]
            claim_line_prfnl["CLM_DT_SGNTR_SK"] = claim["CLM"]["CLM_DT_SGNTR_SK"]
            claim_line_prfnl["CLM_TYPE_CD"] = claim["CLM"]["CLM_TYPE_CD"]
            claim_line_prfnl["CLM_NUM_SK"] = claim["CLM"]["CLM_NUM_SK"]

            claim_line_prfnl["CLM_BENE_PRMRY_PYR_PD_AMT"] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl["CLM_SRVC_DDCTBL_SW"] = random.choice(
                generator.code_systems["CLM_SRVC_DDCTBL_SW"]
            )
            claim_line_prfnl["CLM_PRCSG_IND_CD"] = random.choice(
                generator.code_systems["CLM_PRCSG_IND_CD"]
            )
            claim_line_prfnl["CLM_PMT_80_100_CD"] = random.choice(
                generator.code_systems["CLM_PMT_80_100_CD"]
            )

            claim_line_prfnl["CLM_MTUS_IND_CD"] = random.choice(
                generator.code_systems["CLM_MTUS_IND_CD"]
            )
            claim_line_prfnl["CLM_LINE_PRFNL_MTUS_CNT"] = random.randint(0, 10)
            # claim_line_prfnl['CLM_PRCNG_LCLTY_CD'] =
            # random.choice(generator.code_systems['CLM_PRCNG_LCLTY_CD'])
            # not yet available from the IDR
            claim_line_prfnl["CLM_PHYSN_ASTNT_CD"] = random.choice(
                generator.code_systems["CLM_PHYSN_ASTNT_CD"]
            )

            if random.randint(0, 10) == 6:
                claim_line_prfnl["CLM_LINE_HCT_HGB_TYPE_CD"] = random.choice(["R1", "R2"])
                claim_line_prfnl["CLM_LINE_CARR_CLNCL_LAB_NUM"] = random.choice(
                    [
                        "11D1111111",
                        "22D2222222",
                    ]
                )

            # these don't have much variance in our synthetic data, but they are not strictly
            # the same in actual data!
            claim_line["CLM_LINE_MDCR_COINSRNC_AMT"] = round(random.uniform(0, 5), 2)

            # pick a random diagnosis.

            claim_line["CLM_LINE_DGNS_CD"] = random.choice(diagnoses)["CLM_DGNS_CD"]
            claim_line["CLM_POS_CD"] = random.choice(generator.code_systems["CLM_POS_CD"])
            claim_line["CLM_RNDRG_PRVDR_PRTCPTG_CD"] = random.choice(
                generator.code_systems["CLM_RNDRG_PRVDR_PRTCPTG_CD"]
            )

        if clm_type_cd >= 71 and clm_type_cd <= 72:
            claim_line["CLM_RNDRG_PRVDR_TAX_NUM"] = random.choice(["1928347912", "912834729"])
            claim_line["CLM_RNDRG_PRVDR_PIN_NUM"] = random.choice(["29364819", "19238747"])
            claim_line["CLM_RNDRG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)
            if random.choice([0, 10]) == 7:
                claim_line["CLM_LINE_ANSTHSA_UNIT_CNT"] = random.uniform(0, 10)
            if random.choice([0, 15]) == 7:
                claim_line["CLM_LINE_RX_NUM"] = random.choice(["1234", "423482347"])

        if clm_type_cd == 81 or clm_type_cd == 82:
            claim_line_prfnl["CLM_LINE_DMERC_SCRN_SVGS_AMT"] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl["CLM_SUPLR_TYPE_CD"] = random.choice(
                generator.code_systems["CLM_SUPLR_TYPE_CD"]
            )
            claim_line_prfnl["CLM_LINE_PRFNL_DME_PRICE_AMT"] = round(random.uniform(0, 10000), 2)
            claim_line["CLM_RNDRG_PRVDR_NPI_NUM"] = random.choice(type_1_npis)

        add_meta_timestamps(claim_line_prfnl, claim["CLM"], max_date)

        claim_line["CLM_LINE_HCPCS_CD"] = random.choice(get_hcpcs_proc_codes())
        num_mods = random.randint(0, 5)
        if num_mods:
            claim_line["HCPCS_1_MDFR_CD"] = random.choice(hcpcs_mods)
        if num_mods > 1:
            claim_line["HCPCS_2_MDFR_CD"] = random.choice(hcpcs_mods)
        if num_mods > 2:
            claim_line["HCPCS_3_MDFR_CD"] = random.choice(hcpcs_mods)
        if num_mods > 3:
            claim_line["HCPCS_4_MDFR_CD"] = random.choice(hcpcs_mods)
        if num_mods > 4:
            claim_line["HCPCS_5_MDFR_CD"] = random.choice(hcpcs_mods)
        if random.choice([0, 1]):
            claim_line["CLM_LINE_NDC_CD"] = random.choice(available_ndc)
            claim_line["CLM_LINE_NDC_QTY"] = round(random.uniform(1, 1000), 2)
            claim_line["CLM_LINE_NDC_QTY_QLFYR_CD"] = "ML"
        claim_line["CLM_LINE_SRVC_UNIT_QTY"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_REV_CTR_CD"] = random.choice(generator.code_systems["CLM_REV_CNTR_CD"])
        claim_line["CLM_LINE_BENE_PMT_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_BENE_PD_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_ALOWD_CHRG_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_SBMT_CHRG_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_CVRD_PD_AMT"] = round(random.uniform(0, 5), 2)
        claim_line["CLM_LINE_BLOOD_DDCTBL_AMT"] = round(random.uniform(0, 15), 2)
        claim_line["CLM_LINE_MDCR_DDCTBL_AMT"] = round(random.uniform(0, 5), 2)

        claim_line["CLM_LINE_PRVDR_PMT_AMT"] = round(random.uniform(0, 1500), 2)
        claim_line["CLM_LINE_NCVRD_CHRG_AMT"] = round(random.uniform(0, 1500), 2)

        add_meta_timestamps(claim_line, claim["CLM"], max_date)

        claim_line_inst["CLM_LINE_INSTNL_ADJSTD_AMT"] = round(random.uniform(0, 1500), 2)
        claim_line_inst["CLM_LINE_INSTNL_RDCD_AMT"] = round(random.uniform(0, 1500), 2)
        claim_line_inst["CLM_DDCTBL_COINSRNC_CD"] = random.choice(
            generator.code_systems["CLM_DDCTBL_COINSRNC_CD"]
        )
        claim_line_inst["CLM_LINE_INSTNL_RATE_AMT"] = round(random.uniform(0, 15), 2)
        claim_line_inst["CLM_LINE_INSTNL_MSP1_PD_AMT"] = round(random.uniform(0, 15), 2)
        claim_line_inst["CLM_LINE_INSTNL_MSP2_PD_AMT"] = round(random.uniform(0, 2), 2)
        claim_line_inst["CLM_LINE_INSTNL_REV_CTR_DT"] = claim["CLM"]["CLM_FROM_DT"]

        # In contrast to v2 DD this appears to populated in many.
        claim_line_inst["CLM_REV_DSCNT_IND_CD"] = random.choice(
            generator.code_systems["CLM_REV_DSCNT_IND_CD"]
        )
        claim_line_inst["CLM_OTAF_ONE_IND_CD"] = random.choice(
            generator.code_systems["CLM_OTAF_IND_CD"]
        )
        claim_line_inst["CLM_REV_PACKG_IND_CD"] = random.choice(
            generator.code_systems["CLM_REV_PACKG_IND_CD"]
        )
        claim_line_inst["CLM_REV_PMT_MTHD_CD"] = random.choice(
            generator.code_systems["CLM_REV_PMT_MTHD_CD"]
        )
        claim_line_inst["CLM_REV_CNTR_STUS_CD"] = random.choice(
            generator.code_systems["CLM_REV_CNTR_STUS_CD"]
        )
        claim_line_inst["CLM_ANSI_SGNTR_SK"] = random.choice(
            [
                "8585",
                "1",
                "4365",
                "1508",
                "5555",
                "9204",
                "6857",
                "5816",
                "11978",
            ]
        )
        add_meta_timestamps(claim_line_inst, claim["CLM"], max_date)

        claim_line["CLM_UNIQ_ID"] = claim["CLM"]["CLM_UNIQ_ID"]
        claim_line["CLM_LINE_NUM"] = line_num
        claim_line_inst["CLM_LINE_NUM"] = line_num
        claim_line_prfnl["CLM_LINE_NUM"] = line_num
        claim["CLM_LINE"].append(claim_line)
        if clm_type_cd >= 10 and clm_type_cd <= 65:
            claim["CLM_LINE_INSTNL"].append(claim_line_inst)
        elif clm_type_cd >= 71 and clm_type_cd <= 82:
            claim["CLM_LINE_PRFNL"].append(claim_line_prfnl)

        # CLM_REV_APC_HIPPS_CD never populated for CLM_TYPE_CD 60 apart from null values (00000,0,~)
    return claim


def gen_pac_version_of_claim(claim, max_date):
    # note the fields to delete

    # Generating a Synthetic PAC claim is done in a rather naive way.
    # 1. Create a new CLM_UNIQ_ID
    # 2. Create a new 5 part key (eg GEO_BENE_SK, CLM_DT_SGNTR_SK)
    # 3. Update the relevant parts
    # 4. Delete information that's not accessible from that given source. This can probably be done
    # via config files in the future.

    pac_claim = copy.deepcopy(claim)
    pac_claim["CLM"]["CLM_UNIQ_ID"] = "".join(random.choices(string.digits, k=13))
    pac_clm_type_cd = int(pac_claim["CLM"]["CLM_TYPE_CD"])

    if pac_clm_type_cd in (60, 61, 62, 63, 64):
        pac_claim["CLM"]["CLM_TYPE_CD"] = random.choices(
            [1011, 2011, 1041, 2041], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd == 40:
        pac_claim["CLM"]["CLM_TYPE_CD"] = random.choices(
            [1013, 2013, 1071, 2071], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd == 10:
        pac_claim["CLM"]["CLM_TYPE_CD"] = random.choices(
            [1032, 2032, 1033, 2033], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd == 20:
        pac_claim["CLM"]["CLM_TYPE_CD"] = random.choice([1021, 2021])

    if pac_clm_type_cd == 30:
        pac_claim["CLM"]["CLM_TYPE_CD"] = random.choices([1018, 2018])[0]

    if pac_clm_type_cd == 50:
        pac_claim["CLM"]["CLM_TYPE_CD"] = random.choices(
            [1081, 2081, 1082, 2082], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if "CLM_BLOOD_PT_FRNSH_QTY" in pac_claim["CLM"]:
        pac_claim["CLM"].pop("CLM_BLOOD_PT_FRNSH_QTY")
    pac_claim["CLM"]["CLM_DT_SGNTR_SK"] = "".join(random.choices(string.digits, k=12))
    pac_claim["CLM_DT_SGNTR"]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
    pac_claim["CLM"]["GEO_BENE_SK"] = "".join(random.choices(string.digits, k=5))
    pac_claim["CLM_FISS"] = {}
    pac_claim["CLM_FISS"]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
    pac_claim["CLM_FISS"]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
    pac_claim["CLM_FISS"]["CLM_NUM_SK"] = pac_claim["CLM"]["CLM_NUM_SK"]
    pac_claim["CLM_FISS"]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]
    pac_claim["CLM_FISS"]["CLM_CRNT_STUS_CD"] = random.choice(
        [
            "A",
            "F",
            "I",
            "S",
            "M",
            "P",
            "R",
            "D",
            "T",
            "U",
        ]
    )
    add_meta_timestamps(pac_claim["CLM_FISS"], claim["CLM"], max_date)

    pac_claim["CLM_LCTN_HSTRY"] = {}
    pac_claim["CLM_LCTN_HSTRY"]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
    pac_claim["CLM_LCTN_HSTRY"]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
    pac_claim["CLM_LCTN_HSTRY"]["CLM_NUM_SK"] = pac_claim["CLM"]["CLM_NUM_SK"]
    pac_claim["CLM_LCTN_HSTRY"]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]
    pac_claim["CLM_LCTN_HSTRY"]["CLM_LCTN_CD_SQNC_NUM"] = "1"
    pac_claim["CLM_LCTN_HSTRY"]["CLM_AUDT_TRL_STUS_CD"] = random.choice(
        [
            "A",
            "F",
            "I",
            "S",
            "M",
            "P",
            "R",
            "D",
            "T",
            "U",
            "1",
            "2",
            "4",
            "8",
        ]
    )
    add_meta_timestamps(pac_claim["CLM_LCTN_HSTRY"], claim["CLM"], max_date)

    for i in range(len(pac_claim["CLM_LINE"])):
        pac_claim["CLM_LINE"][i]["CLM_LINE_NUM"] = i + 1
        pac_claim["CLM_LINE"][i]["CLM_UNIQ_ID"] = pac_claim["CLM"]["CLM_UNIQ_ID"]
        pac_claim["CLM_LINE"][i]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
        pac_claim["CLM_LINE"][i]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
        pac_claim["CLM_LINE"][i]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]
    # Update CLM_LINE_INSTNL for institutional claims only
    if "CLM_LINE_INSTNL" in pac_claim and len(pac_claim["CLM_LINE_INSTNL"]) > 0:
        for i in range(len(pac_claim["CLM_LINE_INSTNL"])):
            pac_claim["CLM_LINE_INSTNL"][i]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
            pac_claim["CLM_LINE_INSTNL"][i]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
            pac_claim["CLM_LINE_INSTNL"][i]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]

    # Update CLM_LINE_PRFNL for professional claims only
    if "CLM_LINE_PRFNL" in pac_claim and len(pac_claim["CLM_LINE_PRFNL"]) > 0:
        for i in range(len(pac_claim["CLM_LINE_PRFNL"])):
            pac_claim["CLM_LINE_PRFNL"][i]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
            pac_claim["CLM_LINE_PRFNL"][i]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
            pac_claim["CLM_LINE_PRFNL"][i]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]

    for i in range(len(pac_claim["CLM_VAL"])):
        pac_claim["CLM_VAL"][i]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
        pac_claim["CLM_VAL"][i]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
        pac_claim["CLM_VAL"][i]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]

    # Update CLM_INSTNL for institutional claims only
    if pac_claim.get("CLM_INSTNL"):
        pac_claim["CLM_INSTNL"]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
        pac_claim["CLM_INSTNL"]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
        pac_claim["CLM_INSTNL"]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]

    for i in range(len(pac_claim["CLM_PROD"])):
        pac_claim["CLM_PROD"][i]["CLM_DT_SGNTR_SK"] = pac_claim["CLM"]["CLM_DT_SGNTR_SK"]
        pac_claim["CLM_PROD"][i]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
        pac_claim["CLM_PROD"][i]["CLM_TYPE_CD"] = pac_claim["CLM"]["CLM_TYPE_CD"]
    if "CLM_MDCR_EXHSTD_DT" in pac_claim["CLM_DT_SGNTR"]:
        pac_claim["CLM_DT_SGNTR"].pop("CLM_MDCR_EXHSTD_DT")
    if "CLM_NCVRD_FROM_DT" in pac_claim["CLM_DT_SGNTR"]:
        pac_claim["CLM_DT_SGNTR"].pop("CLM_NCVRD_FROM_DT")
    if "CLM_NCVRD_THRU_DT" in pac_claim["CLM_DT_SGNTR"]:
        pac_claim["CLM_DT_SGNTR"].pop("CLM_NCVRD_THRU_DT")
    if "CLM_NCH_WKLY_PROC_DT" in pac_claim["CLM_DT_SGNTR"]:
        pac_claim["CLM_DT_SGNTR"].pop("CLM_NCH_WKLY_PROC_DT")
    if "CLM_ACTV_CARE_THRU_DT" in pac_claim["CLM_DT_SGNTR"]:
        pac_claim["CLM_DT_SGNTR"].pop("CLM_ACTV_CARE_THRU_DT")
    # Remove institutional-specific fields for institutional claims only
    if pac_claim.get("CLM_INSTNL"):
        if "CLM_MDCR_IP_BENE_DDCTBL_AMT" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_MDCR_IP_BENE_DDCTBL_AMT")
        if "CLM_MDCR_INSTNL_PRMRY_PYR_AMT" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_MDCR_INSTNL_PRMRY_PYR_AMT")
        if "CLM_PPS_IND_CD" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_PPS_IND_CD")
        if "CLM_MDCR_HOSPC_PRD_CNT" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_MDCR_HOSPC_PRD_CNT")
        if "CLM_INSTNL_DRG_OUTLIER_AMT" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_INSTNL_DRG_OUTLIER_AMT")
        if "CLM_MDCR_HHA_TOT_VISIT_CNT" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_MDCR_HHA_TOT_VISIT_CNT")
        if "CLM_HHA_LUP_IND_CD" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_HHA_LUP_IND_CD")
        if "CLM_HHA_RFRL_CD" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_HHA_RFRL_CD")
        if "CLM_MDCR_INSTNL_BENE_PD_AMT" in pac_claim["CLM_INSTNL"]:
            pac_claim["CLM_INSTNL"].pop("CLM_MDCR_INSTNL_BENE_PD_AMT")

    # Remove institutional line-specific fields for institutional claims only
    if "CLM_LINE_INSTNL" in pac_claim and len(pac_claim["CLM_LINE_INSTNL"]) > 0:
        for i in range(len(pac_claim["CLM_LINE_INSTNL"])):
            if i == len(pac_claim["CLM_LINE_INSTNL"]):
                continue
            if "CLM_ANSI_SGNTR_SK" in pac_claim["CLM_LINE_INSTNL"][i]:
                pac_claim["CLM_LINE_INSTNL"][i].pop("CLM_ANSI_SGNTR_SK")
            if "CLM_OTAF_ONE_IND_CD" in pac_claim["CLM_LINE_INSTNL"][i]:
                pac_claim["CLM_LINE_INSTNL"][i].pop("CLM_OTAF_ONE_IND_CD")
            if "CLM_REV_CNTR_STUS_CD" in pac_claim["CLM_LINE_INSTNL"][i]:
                pac_claim["CLM_LINE_INSTNL"][i].pop("CLM_REV_CNTR_STUS_CD")

    for i in range(len(pac_claim["CLM_LINE"])):
        if i == len(pac_claim["CLM_LINE"]):
            continue
        if "CLM_LINE_ANSTHSA_UNIT_CNT" in pac_claim["CLM_LINE"][i]:
            pac_claim["CLM_LINE"][i].pop("CLM_LINE_ANSTHSA_UNIT_CNT")
        if "CLM_RNDRG_PRVDR_PRTCPTG_CD" in pac_claim["CLM_LINE"][i]:
            pac_claim["CLM_LINE"][i].pop("CLM_RNDRG_PRVDR_PRTCPTG_CD")

    for i in range(len(pac_claim["CLM_LINE_PRFNL"])):
        if i == len(pac_claim["CLM_LINE_PRFNL"]):
            continue
        if "CLM_MTUS_IND_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_MTUS_IND_CD")
        if "CLM_PRCNG_LCLTY_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_PRCNG_LCLTY_CD")
        if "CLM_PHYSN_ASTNT_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_PHYSN_ASTNT_CD")
        if "CLM_LINE_PRFNL_MTUS_CNT" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_LINE_PRFNL_MTUS_CNT")
        if "CLM_LINE_CARR_HPSA_SCRCTY_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_LINE_CARR_HPSA_SCRCTY_CD")
        if "CLM_PRMRY_PYR_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_PRMRY_PYR_CD")
        if "CLM_FED_TYPE_SRVC_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_FED_TYPE_SRVC_CD")
        if "CLM_PMT_80_100_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_PMT_80_100_CD")
        if "CLM_PRCSG_IND_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_PRCSG_IND_CD")
        if "CLM_PRVDR_SPCLTY_CD" in pac_claim["CLM_LINE_PRFNL"][i]:
            pac_claim["CLM_LINE_PRFNL"][i].pop("CLM_PRVDR_SPCLTY_CD")

    # Update CLM_INSTNL for institutional claims only
    if pac_claim.get("CLM_INSTNL"):
        # pac_claim['CLM_INSTNL']['CLM_UNIQ_ID'] = pac_claim['CLM']['CLM_UNIQ_ID']
        pac_claim["CLM_INSTNL"]["GEO_BENE_SK"] = pac_claim["CLM"]["GEO_BENE_SK"]
        pac_claim["CLM_INSTNL"]["CLM_DT_SGNTR_SK"] = pac_claim["CLM_DT_SGNTR"]["CLM_DT_SGNTR_SK"]

    if pac_claim["CLM"]["CLM_TYPE_CD"] in fiss_clm_type_cds:
        pac_claim["CLM"]["CLM_SRC_ID"] = 21000  # FISS
    elif pac_claim["CLM"]["CLM_TYPE_CD"] in mcs_clm_type_cds:
        pac_claim["CLM"]["CLM_SRC_ID"] = 22000  # MCS
    elif pac_claim["CLM"]["CLM_TYPE_CD"] in vms_cds:
        pac_claim["CLM"]["CLM_SRC_ID"] = 23000  # VMS

    if "CLM_DCMTN" in pac_claim:
        pac_claim.pop("CLM_DCMTN")

    return pac_claim


def add_meta_timestamps(obj, clm, max_date):
    if date.fromisoformat(clm["CLM_IDR_LD_DT"]) < date(2021, 4, 19):
        has_insrt_ts = random.random() > 0.5
    else:
        has_insrt_ts = True
    obj["IDR_INSRT_TS"] = (
        faker.date_time_between_dates(
            datetime.fromisoformat(clm["CLM_IDR_LD_DT"]),
            datetime.fromisoformat(max_date),
        )
        if has_insrt_ts
        else None
    )
    obj["IDR_UPDT_TS"] = (
        faker.date_time_between_dates(obj["IDR_INSRT_TS"], datetime.fromisoformat(max_date))
        if has_insrt_ts and random.random() > 0.8
        else None
    )


def main():
    parser = argparse.ArgumentParser(
        description="Generate Synthetic Data for Ingestion by the BFD v3 pipeline."
    )
    parser.add_argument(
        "--sushi",
        "-s",
        action="store_true",
        help="Generate new StructureDefinitions. Use when testing locally if new .fsh files "
        "have been added.",
    )
    parser.add_argument(
        "--benes",
        "-b",
        type=str,
        help="Pull BENE_SKs from the input file. Expected format is that of "
        "SYNTHETIC_BENE_HSTRY.csv",
    )
    parser.add_argument(
        "--min-claims", type=int, default=5, help="Minimum number of claims to generate per person"
    )
    parser.add_argument(
        "--max-claims", type=int, default=5, help="Maximum number of claims to generate per person"
    )

    args = parser.parse_args()
    if args.sushi:
        print("Running sushi build")
        _, stderr = run_command("sushi build", cwd="./sushi")
        if stderr:
            print("SUSHI errors:")
            print(stderr)

    bene_sk_list = [-1]
    if args.benes:
        df = pd.read_csv(args.benes)
        bene_sk_list = df["BENE_SK"].unique()

    CLM = []
    CLM_LINE = []
    CLM_VAL = []
    CLM_INSTNL = []
    CLM_LINE_INSTNL = []
    CLM_DT_SGNTR = []
    CLM_PROD = []
    CLM_DCMTN = []
    CLM_FISS = []
    CLM_LCTN_HSTRY = []
    CLM_PRFNL = []
    CLM_LINE_PRFNL = []
    CLM_LINE_RX = []
    pt_complete = 0
    min_claims = args.min_claims
    max_claims = args.max_claims
    if min_claims > max_claims:
        print(
            f"error: min claims value of {min_claims} is greater than "
            f"max claims value of {max_claims}"
        )
        sys.exit(1)
    max_date = str(date.today())
    for pt_complete, pt_bene_sk in enumerate(bene_sk_list):
        if (pt_complete) % 1000 == 0 and pt_complete > 0:
            print(
                f"Completed {pt_complete} patients with between {min_claims} and {max_claims} "
                "claims per patient."
            )
        for _ in range(random.randint(min_claims, max_claims)):
            clm_from_dt_min = "2018-01-01"
            claim = gen_claim(bene_sk=pt_bene_sk, min_date=clm_from_dt_min, max_date=max_date)
            CLM.append(claim["CLM"])
            CLM_LINE.extend(claim["CLM_LINE"])
            CLM_VAL.extend(claim["CLM_VAL"])
            CLM_DT_SGNTR.append(claim["CLM_DT_SGNTR"])
            CLM_PROD.extend(claim["CLM_PROD"])

            if "CLM_INSTNL" in claim:
                CLM_INSTNL.append(claim["CLM_INSTNL"])
            CLM_LINE_INSTNL.extend(claim["CLM_LINE_INSTNL"])
            CLM_DCMTN.append(claim["CLM_DCMTN"])
            if claim["CLM"]["CLM_TYPE_CD"] in (1, 2, 3, 4):
                CLM_LINE_RX.extend(claim["CLM_LINE_RX"])
            else:
                # Only add professional data for non-Part D claims
                CLM_PRFNL.append(claim["CLM_PRFNL"])
                CLM_LINE_PRFNL.extend(claim["CLM_LINE_PRFNL"])
            # obviously we don't have pac claims for PD claims
            if random.choice([0, 1]) and claim["CLM"]["CLM_TYPE_CD"] not in (
                1,
                2,
                3,
                4,
            ):
                pac_claim = gen_pac_version_of_claim(claim, max_date)
                CLM.append(pac_claim["CLM"])
                CLM_LINE.extend(pac_claim["CLM_LINE"])
                CLM_VAL.extend(pac_claim["CLM_VAL"])
                CLM_DT_SGNTR.append(pac_claim["CLM_DT_SGNTR"])
                CLM_PROD.extend(pac_claim["CLM_PROD"])

                if pac_claim.get("CLM_INSTNL"):
                    CLM_INSTNL.append(pac_claim["CLM_INSTNL"])
                if "CLM_LINE_INSTNL" in pac_claim and len(pac_claim["CLM_LINE_INSTNL"]) > 0:
                    CLM_LINE_INSTNL.extend(pac_claim["CLM_LINE_INSTNL"])
                CLM_FISS.append(pac_claim["CLM_FISS"])
                CLM_LCTN_HSTRY.append(pac_claim["CLM_LCTN_HSTRY"])

    save_output_files(
        CLM,
        CLM_LINE,
        CLM_VAL,
        CLM_DT_SGNTR,
        CLM_PROD,
        CLM_INSTNL,
        CLM_LINE_INSTNL,
        CLM_DCMTN,
        CLM_FISS,
        CLM_LCTN_HSTRY,
        CLM_PRFNL,
        CLM_LINE_PRFNL,
        CLM_LINE_RX,
    )


if __name__ == "__main__":
    main()
