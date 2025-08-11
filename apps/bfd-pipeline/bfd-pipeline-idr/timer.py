import logging
import time

logger = logging.getLogger(__name__)


class Timer:
    def __init__(self, name: str) -> None:
        self.elapsed = 0.0
        self.perf_start = 0.0
        self.name = name

    def start(self) -> None:
        self.perf_start = time.perf_counter()

    def stop(self) -> None:
        segment = time.perf_counter() - self.perf_start
        logger.info("Segment for %s: %.6f seconds", self.name, segment)
        self.elapsed += segment

    def print_results(self) -> None:
        logger.info("Time taken for %s: %.6f seconds", self.name, self.elapsed)
