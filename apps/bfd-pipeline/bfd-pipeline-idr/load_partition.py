from dataclasses import dataclass
from enum import IntFlag, auto


class PartitionType(IntFlag):
    DEFAULT = 0
    PART_D = auto()
    INSTITUTIONAL = auto()
    PROFESSIONAL = auto()
    PAC = auto()


@dataclass
class LoadPartition:
    claim_type_codes: list[int]
    partition_type: PartitionType


DEFAULT_PARTITION = LoadPartition([], PartitionType.DEFAULT)
