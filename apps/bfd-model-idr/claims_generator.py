import copy
import random
import string
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from enum import Enum, auto
from pathlib import Path
from typing import Annotated, Any

import click
import pandas as pd
import tqdm
from dateutil.relativedelta import relativedelta
from faker import Faker
from pydanclick import from_pydantic
from pydantic import BaseModel, Field

import field_constants as f
from claims_static_data import (
    ADJUDICATED_PROFESSIONAL_CLAIM_TYPES,
    AVAIL_CLM_RLT_COND_SK,
    AVAIL_CONTRACT_NAMES,
    AVAIL_CONTRACT_NUMS,
    AVAIL_OSCAR_CODES_INSTITUTIONAL,
    AVAIL_PBP_NUMS,
    AVAIL_PBP_TYPE_CODES,
    AVAILABLE_FAMILY_NAMES,
    AVAILABLE_GIVEN_NAMES,
    AVAILABLE_NDC,
    AVAILABLE_PROVIDER_LEGAL_NAMES,
    AVAILABLE_PROVIDER_NAMES,
    AVAILABLE_PROVIDER_TX_CODES,
    AVAILABLE_PROVIDER_TYPE_CODES,
    CLM_POA_IND_CHOICES,
    FISS_CLM_TYPE_CDS,
    HCPCS_MODS,
    INSTITUTIONAL_CLAIM_TYPES,
    MCS_CLM_TYPE_CDS,
    PHARMACY_CLM_TYPE_CDS,
    PROFESSIONAL_CLAIM_TYPES,
    TARGET_RLT_COND_CODES,
    TARGET_SEQUENCE_NUMBERS,
    TYPE_1_NPIS,
    TYPE_2_NPIS,
    VMS_CDS,
    get_drg_dgns_codes,
    get_hcpcs_proc_codes,
    get_icd_10_dgns_codes,
    get_icd_10_prcdr_codes,
)
from generator_util import (
    BENE_HSTRY,
    BENE_XREF,
    CLM,
    CLM_ANSI_SGNTR,
    CLM_DCMTN,
    CLM_DT_SGNTR,
    CLM_FISS,
    CLM_INSTNL,
    CLM_LCTN_HSTRY,
    CLM_LINE,
    CLM_LINE_DCMTN,
    CLM_LINE_INSTNL,
    CLM_LINE_PRFNL,
    CLM_LINE_RX,
    CLM_PRFNL,
    CLM_PROD,
    CLM_RLT_COND_SGNTR_MBR,
    CLM_VAL,
    CNTRCT_PBP_CNTCT,
    CNTRCT_PBP_NUM,
    PRVDR_HSTRY,
    GeneratorUtil,
    RowAdapter,
    adapters_to_dicts,
    gen_basic_id,
    gen_multipart_id,
    gen_numeric_id,
    load_file_dict,
    probability,
)

NOW = date.today()
INT_TO_STRING_COLS = [
    f.BENE_SK,
    f.CLM_TYPE_CD,
    f.CLM_NUM_SK,
    f.PRVDR_PRSCRBNG_PRVDR_NPI_NUM,
    f.PRVDR_RFRG_PRVDR_NPI_NUM,
    f.PRVDR_BLG_PRVDR_NPI_NUM,
    f.CLM_ATNDG_PRVDR_NPI_NUM,
    f.CLM_OPRTG_PRVDR_NPI_NUM,
    f.CLM_OTHR_PRVDR_NPI_NUM,
    f.CLM_RNDRG_PRVDR_NPI_NUM,
    f.CLM_BLG_PRVDR_NPI_NUM,
    f.CLM_RFRG_PRVDR_PIN_NUM,
    f.PRVDR_ATNDG_PRVDR_NPI_NUM,
    f.PRVDR_SRVC_PRVDR_NPI_NUM,
    f.PRVDR_OTHR_PRVDR_NPI_NUM,
    f.PRVDR_RNDRNG_PRVDR_NPI_NUM,
    f.PRVDR_OPRTG_PRVDR_NPI_NUM,
]
"""Columns you want as string without decimal/nan"""


class OptionsModel(BaseModel):
    sushi: Annotated[
        bool,
        Field(
            description=(
                "Generate new StructureDefinitions. Use when testing locally if new .fsh files "
                "have been added."
            )
        ),
    ] = False
    min_claims: Annotated[
        int, Field(description="Minimum number of claims to generate per person")
    ] = 5
    max_claims: Annotated[
        int, Field(description="Maximum number of claims to generate per person")
    ] = 5
    force_gen_claims: Annotated[
        bool,
        Field(
            description=(
                "Generate _new_ claims when an existing bene has claims provided via --files; "
                " -respects -max-claims"
            )
        ),
    ] = False


@dataclass
class _GeneratedClaim:
    CLM: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_VAL: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_PROD: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_LINE: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_LINE_DCMTN: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_DT_SGNTR: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_INSTNL: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_LINE_INSTNL: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_DCMTN: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_PRFNL: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_LINE_PRFNL: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_LINE_RX: list[dict[str, Any]] = field(default_factory=list[dict[str, Any]])
    CLM_RLT_COND_SGNTR_MBR: dict[str, Any] = field(default_factory=dict[str, Any])
    RLT_COND_MBR_RECORD: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_FISS: dict[str, Any] = field(default_factory=dict[str, Any])
    CLM_LCTN_HSTRY: dict[str, Any] = field(default_factory=dict[str, Any])


class NormalizeStrategy(Enum):
    NORMALIZE = auto()
    NO_NORMALIZE = auto()


class ClmLineNumCastStrategy(Enum):
    CAST_TO_STR = auto()
    NO_TYPE_CAST = auto()


generator = GeneratorUtil()
faker = Faker()


def save_output_files(
    clm: list[dict[str, Any]],
    clm_line: list[dict[str, Any]],
    clm_line_dcmtn: list[dict[str, Any]],
    clm_val: list[dict[str, Any]],
    clm_dt_sgntr: list[dict[str, Any]],
    clm_prod: list[dict[str, Any]],
    clm_instnl: list[dict[str, Any]],
    clm_line_instnl: list[dict[str, Any]],
    clm_dcmtn: list[dict[str, Any]],
    clm_fiss: list[dict[str, Any]],
    clm_lctn_hstry: list[dict[str, Any]],
    clm_prfnl: list[dict[str, Any]],
    clm_line_prfnl: list[dict[str, Any]],
    clm_line_rx: list[dict[str, Any]],
    clm_rlt_cond_sgntr_mbr: list[dict[str, Any]],
    prvdr_hstry: list[dict[str, Any]],
    cntrct_pbp_num: list[dict[str, Any]],
    cntrct_pbp_cntct: list[dict[str, Any]],
    clm_ansi_sgntr: list[dict[str, Any]],
):
    Path("out").mkdir(exist_ok=True)

    normalized_clms = pd.json_normalize(clm)  # type: ignore
    normalized_clms[f.CLM_BLOOD_PT_FRNSH_QTY] = normalized_clms[f.CLM_BLOOD_PT_FRNSH_QTY].astype(
        "Int64"
    )
    normalized_clms[f.CLM_BLG_PRVDR_OSCAR_NUM] = normalized_clms[f.CLM_BLG_PRVDR_OSCAR_NUM].astype(
        "string"
    )

    exports = [
        (
            normalized_clms,
            f"out/{CLM}.csv",
            NormalizeStrategy.NO_NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_line,
            f"out/{CLM_LINE}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.CAST_TO_STR,
        ),
        (
            clm_line_dcmtn,
            f"out/{CLM_LINE_DCMTN}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.CAST_TO_STR,
        ),
        (
            clm_val,
            f"out/{CLM_VAL}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_dt_sgntr,
            f"out/{CLM_DT_SGNTR}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_prod,
            f"out/{CLM_PROD}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_instnl,
            f"out/{CLM_INSTNL}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_line_instnl,
            f"out/{CLM_LINE_INSTNL}.csv",
            NormalizeStrategy.NO_NORMALIZE,
            ClmLineNumCastStrategy.CAST_TO_STR,
        ),
        (
            clm_dcmtn,
            f"out/{CLM_DCMTN}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_lctn_hstry,
            f"out/{CLM_LCTN_HSTRY}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_fiss,
            f"out/{CLM_FISS}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_prfnl,
            f"out/{CLM_PRFNL}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_line_prfnl,
            f"out/{CLM_LINE_PRFNL}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.CAST_TO_STR,
        ),
        (
            clm_line_rx,
            f"out/{CLM_LINE_RX}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_rlt_cond_sgntr_mbr,
            f"out/{CLM_RLT_COND_SGNTR_MBR}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            prvdr_hstry,
            f"out/{PRVDR_HSTRY}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            cntrct_pbp_num,
            f"out/{CNTRCT_PBP_NUM}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            cntrct_pbp_cntct,
            f"out/{CNTRCT_PBP_CNTCT}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
        (
            clm_ansi_sgntr,
            f"out/{CLM_ANSI_SGNTR}.csv",
            NormalizeStrategy.NORMALIZE,
            ClmLineNumCastStrategy.NO_TYPE_CAST,
        ),
    ]

    print("Exporting finished synthetic claims to ./out...")
    with tqdm.tqdm(exports) as t:
        for [df, out_file, normalize, clm_line_num_cast] in t:
            t.set_postfix(file=out_file)  # type: ignore
            export_df(df, out_file, normalize, clm_line_num_cast)
    print("Finished exporting generated claims")


def export_df(
    data: list[dict[str, Any]] | dict[str, Any] | pd.DataFrame,
    out_path: str,
    normalize: NormalizeStrategy = NormalizeStrategy.NORMALIZE,
    clm_line_num_cast: ClmLineNumCastStrategy = ClmLineNumCastStrategy.NO_TYPE_CAST,
):
    df = pd.json_normalize(data) if normalize == NormalizeStrategy.NORMALIZE else pd.DataFrame(data)  # type: ignore
    df = clean_int_columns(df, INT_TO_STRING_COLS)

    if clm_line_num_cast == ClmLineNumCastStrategy.CAST_TO_STR and f.CLM_LINE_NUM in df.columns:
        df[f.CLM_LINE_NUM] = df[f.CLM_LINE_NUM].astype("str")
    df.to_csv(out_path, index=False)


def clean_int_columns(df: pd.DataFrame, cols: list[str]):
    for col in cols:
        if col in df.columns:
            df[col] = (
                pd.to_numeric(df[col], errors="coerce")  # type: ignore
                .round(0)
                .astype("Int64")
                .astype("string")
                .fillna("")  # type: ignore
            )
    return df


def run_command(cmd: list[str], cwd: str | None = None):
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


def random_date(start_date: str, end_date: str):
    start_formatted = date.fromisoformat(start_date).toordinal()
    end_formatted = date.fromisoformat(end_date).toordinal()
    rand_date = random.randint(start_formatted, end_formatted)
    return date.fromordinal(rand_date).isoformat()


def gen_thru_dt(frm_dt: str, max_days: int = 30):
    from_date = date.fromisoformat(frm_dt)
    days_to_add = random.randint(0, max_days)
    return (from_date + timedelta(days=days_to_add)).isoformat()


def add_days(input_dt: str, days_to_add: int = 0):
    return (date.fromisoformat(input_dt) + timedelta(days=days_to_add)).isoformat()


def add_diagnoses(clm_type_cd: int = -1):
    diagnosis_list: list[dict[str, Any]] = []
    num_diagnoses = 0
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        # inpatient uses concepts of principal, admitting, other, external
        principal_diagnosis = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "P",
            f.CLM_POA_IND: "~",
        }
        first_diagnosis = {
            f.CLM_DGNS_CD: principal_diagnosis[f.CLM_DGNS_CD],
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "D",
            f.CLM_POA_IND: random.choice(CLM_POA_IND_CHOICES),
        }
        admitting_diagnosis = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "A",
            f.CLM_POA_IND: "~",
        }
        external_1 = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "E",
            f.CLM_POA_IND: "0",  # ALWAYS for ICD-10 codes. not always for icd-9.
        }
        first_external = {
            f.CLM_DGNS_CD: external_1[f.CLM_DGNS_CD],
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "1",
            f.CLM_POA_IND: "~",
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
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "P",
            f.CLM_POA_IND: "~",
        }
        first_diagnosis = {
            f.CLM_DGNS_CD: principal_diagnosis[f.CLM_DGNS_CD],
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "D",
            f.CLM_POA_IND: "~",
        }
        rfv_diag = {
            f.CLM_DGNS_CD: principal_diagnosis[f.CLM_DGNS_CD],
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "R",
            f.CLM_POA_IND: "~",
        }
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        diagnosis_list.append(rfv_diag)
        num_diagnoses = random.randint(2, 15)
    elif clm_type_cd in ADJUDICATED_PROFESSIONAL_CLAIM_TYPES:
        # professional claims use principal diagnosis and other diagnoses
        principal_diagnosis = {
            f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "P",
            f.CLM_POA_IND: "~",
        }
        first_diagnosis = {
            f.CLM_DGNS_CD: principal_diagnosis[f.CLM_DGNS_CD],
            f.CLM_VAL_SQNC_NUM: "1",
            f.CLM_DGNS_PRCDR_ICD_IND: "0",
            f.CLM_PROD_TYPE_CD: "D",
            f.CLM_POA_IND: "~",
        }
        diagnosis_list.append(principal_diagnosis)
        diagnosis_list.append(first_diagnosis)
        num_diagnoses = random.randint(2, 8)  # Professional claims typically have fewer diagnoses

    if num_diagnoses > 1 and clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        for diagnosis_sqnc in range(2, num_diagnoses):
            diagnosis = {
                f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
                f.CLM_VAL_SQNC_NUM: diagnosis_sqnc,
                f.CLM_DGNS_PRCDR_ICD_IND: "0",
                f.CLM_PROD_TYPE_CD: "D",
                f.CLM_POA_IND: random.choice(CLM_POA_IND_CHOICES),
            }
            diagnosis_list.append(diagnosis)
    elif clm_type_cd == 40:
        for diagnosis_sqnc in range(2, num_diagnoses):
            diagnosis = {
                f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
                f.CLM_VAL_SQNC_NUM: diagnosis_sqnc,
                f.CLM_DGNS_PRCDR_ICD_IND: "0",
                f.CLM_PROD_TYPE_CD: "D",
            }
            diagnosis_list.append(diagnosis)
    elif clm_type_cd in ADJUDICATED_PROFESSIONAL_CLAIM_TYPES:
        for diagnosis_sqnc in range(2, num_diagnoses):
            diagnosis = {
                f.CLM_DGNS_CD: random.choice(get_icd_10_dgns_codes()),
                f.CLM_VAL_SQNC_NUM: diagnosis_sqnc,
                f.CLM_DGNS_PRCDR_ICD_IND: "0",
                f.CLM_PROD_TYPE_CD: "D",
                f.CLM_POA_IND: "~",
            }
            diagnosis_list.append(diagnosis)

    return diagnosis_list


def gen_procedure_icd10pcs():
    procedure: dict[str, str | int] = {}
    procedure[f.CLM_PROD_TYPE_CD] = "S"
    procedure[f.CLM_PRCDR_CD] = random.choice(get_icd_10_prcdr_codes())
    procedure[f.CLM_DGNS_PRCDR_ICD_IND] = "0"
    return procedure


def gen_synthetic_clm_ansi_sgntr(src_path: str = f"sample-data/{CLM_ANSI_SGNTR}.csv"):
    csv_df = pd.read_csv(  # type: ignore
        src_path,
        dtype=str,
        na_filter=False,
    )
    clm_ansi_sgntr: list[dict[str, Any]] = csv_df.to_dict(orient="records")  # type: ignore

    # Return the data from the source but with every CLM_ANSI_SGNTR_SK made negative to indicate
    # it's synthetic
    return [x | {f.CLM_ANSI_SGNTR_SK: f"-{x[f.CLM_ANSI_SGNTR_SK]}"} for x in clm_ansi_sgntr]


def gen_claim(bene_sk: str = "-1", min_date: str = "2018-01-01", max_date: str = str(NOW)):
    claim = _GeneratedClaim()
    clm_dt_sgntr: dict[str, Any] = {}
    clm_dt_sgntr[f.CLM_DT_SGNTR_SK] = gen_basic_id(field=f.CLM_DT_SGNTR_SK, length=12)
    claim.CLM[f.CLM_DT_SGNTR_SK] = clm_dt_sgntr[f.CLM_DT_SGNTR_SK]
    claim.CLM[f.CLM_UNIQ_ID] = gen_basic_id(field=f.CLM_UNIQ_ID, length=13)

    clm_rlt_cond_sgntr_sk = gen_numeric_id(field=f.CLM_RLT_COND_SGNTR_SK, start=-2)
    claim.CLM[f.CLM_RLT_COND_SGNTR_SK] = clm_rlt_cond_sgntr_sk

    rlt_cond_mbr_record: dict[str, Any] = {}
    rlt_cond_mbr_record[f.CLM_RLT_COND_SGNTR_SK] = clm_rlt_cond_sgntr_sk
    rlt_cond_mbr_record[f.CLM_RLT_COND_SGNTR_SQNC_NUM] = random.choice(TARGET_SEQUENCE_NUMBERS)
    rlt_cond_mbr_record[f.CLM_RLT_COND_CD] = random.choice(TARGET_RLT_COND_CODES)

    claim.CLM_RLT_COND_SGNTR_MBR = rlt_cond_mbr_record

    # clm_type_cd = 60
    clm_type_cd = random.choice([1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 71, 72, 81, 82])
    claim.CLM[f.CLM_TYPE_CD] = clm_type_cd

    clm_src_id = 20000
    claim.CLM[f.CLM_SRC_ID] = clm_src_id
    claim.CLM[f.META_SRC_SK] = 7
    claim.CLM[f.CLM_FROM_DT] = random_date(min_date, max_date)
    claim.CLM[f.CLM_THRU_DT] = gen_thru_dt(claim.CLM[f.CLM_FROM_DT])

    # NON-PDE
    claim.CLM[f.CLM_CNTL_NUM] = gen_multipart_id(
        field=f.CLM_CNTL_NUM, parts=[(string.digits, 14), (string.ascii_uppercase, 3)]
    )
    # PDE -> diff Claim control number process.
    if clm_type_cd in PHARMACY_CLM_TYPE_CDS:
        claim.CLM[f.CLM_ORIG_CNTL_NUM] = gen_multipart_id(
            field=f.CLM_ORIG_CNTL_NUM, parts=[(string.digits, 14), (string.ascii_uppercase, 3)]
        )
        claim.CLM[f.CLM_RLT_COND_SGNTR_SK] = "-1"
        claim.CLM[f.META_SRC_SK] = 1

    if clm_type_cd in (20, 30, 40, 60, 61, 62, 63, 71, 72):
        claim.CLM[f.CLM_BLOOD_PT_FRNSH_QTY] = random.randint(0, 20)

    claim.CLM[f.CLM_NUM_SK] = gen_numeric_id(field=f.CLM_NUM_SK)
    claim.CLM[f.CLM_EFCTV_DT] = str(date.today())
    claim.CLM[f.CLM_IDR_LD_DT] = random_date(claim.CLM[f.CLM_FROM_DT], max_date)
    claim.CLM[f.CLM_OBSLT_DT] = "9999-12-31"
    claim.CLM[f.GEO_BENE_SK] = gen_numeric_id(field=f.GEO_BENE_SK)
    claim.CLM[f.BENE_SK] = bene_sk
    claim.CLM[f.CLM_DISP_CD] = random.choice(generator.code_systems[f.CLM_DISP_CD])
    claim.CLM[f.CLM_QUERY_CD] = random.choice(generator.code_systems[f.CLM_QUERY_CD])
    claim.CLM[f.CLM_ADJSTMT_TYPE_CD] = random.choice(generator.code_systems[f.CLM_ADJSTMT_TYPE_CD])

    if clm_type_cd in PHARMACY_CLM_TYPE_CDS:
        claim.CLM.pop(f.CLM_QUERY_CD)
        claim.CLM[f.CLM_SRVC_PRVDR_GNRC_ID_NUM] = random.choice(TYPE_2_NPIS)
        claim.CLM[f.PRVDR_SRVC_PRVDR_NPI_NUM] = claim.CLM[f.CLM_SRVC_PRVDR_GNRC_ID_NUM]
        claim.CLM[f.CLM_PD_DT] = random_date(claim.CLM[f.CLM_FROM_DT], claim.CLM[f.CLM_THRU_DT])
        claim.CLM[f.PRVDR_PRSCRBNG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        claim.CLM[f.CLM_SBMT_CHRG_AMT] = round(random.uniform(1, 1000000), 2)
        claim.CLM[f.CLM_SBMT_FRMT_CD] = random.choice(generator.code_systems[f.CLM_SBMT_FRMT_CD])
        claim.CLM[f.CLM_SBMTR_CNTRCT_NUM] = random.choice(AVAIL_CONTRACT_NUMS)
        claim.CLM[f.CLM_SBMTR_CNTRCT_PBP_NUM] = random.choice(AVAIL_PBP_NUMS)
        claim.CLM[f.CLM_BENE_PMT_AMT] = round(random.uniform(0, 1000), 2)
        claim.CLM[f.CLM_OTHR_TP_PD_AMT] = round(random.uniform(0, 1000), 2)
        claim_line = {}
        claim_line[f.CLM_UNIQ_ID] = claim.CLM[f.CLM_UNIQ_ID]
        claim_line[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
        claim_line[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
        claim_line[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
        claim_line[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
        claim_line[f.CLM_LINE_CVRD_PD_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line[f.CLM_LINE_NCVRD_PD_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line[f.CLM_LINE_NCVRD_CHRG_AMT] = round(random.uniform(0, 1500), 2)
        claim_line[f.CLM_LINE_NDC_CD] = random.choice(AVAILABLE_NDC)
        claim_line[f.CLM_LINE_SRVC_UNIT_QTY] = random.randint(1, 10)
        claim_line[f.CLM_LINE_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
        claim_line[f.CLM_LINE_THRU_DT] = claim.CLM[f.CLM_THRU_DT]
        claim_line[f.CLM_LINE_NDC_QTY] = random.randint(1, 10)
        claim_line[f.CLM_LINE_NDC_QTY_QLFYR_CD] = "ML"
        claim_line[f.CLM_LINE_BENE_PD_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line[f.CLM_LINE_PRVDR_PMT_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line[f.CLM_LINE_SBMT_CHRG_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_BENE_PMT_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_BLOOD_DDCTBL_AMT] = round(random.uniform(0, 15), 2)
        claim_line[f.CLM_LINE_MDCR_DDCTBL_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_NUM] = "1"
        claim_line[f.CLM_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
        claim_line[f.CLM_LINE_RX_NUM] = round(random.uniform(0, 100000), 2)
        claim_line[f.CLM_LINE_GRS_CVRD_CST_TOT_AMT] = round(random.uniform(0, 1000), 2)
        claim_line[f.CLM_LINE_OTHR_TP_PD_AMT] = round(random.uniform(0, 1000), 2)

        claim_line_rx: dict[str, Any] = {}
        claim_line_rx[f.CLM_UNIQ_ID] = claim.CLM[f.CLM_UNIQ_ID]
        claim_line_rx[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
        claim_line_rx[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
        claim_line_rx[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
        claim_line_rx[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
        claim_line_rx[f.CLM_LINE_NUM] = "1"
        claim_line_rx[f.CLM_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
        claim_line_rx[f.CLM_DSPNSNG_STUS_CD] = random.choice(["P", "C"])
        claim_line_rx[f.CLM_LINE_RX_ORGN_CD] = random.choice(
            generator.code_systems[f.CLM_LINE_RX_ORGN_CD]
        )
        claim_line_rx[f.CLM_BRND_GNRC_CD] = random.choice(
            generator.code_systems[f.CLM_BRND_GNRC_CD]
        )
        claim_line_rx[f.CLM_PTNT_RSDNC_CD] = random.choice(
            generator.code_systems[f.CLM_PTNT_RSDNC_CD]
        )
        claim_line_rx[f.CLM_PHRMCY_SRVC_TYPE_CD] = random.choice(
            generator.code_systems[f.CLM_PHRMCY_SRVC_TYPE_CD]
        )
        claim_line_rx[f.CLM_LINE_AUTHRZD_FILL_NUM] = (
            "0"  # for whatever reason, this is always zero in the IDR
        )
        claim_line_rx[f.CLM_LTC_DSPNSNG_MTHD_CD] = random.choice(
            generator.code_systems[f.CLM_LTC_DSPNSNG_MTHD_CD]
        )
        claim_line_rx[f.CLM_CMPND_CD] = random.choice(generator.code_systems[f.CLM_CMPND_CD])
        claim_line_rx[f.CLM_LINE_DAYS_SUPLY_QTY] = random.randint(1, 10)
        claim_line_rx[f.CLM_LINE_RX_FILL_NUM] = random.randint(1, 10)
        claim_line_rx[f.CLM_DAW_PROD_SLCTN_CD] = random.choice([0, 1, 2, 3, 4, 5, 6, 7, 8, 9])
        claim_line_rx[f.CLM_DRUG_CVRG_STUS_CD] = random.choice(
            generator.code_systems[f.CLM_DRUG_CVRG_STUS_CD]
        )
        claim_line_rx[f.CLM_CTSTRPHC_CVRG_IND_CD] = random.choice(
            generator.code_systems[f.CLM_CTSTRPHC_CVRG_IND_CD]
        )
        claim_line_rx[f.CLM_LINE_GRS_ABOVE_THRSHLD_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_GRS_BLW_THRSHLD_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_LIS_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_TROOP_TOT_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_PLRO_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_RPTD_MFTR_DSCNT_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_INGRDNT_CST_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_VCCN_ADMIN_FEE_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_SRVC_CST_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_LINE_SLS_TAX_AMT] = round(random.uniform(1, 1000000), 2)
        claim_line_rx[f.CLM_PRCNG_EXCPTN_CD] = random.choice(["", "O", "M"])
        claim_line_rx[f.CLM_CMS_CALCD_MFTR_DSCNT_AMT] = round(random.uniform(0, 1000), 2)
        claim_line_rx[f.CLM_LINE_REBT_PASSTHRU_POS_AMT] = round(random.uniform(0, 1000), 2)
        claim_line_rx[f.CLM_PHRMCY_PRICE_DSCNT_AT_POS_AMT] = round(random.uniform(0, 1000), 2)
        add_meta_timestamps(claim_line_rx, claim.CLM, max_date)

        claim.CLM_LINE.append(claim_line)
        claim.CLM_LINE_RX.append(claim_line_rx)

    if clm_type_cd in INSTITUTIONAL_CLAIM_TYPES:
        tob_code = random.choice(generator.code_systems[f.CLM_BILL_FREQ_CD])
        claim.CLM[f.CLM_BILL_FAC_TYPE_CD] = tob_code[0]
        claim.CLM[f.CLM_BILL_CLSFCTN_CD] = tob_code[1]
        claim.CLM[f.CLM_BILL_FREQ_CD] = tob_code[2]

    claim.CLM[f.CLM_CNTRCTR_NUM] = random.choice(generator.code_systems[f.CLM_CNTRCTR_NUM])
    claim.CLM[f.CLM_NCH_PRMRY_PYR_CD] = random.choice(
        generator.code_systems[f.CLM_NCH_PRMRY_PYR_CD]
    )

    add_meta_timestamps(claim.CLM, claim.CLM, max_date)
    add_meta_timestamps(clm_dt_sgntr, claim.CLM, max_date)
    add_meta_timestamps(rlt_cond_mbr_record, claim.CLM, max_date)

    claim.CLM[f.CLM_FINL_ACTN_IND] = "Y"
    if claim.CLM[f.CLM_TYPE_CD] == 3 or claim.CLM[f.CLM_ADJSTMT_TYPE_CD] == "1":
        claim.CLM[f.CLM_FINL_ACTN_IND] = "N"

    clm_ltst_clm_ind = "N"
    if clm_type_cd in (1, 2, 3, 4, 10, 20, 30, 40, 50, 60, 61, 62, 63, 71, 72, 81, 82):
        clm_ltst_clm_ind = random.choice(["Y", "N"])
    claim.CLM[f.CLM_LTST_CLM_IND] = clm_ltst_clm_ind

    claim.CLM_DCMTN[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
    claim.CLM_DCMTN[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
    claim.CLM_DCMTN[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
    claim.CLM_DCMTN[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]

    # CLM_RIC_CDs are generally tied to the claim type code.
    if clm_type_cd in (20, 30, 50, 60, 61, 62, 63, 64):
        # part A!
        claim.CLM_DCMTN[f.CLM_NRLN_RIC_CD] = "V"  # inpatient
    elif clm_type_cd == 40:
        # outpatient
        claim.CLM_DCMTN[f.CLM_NRLN_RIC_CD] = "W"  # outpatient
    elif clm_type_cd == 10:
        claim.CLM_DCMTN[f.CLM_NRLN_RIC_CD] = random.choice(["U", "V", "W"])
    elif clm_type_cd in (71, 72):
        claim.CLM_DCMTN[f.CLM_NRLN_RIC_CD] = "O"
    elif clm_type_cd in (81, 82):
        claim.CLM_DCMTN[f.CLM_NRLN_RIC_CD] = "M"

    add_meta_timestamps(claim.CLM_DCMTN, claim.CLM, max_date)

    # provider elements:
    if (clm_type_cd < 65 and clm_type_cd >= 10) or clm_type_cd in FISS_CLM_TYPE_CDS:
        claim.CLM[f.PRVDR_BLG_PRVDR_NPI_NUM] = random.choice(TYPE_2_NPIS)
        claim.CLM[f.CLM_ATNDG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        claim.CLM[f.PRVDR_ATNDG_PRVDR_NPI_NUM] = claim.CLM[f.CLM_ATNDG_PRVDR_NPI_NUM]
        claim.CLM[f.CLM_OPRTG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        claim.CLM[f.PRVDR_OPRTG_PRVDR_NPI_NUM] = claim.CLM[f.CLM_OPRTG_PRVDR_NPI_NUM]
        claim.CLM[f.CLM_OTHR_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        claim.CLM[f.PRVDR_OTHR_PRVDR_NPI_NUM] = claim.CLM[f.CLM_OTHR_PRVDR_NPI_NUM]
        claim.CLM[f.CLM_RNDRG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        claim.CLM[f.PRVDR_RNDRNG_PRVDR_NPI_NUM] = claim.CLM[f.CLM_RNDRG_PRVDR_NPI_NUM]
        claim.CLM[f.CLM_BLG_PRVDR_OSCAR_NUM] = random.choice(AVAIL_OSCAR_CODES_INSTITUTIONAL)
        claim.CLM[f.CLM_MDCR_COINSRNC_AMT] = round(random.uniform(0, 25), 2)
        claim.CLM[f.CLM_BLG_PRVDR_ZIP5_CD] = random.choice(["75205", "77550", "77005"])
        claim.CLM[f.CLM_RLT_COND_SGNTR_SK] = random.choice(AVAIL_CLM_RLT_COND_SK)

    if clm_type_cd == 40 or (clm_type_cd > 70 and clm_type_cd <= 82):
        claim.CLM[f.PRVDR_RFRG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
    if clm_type_cd > 70 and clm_type_cd <= 82:
        claim.CLM[f.CLM_BLG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
        claim.CLM[f.CLM_RLT_COND_SGNTR_SK] = "0"
        if random.choice([0, 1]):
            claim.CLM[f.CLM_BLG_PRVDR_NPI_NUM] = random.choice(TYPE_2_NPIS)
            claim.CLM[f.PRVDR_BLG_PRVDR_NPI_NUM] = claim.CLM[f.CLM_BLG_PRVDR_NPI_NUM]

    # generate claim header financial elements here
    claim.CLM[f.CLM_SBMT_CHRG_AMT] = round(random.uniform(1, 1000000), 2)
    if clm_type_cd == 71 or clm_type_cd == 72:
        claim.CLM[f.CLM_RFRG_PRVDR_PIN_NUM] = random.choice(
            [
                9181272397,
                9181272391,
                918127239123,
            ]
        )
    if clm_type_cd > 70 and clm_type_cd <= 82:
        claim.CLM[f.CLM_ALOWD_CHRG_AMT] = round(random.uniform(1, 1000000), 2)
        claim.CLM[f.CLM_BENE_PD_AMT] = round(random.uniform(1, 1000000), 2)
        claim.CLM[f.CLM_BENE_PMT_AMT] = round(random.uniform(1, 1000000), 2)
        claim.CLM[f.CLM_PRVDR_PMT_AMT] = round(random.uniform(1, 1000000), 2)
    claim.CLM[f.CLM_PMT_AMT] = round(random.uniform(1, claim.CLM[f.CLM_SBMT_CHRG_AMT]), 2)
    claim.CLM[f.CLM_MDCR_DDCTBL_AMT] = round(random.uniform(1, 1676), 2)
    claim.CLM[f.CLM_NCVRD_CHRG_AMT] = round(
        claim.CLM[f.CLM_SBMT_CHRG_AMT] - claim.CLM[f.CLM_PMT_AMT], 2
    )
    claim.CLM[f.CLM_BLOOD_LBLTY_AMT] = round(random.uniform(0, 25), 2)

    if clm_type_cd in (40, 71, 72, 81, 82):
        # be sure to check that DME claims meet the above.
        claim.CLM[f.CLM_PRVDR_PMT_AMT] = round(random.uniform(0, 25), 2)

    if clm_type_cd in (71, 72, 81, 82):
        claim.CLM[f.CLM_BENE_INTRST_PD_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        claim.CLM[f.CLM_BENE_PMT_COINSRNC_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        claim.CLM[f.CLM_BLOOD_CHRG_AMT] = round(random.uniform(0, 500), 2)
        claim.CLM[f.CLM_BLOOD_NCVRD_CHRG_AMT] = round(random.uniform(0, 500), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        claim.CLM[f.CLM_COB_PTNT_RESP_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (81, 82):
        claim.CLM[f.CLM_OTHR_TP_PD_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64, 71, 72, 81, 82):
        claim.CLM[f.CLM_PRVDR_INTRST_PD_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        claim.CLM[f.CLM_PRVDR_OTAF_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (40, 50, 60, 61, 62, 63, 64, 81, 82):
        claim.CLM[f.CLM_PRVDR_RMNG_DUE_AMT] = round(random.uniform(0, 1000), 2)
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        claim.CLM[f.CLM_TOT_CNTRCTL_AMT] = round(random.uniform(0, 10000), 2)

    claim.CLM_VAL = []
    # CLM_OPRTNL_DSPRTNT_AMT + CLM_OPRTNL_IME_AMT
    if clm_type_cd in (20, 40, 60, 61, 62, 63, 64):
        # Note, this is a table we'll use sparsely, it appears. I've replaced the 5 key unique
        # identifier with CLM_UNIQ_ID.
        clm_val_dsprtnt = {
            f.CLM_DT_SGNTR_SK: claim.CLM[f.CLM_DT_SGNTR_SK],
            f.CLM_NUM_SK: claim.CLM[f.CLM_NUM_SK],
            f.GEO_BENE_SK: claim.CLM[f.GEO_BENE_SK],
            f.CLM_TYPE_CD: claim.CLM[f.CLM_TYPE_CD],
            f.CLM_VAL_CD: 18,
            f.CLM_VAL_AMT: round(random.uniform(1, 15000), 2),
            f.CLM_VAL_SQNC_NUM: 14,
        }
        claim.CLM_VAL.append(clm_val_dsprtnt)
        clm_val_ime = {
            f.CLM_DT_SGNTR_SK: claim.CLM[f.CLM_DT_SGNTR_SK],
            f.CLM_NUM_SK: claim.CLM[f.CLM_NUM_SK],
            f.GEO_BENE_SK: claim.CLM[f.GEO_BENE_SK],
            f.CLM_TYPE_CD: claim.CLM[f.CLM_TYPE_CD],
            f.CLM_VAL_CD: 19,
            f.CLM_VAL_AMT: round(random.uniform(1, 15000), 2),
            f.CLM_VAL_SQNC_NUM: 3,
        }
        claim.CLM_VAL.append(clm_val_ime)
        for clm_val in claim.CLM_VAL:
            add_meta_timestamps(clm_val, claim.CLM, max_date)

    # Add procedures
    claim.CLM_PROD = []
    if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
        num_procedures_to_add = random.randint(1, 5)
        for proc in range(1, num_procedures_to_add):
            procedure = gen_procedure_icd10pcs()
            procedure[f.CLM_PRCDR_PRFRM_DT] = random_date(
                claim.CLM[f.CLM_FROM_DT], claim.CLM[f.CLM_THRU_DT]
            )
            procedure[f.CLM_VAL_SQNC_NUM] = proc
            procedure[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
            procedure[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
            procedure[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
            procedure[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
            add_meta_timestamps(procedure, claim.CLM, max_date)
            claim.CLM_PROD.append(procedure)

    # add diagnoses
    diagnoses = add_diagnoses(clm_type_cd=clm_type_cd)
    for diagnosis in diagnoses:
        diagnosis[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
        diagnosis[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
        diagnosis[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
        diagnosis[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
        claim.CLM_PROD.append(diagnosis)
        add_meta_timestamps(diagnosis, claim.CLM, max_date)

    # clm_dt_sgntr info
    if clm_type_cd in (10, 20, 30, 50, 60, 61, 62, 63, 64):
        clm_dt_sgntr[f.CLM_ACTV_CARE_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
        clm_dt_sgntr[f.CLM_DSCHRG_DT] = claim.CLM[f.CLM_THRU_DT]
        if clm_type_cd in (20, 30):
            if random.choice([0, 1]):
                clm_dt_sgntr[f.CLM_QLFY_STAY_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
                clm_dt_sgntr[f.CLM_QLFY_STAY_THRU_DT] = claim.CLM[f.CLM_THRU_DT]
            else:
                clm_dt_sgntr[f.CLM_QLFY_STAY_FROM_DT] = "1000-01-01"
                clm_dt_sgntr[f.CLM_QLFY_STAY_THRU_DT] = "1000-01-01"

        if clm_type_cd in (50, 60, 61, 62, 63, 64):
            clm_dt_sgntr[f.CLM_MDCR_EXHSTD_DT] = claim.CLM[f.CLM_THRU_DT]
            if random.choice([0, 1]):
                clm_dt_sgntr[f.CLM_NCVRD_FROM_DT] = claim.CLM[f.CLM_THRU_DT]
                clm_dt_sgntr[f.CLM_NCVRD_THRU_DT] = claim.CLM[f.CLM_THRU_DT]
            else:
                clm_dt_sgntr[f.CLM_NCVRD_FROM_DT] = "1000-01-01"
                clm_dt_sgntr[f.CLM_NCVRD_THRU_DT] = "1000-01-01"
            if clm_type_cd >= 60:
                clm_dt_sgntr[f.CLM_ACTV_CARE_THRU_DT] = claim.CLM[f.CLM_THRU_DT]

    clm_dt_sgntr[f.CLM_SUBMSN_DT] = claim.CLM[
        f.CLM_THRU_DT
    ]  # This synthetic hospital is really on top of it!

    # clm_dt_sgntr[fc.CLM_MDCR_NCH_PTNT_STUS_IND_CD] =
    # random.choice(code_systems[fc.CLM_MDCR_NCH_PTNT_STUS_IND_CD])
    clm_dt_sgntr[f.CLM_CMS_PROC_DT] = claim.CLM[f.CLM_THRU_DT]
    clm_dt_sgntr[f.CLM_NCH_WKLY_PROC_DT] = claim.CLM[f.CLM_THRU_DT]
    claim.CLM_DT_SGNTR = clm_dt_sgntr

    if clm_type_cd in INSTITUTIONAL_CLAIM_TYPES:
        institutional_parts: dict[str, Any] = {}
        institutional_parts[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
        institutional_parts[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
        institutional_parts[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
        institutional_parts[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
        if clm_type_cd == 40:
            institutional_parts[f.CLM_OP_SRVC_TYPE_CD] = random.choice(
                generator.code_systems[f.CLM_OP_SRVC_TYPE_CD]
            )
        institutional_parts[f.CLM_FI_ACTN_CD] = random.choice(
            generator.code_systems[f.CLM_FI_ACTN_CD]
        )
        institutional_parts[f.CLM_ADMSN_TYPE_CD] = random.choice(
            generator.code_systems[f.CLM_ADMSN_TYPE_CD]
        )
        institutional_parts[f.BENE_PTNT_STUS_CD] = random.choice(
            generator.code_systems[f.BENE_PTNT_STUS_CD]
        )
        institutional_parts[f.CLM_MDCR_INSTNL_MCO_PD_SW] = random.choice(
            generator.code_systems[f.CLM_MDCR_INSTNL_MCO_PD_SW]
        )
        institutional_parts[f.CLM_ADMSN_SRC_CD] = random.choice(
            generator.code_systems[f.CLM_ADMSN_SRC_CD]
        )
        institutional_parts[f.DGNS_DRG_CD] = random.choice(get_drg_dgns_codes())
        institutional_parts[f.DGNS_DRG_OUTLIER_CD] = random.choice(
            generator.code_systems[f.DGNS_DRG_OUTLIER_CD]
        )
        institutional_parts[f.CLM_INSTNL_CVRD_DAY_CNT] = random.randint(0, 10)
        institutional_parts[f.CLM_MDCR_IP_LRD_USE_CNT] = random.randint(0, 10)
        institutional_parts[f.CLM_INSTNL_PER_DIEM_AMT] = round(random.uniform(0, 350), 2)
        institutional_parts[f.CLM_HIPPS_UNCOMPD_CARE_AMT] = round(random.uniform(0, 350), 2)
        institutional_parts[f.CLM_MDCR_INSTNL_PRMRY_PYR_AMT] = round(random.uniform(0, 3500), 2)
        institutional_parts[f.CLM_INSTNL_DRG_OUTLIER_AMT] = round(random.uniform(0, 3500), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_DSPRPRTNT_AMT] = round(random.uniform(0, 3500), 2)
        institutional_parts[f.CLM_INSTNL_MDCR_COINS_DAY_CNT] = random.randint(0, 5)
        institutional_parts[f.CLM_INSTNL_NCVRD_DAY_CNT] = random.randint(0, 5)
        institutional_parts[f.CLM_MDCR_IP_PPS_DRG_WT_NUM] = round(random.uniform(0.5, 1.5), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_EXCPTN_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_CPTL_FSP_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_CPTL_IME_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_OUTLIER_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_CPTL_HRMLS_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_MDCR_IP_PPS_CPTL_TOT_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_MDCR_IP_BENE_DDCTBL_AMT] = round(random.uniform(0, 25), 2)
        institutional_parts[f.CLM_PPS_IND_CD] = random.choice(["", "2"])

        if clm_type_cd in (10, 20):
            institutional_parts[f.CLM_FINL_STDZD_PYMT_AMT] = round(random.uniform(0, 10000), 2)
        if clm_type_cd == 20:
            institutional_parts[f.CLM_HAC_RDCTN_PYMT_AMT] = round(random.uniform(0, 5000), 2)
            institutional_parts[f.CLM_HIPPS_MODEL_BNDLD_PMT_AMT] = round(
                random.uniform(0, 10000), 2
            )
            institutional_parts[f.CLM_SITE_NTRL_CST_BSD_PYMT_AMT] = round(
                random.uniform(0, 10000), 2
            )
            institutional_parts[f.CLM_SITE_NTRL_IP_PPS_PYMT_AMT] = round(
                random.uniform(0, 10000), 2
            )
            institutional_parts[f.CLM_SS_OUTLIER_STD_PYMT_AMT] = round(random.uniform(0, 10000), 2)
        if clm_type_cd in (20, 30, 60, 61, 62, 63, 64):
            institutional_parts[f.CLM_HIPPS_READMSN_RDCTN_AMT] = round(random.uniform(0, 5000), 2)
            institutional_parts[f.CLM_HIPPS_VBP_AMT] = round(random.uniform(0, 5000), 2)
            institutional_parts[f.CLM_INSTNL_LOW_VOL_PMT_AMT] = round(random.uniform(0, 10000), 2)
            institutional_parts[f.CLM_MDCR_IP_1ST_YR_RATE_AMT] = round(random.uniform(0, 10000), 2)
            institutional_parts[f.CLM_MDCR_IP_SCND_YR_RATE_AMT] = round(random.uniform(0, 10000), 2)
            institutional_parts[f.CLM_PPS_MD_WVR_STDZD_VAL_AMT] = round(random.uniform(0, 10000), 2)
        if clm_type_cd in (40, 61, 64, 62, 20, 63, 30, 60):
            institutional_parts[f.CLM_INSTNL_PRFNL_AMT] = round(random.uniform(0, 10000), 2)

        add_meta_timestamps(institutional_parts, claim.CLM, max_date)

        if clm_type_cd == 10:
            if random.choice([0, 1]):
                institutional_parts[f.CLM_HHA_LUP_IND_CD] = "L"
            institutional_parts[f.CLM_HHA_RFRL_CD] = random.choice(
                generator.code_systems[f.CLM_HHA_RFRL_CD]
            )
            institutional_parts[f.CLM_MDCR_HHA_TOT_VISIT_CNT] = round(random.uniform(0, 25), 2)

        if clm_type_cd == 40:
            institutional_parts[f.CLM_MDCR_INSTNL_BENE_PD_AMT] = round(random.uniform(0, 25), 2)

        if clm_type_cd == 50:
            institutional_parts[f.CLM_MDCR_HOSPC_PRD_CNT] = random.choice(["1", "2", "3"])
        # We'll throw in a non-payment code on occasion
        if random.choice([0, 10]) > 1:
            institutional_parts[f.CLM_MDCR_NPMT_RSN_CD] = random.choice(
                generator.code_systems[f.CLM_MDCR_NPMT_RSN_CD]
            )
        claim.CLM_INSTNL = institutional_parts

    # professional "stuff"
    claim.CLM_PRFNL[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
    claim.CLM_PRFNL[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
    claim.CLM_PRFNL[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
    claim.CLM_PRFNL[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
    claim.CLM_PRFNL[f.CLM_CARR_PMT_DNL_CD] = random.choice(
        generator.code_systems[f.CLM_CARR_PMT_DNL_CD]
    )
    claim.CLM_PRFNL[f.CLM_MDCR_PRFNL_PRMRY_PYR_AMT] = round(random.uniform(10, 1000), 2)
    claim.CLM_PRFNL[f.CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW] = random.choice(
        generator.code_systems[f.CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW]
    )
    claim.CLM_PRFNL[f.CLM_CLNCL_TRIL_NUM] = str(random.randint(0, 10000))
    if clm_type_cd in (71, 72, 81, 82):
        claim.CLM_PRFNL[f.CLM_PRVDR_ACNT_RCVBL_OFST_AMT] = round(random.uniform(0, 1000), 2)
    add_meta_timestamps(claim.CLM_PRFNL, claim.CLM, max_date)

    num_clm_lines = random.randint(1, 15)
    for line_num in range(1, num_clm_lines + 1):
        if clm_type_cd in PHARMACY_CLM_TYPE_CDS:
            # handled above
            continue
        claim_line: dict[str, Any] = {}
        claim_line_inst: dict[str, Any] = {}
        claim_line_prfnl: dict[str, Any] = {}
        claim_line[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
        claim_line[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
        claim_line[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
        claim_line[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]
        claim_line[f.CLM_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
        claim_line[f.CLM_LINE_FROM_DT] = claim.CLM[f.CLM_FROM_DT]
        claim_line[f.CLM_LINE_THRU_DT] = claim.CLM[f.CLM_THRU_DT]
        if probability(0.10):
            claim_line[f.CLM_LINE_PMD_UNIQ_TRKNG_NUM] = gen_basic_id(
                field=f.CLM_LINE_PMD_UNIQ_TRKNG_NUM,
                length=13,  # varchar(14) so 13 + 1 for '-' prefix
                allowed_chars=string.ascii_uppercase + string.digits,
            )

        if clm_type_cd >= 10 and clm_type_cd <= 64:
            claim_line_inst[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
            claim_line_inst[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
            claim_line_inst[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
            claim_line_inst[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]

        if clm_type_cd >= 71 and clm_type_cd <= 82:
            claim_line[f.CLM_RNDRG_PRVDR_TYPE_CD] = random.choice(
                generator.code_systems[f.CLM_PRVDR_TYPE_CD]
            )
            claim_line_prfnl[f.GEO_BENE_SK] = claim.CLM[f.GEO_BENE_SK]
            claim_line_prfnl[f.CLM_DT_SGNTR_SK] = claim.CLM[f.CLM_DT_SGNTR_SK]
            claim_line_prfnl[f.CLM_TYPE_CD] = claim.CLM[f.CLM_TYPE_CD]
            claim_line_prfnl[f.CLM_NUM_SK] = claim.CLM[f.CLM_NUM_SK]

            claim_line_prfnl[f.CLM_BENE_PRMRY_PYR_PD_AMT] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl[f.CLM_SRVC_DDCTBL_SW] = random.choice(
                generator.code_systems[f.CLM_SRVC_DDCTBL_SW]
            )
            claim_line_prfnl[f.CLM_PRCSG_IND_CD] = random.choice(
                generator.code_systems[f.CLM_PRCSG_IND_CD]
            )
            claim_line_prfnl[f.CLM_PMT_80_100_CD] = random.choice(
                generator.code_systems[f.CLM_PMT_80_100_CD]
            )

            claim_line_prfnl[f.CLM_MTUS_IND_CD] = random.choice(
                generator.code_systems[f.CLM_MTUS_IND_CD]
            )
            claim_line_prfnl[f.CLM_LINE_PRFNL_MTUS_CNT] = random.randint(0, 10)
            # claim_line_prfnl[fc.CLM_PRCNG_LCLTY_CD] =
            # random.choice(generator.code_systems[fc.CLM_PRCNG_LCLTY_CD])
            # not yet available from the IDR
            claim_line_prfnl[f.CLM_PHYSN_ASTNT_CD] = random.choice(
                generator.code_systems[f.CLM_PHYSN_ASTNT_CD]
            )

            claim_line_prfnl[f.CLM_LINE_CARR_CLNCL_CHRG_AMT] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl[f.CLM_LINE_CARR_PSYCH_OT_LMT_AMT] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl[f.CLM_LINE_PRFNL_INTRST_AMT] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl[f.CLM_MDCR_PRMRY_PYR_ALOWD_AMT] = round(random.uniform(0, 10000), 2)

            if random.randint(0, 10) == 6:
                claim_line_prfnl[f.CLM_LINE_HCT_HGB_TYPE_CD] = random.choice(["R1", "R2"])
                claim_line_prfnl[f.CLM_LINE_CARR_CLNCL_LAB_NUM] = random.choice(
                    [
                        "11D1111111",
                        "22D2222222",
                    ]
                )

            # these don't have much variance in our synthetic data, but they are not strictly
            # the same in actual data!
            claim_line[f.CLM_LINE_MDCR_COINSRNC_AMT] = round(random.uniform(0, 5), 2)

            # pick a random diagnosis.

            claim_line[f.CLM_LINE_DGNS_CD] = random.choice(diagnoses)[f.CLM_DGNS_CD]
            claim_line[f.CLM_POS_CD] = random.choice(generator.code_systems[f.CLM_POS_CD])
            claim_line[f.CLM_RNDRG_PRVDR_PRTCPTG_CD] = random.choice(
                generator.code_systems[f.CLM_RNDRG_PRVDR_PRTCPTG_CD]
            )

        if clm_type_cd >= 71 and clm_type_cd <= 72:
            claim_line[f.CLM_RNDRG_PRVDR_TAX_NUM] = random.choice(["1928347912", "912834729"])
            claim_line[f.CLM_RNDRG_PRVDR_PIN_NUM] = random.choice(["29364819", "19238747"])
            claim_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
            claim_line[f.CLM_RNDRG_PRVDR_NPI_NUM] = claim_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM]
            if random.choice([0, 10]) == 7:
                claim_line[f.CLM_LINE_ANSTHSA_UNIT_CNT] = random.uniform(0, 10)
            if random.choice([0, 15]) == 7:
                claim_line[f.CLM_LINE_RX_NUM] = random.choice(["1234", "423482347"])

        if clm_type_cd == 81 or clm_type_cd == 82:
            claim_line_prfnl[f.CLM_LINE_DMERC_SCRN_SVGS_AMT] = round(random.uniform(0, 10000), 2)
            claim_line_prfnl[f.CLM_SUPLR_TYPE_CD] = random.choice(
                generator.code_systems[f.CLM_SUPLR_TYPE_CD]
            )
            claim_line_prfnl[f.CLM_LINE_PRFNL_DME_PRICE_AMT] = round(random.uniform(0, 10000), 2)
            claim_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM] = random.choice(TYPE_1_NPIS)
            claim_line[f.CLM_RNDRNG_PRVDR_NPI_NUM] = claim_line[f.PRVDR_RNDRNG_PRVDR_NPI_NUM]

        add_meta_timestamps(claim_line_prfnl, claim.CLM, max_date)

        claim_line[f.CLM_LINE_HCPCS_CD] = random.choice(get_hcpcs_proc_codes())
        num_mods = random.randint(0, 5)
        if num_mods:
            claim_line[f.HCPCS_1_MDFR_CD] = random.choice(HCPCS_MODS)
        if num_mods > 1:
            claim_line[f.HCPCS_2_MDFR_CD] = random.choice(HCPCS_MODS)
        if num_mods > 2:
            claim_line[f.HCPCS_3_MDFR_CD] = random.choice(HCPCS_MODS)
        if num_mods > 3:
            claim_line[f.HCPCS_4_MDFR_CD] = random.choice(HCPCS_MODS)
        if num_mods > 4:
            claim_line[f.HCPCS_5_MDFR_CD] = random.choice(HCPCS_MODS)
        if random.choice([0, 1]):
            claim_line[f.CLM_LINE_NDC_CD] = random.choice(AVAILABLE_NDC)
            claim_line[f.CLM_LINE_NDC_QTY] = round(random.uniform(1, 1000), 2)
            claim_line[f.CLM_LINE_NDC_QTY_QLFYR_CD] = "ML"
        claim_line[f.CLM_LINE_SRVC_UNIT_QTY] = round(random.uniform(0, 5), 2)
        if clm_type_cd in INSTITUTIONAL_CLAIM_TYPES:
            claim_line[f.CLM_LINE_REV_CTR_CD] = random.choice(
                generator.code_systems[f.CLM_REV_CNTR_CD]
            )
        claim_line[f.CLM_LINE_BENE_PMT_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_BENE_PD_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_ALOWD_CHRG_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_SBMT_CHRG_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_CVRD_PD_AMT] = round(random.uniform(0, 5), 2)
        claim_line[f.CLM_LINE_BLOOD_DDCTBL_AMT] = round(random.uniform(0, 15), 2)
        claim_line[f.CLM_LINE_MDCR_DDCTBL_AMT] = round(random.uniform(0, 5), 2)

        claim_line[f.CLM_LINE_PRVDR_PMT_AMT] = round(random.uniform(0, 1500), 2)
        claim_line[f.CLM_LINE_NCVRD_CHRG_AMT] = round(random.uniform(0, 1500), 2)

        claim_line[f.CLM_LINE_FINL_ACTN_IND] = random.choice(["Y", "N"])
        claim_line[f.CLM_LINE_LTST_CLM_IND] = random.choice(["Y", "N"])
        if clm_type_cd in (20, 30, 40, 50, 60, 61, 62, 63, 64, 71, 72, 81, 82):
            claim_line[f.CLM_LINE_OTAF_AMT] = round(random.uniform(0, 1000), 2)

        add_meta_timestamps(claim_line, claim.CLM, max_date)

        claim_line_inst[f.CLM_LINE_INSTNL_ADJSTD_AMT] = round(random.uniform(0, 1500), 2)
        claim_line_inst[f.CLM_LINE_INSTNL_RDCD_AMT] = round(random.uniform(0, 1500), 2)
        claim_line_inst[f.CLM_DDCTBL_COINSRNC_CD] = random.choice(
            generator.code_systems[f.CLM_DDCTBL_COINSRNC_CD]
        )
        claim_line_inst[f.CLM_LINE_INSTNL_RATE_AMT] = round(random.uniform(0, 15), 2)
        claim_line_inst[f.CLM_LINE_INSTNL_MSP1_PD_AMT] = round(random.uniform(0, 15), 2)
        claim_line_inst[f.CLM_LINE_INSTNL_MSP2_PD_AMT] = round(random.uniform(0, 2), 2)
        claim_line_inst[f.CLM_LINE_INSTNL_REV_CTR_DT] = claim.CLM[f.CLM_FROM_DT]

        # In contrast to v2 DD this appears to populated in many.
        claim_line_inst[f.CLM_REV_DSCNT_IND_CD] = random.choice(
            generator.code_systems[f.CLM_REV_DSCNT_IND_CD]
        )
        claim_line_inst[f.CLM_OTAF_ONE_IND_CD] = random.choice(
            generator.code_systems[f.CLM_OTAF_IND_CD]
        )
        claim_line_inst[f.CLM_REV_PACKG_IND_CD] = random.choice(
            generator.code_systems[f.CLM_REV_PACKG_IND_CD]
        )
        claim_line_inst[f.CLM_REV_PMT_MTHD_CD] = random.choice(
            generator.code_systems[f.CLM_REV_PMT_MTHD_CD]
        )
        claim_line_inst[f.CLM_REV_CNTR_STUS_CD] = random.choice(
            generator.code_systems[f.CLM_REV_CNTR_STUS_CD]
        )
        claim_line_inst[f.CLM_ANSI_SGNTR_SK] = random.choice(
            [
                "-8585",
                "-1",
                "-4365",
                "-1508",
                "-5555",
                "-9204",
                "-6857",
                "-5816",
                "-11978",
            ]
        )
        claim_line_inst[f.CLM_LINE_ADD_ON_PYMT_AMT] = round(random.uniform(0, 10000), 2)
        claim_line_inst[f.CLM_LINE_NON_EHR_RDCTN_AMT] = round(random.uniform(0, 500), 2)
        claim_line_inst[f.CLM_REV_CNTR_TDAPA_AMT] = round(random.uniform(0, 10000), 2)
        add_meta_timestamps(claim_line_inst, claim.CLM, max_date)

        claim_line[f.CLM_UNIQ_ID] = claim.CLM[f.CLM_UNIQ_ID]
        claim_line[f.CLM_LINE_NUM] = line_num
        claim_line_inst[f.CLM_LINE_NUM] = line_num
        claim_line_prfnl[f.CLM_LINE_NUM] = line_num
        claim.CLM_LINE.append(claim_line)
        if clm_type_cd >= 10 and clm_type_cd <= 65:
            claim.CLM_LINE_INSTNL.append(claim_line_inst)
        elif clm_type_cd >= 71 and clm_type_cd <= 82:
            claim.CLM_LINE_PRFNL.append(claim_line_prfnl)

        # CLM_REV_APC_HIPPS_CD never populated for CLM_TYPE_CD 60 apart from null values (00000,0,~)
    return claim


def gen_pac_version_of_claim(claim: _GeneratedClaim, max_date: str):
    # note the fields to delete

    # Generating a Synthetic PAC claim is done in a rather naive way.
    # 1. Create a new CLM_UNIQ_ID
    # 2. Create a new 5 part key (eg GEO_BENE_SK, CLM_DT_SGNTR_SK)
    # 3. Update the relevant parts
    # 4. Delete information that's not accessible from that given source. This can probably be done
    # via config files in the future.

    pac_claim = copy.deepcopy(claim)
    pac_claim.CLM[f.CLM_UNIQ_ID] = gen_basic_id(field=f.CLM_UNIQ_ID, length=13)
    pac_clm_type_cd = int(pac_claim.CLM[f.CLM_TYPE_CD])

    if pac_clm_type_cd in (60, 61, 62, 63, 64):
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choices(
            [1011, 2011, 1041, 2041], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd == 40:
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choices(
            [1013, 2013, 1071, 2071], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd == 10:
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choices(
            [1032, 2032, 1033, 2033], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd == 20:
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choice([1021, 2021])

    if pac_clm_type_cd == 30:
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choice([1018, 2018])

    if pac_clm_type_cd == 50:
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choices(
            [1081, 2081, 1082, 2082], weights=[0.48, 0.48, 0.02, 0.02]
        )[0]

    if pac_clm_type_cd in (71, 72):
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choice([1700, 2700])

    if pac_clm_type_cd in (81, 82):
        pac_claim.CLM[f.CLM_TYPE_CD] = random.choice([1800, 2800])

    if f.CLM_BLOOD_PT_FRNSH_QTY in pac_claim.CLM:
        pac_claim.CLM.pop(f.CLM_BLOOD_PT_FRNSH_QTY)
    if f.CLM_NCH_PRMRY_PYR_CD in pac_claim.CLM:
        pac_claim.CLM.pop(f.CLM_NCH_PRMRY_PYR_CD)

    if pac_claim.CLM[f.CLM_TYPE_CD] < 2000:
        pac_claim.CLM[f.CLM_FINL_ACTN_IND] = "N"

    pac_claim.CLM[f.CLM_DT_SGNTR_SK] = gen_basic_id(field=f.CLM_DT_SGNTR_SK, length=12)
    pac_claim.CLM_DT_SGNTR[f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
    pac_claim.CLM[f.GEO_BENE_SK] = gen_basic_id(field=f.GEO_BENE_SK, length=5)
    pac_claim.CLM_FISS = {}
    pac_claim.CLM_FISS[f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
    pac_claim.CLM_FISS[f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
    pac_claim.CLM_FISS[f.CLM_NUM_SK] = pac_claim.CLM[f.CLM_NUM_SK]
    pac_claim.CLM_FISS[f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]
    add_meta_timestamps(pac_claim.CLM_FISS, claim.CLM, max_date)

    pac_claim.CLM_LCTN_HSTRY = {}
    pac_claim.CLM_LCTN_HSTRY[f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
    pac_claim.CLM_LCTN_HSTRY[f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
    pac_claim.CLM_LCTN_HSTRY[f.CLM_NUM_SK] = pac_claim.CLM[f.CLM_NUM_SK]
    pac_claim.CLM_LCTN_HSTRY[f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]
    pac_claim.CLM_LCTN_HSTRY[f.CLM_LCTN_CD_SQNC_NUM] = "1"
    pac_claim.CLM_LCTN_HSTRY[f.CLM_AUDT_TRL_STUS_CD] = random.choice(
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
    add_meta_timestamps(pac_claim.CLM_LCTN_HSTRY, claim.CLM, max_date)

    for i in range(len(pac_claim.CLM_LINE)):
        pac_claim.CLM_LINE[i][f.CLM_LINE_NUM] = i + 1
        pac_claim.CLM_LINE[i][f.CLM_UNIQ_ID] = pac_claim.CLM[f.CLM_UNIQ_ID]
        pac_claim.CLM_LINE[i][f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
        pac_claim.CLM_LINE[i][f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
        pac_claim.CLM_LINE[i][f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]
        tracking_num = pac_claim.CLM_LINE[i].get(f.CLM_LINE_PMD_UNIQ_TRKNG_NUM)
        if tracking_num:
            claim_line_dcmtn: dict[str, Any] = {}
            claim_line_dcmtn[f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
            claim_line_dcmtn[f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
            claim_line_dcmtn[f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]
            claim_line_dcmtn[f.CLM_NUM_SK] = pac_claim.CLM[f.CLM_NUM_SK]
            claim_line_dcmtn[f.CLM_LINE_PA_UNIQ_TRKNG_NUM] = tracking_num
            pac_claim.CLM_LINE[i].pop(f.CLM_LINE_PMD_UNIQ_TRKNG_NUM)
            add_meta_timestamps(claim_line_dcmtn, claim.CLM, max_date)
            claim_line_dcmtn[f.CLM_LINE_NUM] = i + 1
            pac_claim.CLM_LINE_DCMTN.append(claim_line_dcmtn)

    # Update CLM_LINE_INSTNL for institutional claims only
    if len(pac_claim.CLM_LINE_INSTNL) > 0:
        for i in range(len(pac_claim.CLM_LINE_INSTNL)):
            pac_claim.CLM_LINE_INSTNL[i][f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
            pac_claim.CLM_LINE_INSTNL[i][f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
            pac_claim.CLM_LINE_INSTNL[i][f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]

    # Update CLM_LINE_PRFNL for professional claims only
    if len(pac_claim.CLM_LINE_PRFNL) > 0:
        for i in range(len(pac_claim.CLM_LINE_PRFNL)):
            pac_claim.CLM_LINE_PRFNL[i][f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
            pac_claim.CLM_LINE_PRFNL[i][f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
            pac_claim.CLM_LINE_PRFNL[i][f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]

    for i in range(len(pac_claim.CLM_VAL)):
        pac_claim.CLM_VAL[i][f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
        pac_claim.CLM_VAL[i][f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
        pac_claim.CLM_VAL[i][f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]

    # Update CLM_INSTNL for institutional claims only
    if pac_claim.CLM_INSTNL:
        pac_claim.CLM_INSTNL[f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
        pac_claim.CLM_INSTNL[f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
        pac_claim.CLM_INSTNL[f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]

    for i in range(len(pac_claim.CLM_PROD)):
        pac_claim.CLM_PROD[i][f.CLM_DT_SGNTR_SK] = pac_claim.CLM[f.CLM_DT_SGNTR_SK]
        pac_claim.CLM_PROD[i][f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
        pac_claim.CLM_PROD[i][f.CLM_TYPE_CD] = pac_claim.CLM[f.CLM_TYPE_CD]
    if f.CLM_MDCR_EXHSTD_DT in pac_claim.CLM_DT_SGNTR:
        pac_claim.CLM_DT_SGNTR.pop(f.CLM_MDCR_EXHSTD_DT)
    if f.CLM_NCVRD_FROM_DT in pac_claim.CLM_DT_SGNTR:
        pac_claim.CLM_DT_SGNTR.pop(f.CLM_NCVRD_FROM_DT)
    if f.CLM_NCVRD_THRU_DT in pac_claim.CLM_DT_SGNTR:
        pac_claim.CLM_DT_SGNTR.pop(f.CLM_NCVRD_THRU_DT)
    if f.CLM_NCH_WKLY_PROC_DT in pac_claim.CLM_DT_SGNTR:
        pac_claim.CLM_DT_SGNTR.pop(f.CLM_NCH_WKLY_PROC_DT)
    if f.CLM_ACTV_CARE_THRU_DT in pac_claim.CLM_DT_SGNTR:
        pac_claim.CLM_DT_SGNTR.pop(f.CLM_ACTV_CARE_THRU_DT)
    # Remove institutional-specific fields for institutional claims only
    if pac_claim.CLM_INSTNL:
        if f.CLM_MDCR_IP_BENE_DDCTBL_AMT in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_MDCR_IP_BENE_DDCTBL_AMT)
        if f.CLM_MDCR_INSTNL_PRMRY_PYR_AMT in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_MDCR_INSTNL_PRMRY_PYR_AMT)
        if f.CLM_PPS_IND_CD in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_PPS_IND_CD)
        if f.CLM_MDCR_HOSPC_PRD_CNT in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_MDCR_HOSPC_PRD_CNT)
        if f.CLM_INSTNL_DRG_OUTLIER_AMT in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_INSTNL_DRG_OUTLIER_AMT)
        if f.CLM_MDCR_HHA_TOT_VISIT_CNT in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_MDCR_HHA_TOT_VISIT_CNT)
        if f.CLM_HHA_LUP_IND_CD in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_HHA_LUP_IND_CD)
        if f.CLM_HHA_RFRL_CD in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_HHA_RFRL_CD)
        if f.CLM_MDCR_INSTNL_BENE_PD_AMT in pac_claim.CLM_INSTNL:
            pac_claim.CLM_INSTNL.pop(f.CLM_MDCR_INSTNL_BENE_PD_AMT)

    # Remove institutional line-specific fields for institutional claims only
    if len(pac_claim.CLM_LINE_INSTNL) > 0:
        for i in range(len(pac_claim.CLM_LINE_INSTNL)):
            if i == len(pac_claim.CLM_LINE_INSTNL):
                continue
            if f.CLM_ANSI_SGNTR_SK in pac_claim.CLM_LINE_INSTNL[i]:
                pac_claim.CLM_LINE_INSTNL[i].pop(f.CLM_ANSI_SGNTR_SK)
            if f.CLM_OTAF_ONE_IND_CD in pac_claim.CLM_LINE_INSTNL[i]:
                pac_claim.CLM_LINE_INSTNL[i].pop(f.CLM_OTAF_ONE_IND_CD)
            if f.CLM_REV_CNTR_STUS_CD in pac_claim.CLM_LINE_INSTNL[i]:
                pac_claim.CLM_LINE_INSTNL[i].pop(f.CLM_REV_CNTR_STUS_CD)

    for i in range(len(pac_claim.CLM_LINE)):
        if i == len(pac_claim.CLM_LINE):
            continue
        if f.CLM_LINE_ANSTHSA_UNIT_CNT in pac_claim.CLM_LINE[i]:
            pac_claim.CLM_LINE[i].pop(f.CLM_LINE_ANSTHSA_UNIT_CNT)
        if f.CLM_RNDRG_PRVDR_PRTCPTG_CD in pac_claim.CLM_LINE[i]:
            pac_claim.CLM_LINE[i].pop(f.CLM_RNDRG_PRVDR_PRTCPTG_CD)

    for i in range(len(pac_claim.CLM_LINE_PRFNL)):
        if i == len(pac_claim.CLM_LINE_PRFNL):
            continue
        if f.CLM_MTUS_IND_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_MTUS_IND_CD)
        if f.CLM_PRCNG_LCLTY_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_PRCNG_LCLTY_CD)
        if f.CLM_PHYSN_ASTNT_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_PHYSN_ASTNT_CD)
        if f.CLM_LINE_PRFNL_MTUS_CNT in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_LINE_PRFNL_MTUS_CNT)
        if f.CLM_LINE_CARR_HPSA_SCRCTY_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_LINE_CARR_HPSA_SCRCTY_CD)
        if f.CLM_PRMRY_PYR_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_PRMRY_PYR_CD)
        if f.CLM_FED_TYPE_SRVC_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_FED_TYPE_SRVC_CD)
        if f.CLM_PMT_80_100_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_PMT_80_100_CD)
        if f.CLM_PRCSG_IND_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_PRCSG_IND_CD)
        if f.CLM_PRVDR_SPCLTY_CD in pac_claim.CLM_LINE_PRFNL[i]:
            pac_claim.CLM_LINE_PRFNL[i].pop(f.CLM_PRVDR_SPCLTY_CD)

    # Update CLM_INSTNL for institutional claims only
    if pac_claim.CLM_INSTNL:
        # pac_claim[fc.CLM_INSTNL][fc.CLM_UNIQ_ID] = pac_claim[fc.CLM][fc.CLM_UNIQ_ID]
        pac_claim.CLM_INSTNL[f.GEO_BENE_SK] = pac_claim.CLM[f.GEO_BENE_SK]
        pac_claim.CLM_INSTNL[f.CLM_DT_SGNTR_SK] = pac_claim.CLM_DT_SGNTR[f.CLM_DT_SGNTR_SK]

    if pac_claim.CLM[f.CLM_TYPE_CD] in FISS_CLM_TYPE_CDS:
        pac_claim.CLM[f.CLM_SRC_ID] = 21000  # FISS
        pac_claim.CLM[f.META_SRC_SK] = 1003  # FISS
    elif pac_claim.CLM[f.CLM_TYPE_CD] in MCS_CLM_TYPE_CDS:
        pac_claim.CLM[f.CLM_SRC_ID] = 22000  # MCS
        pac_claim.CLM[f.META_SRC_SK] = 1001  # MCS
    elif pac_claim.CLM[f.CLM_TYPE_CD] in VMS_CDS:
        pac_claim.CLM[f.CLM_SRC_ID] = 23000  # VMS
        pac_claim.CLM[f.META_SRC_SK] = 1002  # VMS

    if pac_claim.CLM_DCMTN:
        if pac_claim.CLM[f.CLM_TYPE_CD] in FISS_CLM_TYPE_CDS:
            pac_claim.CLM[f.CLM_RIC_CD] = pac_claim.CLM_DCMTN[f.CLM_NRLN_RIC_CD]
        pac_claim.CLM_DCMTN = {}

    pac_clm_rlt_cond_sgntr_sk = random.randint(2, 999999999999)
    pac_claim.CLM[f.CLM_RLT_COND_SGNTR_SK] = pac_clm_rlt_cond_sgntr_sk

    pac_rlt_cond_mbr_record: dict[str, Any] = {}
    pac_rlt_cond_mbr_record[f.CLM_RLT_COND_SGNTR_SK] = pac_clm_rlt_cond_sgntr_sk
    pac_rlt_cond_mbr_record[f.CLM_RLT_COND_SGNTR_SQNC_NUM] = random.choice(TARGET_SEQUENCE_NUMBERS)
    pac_rlt_cond_mbr_record[f.CLM_RLT_COND_CD] = random.choice(TARGET_RLT_COND_CODES)

    add_meta_timestamps(pac_rlt_cond_mbr_record, pac_claim.CLM, max_date)
    pac_claim.CLM_RLT_COND_SGNTR_MBR = pac_rlt_cond_mbr_record

    return pac_claim


def gen_provider_history(amount: int):
    names = random.sample(AVAILABLE_GIVEN_NAMES, amount)
    provider_history: list[dict[str, Any]] = []

    for name in names:
        prvdr_sk = gen_basic_id(field="PRVDR_SK", length=9)
        provider_history_row = {
            f.PRVDR_SK: prvdr_sk,
            f.PRVDR_HSTRY_EFCTV_DT: str(date.today()),
            f.PRVDR_HSTRY_OBSLT_DT: "9999-12-31",
            f.PRVDR_1ST_NAME: name,
            f.PRVDR_MDL_NAME: random.choice(AVAILABLE_GIVEN_NAMES),
            f.PRVDR_LAST_NAME: random.choice(AVAILABLE_FAMILY_NAMES),
            f.PRVDR_NAME: random.choice(AVAILABLE_PROVIDER_NAMES),
            f.PRVDR_LGL_NAME: random.choice(AVAILABLE_PROVIDER_LEGAL_NAMES),
            f.PRVDR_NPI_NUM: prvdr_sk,
            f.PRVDR_EMPLR_ID_NUM: gen_basic_id(field=f.PRVDR_EMPLR_ID_NUM, length=9),
            f.PRVDR_OSCAR_NUM: gen_basic_id(field=f.PRVDR_OSCAR_NUM, length=6),
            f.PRVDR_TXNMY_CMPST_CD: random.choice(AVAILABLE_PROVIDER_TX_CODES),
            f.PRVDR_TYPE_CD: random.choice(AVAILABLE_PROVIDER_TYPE_CODES),
        }
        generate_meta_sk_pair(provider_history_row)

        provider_history.append(provider_history_row)

    return provider_history


def gen_contract_plan(amount: int):
    pbp_nums = random.sample(AVAIL_PBP_NUMS, amount)
    contract_pbp_num: list[dict[str, Any]] = []
    contract_pbp_contact: list[dict[str, Any]] = []
    today = date.today()
    last_day = today.replace(month=12, day=31)

    for pbp_num in pbp_nums:
        effective_date = faker.date_between_dates(date.fromisoformat("2020-01-01"), now)
        end_date = faker.date_between_dates(effective_date, now + relativedelta(years=3))
        obsolete_date = random.choice(
            [faker.date_between_dates(effective_date, now), date.fromisoformat("9999-12-31")]
        )
        contract_pbp_num.append(
            {
                f.CNTRCT_PBP_SK: gen_basic_id(field=f.CNTRCT_PBP_SK, length=12),
                f.CNTRCT_NUM: random.choice(AVAIL_CONTRACT_NUMS),
                f.CNTRCT_PBP_NUM: pbp_num,
                f.CNTRCT_PBP_NAME: random.choice(AVAIL_CONTRACT_NAMES),
                f.CNTRCT_PBP_TYPE_CD: random.choice(AVAIL_PBP_TYPE_CODES),
                f.CNTRCT_DRUG_PLAN_IND_CD: random.choice(["Y", "N"]),
                f.CNTRCT_PBP_SK_EFCTV_DT: effective_date.isoformat(),
                f.CNTRCT_PBP_END_DT: end_date.isoformat(),
                f.CNTRCT_PBP_SK_OBSLT_DT: obsolete_date.isoformat(),
            }
        )

        contract_pbp_contact.append(
            {
                f.CNTRCT_PBP_SK: gen_basic_id(field=f.CNTRCT_PBP_SK, length=12),
                f.CNTRCT_PLAN_CNTCT_OBSLT_DT: "9999-12-31",
                f.CNTRCT_PLAN_CNTCT_TYPE_CD: random.choice(["~", "30", "62"]),
                f.CNTRCT_PLAN_FREE_EXTNSN_NUM: "".join(random.choices(string.digits, k=7)),
                f.CNTRCT_PLAN_CNTCT_FREE_NUM: "".join(random.choices(string.digits, k=10)),
                f.CNTRCT_PLAN_CNTCT_EXTNSN_NUM: "".join(random.choices(string.digits, k=7)),
                f.CNTRCT_PLAN_CNTCT_TEL_NUM: "".join(random.choices(string.digits, k=10)),
                f.CNTRCT_PBP_END_DT: last_day.isoformat(),
                f.CNTRCT_PBP_BGN_DT: today.isoformat(),
                f.CNTRCT_PLAN_CNTCT_ST_1_ADR: random.choice(
                    [
                        "319 E. Street",
                        "North Street",
                        "West Street",
                    ]
                ),
                f.CNTRCT_PLAN_CNTCT_ST_2_ADR: random.choice(["Avenue M", ""]),
                f.CNTRCT_PLAN_CNTCT_CITY_NAME: random.choice(
                    [
                        "Los Angeles",
                        "San Jose",
                        "San Francisco",
                    ]
                ),
                f.CNTRCT_PLAN_CNTCT_STATE_CD: "CA",
                f.CNTRCT_PLAN_CNTCT_ZIP_CD: "".join(random.choices(string.digits, k=9)),
            }
        )

    return contract_pbp_num, contract_pbp_contact


def add_meta_timestamps(obj: dict[str, Any], clm: dict[str, Any], max_date: str):
    if date.fromisoformat(clm[f.CLM_IDR_LD_DT]) < date(2021, 4, 19):
        has_insrt_ts = random.random() > 0.5
    else:
        has_insrt_ts = True
    obj[f.IDR_INSRT_TS] = (
        faker.date_time_between_dates(
            datetime.fromisoformat(clm[f.CLM_IDR_LD_DT]),
            datetime.fromisoformat(max_date),
        )
        if has_insrt_ts
        else None
    )
    obj[f.IDR_UPDT_TS] = (
        faker.date_time_between_dates(obj[f.IDR_INSRT_TS], datetime.fromisoformat(max_date))
        if has_insrt_ts and random.random() > 0.8
        else None
    )


def generate_meta_sk_pair(obj: dict[str, Any]):
    def encode(d: datetime | date):
        d = d.date() if isinstance(d, datetime) else d
        yyyymmdd = d.year * 10000 + d.month * 100 + d.day
        base = (yyyymmdd - 19000000) * 1000
        seq = random.randint(1, 999)
        return base + seq

    max_dt = datetime.fromisoformat(str(NOW))
    min_dt = datetime(2010, 1, 1)

    if random.random() < 0.05:
        update_dt = faker.date_time_between_dates(min_dt, max_dt)
        obj[f.META_SK] = 501
        obj[f.META_LST_UPDT_SK] = encode(update_dt)
        return

    insert_dt = faker.date_time_between_dates(min_dt, max_dt)
    obj[f.META_SK] = encode(insert_dt)

    roll = random.random()
    if roll > 0.8:
        update_dt = faker.date_time_between_dates(insert_dt, max_dt)
        obj[f.META_LST_UPDT_SK] = encode(update_dt)
    elif roll > 0.6:
        obj[f.META_LST_UPDT_SK] = obj[f.META_SK]
    else:
        obj[f.META_LST_UPDT_SK] = 0


@click.command
@click.argument("paths", nargs=-1, type=click.Path(exists=True))
@from_pydantic("opts", OptionsModel)
def main(opts: OptionsModel, paths: tuple[Path, ...]):
    """Generate synthetic claims data. Provided file PATHS will be updated with new fields."""
    if opts.sushi:
        print("Running sushi build")
        _, stderr = run_command(["sushi", "build"], cwd="./sushi")
        if stderr:
            print("SUSHI errors:")
            print(stderr)

    files: dict[str, list[RowAdapter]] = {
        BENE_HSTRY: [],
        CLM: [],
        CLM_LINE: [],
        CLM_LINE_DCMTN: [],
        CLM_VAL: [],
        CLM_DT_SGNTR: [],
        CLM_PROD: [],
        CLM_INSTNL: [],
        CLM_LINE_INSTNL: [],
        CLM_DCMTN: [],
        CLM_LCTN_HSTRY: [],
        CLM_FISS: [],
        CLM_PRFNL: [],
        CLM_LINE_PRFNL: [],
        CLM_LINE_RX: [],
        CLM_RLT_COND_SGNTR_MBR: [],
        PRVDR_HSTRY: [],
        CNTRCT_PBP_NUM: [],
        CNTRCT_PBP_CNTCT: [],
    }
    load_file_dict(files=files, paths=list(paths))

    bene_sks = {-1}
    if any(BENE_HSTRY in str(file) for file in paths):
        bene_sks: set[int] = {row[f.BENE_SK] for row in files[BENE_HSTRY]}

    # Naively check if the tables that make up CLAIM_ITEM, CLAIM_DATE_SIGNATURE, and BENEFICIARY
    # have been provided if CLAIM was provided. A thorough check would take too long due to the
    # volume of data
    clm_required_tables = [
        CLM_PROD,
        CLM_LINE,
        CLM_VAL,
        CLM_RLT_COND_SGNTR_MBR,
        BENE_HSTRY,
        BENE_XREF,
    ]
    if any(CLM in str(file) for file in paths) and any(
        file not in clm_required_tables for file in paths
    ):
        print(f"{', '.join(clm_required_tables)} must be provided if {CLM} is provided")
        return

    clm = adapters_to_dicts(files[CLM])
    clm_line = adapters_to_dicts(files[CLM_LINE])
    clm_line_dcmtn = adapters_to_dicts(files[CLM_LINE_DCMTN])
    clm_val = adapters_to_dicts(files[CLM_VAL])
    clm_instnl = adapters_to_dicts(files[CLM_INSTNL])
    clm_line_instnl = adapters_to_dicts(files[CLM_LINE_INSTNL])
    clm_dt_sgntr = adapters_to_dicts(files[CLM_DT_SGNTR])
    clm_prod = adapters_to_dicts(files[CLM_PROD])
    clm_dcmtn = adapters_to_dicts(files[CLM_DCMTN])
    clm_fiss = adapters_to_dicts(files[CLM_FISS])
    clm_lctn_hstry = adapters_to_dicts(files[CLM_LCTN_HSTRY])
    clm_prfnl = adapters_to_dicts(files[CLM_PRFNL])
    clm_line_prfnl = adapters_to_dicts(files[CLM_LINE_PRFNL])
    clm_line_rx = adapters_to_dicts(files[CLM_LINE_RX])
    clm_rlt_cond_sgntr_mbr = adapters_to_dicts(files[CLM_RLT_COND_SGNTR_MBR])
    cntrct_pbp_num = adapters_to_dicts(files[CNTRCT_PBP_NUM])
    cntrct_pbp_cntct = adapters_to_dicts(files[CNTRCT_PBP_CNTCT])
    if not cntrct_pbp_num or cntrct_pbp_cntct:
        cntrct_pbp_num, cntrct_pbp_cntct = gen_contract_plan(amount=10)
    prvdr_hstry = adapters_to_dicts(files[PRVDR_HSTRY])
    if not prvdr_hstry:
        prvdr_hstry = gen_provider_history(amount=14)
    clm_ansi_sgntr = gen_synthetic_clm_ansi_sgntr()
    min_claims: int = opts.min_claims
    max_claims: int = opts.max_claims
    if min_claims > max_claims:
        print(
            f"error: min claims value of {min_claims} is greater than "
            f"max claims value of {max_claims}"
        )
        sys.exit(1)
    max_date = str(date.today())

    bene_sks_with_claims: dict[int, list[RowAdapter]] = {}
    for claim in files[CLM]:
        clm_bene_sk: int = claim[f.BENE_SK]
        bene_sks_with_claims[clm_bene_sk] = [*bene_sks_with_claims.get(clm_bene_sk, []), claim]

    print("Generating synthetic claims data for provided BENE_SKs...")
    for pt_bene_sk in tqdm.tqdm(bene_sks):
        claims_from_file = bene_sks_with_claims.get(pt_bene_sk, [])
        if not claims_from_file or opts.force_gen_claims:
            for _ in range(random.randint(min_claims, max_claims - len(claims_from_file))):
                clm_from_dt_min = "2018-01-01"
                claim = gen_claim(
                    bene_sk=str(pt_bene_sk), min_date=clm_from_dt_min, max_date=max_date
                )
                clm.append(claim.CLM)
                clm_line.extend(claim.CLM_LINE)
                # clm_line_dcmtn.extend(claim.CLM_LINE_DCMTN)
                clm_val.extend(claim.CLM_VAL)
                clm_dt_sgntr.append(claim.CLM_DT_SGNTR)
                clm_prod.extend(claim.CLM_PROD)
                clm_rlt_cond_sgntr_mbr.append(claim.CLM_RLT_COND_SGNTR_MBR)

                if claim.CLM_INSTNL:
                    clm_instnl.append(claim.CLM_INSTNL)
                clm_line_instnl.extend(claim.CLM_LINE_INSTNL)
                clm_dcmtn.append(claim.CLM_DCMTN)
                if claim.CLM[f.CLM_TYPE_CD] in PHARMACY_CLM_TYPE_CDS:
                    clm_line_rx.extend(claim.CLM_LINE_RX)
                elif claim.CLM[f.CLM_TYPE_CD] in PROFESSIONAL_CLAIM_TYPES:
                    # Only add professional data for non-Part D claims
                    clm_prfnl.append(claim.CLM_PRFNL)
                    clm_line_prfnl.extend(claim.CLM_LINE_PRFNL)

                # obviously we don't have pac claims for PD claims
                if random.choice([0, 1]) and claim.CLM[f.CLM_TYPE_CD] not in (
                    1,
                    2,
                    3,
                    4,
                ):
                    pac_claim = gen_pac_version_of_claim(claim, max_date)
                    clm.append(pac_claim.CLM)
                    clm_line.extend(pac_claim.CLM_LINE)
                    clm_val.extend(pac_claim.CLM_VAL)
                    clm_dt_sgntr.append(pac_claim.CLM_DT_SGNTR)
                    clm_prod.extend(pac_claim.CLM_PROD)
                    clm_rlt_cond_sgntr_mbr.append(pac_claim.CLM_RLT_COND_SGNTR_MBR)

                    if pac_claim.CLM_INSTNL:
                        clm_instnl.append(pac_claim.CLM_INSTNL)
                    if len(pac_claim.CLM_LINE_INSTNL) > 0:
                        clm_line_instnl.extend(pac_claim.CLM_LINE_INSTNL)
                    clm_fiss.append(pac_claim.CLM_FISS)
                    clm_lctn_hstry.append(pac_claim.CLM_LCTN_HSTRY)
                    clm_line_dcmtn.extend(pac_claim.CLM_LINE_DCMTN)

    print("Done generating synthetic claims data for provided BENE_SKs")

    save_output_files(
        clm,
        clm_line,
        clm_line_dcmtn,
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
        clm_rlt_cond_sgntr_mbr,
        prvdr_hstry,
        cntrct_pbp_num,
        cntrct_pbp_cntct,
        clm_ansi_sgntr,
    )


if __name__ == "__main__":
    main()
