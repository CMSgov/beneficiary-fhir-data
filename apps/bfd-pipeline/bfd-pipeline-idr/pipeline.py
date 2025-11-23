import logging
import os
import sys
from abc import ABC
from concurrent.futures import Future, ProcessPoolExecutor
from datetime import UTC, datetime
from multiprocessing import Process
from typing import Any, Tuple

from hamilton import driver, telemetry  # type: ignore
from hamilton.execution import executors
from hamilton.execution.grouping import TaskImplementation  # type: ignore

import pipeline_nodes
from constants import CLAIM_AUX_TABLES
from loader import get_connection_string

telemetry.disable_telemetry()

console_handler = logging.StreamHandler()
formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
console_handler.setFormatter(formatter)
logging.basicConfig(level=logging.INFO, handlers=[console_handler])

logger = logging.getLogger(__name__)


class SingleProcessExecutor(executors.TaskExecutor, ABC):
    def __init__(self, max_tasks: int) -> None:
        self.active_futures: list[tuple[ProcessPoolExecutor, Future[Any]]] = []
        self.max_tasks = max_tasks

    def _prune_active_futures(self) -> None:
        new_futures: list[tuple[ProcessPoolExecutor, Future[Any]]] = []
        for f in self.active_futures:
            if f[1].done():
                f[0].shutdown()
            else:
                new_futures.append(f)
        self.active_futures = new_futures

    def init(self) -> None:
        pass

    def finalize(self) -> None:
        for f in self.active_futures:
            f[0].shutdown(cancel_futures=True)

    def submit_task(self, task: TaskImplementation) -> executors.TaskFuture:
        executor = ProcessPoolExecutor(max_workers=1)
        future = executor.submit(executors.base_execute_task, task)
        self.active_futures.append((executor, future))
        return executors.TaskFutureWrappingPythonFuture(future)

    def can_submit_task(self) -> bool:
        self._prune_active_futures()
        return len(self.active_futures) < self.max_tasks


def parse_bool(var: str) -> bool:
    # bool(str) interprets anything non-empty as true so we gotta do it manually
    return var.lower() == "true" or var == "1"


def main() -> None:
    logger.info("load start")

    # parallelism = int(os.environ.get("PARALLELISM", "18"))
    # ray.init(logging_level="info", num_cpus=parallelism)  # type: ignore

    # dict_builder = base.DictResult()
    # adapter = FutureAdapter(result_builder=dict_builder)
    load_type = str(os.environ.get("IDR_LOAD_TYPE", "incremental"))
    logger.info("load_type %s", load_type)
    hamilton_driver = (
        driver.Builder()
        .enable_dynamic_execution(allow_experimental_mode=True)
        .with_config({"load_type": load_type})
        .with_modules(pipeline_nodes)
        #  .with_adapters(adapter)
        # .with_local_executor(executors.MultiProcessingExecutor(max_tasks=32))
        # .with_remote_executor(executors.MultiProcessingExecutor(max_tasks=32))
        .with_local_executor(SingleProcessExecutor(max_tasks=32))
        .with_remote_executor(SingleProcessExecutor(max_tasks=32))
        .build()
    )

    batch_size = int(os.environ.get("IDR_BATCH_SIZE", "100_000"))
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "local":
        connection_string = "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"
    else:
        connection_string = get_connection_string()

    # temporary flags to load a subset of data for testing
    load_benes = parse_bool(os.environ.get("IDR_LOAD_BENES", "true"))
    load_claims = parse_bool(os.environ.get("IDR_LOAD_CLAIMS", "true"))

    logger.info("load_benes %s", load_benes)
    logger.info("load_claims %s", load_claims)
    start_time = datetime.now(UTC)

    if load_benes and load_claims:
        hamilton_driver.execute(  # type: ignore
            final_vars=["idr_beneficiary"],
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
                "start_time": start_time,
            },
        )
    elif load_benes:
        # Since the DAG contains the dependency ordering, we need to override all claims nodes
        # in order to skip them and load only beneficiary data
        overrides = dict.fromkeys(CLAIM_AUX_TABLES)
        overrides["idr_claim"] = None
        hamilton_driver.execute(  # type: ignore
            final_vars=["idr_beneficiary"],
            overrides=overrides,
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
                "start_time": start_time,
            },
        )
    elif load_claims and load_type == "initial":
        # idr_claim only depends on claim aux nodes so we set idr_claim as our last node to execute
        hamilton_driver.execute(  # type: ignore
            final_vars=CLAIM_AUX_TABLES,  # type: ignore
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
                "start_time": start_time,
            },
        )
    elif load_claims and load_type == "incremental":
        # idr_claim only depends on claim aux nodes so we set idr_claim as our last node to execute
        hamilton_driver.execute(  # type: ignore
            final_vars=["idr_claim"],
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
                "start_time": start_time,
            },
        )


if __name__ == "__main__":
    main()
