import os
import random
from datetime import date, datetime
from pathlib import Path

import yaml
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


SECURITY_LABELS_YML = Path(os.path.realpath(__file__)).parent.joinpath("security_labels.yml")
SECURITY_LABELS = TypeAdapter(list[SecurityLabelModel]).validate_python(
    yaml.safe_load(SECURITY_LABELS_YML.read_text()), by_alias=True
)


SECURITY_LABELS_ICD10_PROCEDURE_SYSTEMS = ["http://www.cms.gov/Medicare/Coding/ICD10"]
SECURITY_LABELS_ICD10_DIAGNOSIS_SYSTEMS = ["http://hl7.org/fhir/sid/icd-10-cm"]
SECURITY_LABELS_HCPCS_SYSTEMS = ["https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets"]
SECURITY_LABELS_CPT_SYSTEMS = ["http://www.ama-assn.org/go/cpt"]
SECURITY_LABELS_DRG_SYSTEMS = [
    "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software"
]

NOW = date.today()

FISS_CLM_TYPE_CDS = [
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
MCS_CLM_TYPE_CDS = [1700, 2700]
VMS_CDS = [1800, 2800]
PHARMACY_CLM_TYPE_CDS = [1, 2, 3, 4]
INSTITUTIONAL_CLAIM_TYPES = [10, 20, 30, 40, 50, 60, 61, 62, 63, 64, *FISS_CLM_TYPE_CDS]
ADJUDICATED_PROFESSIONAL_CLAIM_TYPES = [71, 72, 81, 82]
PROFESSIONAL_CLAIM_TYPES = [
    *ADJUDICATED_PROFESSIONAL_CLAIM_TYPES,
    *MCS_CLM_TYPE_CDS,
    *VMS_CDS,
]
TYPE_1_NPIS = [
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
TYPE_2_NPIS = [
    1093792350,
    1548226988,
    1477643690,
    1104867175,
    1669572467,
    1508565987,
    1649041195,
]
AVAIL_OSCAR_CODES_INSTITUTIONAL = [
    "-39T14",
    "-000000",
    "-001500",
    "-001502",
    "-001503",
    "-001504",
    "-001505",
    "-001509",
    "-001510",
]
AVAILABLE_SAMHSA_ICD_10_DGNS_CODES = [
    x.normalized_code  # IDR has codes without the dot
    for x in SECURITY_LABELS
    if x.system in SECURITY_LABELS_ICD10_DIAGNOSIS_SYSTEMS
]
AVAILABLE_NON_SAMHSA_ICD_10_DGNS_CODES = [
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
AVAILABLE_SAMHSA_ICD_10_PRCDR_CODES = [
    x.normalized_code
    for x in SECURITY_LABELS
    if x.system in SECURITY_LABELS_ICD10_PROCEDURE_SYSTEMS
]
AVAILABLE_NON_SAMHSA_ICD_10_PRCDR_CODES = [
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
PROC_CODES_SAMHSA_CPT_HCPCS = [
    x.normalized_code
    for x in SECURITY_LABELS
    if x.system in SECURITY_LABELS_HCPCS_SYSTEMS or x.system in SECURITY_LABELS_CPT_SYSTEMS
]
PROC_CODES_NON_SAMHSA_CPT_HCPCS = ["99213", "99453", "J2270"]
HCPCS_MODS = ["1P", "22", "23", "28", "32", "U6", "US", "PC", "PD"]
AVAILABLE_NDC = [
    "00338004904",
    "00264180032",
    "00338011704",
    "00264180031",
    "00264780020",
]
CLM_POA_IND_CHOICES = ["N", "1", "U", "X", "W", "0", "~", "Z", "Y", ""]
AVAIL_CLM_RLT_COND_SK = ["193064687", "117814", "193065597", "117853", "193074307"]
NON_SAMHSA_DGNS_DRG_CDS = list(range(43))
SAMHSA_DGNS_DRG_CDS = [
    int(x.normalized_code) for x in SECURITY_LABELS if x.system in SECURITY_LABELS_DRG_SYSTEMS
]
TARGET_SEQUENCE_NUMBERS = [0, 1, 2, 3, 4, 5, 6, 7]
TARGET_RLT_COND_CODES = ["21", "39", "C5", "42", "64", "W2", "D9", "09", "R1"]
AVAILABLE_GIVEN_NAMES = [
    "Wallace",
    "Gromit",
    "Wednesday",
    "Indiana",
    "Tiana",
    "Tony",
    "Jack",
    "Sally",
    "Coraline",
    "Victor",
    "Chip",
    "Colin",
    "Nadia",
    "",
]
AVAILABLE_FAMILY_NAMES = [
    "Madrigal",
    "Stark",
    "Addams",
    "Jones",
    "Rogers",
    "Garcia",
    "Frankenstein",
    "",
]
AVAILABLE_PROVIDER_NAMES = [
    "CBS PHARMACY",
    "WAL-PART PHARMACY",
    "BITE AID PHARMACY",
    "HEALTHCARE CENTER",
    "",
]
AVAILABLE_PROVIDER_LEGAL_NAMES = [
    "HEALTHCARE SERVICES LLC",
    "CBS Health Corporation",
    "WALPART INC",
    "Bite Aid Corporation",
    "",
]
AVAILABLE_PROVIDER_TX_CODES = [
    "2081P0301X",
    "208VP0000X",
    "207XX0004X",
    "207VX0201X",
    "207RC0000X",
    "207QB0505X",
]
AVAILABLE_PROVIDER_TYPE_CODES = ["BP", "D", "N2", "UI", "BG", "A", "~"]


def get_icd_10_dgns_codes(enable_samhsa: bool) -> list[str]:
    # Choose SAMHSA codes 1% of the time
    return (
        random.choices(
            population=[
                AVAILABLE_SAMHSA_ICD_10_DGNS_CODES,
                AVAILABLE_NON_SAMHSA_ICD_10_DGNS_CODES,
            ],
            weights=(1, 99),
            k=1,
        )[0]
        if enable_samhsa
        else AVAILABLE_NON_SAMHSA_ICD_10_DGNS_CODES
    )


def get_icd_10_prcdr_codes(enable_samhsa: bool) -> list[str]:
    return (
        random.choices(
            population=[
                AVAILABLE_SAMHSA_ICD_10_PRCDR_CODES,
                AVAILABLE_NON_SAMHSA_ICD_10_PRCDR_CODES,
            ],
            weights=(1, 99),
            k=1,
        )[0]
        if enable_samhsa
        else AVAILABLE_NON_SAMHSA_ICD_10_PRCDR_CODES
    )


def get_hcpcs_proc_codes(enable_samhsa: bool) -> list[str]:
    return (
        random.choices(
            population=[PROC_CODES_SAMHSA_CPT_HCPCS, PROC_CODES_NON_SAMHSA_CPT_HCPCS],
            weights=(1, 99),
            k=1,
        )[0]
        if enable_samhsa
        else PROC_CODES_NON_SAMHSA_CPT_HCPCS
    )


def get_drg_dgns_codes(enable_samhsa: bool) -> list[int]:
    return (
        random.choices(
            population=[SAMHSA_DGNS_DRG_CDS, NON_SAMHSA_DGNS_DRG_CDS],
            weights=(1, 99),
            k=1,
        )[0]
        if enable_samhsa
        else NON_SAMHSA_DGNS_DRG_CDS
    )
