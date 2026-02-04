import json
import sys
from dataclasses import asdict, dataclass, field
from decimal import Decimal
from pathlib import Path

import pandas as pd

prvdr_info_file = "sample-data/PRVDR_HSTRY_POC.csv"
df = pd.read_csv(prvdr_info_file, dtype={"PRVDR_SK": str})
df.head()

cur_sample = sys.argv[1]
cur_sample_data = {}
with Path(cur_sample).open("r") as file:
    cur_sample_data = json.load(file)

header_columns = {
    "PRVDR_BLG_PRVDR_NPI_NUM": "",
    "PRVDR_RFRG_PRVDR_NPI_NUM": "referring",
    "PRVDR_OTHR_PRVDR_NPI_NUM": "otheroperating",
    "PRVDR_ATNDG_PRVDR_NPI_NUM": "attending",
    "PRVDR_OPRTG_PRVDR_NPI_NUM": "operating",
    "PRVDR_RNDRNG_PRVDR_NPI_NUM": "rendering",
    "PRVDR_PRSCRBNG_PRVDR_NPI_NUM": "prescribing",
    "PRVDR_SRVC_PRVDR_NPI_NUM": "",
}
line_columns = {
    "PRVDR_RNDRNG_PRVDR_NPI_NUM": "rendering",
    "CLM_LINE_ORDRG_PRVDR_NPI_NUM": "",
    "CLM_FAC_PRVDR_NPI_NUM": "",
}

line_supporting_info_columns = [
    "CLM_LINE_PMD_UNIQ_TRKNG_NUM",
    "CLM_LINE_PA_UNIQ_TRKNG_NUM",
]
npis_used = []
cur_sample_data["providerList"] = []
cur_careteam_sequence = 1
# we only use PRVDR_SRVC_PRVDR_NPI_NUM for part D events.
if cur_sample_data.get("CLM_TYPE_CD") not in (1, 2, 3, 4):
    header_columns.pop("PRVDR_SRVC_PRVDR_NPI_NUM")

populate_fields_except_na = [
    "PRVDR_LGL_NAME",
    "PRVDR_OSCAR_NUM",
    "PRVDR_LAST_NAME",
    "PRVDR_1ST_NAME",
    "PRVDR_MDL_NAME",
    "PRVDR_TYPE_CD",
]
provider_list = []

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


# There may be an opportunity to consolidate even the duplicate NPIs into a
# single careTeam reference, but we should wait to get feedback on this
# The reason being: it's possible to lose context on rendering vs ordering
for column in header_columns:
    if column not in cur_sample_data:
        continue
    provider_object = {}
    provider_object["PRVDR_SK"] = cur_sample_data.get(column)
    if provider_object.get("PRVDR_SK") in npis_used:
        provider_object["isDuplicate"] = True
    else:
        npis_used.append(provider_object.get("PRVDR_SK"))

    prvdr_hstry_for_npi = json.loads(
        df[df["PRVDR_SK"] == str(provider_object.get("PRVDR_SK"))].iloc[0].to_json()
    )
    provider_object["NPI_TYPE"] = (
        "2" if prvdr_hstry_for_npi.get("PRVDR_LGL_NAME") else "1"
    )

    provider_object.update(
        {
            fld: prvdr_hstry_for_npi[fld]
            for fld in populate_fields_except_na
            if prvdr_hstry_for_npi.get(fld)
        }
    )

    if prvdr_hstry_for_npi.get("PRVDR_TXNMY_CMPST_CD"):
        taxonomy_val = prvdr_hstry_for_npi.get("PRVDR_TXNMY_CMPST_CD")
        taxonomy_codes = [
            taxonomy_val[i : i + 10] for i in range(len(taxonomy_val), 10)
        ]
        provider_object["taxonomyCodes"] = taxonomy_codes

    # assign care team type + sequence for header-level info
    if len(header_columns.get(column)) > 0:
        provider_object["careTeamType"] = header_columns.get(column)
        provider_object["careTeamSequenceNumber"] = cur_careteam_sequence
        cur_careteam_sequence += 1

        line_items = cur_sample_data.get("lineItemComponents", [])
        for item in line_items:
            # If there's a column that matches the header level NPI at the line level,
            # then we want to populate the sequence.
            # Because there can be multiple rendering providers on line items,
            # we need to ensure those NPIs match.
            if column in item and item.get(column) == cur_sample_data.get(column):
                if item.get("careTeamSequences"):
                    item["careTeamSequence"].append(
                        provider_object.get("careTeamSequenceNumber")
                    )
                else:
                    item["careTeamSequence"] = [
                        provider_object.get("careTeamSequenceNumber")
                    ]

    # We may want to remove this in the future, depending on requirements
    # regarding address info.
    if (
        column == "PRVDR_BLG_PRVDR_NPI_NUM"
        and "CLM_BLG_PRVDR_ZIP5_CD" in cur_sample_data
    ):
        provider_object["prvdr_zip"] = cur_sample_data.get("CLM_BLG_PRVDR_ZIP5_CD")

    provider_list.append(provider_object)

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

    for line_col in line_columns:
        if line_col in item and item.get(line_col) not in npis_used:
            npi = item.get(line_col)
            provider_object = {}
            provider_object["PRVDR_SK"] = item.get(line_col)
            npis_used.append(provider_object.get("PRVDR_SK"))

            prvdr_hstry_for_npi = json.loads(
                df[df["PRVDR_SK"] == str(provider_object.get("PRVDR_SK"))]
                .iloc[0]
                .to_json()
            )
            provider_object["NPI_TYPE"] = (
                "2" if prvdr_hstry_for_npi.get("PRVDR_LGL_NAME") else "1"
            )
            provider_object.update(
                {
                    fld: prvdr_hstry_for_npi[fld]
                    for fld in populate_fields_except_na
                    if prvdr_hstry_for_npi.get(fld)
                }
            )

            if prvdr_hstry_for_npi.get("PRVDR_TXNMY_CMPST_CD"):
                taxonomy_val = prvdr_hstry_for_npi.get("PRVDR_TXNMY_CMPST_CD")
                taxonomy_codes = [
                    taxonomy_val[i : i + 10] for i in range(len(taxonomy_val), 10)
                ]
                provider_object["taxonomyCodes"] = taxonomy_codes

            if len(line_columns.get(line_col)) > 0:
                provider_object["careTeamType"] = line_columns.get(line_col)
                provider_object["careTeamSequenceNumber"] = cur_careteam_sequence
                cur_careteam_sequence += 1

            item["careTeamSequence"] = [provider_object.get("careTeamSequenceNumber")]
            provider_list.append(provider_object)

        elif (
            line_col in item
            and "careTeamSequence" not in item
            and item.get(line_col) in npis_used
        ):
            npi = item.get(line_col)

            matching_providers = [
                x.get("careTeamSequenceNumber")
                for x in provider_list
                if "careTeamSequenceNumber" in x
                and x.get("PRVDR_SK") == npi
                and "careTeamType" in x
                and x.get("careTeamType") == line_columns.get(line_col)
            ]
            careTeamSequence = matching_providers[0]
            item["careTeamSequence"] = [careTeamSequence]

    # for part D claims, sum CLM_LINE_INGRDNT_CST_AMT, CLM_LINE_SRVC_CST_AMT, CLM_LINE_SLS_TAX_AMT,
    # CLM_LINE_VCCN_ADMIN_FEE_AMT to set TOT_RX_CST_AMT
    tot_rx_amt = sum(
        convert_to_decimal(item.get(financial_field))
        for financial_field in rx_line_financial_fields
    )
    if tot_rx_amt > 0.0:
        item["TOT_RX_CST_AMT"] = str(tot_rx_amt)

cur_sample_data["providerList"] = provider_list


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
        CLM_PROD_TYPE_CD=x.get("CLM_PROD_TYPE_CD"),
        CLM_POA_IND=x.get("CLM_POA_IND"),
        CLM_DGNS_PRCDR_ICD_IND=x.get("CLM_DGNS_PRCDR_ICD_IND"),
        ROW_NUM=x.get("CLM_VAL_SQNC_NUM"),
    )
    for x in cur_sample_data.get("diagnoses", [])
    if x.get("CLM_PROD_TYPE_CD") in ["D", "P", "A", "R", "E"]
]

# Sort diagnoses keys
type_priority = {"P": 1, "A": 2, "R": 3, "E": 4}

# We need to preserve the list but sort it
diagnosis_codes.sort(key=lambda d: type_priority.get(d.CLM_PROD_TYPE_CD, 99))

# Assign sequential ROW_NUM
for idx, diag in enumerate(diagnosis_codes):
    diag.ROW_NUM = str(idx + 1)

    # Map type directly based on the instance's CLM_PROD_TYPE_CD
    if diag.CLM_PROD_TYPE_CD in ["P", "A", "R", "E"]:
        diag.clm_prod_type_cd_map = [diag.CLM_PROD_TYPE_CD]
    else:
        diag.clm_prod_type_cd_map = ["D"]

cur_sample_data["diagnoses"] = [asdict(d) for d in diagnosis_codes]

# add diagnosisSequence where necessary
for item in cur_sample_data.get("lineItemComponents", []):
    if "CLM_LINE_DGNS_CD" in item:
        line_dgns_cd = item.get("CLM_LINE_DGNS_CD")
        if match := next(
            (
                d
                for d in diagnosis_codes
                if line_dgns_cd == d.CLM_DGNS_CD and d.CLM_PROD_TYPE_CD == "D"
            ),
            None,
        ):
            item["diagnosisSequence"] = [int(match.ROW_NUM)]

filename = "out/temporary-sample.json"

with Path(filename).open("w") as f:
    json.dump(cur_sample_data, f, indent=4)

print("Temporary augmented file created")
