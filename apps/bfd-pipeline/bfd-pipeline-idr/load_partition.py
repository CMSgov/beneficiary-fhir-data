from collections.abc import Generator
from dataclasses import dataclass
from datetime import UTC, date, datetime
from enum import IntFlag, StrEnum, auto

from dateutil.relativedelta import relativedelta


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
        if self.date_interval is None or load_type == LoadType.INCREMENTAL:
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
