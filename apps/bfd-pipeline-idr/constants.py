from dateutil.relativedelta import relativedelta

from load_partition import LoadPartition, LoadPartitionGroup, PartitionType
from settings import PARTITION_TYPE

DEFAULT_MAX_DATE = "9999-12-31"
DEFAULT_MIN_DATE = "0001-01-01"
ALTERNATE_DEFAULT_DATE = "1000-01-01"
BENEFICIARY_TABLE = "idr.beneficiary"
CLAIM_RX_TABLE = "idr.claim_rx"
CLAIM_PROFESSIONAL_NCH_TABLE = "idr.claim_professional_nch"
CLAIM_PROFESSIONAL_SS_TABLE = "idr.claim_professional_ss"
CLAIM_INSTITUTIONAL_NCH_TABLE = "idr.claim_institutional_nch"
CLAIM_INSTITUTIONAL_SS_TABLE = "idr.claim_institutional_ss"

IDR_PREFIX = "cms_vdm_view_mdcr_prd"
IDR_BENE_HISTORY_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_hstry"
IDR_BENE_MBI_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mbi_id"
IDR_BENE_XREF_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_xref"
IDR_BENE_ENTITLEMENT_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mdcr_entlmt"
IDR_BENE_ENTITLEMENT_REASON_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mdcr_entlmt_rsn"
IDR_BENE_STATUS_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mdcr_stus"
IDR_BENE_THIRD_PARTY_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_tp"
IDR_BENE_COMBINED_DUAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_cmbnd_dual_mdcr"
IDR_BENE_LOW_INCOME_SUBSIDY_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_lis"
IDR_BENE_MA_PART_D_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mapd_enrlmt"
IDR_BENE_MA_PART_D_RX_TABLE = f"{IDR_PREFIX}.v2_mdcr_bene_mapd_enrlmt_rx"

IDR_CLAIM_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm"
IDR_CLAIM_ANSI_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_ansi_sgntr"
IDR_CLAIM_DATE_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_dt_sgntr"
IDR_CLAIM_INSTITUTIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_instnl"
IDR_CLAIM_PROFESSIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_prfnl"
IDR_CLAIM_DOCUMENTATION_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_dcmtn"
IDR_CLAIM_LINE_DOCUMENTATION_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_dcmtn"
IDR_CLAIM_VAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_val"
IDR_CLAIM_LINE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line"
IDR_CLAIM_LINE_INSTITUTIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_instnl"
IDR_CLAIM_LINE_PROFESSIONAL_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_prfnl"
IDR_CLAIM_PROD_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_prod"
IDR_CLAIM_FISS_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_fiss"
IDR_CLAIM_LINE_RX_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_rx"
IDR_CLAIM_LINE_FISS_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_fiss"
IDR_CLAIM_LINE_MCS_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_mcs"
IDR_CLAIM_LINE_FISS_BENEFIT_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_line_fiss_bnft_svg"
IDR_CLAIM_LOCATION_HISTORY_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_lctn_hstry"
IDR_CLAIM_RELATED_CONDITION_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_rlt_cond_sgntr_mbr"
IDR_CLAIM_OCCURRENCE_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_mdcr_clm_ocrnc_sgntr_mbr"
IDR_CLAIM_RELATED_OCCURRENCE_SIGNATURE_TABLE = f"{IDR_PREFIX}.v2_clm_rlt_ocrnc_sgntr_mbr"
IDR_PROVIDER_HISTORY_TABLE = f"{IDR_PREFIX}.v2_mdcr_prvdr_hstry"
IDR_CONTRACT_PBP_NUM_TABLE = f"{IDR_PREFIX}.v2_mdcr_cntrct_pbp_num"
IDR_CONTRACT_PBP_CONTACT_TABLE = f"{IDR_PREFIX}.v2_mdcr_cntrct_pbp_cntct"
IDR_CONTRACT_PBP_SEGMENT_TABLE = f"{IDR_PREFIX}.v2_mdcr_cntrct_pbp_sgmt"

PAC_PHASE_1_MIN = 1000
PAC_PHASE_1_MAX = 1999

DEATH_DATE_CUTOFF_YEARS = 4

match PARTITION_TYPE:
    case "year" | "years":
        partition_range = relativedelta(years=1)
    case "month" | "months":
        partition_range = relativedelta(months=1)
    case "day" | "days":
        partition_range = relativedelta(days=1)
    case _:
        raise ValueError("invalid partition type " + PARTITION_TYPE)

PART_D_CLAIM_TYPE_CODES = [1, 2, 3, 4]

PART_D_PARTITIONS = [
    LoadPartitionGroup("part_d_original", [1], PartitionType.PART_D, partition_range),
    LoadPartitionGroup("part_d_adjustment", [2, 3, 4], PartitionType.PART_D, partition_range),
]

INSTITUTIONAL_NCH_PARTITIONS = [
    # Outpatient
    LoadPartitionGroup("outpatient", [40], PartitionType.INSTITUTIONAL, partition_range),
    # HHA, SNF, Hospice, Inpatient, MA
    LoadPartitionGroup(
        "institutional",
        [10, 20, 30, 50, 60, 61, 62, 63, 64],
        PartitionType.INSTITUTIONAL,
        partition_range,
    ),
]

INSTITUTIONAL_SS_PARTITIONS = [
    LoadPartitionGroup(
        "institututional_pac",
        [
            1000,
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
            1900,
            2000,
            2011,
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
            2088,
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
            2900,
        ],
        PartitionType.INSTITUTIONAL | PartitionType.PAC,
        partition_range,
    )
]

PROFESSIONAL_NCH_PARTITIONS = [
    LoadPartitionGroup(
        "professional",
        [71, 72, 81, 82],
        PartitionType.PROFESSIONAL,
        partition_range,
    ),
]

PROFESSIONAL_SS_PARTITIONS = [
    LoadPartitionGroup(
        "professional_pac",
        [1700, 1800, 2700, 2800],
        PartitionType.PROFESSIONAL | PartitionType.PAC,
        partition_range,
    )
]


ALL_CLAIM_PARTITIONS = [
    *PART_D_PARTITIONS,
    *INSTITUTIONAL_NCH_PARTITIONS,
    *INSTITUTIONAL_SS_PARTITIONS,
    *PROFESSIONAL_NCH_PARTITIONS,
    *PROFESSIONAL_SS_PARTITIONS,
]

ALL_CLAIM_TYPE_CODES = [code for c in ALL_CLAIM_PARTITIONS for code in c.claim_type_codes]

COMBINED_CLAIM_PARTITION = LoadPartitionGroup(
    "all_claims",
    [code for partition in ALL_CLAIM_PARTITIONS for code in partition.claim_type_codes],
    PartitionType.INSTITUTIONAL
    | PartitionType.PROFESSIONAL
    | PartitionType.PART_D
    | PartitionType.PAC,
    None,
)

DEFAULT_PARTITION = LoadPartition("default", [], PartitionType.ALL, None, None, 0)

NON_CLAIM_PARTITION = LoadPartitionGroup("default", [], PartitionType.ALL, None, 1)

# Need to declare this separately because python struggles
# with type-hinting empty arrays :(
EMPTY_PARTITION: list[LoadPartitionGroup] = []
