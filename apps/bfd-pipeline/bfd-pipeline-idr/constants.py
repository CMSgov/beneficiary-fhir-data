from dateutil.relativedelta import relativedelta

from load_partition import LoadPartition, LoadPartitionGroup, PartitionType
from settings import PARTITION_TYPE

DEFAULT_MAX_DATE = "9999-12-31"
DEFAULT_MIN_DATE = "0001-01-01"
MIN_CLAIM_LOAD_DATE = "2014-06-30"
ALTERNATE_DEFAULT_DATE = "1000-01-01"

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

INSTITUTIONAL_ADJUDICATED_PARTITIONS = [
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

INSTITUTIONAL_PAC_PARTITIONS = [
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

PROFESSIONAL_ADJUDICATED_PARTITIONS = [
    LoadPartitionGroup(
        "professional",
        [71, 72, 81, 82],
        PartitionType.PROFESSIONAL,
        partition_range,
    ),
]

PROFESSIONAL_PAC_PARTITIONS = [
    LoadPartitionGroup(
        "professional_pac",
        [1700, 1800, 2700, 2800],
        PartitionType.PROFESSIONAL | PartitionType.PAC,
        partition_range,
    )
]


ALL_CLAIM_PARTITIONS = [
    *PART_D_PARTITIONS,
    *INSTITUTIONAL_ADJUDICATED_PARTITIONS,
    *INSTITUTIONAL_PAC_PARTITIONS,
    *PROFESSIONAL_ADJUDICATED_PARTITIONS,
    *PROFESSIONAL_PAC_PARTITIONS,
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
