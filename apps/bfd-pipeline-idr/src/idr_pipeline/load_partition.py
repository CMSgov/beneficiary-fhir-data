from collections.abc import Generator
from dataclasses import dataclass
from datetime import UTC, date, datetime
from enum import IntFlag, StrEnum, auto

from dateutil.relativedelta import relativedelta

from idr_pipeline.constants import (
    ALL_CLAIM_TYPE_CODES,
    INSTITUTIONAL_NCH_CLAIM_TYPE_CODES,
    INSTITUTIONAL_OUTPATIENT_CLAIM_TYPE_CODE,
    INSTITUTIONAL_SS_CLAIM_TYPE_CODES,
    PART_D_CLAIM_TYPE_CODES,
    PART_D_ORIGINAL_CLAIM_TYPE_CODE,
    PROFESSIONAL_NCH_CLAIM_TYPE_CODES,
    PROFESSIONAL_SS_CLAIM_TYPE_CODES,
)

from .settings import SETTINGS


class LoadType(StrEnum):
    INITIAL = "initial"
    INCREMENTAL = "incremental"


class PartitionType(IntFlag):
    PART_D = auto()
    INSTITUTIONAL = auto()
    PROFESSIONAL = auto()
    PAC = auto()
    ALL = auto()


@dataclass
class LoadPartition:
    name: str
    claim_type_codes: list[int]
    partition_type: PartitionType
    start_date: date | None
    end_date: date | None
    priority: int


@dataclass
class LoadPartitionGroup:
    name: str
    claim_type_codes: list[int]
    partition_type: PartitionType
    date_interval: relativedelta | None
    priority: int = 0

    def generate_ranges(self, load_type: LoadType, start_date: date) -> Generator[LoadPartition]:
        if (
            self.date_interval is None
            or not SETTINGS.enable_date_partitions
            or load_type == LoadType.INCREMENTAL
        ):
            yield LoadPartition(
                self.name, self.claim_type_codes, self.partition_type, None, None, self.priority
            )
            return

        start = date(year=start_date.year, month=start_date.month, day=1)
        now = datetime.date(datetime.now(UTC))
        while start < now:
            end = start + self.date_interval - relativedelta(days=1)
            start_str = start.strftime("%Y-%m-%d")
            end_str = end.strftime("%Y-%m-%d")
            yield LoadPartition(
                f"{self.name}-{start_str}-{end_str}",
                self.claim_type_codes,
                self.partition_type,
                start,
                end,
                self.priority,
            )
            start += self.date_interval


match SETTINGS.partition_type:
    case "year" | "years":
        partition_range = relativedelta(years=1)
    case "month" | "months":
        partition_range = relativedelta(months=1)
    case "day" | "days":
        partition_range = relativedelta(days=1)
    case _:
        raise ValueError("invalid partition type " + SETTINGS.partition_type)


PART_D_PARTITIONS = [
    LoadPartitionGroup(
        "part_d_original", [PART_D_ORIGINAL_CLAIM_TYPE_CODE], PartitionType.PART_D, partition_range
    ),
    LoadPartitionGroup(
        "part_d_adjustment",
        [c for c in PART_D_CLAIM_TYPE_CODES if c != PART_D_ORIGINAL_CLAIM_TYPE_CODE],
        PartitionType.PART_D,
        partition_range,
    ),
]

INSTITUTIONAL_NCH_PARTITIONS = [
    # Outpatient
    LoadPartitionGroup(
        "outpatient",
        [INSTITUTIONAL_OUTPATIENT_CLAIM_TYPE_CODE],
        PartitionType.INSTITUTIONAL,
        partition_range,
    ),
    # HHA, SNF, Hospice, Inpatient, MA
    LoadPartitionGroup(
        "institutional",
        [
            c
            for c in INSTITUTIONAL_NCH_CLAIM_TYPE_CODES
            if c != INSTITUTIONAL_OUTPATIENT_CLAIM_TYPE_CODE
        ],
        PartitionType.INSTITUTIONAL,
        partition_range,
    ),
]

INSTITUTIONAL_SS_PARTITIONS = [
    LoadPartitionGroup(
        "institututional_pac",
        INSTITUTIONAL_SS_CLAIM_TYPE_CODES,
        PartitionType.INSTITUTIONAL | PartitionType.PAC,
        partition_range,
    )
]

PROFESSIONAL_NCH_PARTITIONS = [
    LoadPartitionGroup(
        "professional",
        PROFESSIONAL_NCH_CLAIM_TYPE_CODES,
        PartitionType.PROFESSIONAL,
        partition_range,
    ),
]

PROFESSIONAL_SS_PARTITIONS = [
    LoadPartitionGroup(
        "professional_pac",
        PROFESSIONAL_SS_CLAIM_TYPE_CODES,
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


COMBINED_CLAIM_PARTITION = LoadPartitionGroup(
    "all_claims",
    ALL_CLAIM_TYPE_CODES,
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
