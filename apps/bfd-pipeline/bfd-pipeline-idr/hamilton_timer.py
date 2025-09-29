import logging
import time
import sys

logger = logging.getLogger("pipeline_timer")
logger.setLevel(logging.INFO)
if not logger.handlers:
    console_handler = logging.StreamHandler(sys.stdout)
    formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

class Timer:
    def __init__(self, name: str) -> None:
        self.elapsed = 0.0
        self.perf_start = 0.0
        self.name = name

    def start(self) -> None:
        self.perf_start = time.perf_counter()

    def stop(self) -> None:
        segment = time.perf_counter() - self.perf_start
        logger.info("%s: %.6f seconds", self.name, segment)
        self.elapsed += segment

    def print_results(self) -> None:
        logger.info("Total time taken for %s: %.6f seconds", self.name, self.elapsed)
