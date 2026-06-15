import os
import signal
import threading
from collections.abc import Callable, Generator
from concurrent.futures import Future, ProcessPoolExecutor
from multiprocessing import Manager
from queue import Queue
from threading import Event
from types import FrameType
from typing import Never

import anyio
from loguru import logger


class ExternallyCanceled(Exception):
    pass


class ParallelStagesExecutor:
    def __init__(self, max_workers: int | None = None) -> None:
        self.max_workers: int | None = max_workers
        self._manager = Manager()

    @staticmethod
    def _wrap[T, R](
        fn: Callable[[T], R],
        item: T,
        index: int,
        errors_queue: Queue[BaseException],
        cancel_signal: Event,
    ) -> tuple[int, R | None]:

        def _watch_for_parent_cancel(cancel_signal: Event) -> None:
            cancel_signal.wait()
            os.kill(os.getpid(), signal.SIGUSR1)

        def sigusr1_handler(signum: int, frame: FrameType | None) -> Never:  # noqa: ARG001
            raise ExternallyCanceled("Externally canceled, interrupting")

        signal.signal(signal.SIGUSR1, sigusr1_handler)
        threading.Thread(
            target=lambda: _watch_for_parent_cancel(cancel_signal), daemon=True
        ).start()

        try:
            return (index, fn(item))
        except BaseException as e:
            errors_queue.put(e)
            return (index, None)

    async def _run_stage[T, R](
        self,
        fn: Callable[[T], R],
        items: Generator[T],
        errors_queue: Queue[BaseException],
        pool: ProcessPoolExecutor,
        results: dict[int, R | None],
    ) -> None:
        done = anyio.Event()

        cancel_signal = self._manager.Event()
        async with anyio.create_task_group() as tg:
            try:
                tg.start_soon(self._poll_errors, errors_queue, done)

                async with anyio.create_task_group() as tg2:
                    for idx, item in enumerate(items):

                        async def _task(item: T = item, idx: int = idx) -> None:
                            future = pool.submit(
                                self._wrap, fn, item, idx, errors_queue, cancel_signal
                            )
                            idx_val, val = await self._wait_for_future_result(future)
                            results[idx_val] = val

                        tg2.start_soon(_task)

                done.set()
            except anyio.get_cancelled_exc_class():
                cancel_signal.set()
                raise

        # Final drain in case an error landed after tasks completed
        if not errors_queue.empty():
            raise errors_queue.get()

    async def execute[T, R](
        self, fn: Callable[[T], R], stages: list[Generator[T]]
    ) -> list[list[R | None]]:
        errors_queue: Queue[BaseException] = self._manager.Queue()
        all_results: list[list[R | None]] = []

        with ProcessPoolExecutor(
            max_workers=self.max_workers, initializer=logger.reinstall
        ) as pool:
            for stage in stages:
                results: dict[int, R | None] = {}
                await self._run_stage(fn, stage, errors_queue, pool, results)
                all_results.append([results[i] for i in range(len(results))])

        return all_results

    @staticmethod
    async def _poll_errors(errors_queue: Queue[BaseException], done: anyio.Event) -> None:
        while not done.is_set():
            if not errors_queue.empty():
                raise errors_queue.get()
            await anyio.sleep(0)

    @staticmethod
    async def _wait_for_future_result[R](future: Future[R]) -> R:
        while not future.done():
            await anyio.sleep(0)

        return future.result()
