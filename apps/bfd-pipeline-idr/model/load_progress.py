from collections.abc import Sequence
from datetime import UTC, datetime

from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model.base_model import (
    IdrBaseModel,
)


class LoadProgress(IdrBaseModel):
    table_name: str
    last_ts: datetime
    last_id: int
    batch_partition: str
    job_start_ts: datetime
    batch_complete_ts: datetime

    @staticmethod
    def query_placeholder() -> str:
        return "table_name"

    @staticmethod
    def table() -> str:
        return "idr.load_progress"

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        return f"""
        SELECT table_name, last_ts, last_id, batch_partition, job_start_ts, batch_complete_ts
        FROM idr.load_progress
        WHERE table_name = %({LoadProgress.query_placeholder()})s 
        AND batch_partition = '{partition.name}'
        """

    def is_historical(self) -> bool:
        # 2021-4-18 is the most recent date where idr_insrt_ts could be null in claims data
        return self.last_ts <= datetime(2021, 4, 19, tzinfo=UTC)

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return []
