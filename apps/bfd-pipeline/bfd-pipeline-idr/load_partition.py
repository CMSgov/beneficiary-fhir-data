from collections.abc import Generator
from dataclasses import dataclass
from datetime import UTC, date, datetime, timedelta
from enum import IntFlag, auto


class PartitionType(IntFlag):
    DEFAULT = 0
    PART_D = auto()
    INSTITUTIONAL = auto()
    PROFESSIONAL = auto()
    PAC = auto()


@dataclass
class LoadPartition:
    name: str
    claim_type_codes: list[int]
    partition_type: PartitionType
    start_date: date | None
    end_date: date | None


@dataclass
class LoadPartitionGroup:
    name: str
    claim_type_codes: list[int]
    partition_type: PartitionType
    date_interval: timedelta | None

    def generate_ranges(self, start_date: date) -> Generator[LoadPartition]:
        if self.date_interval is None:
            yield LoadPartition(self.name, self.claim_type_codes, self.partition_type, None, None)
            return
        start = start_date
        end = start_date + self.date_interval
        now = datetime.date(datetime.now(UTC))
        while start < now:
            start_str = start.strftime("%Y-%m-%d")
            end_str = end.strftime("%Y-%m-%d")
            yield LoadPartition(
                f"{self.name}-{start_str}-{end_str}",
                self.claim_type_codes,
                self.partition_type,
                start,
                end,
            )
            next_end = end + self.date_interval
            start = end
            end = next_end


DEFAULT_PARTITION = LoadPartition("default", [], PartitionType.DEFAULT, None, None)
