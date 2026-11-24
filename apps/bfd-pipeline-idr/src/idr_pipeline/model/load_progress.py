from datetime import UTC, datetime
from typing import override

from ..constants import DEFAULT_JOB_ID
from ..load_partition import LoadPartition, LoadType
from ..model.base_model import IdrBaseModel, ModelType, Source


class LoadProgress(IdrBaseModel):
    table_name: str
    last_ts: datetime
    last_id: int
    batch_partition: str
    job_start_ts: datetime
    batch_start_ts: datetime
    batch_complete_ts: datetime
    job_id: int
    max_run_ts: datetime | None = None

    @staticmethod
    def query_placeholder() -> str:
        return "table_name"

    @override
    @staticmethod
    def table() -> str:
        return "idr.load_progress"

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.LOAD_PROGRESS

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        return cls.fetch_query_by_job(partition, DEFAULT_JOB_ID)

    @classmethod
    def fetch_query_by_job(cls, partition: LoadPartition, job_id: int) -> str:
        return f"""
        SELECT {",".join(cls.columns_raw())}
        FROM idr.load_progress
        WHERE table_name = %({LoadProgress.query_placeholder()})s
        AND batch_partition = '{partition.name}'
        AND job_id = {job_id}
        """

    def is_historical(self) -> bool:
        # 2021-4-18 is the most recent date where idr_insrt_ts could be null in claims data
        return self.last_ts <= datetime(2021, 4, 19, tzinfo=UTC)

    def should_skip(self, load_type: LoadType) -> bool:
        # Skip loading for initial loads if this job id has already loaded it
        return load_type == LoadType.INITIAL and self.batch_complete_ts > self.batch_start_ts
