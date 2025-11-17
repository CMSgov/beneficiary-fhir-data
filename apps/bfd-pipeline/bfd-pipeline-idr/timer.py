import logging
import time

logger = logging.getLogger(__name__)


class Timer:
    def __init__(self, name: str) -> None:
        self.perf_start = 0.0
        self.name = name

    def start(self) -> None:
        self.perf_start = time.perf_counter()

    def stop(self, table: type | str) -> None:
        segment = time.perf_counter() - self.perf_start
        table_name = table
        logger.info("%s %s: %.6f seconds", table_name, self.name, segment)
