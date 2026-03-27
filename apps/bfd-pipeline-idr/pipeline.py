import logging
import sys
from concurrent.futures import ProcessPoolExecutor
from datetime import UTC, datetime

from hamilton import driver, telemetry  # type: ignore
from hamilton.execution import executors  # type: ignore

import pipeline_nodes
from load_events import (
    IdrJobLoadEvent,
    get_eligible_events,
    get_tables_to_load,
    get_unreported_jobs,
    update_completion_times,
    update_failure_times,
    update_start_times,
)
from load_partition import LoadType
from logger_config import configure_logger
from model.base_model import LoadMode
from settings import INCREMENTAL_IDR_JOB_GRACE_PERIOD, LOAD_TYPE, MAX_TASKS, TABLES_TO_LOAD

telemetry.disable_telemetry()

logger = logging.getLogger(__name__)


class MultiProcessingExecutor(executors.PoolExecutor):
    def __init__(self, max_tasks: int, max_tasks_per_child: int | None) -> None:
        self.max_tasks_per_child = max_tasks_per_child
        super().__init__(max_tasks)

    def create_pool(self) -> ProcessPoolExecutor:
        return ProcessPoolExecutor(
            max_workers=self.max_tasks, max_tasks_per_child=self.max_tasks_per_child
        )


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    run(mode)


def run(load_mode: str) -> None:
    logger.info("load start")
    load_mode = LoadMode(load_mode)

    load_type = LoadType(LOAD_TYPE)

    logger.info("load_type %s", load_type)
    # Per the docs (https://docs.python.org/3/library/multiprocessing.html#module-multiprocessing.pool)
    # processes in the pool will be reused indefinitely if max_tasks_per_child is not specified.
    # Setting this parameter will cause old processes to be recycled, allowing resources used by
    # these processes to be freed.
    # This will allow memory usage to remain constant over time.
    max_tasks_per_child = 1 if load_mode == LoadMode.IDR else None

    hamilton_driver = (
        driver.Builder()
        .enable_dynamic_execution(allow_experimental_mode=True)
        .with_modules(pipeline_nodes)
        .with_local_executor(
            MultiProcessingExecutor(max_tasks=MAX_TASKS, max_tasks_per_child=max_tasks_per_child)
        )
        .with_remote_executor(
            MultiProcessingExecutor(max_tasks=MAX_TASKS, max_tasks_per_child=max_tasks_per_child)
        )
        .build()
    )

    start_time = datetime.now(UTC)
    tables_to_load = set(TABLES_TO_LOAD) if TABLES_TO_LOAD else None
    idr_job_events: list[IdrJobLoadEvent] = []
    if load_type == LoadType.INCREMENTAL and not tables_to_load:
        idr_job_events = get_eligible_events(load_mode=load_mode, start_time=start_time)
        unreported_jobs = get_unreported_jobs(
            load_mode=load_mode,
            start_time=start_time,
            grace_period=INCREMENTAL_IDR_JOB_GRACE_PERIOD,
        )

        update_start_times(load_mode=load_mode, events=idr_job_events, start_time=start_time)

        tables_to_load = get_tables_to_load(
            unreported_jobs | {event.job_type for event in idr_job_events}
        )

    # if load_benes and load_claims:
    try:
        hamilton_driver.execute(  # type: ignore
            final_vars=["collect_stage4"],
            inputs={
                "load_type": load_type,
                "load_mode": load_mode,
                "start_time": start_time,
                "tables_to_load": tables_to_load,
            },
        )
    except Exception:
        if idr_job_events:
            logger.error(
                "%d IDR job load events failed to be fully processed: %s",
                len(idr_job_events),
                ", ".join(str(event.id) for event in idr_job_events),
            )
            update_failure_times(
                load_mode=load_mode, events=idr_job_events, failure_time=datetime.now(UTC)
            )
        logger.exception("Unrecoverable exception raised during pipeline load")
        raise

    if idr_job_events:
        update_completion_times(
            load_mode=load_mode, events=idr_job_events, completion_time=datetime.now(UTC)
        )


if __name__ == "__main__":
    configure_logger()
    main()
