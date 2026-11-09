# Note: src/idr_model/claim_type_codes.py links to this

# begin rx claim_types (Part D)
PHARMACY_CDS = [1, 2, 3, 4]

# begin INSTNL CLAIM TYPES (Part A)

## INSTNL SS
FISS_SS_CDS = [
    1000,  # begin PHASE I FISS CLAIMS
    1011,
    1012,
    1013,
    1014,
    1018,
    1019,
    1021,
    1022,
    1023,
    1028,
    1029,
    1032,
    1033,
    1034,
    1039,
    1041,
    1042,
    1043,
    1049,
    1065,
    1066,
    1069,
    1071,
    1072,
    1073,
    1074,
    1075,
    1076,
    1077,
    1078,
    1079,
    1081,
    1082,
    1083,
    1084,
    1085,
    1086,
    1087,
    1088,
    1089,
    1091,
    1092,
    1093,
    1094,
    1095,
    1096,
    1097,
    1098,
    1099,
    2000,
    2011,  # begin PHASE II and PHASE   III FISS CLAIMS
    2012,
    2013,
    2014,
    2018,
    2019,
    2021,
    2022,
    2023,
    2028,
    2029,
    2032,
    2033,
    2034,
    2039,
    2041,
    2042,
    2043,
    2049,
    2065,
    2066,
    2069,
    2071,
    2072,
    2073,
    2074,
    2075,
    2076,
    2077,
    2078,
    2079,
    2081,
    2082,
    2083,
    2084,
    2085,
    2086,
    2087,
    2089,
    2091,
    2092,
    2093,
    2094,
    2095,
    2096,
    2097,
    2098,
    2099,
]

# INSTNL NCH
INSTITUTIONAL_HHA_NCH_CD = [10]  # Home Health

INSTITUTIONAL_SNF_NONSWING_NCH_CD = [20]
INSTITUTIONAL_SNF_SWING_NCH_CD = [30]
INSTITUTIONAL_SNF_NCH_CDS = [*INSTITUTIONAL_SNF_NONSWING_NCH_CD, *INSTITUTIONAL_SNF_SWING_NCH_CD]

INSTITUTIONAL_OUTPATIENT_NCH_CD = [40]
INSTITUTIONAL_HOSPICE_NCH_CD = [50]
INSTITUTIONAL_INPATIENT_NCH_CD = [60]
INSTITUTIONAL_FFS_NCH_CDS = [
    *INSTITUTIONAL_HHA_NCH_CD,
    *INSTITUTIONAL_SNF_NCH_CDS,
    *INSTITUTIONAL_OUTPATIENT_NCH_CD,
    *INSTITUTIONAL_HOSPICE_NCH_CD,
    *INSTITUTIONAL_INPATIENT_NCH_CD,
]

# MA NCH Encounter Data
INSTITUTIONAL_MA_INPATIENT_NCH_CD = [61]
INSTITUTIONAL_MA_MEDICAL_EDUCATION_NCH_CD = [62]
INSTITUTIONAL_MA_NOPAY_NCH_CD = [63]
INSTITUTIONAL_MA_FFS_NCH_CD = [64]
INSTITUTIONAL_MA_NCH_CDS = [
    *INSTITUTIONAL_MA_INPATIENT_NCH_CD,
    *INSTITUTIONAL_MA_MEDICAL_EDUCATION_NCH_CD,
    *INSTITUTIONAL_MA_NOPAY_NCH_CD,
    *INSTITUTIONAL_MA_FFS_NCH_CD,
]

# HOSPICE PHASE 1, PHASE 2 & PHASE 3
INSTITUTIONAL_HOSPICE_ELECTION_CDS = [1900, 2900]

## ALL INSTNL
INSTITUTIONAL_CDS = [
    *INSTITUTIONAL_FFS_NCH_CDS,  # 10, 20, 30, 40, 50, 60
    *INSTITUTIONAL_MA_NCH_CDS,  # 61 - 64
    *FISS_SS_CDS,  # 1000-1099 & 2000-2099
    *INSTITUTIONAL_HOSPICE_ELECTION_CDS,  # 1900, 2900
]
# end INSTNL CLAIM TYPES

# begin PROFESSIONAL CLAIM TYPES (Part B)
ADJUDICATED_PROFESSIONAL_CARRIER_NCH_CDS = [71, 72]
ADJUDICATED_PROFESSIONAL_DME_NCH_CDS = [81, 82]

ADJUDICATED_PROFESSIONAL_NCH_CDS = [
    *ADJUDICATED_PROFESSIONAL_CARRIER_NCH_CDS,
    *ADJUDICATED_PROFESSIONAL_DME_NCH_CDS,
]

MCS_SS_CDS = [1700, 2700]  # Multi Carrier SS (Phases I, II)
VMS_SS_CDS = [1800, 2800]  # Viable Medicare Information Processing System SS (Phases I, II)
PROFESSIONAL_SS_CDS = [*MCS_SS_CDS, *VMS_SS_CDS]

PROFESSIONAL_CDS = [
    *ADJUDICATED_PROFESSIONAL_NCH_CDS,  # 71, 72, 81, 82
    *PROFESSIONAL_SS_CDS,  # 1700, 2700 & 1800, 2800
]
# end PROFESSIONAL CLAIM TYPES (Part B)


# begin Medicare Advantage Plan (ENC) Encounter Data (4000-4900) (Part C) (unused so far)
MA_ENC_CDS = [
    4000,  # 000X Medicare Part C Encounter (ENC) Other Type of Bill Groups
    4011,  # 011X Medicare Part C Encounter (ENC) Hospital Inpatient (Including Medicare Part A)
    4012,  # 012X Medicare Part C Encounter (ENC) Hospital Inpatient (Medicare Part B only)
    4013,  # 013X Medicare Part C Encounter (ENC) Hospital Outpatient
    4014,  # 014X Medicare Part C Encounter (ENC) Hospital Laboratory Services Provided to Non-patients
    4018,  # 018X Medicare Part C Encounter (ENC) Hospital Swing Beds
    4021,  # 021X Medicare Part C Encounter (ENC) Skilled Nursing Facility (SNF) Inpatient (Including Medicare Part A)
    4022,  # 022X Medicare Part C Encounter (ENC) Skilled Nursing Facility (SNF) Inpatient (Medicare Part B only)
    4023,  # 023X Medicare Part C Encounter (ENC) Skilled Nursing Facility (SNF) Outpatient
    4024,  # 024X Medicare Part C Encounter (ENC) Skilled Nursing Facility (SNF) Other + Laboratory Services Provided to Non-patients
    4028,  # 028X Medicare Part C Encounter (ENC) Skilled Nursing Facility (SNF) Swing Beds
    4032,  # 032X Medicare Part C Encounter (ENC) Home Health Plus Inpatient (Medicare Part B only)
    4033,  # 033X Medicare Part C Encounter (ENC) Home Health Plus Outpatient
    4034,  # 034X Medicare Part C Encounter (ENC) Home Health Plus Laboratory Services Provided to Non-patients
    4041,  # 041X Medicare Part C Encounter (ENC) Religious Nonmedical Health Care Institutions (RNHCI) - Hospital Inpatient
    4043,  # 043X Medicare Part C Encounter (ENC) Religious Nonmedical Health Care Institutions  (RNHCI) - Outpatient
    4065,  # 065X Medicare Part C Encounter (ENC) Intermediate Care Intermediate Care Level I
    4066,  # 066X Medicare Part C Encounter (ENC) Intermediate Care Intermediate Care Level II
    4071,  # 071X Medicare Part C Encounter (ENC) Clinic Rural Health (RHC)
    4072,  # 072X Medicare Part C Encounter (ENC) Clinic End-Stage Renal Dialysis (ESRD)- Hospital Based or Independent
    4073,  # 073X Medicare Part C Encounter (ENC) Clinic Freestanding
    4074,  # 074X Medicare Part C Encounter (ENC) Clinic Outpatient Rehabilitation Facility (ORF)
    4075,  # 075X Medicare Part C Encounter (ENC) Clinic Comprehensive Outpatient Rehabilitation Facility ( CORF)
    4076,  # 076X Medicare Part C Encounter (ENC) Clinic Community Mental Health Centers (CMHC)
    4077,  # 077X Medicare Part C Encounter (ENC) Clinic Federal Qualified Health Center (FQHC)
    4079,  # 079X Medicare Part C Encounter (ENC) Clinic - Other
    4081,  # 081X Medicare Part C Encounter (ENC) Special Facility Hospice Non-Hospital-based
    4082,  # 082X Medicare Part C Encounter (ENC) Special Facility Hospice Hospital-based
    4083,  # 083X Medicare Part C Encounter (ENC) Special Facility Ambulatory Surgery Center (ASC)
    4084,  # 084X Medicare Part C Encounter (ENC) Special Facility Freestanding Birthing Center
    4085,  # 085X Medicare Part C Encounter (ENC) Special Facility Critical Access Hospital (CAH)
    4086,  # 086X Medicare Part C Encounter (ENC) Special Facility Residential Facility
    4087,  # 087X Medicare Part C Encounter (ENC) Freestanding Non-Residential Opioid Treatment Program (Effective 1/1/21)
    4089,  # 089X Medicare Part C Encounter (ENC) Special Facility - Other
    4700,  # PROF Medicare Part C Encounter (ENC) Professional Part B
    4800,  # DME Medicare Part C Encounter (ENC) Durable Medical Equipment (DME)
]
# end Medicare Advantage Plan (ENC) (Part C)
