import logging
import time

from load_partition import LoadPartition
from model.base_model import T

logger = logging.getLogger(__name__)


class Timer:
    def __init__(self, name: str, model: type[T], partition: LoadPartition) -> None:
        self.perf_start = 0.0
        self.name = name
        self.model = model
        self.partition = partition

    def start(self) -> None:
        self.perf_start = time.perf_counter()

    def stop(self) -> None:
        segment = time.perf_counter() - self.perf_start
        logger.info(
            "%s-%s %s: %.6f seconds", self.model.table(), self.partition.name, self.name, segment
        )
