import logging
import sys
from concurrent.futures import ProcessPoolExecutor
from datetime import UTC, datetime

from hamilton import driver, telemetry  # type: ignore
from hamilton.execution import executors  # type: ignore

import pipeline_nodes
from load_partition import LoadType
from model import LoadMode
from settings import LOAD_TYPE, MAX_TASKS, BFD_TEST_DATE

telemetry.disable_telemetry()

console_handler = logging.StreamHandler()
formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
console_handler.setFormatter(formatter)
logging.basicConfig(level=logging.INFO, handlers=[console_handler])

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
    max_tasks_per_child = 1 if load_mode == LoadMode.PRODUCTION else None

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

    if any(char.isdigit() for char in BFD_TEST_DATE):
        start_time = datetime.strptime(BFD_TEST_DATE, '%Y-%m-%d')
    else:
        start_time = datetime.now()

    # if load_benes and load_claims:
    hamilton_driver.execute(  # type: ignore
        final_vars=["do_stage4"],
        inputs={
            "load_type": load_type,
            "load_mode": load_mode,
            "start_time": start_time,
        },
    )


if __name__ == "__main__":
    main()
