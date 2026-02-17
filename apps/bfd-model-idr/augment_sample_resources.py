import json
import sys
from dataclasses import asdict, dataclass, field
from decimal import Decimal
from pathlib import Path
from typing import Optional

import pandas as pd

prvdr_info_file = "sample-data/PRVDR_HSTRY_POC.csv"
df = pd.read_csv(prvdr_info_file, dtype={"PRVDR_SK": str})

cur_sample = sys.argv[1]
cur_sample_data = {}
with Path(cur_sample).open("r") as file:
    cur_sample_data = json.load(file)

OTHR_PRVDR_MEANING = (
    "supervisor" if cur_sample_data.get("CLM_TYPE_CD") in ("1700", "2700") else "otheroperating"
)

careteam_header_columns = {
    "PRVDR_RFRG_PRVDR_NPI_NUM": "referring",
    "PRVDR_OTHR_PRVDR_NPI_NUM": OTHR_PRVDR_MEANING,
    "PRVDR_ATNDG_PRVDR_NPI_NUM": "attending",
    "PRVDR_OPRTG_PRVDR_NPI_NUM": "operating",
    "PRVDR_RNDRNG_PRVDR_NPI_NUM": "rendering",
    "CLM_PRSBNG_PRVDR_GNRC_ID_NUM": "prescribing",
    # This is purposely commented out to note we do not pull it in on the careteam (for now).
    "PRVDR_SRVC_PRVDR_NPI_NUM": "",
}

line_supporting_info_columns = [
    "CLM_LINE_PMD_UNIQ_TRKNG_NUM",
    "CLM_LINE_PA_UNIQ_TRKNG_NUM",
]
# npis_used = []
cur_sample_data["providerList"] = []
cur_careteam_sequence = 1
# we only use CLM_SRVC_PRVDR_GNRC_ID_NUM for part D events (we filter for PRVDR_SRVC_NPI_)
if cur_sample_data.get("CLM_TYPE_CD") not in (1, 2, 3, 4):
    billing_column = "PRVDR_BLG_PRVDR_NPI_NUM"
else:
    billing_column = "CLM_SRVC_PRVDR_GNRC_ID_NUM"

rx_line_financial_fields = [
    "CLM_LINE_INGRDNT_CST_AMT",
    "CLM_LINE_SRVC_CST_AMT",
    "CLM_LINE_SLS_TAX_AMT",
    "CLM_LINE_VCCN_ADMIN_FEE_AMT",
]


def convert_to_decimal(val: str | None) -> Decimal:
    try:
        return Decimal(val)
    except (TypeError, ValueError):
        return 0.0

@dataclass
class Provider:
    PRVDR_SK: Optional[str] = None
    PRVDR_ID_QLFYR_CD: Optional[str] = None
    NPI_TYPE: Optional[str] = None
    careTeamType: Optional[str] = None
    careTeamSequenceNumber: Optional[str] = None
    PRVDR_LAST_OR_LGL_NAME: Optional[str] = None
    PRVDR_1ST_NAME: Optional[str] = None
    PRVDR_CARETEAM_NAME: Optional[str] = None
    specialtyCode: Optional[str] = None
    PRVDR_OSCAR_NUM: Optional[str] = None
    CLM_BLG_PRVDR_ZIP5_CD: Optional[str] = None
    CLM_PRVDR_GNRC_ID_NUM: Optional[str] = None
    CLM_BLG_PRVDR_TAX_NUM: Optional[str] = None

provider_list = []

# There may be an opportunity to consolidate even the duplicate NPIs into a
# single careTeam reference, but we should wait to get feedback on this
# The reason being: it's possible to lose context on rendering vs ordering
def create_billing_and_service_provider(billing_col_name):
    provider_object = Provider(PRVDR_LAST_OR_LGL_NAME="N/A")

    qualifier = cur_sample_data.get("PRVDR_SRVC_ID_QLFYR_CD")
    if qualifier:
        provider_object.PRVDR_ID_QLFYR_CD = qualifier
        provider_object.CLM_PRVDR_GNRC_ID_NUM = cur_sample_data.get("CLM_SRVC_PRVDR_GNRC_ID_NUM")
    if qualifier in ("01", None):
        # Only pull NPI data if it's an NPI
        npi_num = cur_sample_data.get(billing_col_name)
        prvdr_hstry_for_npi = json.loads(df[df["PRVDR_SK"] == str(npi_num)].iloc[0].to_json())
        provider_object.NPI_TYPE = "2" if prvdr_hstry_for_npi.get("PRVDR_LGL_NAME") else "1"
        provider_object.PRVDR_SK = npi_num
        provider_object.PRVDR_LAST_OR_LGL_NAME = (
            prvdr_hstry_for_npi["PRVDR_LGL_NAME"]
            if provider_object.NPI_TYPE == "2"
            else prvdr_hstry_for_npi["PRVDR_LAST_NAME"]
        )
        if prvdr_hstry_for_npi.get("PRVDR_1ST_NAME"):
            provider_object.PRVDR_1ST_NAME = prvdr_hstry_for_npi.get("PRVDR_1ST_NAME")
        if cur_sample_data.get("CLM_BLG_PRVDR_OSCAR_NUM"):
            provider_object.PRVDR_OSCAR_NUM = cur_sample_data.get("CLM_BLG_PRVDR_OSCAR_NUM")
        if cur_sample_data.get("CLM_BLG_PRVDR_TAX_NUM"):
            provider_object.CLM_BLG_PRVDR_TAX_NUM = cur_sample_data.get("CLM_BLG_PRVDR_TAX_NUM")
        if cur_sample_data.get("CLM_BLG_PRVDR_ZIP5_CD"):
            provider_object.CLM_BLG_PRVDR_ZIP5_CD = cur_sample_data.get("CLM_BLG_PRVDR_ZIP5_CD")

        if qualifier == "01":
            provider_object.PRVDR_SK = cur_sample_data.get("CLM_SRVC_PRVDR_GNRC_ID_NUM")
        # set other fields according to NPPES
    else:
        # only relevant to PDE.
        provider_object.CLM_PRVDR_GNRC_ID_NUM = cur_sample_data["CLM_SRVC_PRVDR_GNRC_ID_NUM"]
    return provider_object

provider_list.append(create_billing_and_service_provider(billing_column))

# For careteam elements, we don't need OSCAR number, TAX num (it's line-item), etc.
# We DO care about qualifiers + taxonomy, so we have a separate method.
def create_careteam_provider(careteam_column):
    provider_object = Provider(PRVDR_CARETEAM_NAME="N/A")
    qualifier = cur_sample_data.get("PRVDR_PRSBNG_ID_QLFYR_CD")
    if qualifier:
        provider_object.PRVDR_ID_QLFYR_CD = qualifier
        provider_object.CLM_PRVDR_GNRC_ID_NUM = cur_sample_data.get("CLM_PRSBNG_PRVDR_GNRC_ID_NUM")
    if qualifier in ("01", None):
        # Only pull NPI data if it's an NPI
        npi_num = cur_sample_data.get(careteam_column)
        prvdr_hstry_for_npi = json.loads(df[df["PRVDR_SK"] == str(npi_num)].iloc[0].to_json())
        provider_object.NPI_TYPE = "2" if prvdr_hstry_for_npi.get("PRVDR_LGL_NAME") else "1"
        provider_object.PRVDR_SK = npi_num
        # set a default name using PRVDR_HSTRY if not available.
        provider_object.PRVDR_CARETEAM_NAME = (
            prvdr_hstry_for_npi["PRVDR_LGL_NAME"]
            if provider_object.NPI_TYPE == "2"
            else prvdr_hstry_for_npi["PRVDR_LAST_NAME"]
            + ", "
            + prvdr_hstry_for_npi["PRVDR_1ST_NAME"]
        )

        provider_object.careTeamType = careteam_header_columns.get(careteam_column)
        if qualifier:
            provider_object.PRVDR_SK = cur_sample_data.get("CLM_PRSBNG_PRVDR_GNRC_ID_NUM")
        else:  # taxonomy codes only present in non-PDE
            cur_prov_type = careteam_column.split("_")[1]
            cur_specialty_code_col = "CLM_" + cur_prov_type + "_FED_PRVDR_SPCLTY_CD"
            if cur_sample_data.get(cur_specialty_code_col):
                provider_object.specialtyCode = cur_sample_data.get(cur_specialty_code_col)
            cur_prvdr_name_col = "CLM_" + cur_prov_type + "_PRVDR_NAME"
            if cur_sample_data.get(cur_prvdr_name_col):
                provider_object.PRVDR_CARETEAM_NAME = cur_sample_data.get(cur_prvdr_name_col)
    else:
        provider_object.CLM_PRVDR_GNRC_ID_NUM = cur_sample_data["CLM_PRSBNG_PRVDR_GNRC_ID_NUM"]
    return provider_object


for careteam_column in careteam_header_columns:
    if careteam_column not in cur_sample_data:
        continue
    provider_object = create_careteam_provider(careteam_column)
    provider_object.careTeamSequenceNumber = sum(
        1 for item in provider_list if provider_object.careTeamType
    )
    provider_list.append(provider_object)


# Distinct method since line items end up cleaner
def create_rendering_line_provider(npi_num):
    provider_object = Provider(
        PRVDR_SK=npi_num, careTeamType="rendering", PRVDR_CARETEAM_NAME="N/A"
    )
    prvdr_hstry_for_npi = json.loads(df[df["PRVDR_SK"] == str(npi_num)].iloc[0].to_json())
    provider_object.NPI_TYPE = "2" if prvdr_hstry_for_npi.get("PRVDR_LGL_NAME") else "1"
    provider_object.PRVDR_CARETEAM_NAME = (
        prvdr_hstry_for_npi["PRVDR_LGL_NAME"]
        if provider_object.NPI_TYPE == "2"
        else prvdr_hstry_for_npi["PRVDR_LAST_NAME"] + ", " + prvdr_hstry_for_npi["PRVDR_1ST_NAME"]
    )
    return provider_object

# now we go through the line items!
for line_item in cur_sample_data["lineItemComponents"]:
    # we only care about PRVDR_RNDRNG_PRVDR_NPI_NUM
    cur_rendering_providers = [
        x.PRVDR_SK for x in provider_list if getattr(x, "careTeamType", None) == "rendering"
    ]
    prvdr_npi_num = line_item.get("PRVDR_RNDRNG_PRVDR_NPI_NUM")
    if prvdr_npi_num and prvdr_npi_num not in cur_rendering_providers:
        # add rendering provider
        provider = create_rendering_line_provider(prvdr_npi_num)
        if line_item.get("CLM_RNDRG_FED_PRVDR_SPCLTY_CD"):
            provider.specialtyCode = line_item.get("CLM_RNDRG_FED_PRVDR_SPCLTY_CD")
        provider.careTeamSequenceNumber = sum(
            1 for item in provider_list if provider_object.careTeamType
        )
        line_item["careTeamSequence"] = provider.careTeamSequenceNumber
        provider_list.append(provider)

    if prvdr_npi_num:
        sequence = next(
            x.careTeamSequenceNumber
            for x in provider_list
            if getattr(x, "careTeamType", None) == "rendering"
            and getattr(x, "PRVDR_SK", None) == prvdr_npi_num
        )
        line_item["careTeamSequence"] = [sequence]

supporting_info_seq = 1
supporting_info_components = cur_sample_data.get("supportingInfoComponents", [])

for si_comp in supporting_info_components:
    si_comp["ROW_NUM"] = supporting_info_seq
    supporting_info_seq += 1

# There can be line item NPIs that are not present at header level, but
# need to be added to the CareTeam. This populates those.
line_items = cur_sample_data.get("lineItemComponents", [])
for item in line_items:
    for line_supporting_info_col in line_supporting_info_columns:
        if item.get(line_supporting_info_col):
            item["SEQUENCE_INFO"] = supporting_info_seq
            supporting_info_seq += 1

    # for part D claims, sum CLM_LINE_INGRDNT_CST_AMT, CLM_LINE_SRVC_CST_AMT, CLM_LINE_SLS_TAX_AMT,
    # CLM_LINE_VCCN_ADMIN_FEE_AMT to set TOT_RX_CST_AMT
    tot_rx_amt = sum(
        convert_to_decimal(item.get(financial_field))
        for financial_field in rx_line_financial_fields
    )
    if tot_rx_amt > 0.0:
        item["TOT_RX_CST_AMT"] = str(tot_rx_amt)

cur_sample_data["providerList"] = [
    {k: v for k, v in asdict(p).items() if v is not None} for p in provider_list
]

# diagnoses section
@dataclass
class Diagnosis:
    CLM_DGNS_CD: str
    CLM_PROD_TYPE_CD: str = "D"
    CLM_POA_IND: str = "~"
    CLM_DGNS_PRCDR_ICD_IND: str = "0"
    ROW_NUM: str = "1"
    clm_prod_type_cd_map: list[str] = field(default_factory=list)

diagnosis_codes = [
    Diagnosis(
        CLM_DGNS_CD=x.get("CLM_DGNS_CD"),
        CLM_POA_IND=x.get("CLM_POA_IND"),
        CLM_DGNS_PRCDR_ICD_IND=x.get("CLM_DGNS_PRCDR_ICD_IND"),
        ROW_NUM=x.get("CLM_VAL_SQNC_NUM"),
    )
    for x in cur_sample_data.get("diagnoses", [])
    if x.get("CLM_PROD_TYPE_CD") == "D"
]

# of note, 1 and E appear to always be the same, so we only care about the E code.
clm_prod_type_cds = ["P", "A", "R", "E"]
# this loop ensures that 'rogue' (eg principal not present in main list) diagnoses are not missed.
for clm_prod_type_cd in clm_prod_type_cds:
    code = [
        x.get("CLM_DGNS_CD")
        for x in cur_sample_data.get("diagnoses", [])
        if x.get("CLM_PROD_TYPE_CD") == clm_prod_type_cd
    ]
    if code and code[0] not in [x.CLM_DGNS_CD for x in diagnosis_codes]:
        diagnosis = Diagnosis(
            CLM_DGNS_CD=code,
            CLM_PROD_TYPE_CD=clm_prod_type_cd,
            CLM_DGNS_PRCDR_ICD_IND=diagnosis_codes[0].CLM_DGNS_PRCDR_ICD_IND,
            ROW_NUM=str(len(diagnosis_codes) + 1),
        )
        diagnosis_codes.append(diagnosis)

for diagnosis_code in diagnosis_codes:
    for clm_prod_type_cd in clm_prod_type_cds:
        cur_code = [
            x.get("CLM_DGNS_CD")
            for x in cur_sample_data.get("diagnoses", [])
            if x.get("CLM_PROD_TYPE_CD") == clm_prod_type_cd
        ]
        if cur_code and cur_code[0] == diagnosis_code.CLM_DGNS_CD:
            diagnosis_code.clm_prod_type_cd_map.append(clm_prod_type_cd)
    if len(diagnosis_code.clm_prod_type_cd_map) == 0:
        diagnosis_code.clm_prod_type_cd_map.append("D")

cur_sample_data["diagnoses"] = [asdict(d) for d in diagnosis_codes]

filename = "out/temporary-sample.json"

with Path(filename).open("w") as f:
    json.dump(cur_sample_data, f, indent=4)

print("Temporary augmented file created")
