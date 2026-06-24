import atexit
import multiprocessing
from datetime import UTC, datetime

import anyio
import click
import psycopg  # type: ignore
from loguru import logger

from batch_worker import LoadingBatchWorkerManager
from db_utils import get_connection_string
from extractor import PostgresExecutor, SnowflakeExecutor
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
from load_synthetic import load_from_csv
from logger_config import configure_logger
from model.base_model import LoadMode, Source
from pipeline_stages import StagedIdrPipeline
from settings import (
    INCREMENTAL_IDR_JOB_GRACE_PERIOD,
    MAX_TASKS,
    TABLES_TO_LOAD,
    bfd_test_date,
)


@click.command
@click.option(
    "--source",
    envvar="IDR_SOURCE",
    type=click.Choice(Source, case_sensitive=False),
    default=Source.POSTGRES,
    show_default=True,
    help="Source to load from",
)
@click.option(
    "--load-mode",
    envvar="IDR_LOAD_MODE",
    type=click.Choice(LoadMode, case_sensitive=False),
    default=LoadMode.LOCAL,
    show_default=True,
    help="Mode - affects db connection string and load progress tracking",
)
@click.option(
    "--load-type",
    envvar="IDR_LOAD_TYPE",
    type=click.Choice(LoadType, case_sensitive=False),
    default=LoadType.INITIAL,
    show_default=True,
    help="Load type - affects claim filtering",
)
@click.option("--seed-from", type=click.Path(exists=True, resolve_path=True))
@click.option(
    "--truncate",
    is_flag=True,
    default=False,
    show_default=True,
    help="Truncate tables before reloading. Default is false",
)
def main(
    source: Source,
    load_mode: LoadMode,
    load_type: LoadType,
    seed_from: str | None,
    truncate: bool,
) -> None:
    if seed_from:
        load_from_csv(
            SnowflakeExecutor()
            if source == Source.SNOWFLAKE
            else PostgresExecutor(psycopg.connect(get_connection_string(LoadMode.SYNTHETIC))),
            seed_from,
            truncate,
        )
    run(source, load_mode, load_type)


def run(source: Source, load_mode: LoadMode, load_type: LoadType) -> None:
    logger.info("load start")
    logger.info("load_type {}", load_type)

    start_time = resolve_test_date(load_mode)

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

    worker_manager = LoadingBatchWorkerManager(get_connection_string(load_mode))
    atexit.register(worker_manager.cleanup)

    staged_pipeline = StagedIdrPipeline(
        max_workers=MAX_TASKS,
        load_mode=load_mode,
        start_time=start_time,
        load_type=load_type,
        source=source,
        worker_client=worker_manager.client,
        tables_to_load=tables_to_load,
    )

    async def run_worker_and_stages() -> None:
        stop_worker = anyio.Event()
        async with anyio.create_task_group() as tg:
            await tg.start(worker_manager.start, stop_worker)
            # We don't submit staged_pipeline.start to the task group because we want to set the
            # stop signal for the background worker once it's complete. If we don't do it this way
            # the pipeline process will run forever
            await staged_pipeline.start()
            stop_worker.set()

    try:
        anyio.run(run_worker_and_stages)
    except BaseException:
        if idr_job_events:
            logger.error(
                "{} IDR job load events failed to be fully processed: {}",
                len(idr_job_events),
                ", ".join(str(event.id) for event in idr_job_events),
            )
            update_failure_times(
                load_mode=load_mode,
                events=idr_job_events,
                failure_time=resolve_test_date(load_mode),
            )
        logger.opt(exception=True).error("Unrecoverable exception raised during pipeline load:")
        raise
    finally:
        if idr_job_events:
            update_completion_times(
                load_mode=load_mode,
                events=idr_job_events,
                completion_time=resolve_test_date(load_mode),
            )

        logger.complete()


def resolve_test_date(load_mode: LoadMode) -> datetime:
    test_date = bfd_test_date()

    if test_date and load_mode != LoadMode.PROD:
        return test_date
    return datetime.now(UTC)


if __name__ == "__main__":
    # Required to have loguru logging consistently configured across parallel pipeline nodes and
    # batch worker
    multiprocessing.set_start_method("spawn")
    # Setup the root logger _once_
    configure_logger()

    main()
