import time


class Timer:
    def __init__(self, name: str) -> None:
        self.elapsed = 0.0
        self.perf_start = 0.0
        self.name = name

    def start(self) -> None:
        self.perf_start = time.perf_counter()

    def stop(self) -> None:
        segment = time.perf_counter() - self.perf_start
        print(f"Segment for {self.name}: {segment:.6f} seconds")
        self.elapsed += segment

    def print_results(self) -> None:
        print(f"Time taken for {self.name}: {self.elapsed:.6f} seconds")
