import contextlib
import os
import signal
import sys
import threading
from abc import ABC, abstractmethod
from collections.abc import Callable, Generator
from concurrent.futures import Future, ProcessPoolExecutor, ThreadPoolExecutor
from multiprocessing import Manager
from pathlib import Path
from queue import Empty, Queue
from threading import Event
from types import FrameType
from typing import Never

import anyio
from loguru import logger

from .exception_utils import (
    SerializedExceptionChain,
    rebuild_exception_chain,
    serialize_exception_chain,
)

type StageTask[T] = Callable[[], T]
type Stage[T] = Generator[StageTask[T]]


class ExternallyCanceled(Exception):
    pass


class Executor(ABC):
    @abstractmethod
    async def execute[T](self, stages: list[Stage[T]]) -> list[list[T | None]]: ...

    @staticmethod
    async def _wait_for_future_result[T](future: Future[T]) -> T:
        while not future.done():
            await anyio.sleep(0.01)

        return future.result()


class MultithreadingExecutor(Executor):
    """
    Executor that uses multithreading instead of multiprocessing.

    This is much slower than the multiprocessing executor due to blocking IO
    so it should only be used for tests.
    """

    def __init__(self, max_workers: int | None = None) -> None:
        self.max_workers: int | None = max_workers

    async def execute[T](self, stages: list[Stage[T]]) -> list[list[T | None]]:
        all_results: list[list[T | None]] = []
        pool = ThreadPoolExecutor(self.max_workers)
        for stage in stages:
            results: dict[int, T | None] = {}
            for i, task in enumerate(stage):

                async def _task(
                    task: StageTask[T] = task, idx: int = i, results: dict[int, T | None] = results
                ) -> None:
                    val = await self._wait_for_future_result(pool.submit(task))
                    results[idx] = val

                async with anyio.create_task_group() as tg:
                    tg.start_soon(_task)

            all_results.append([results[i] for i in range(len(results))])

        return all_results


class MultiprocessingExecutor(Executor):
    def __init__(self, max_workers: int | None = None) -> None:
        self.max_workers: int | None = max_workers
        self._manager = Manager()

    @staticmethod
    def _wrap[T](
        task: StageTask[T],
        index: int,
        errors_queue: Queue[SerializedExceptionChain],
        cancel_signal: Event,
    ) -> tuple[int, T | None]:
        # The next four lines suppress unhandled/unraised Exception output to prevent noise when
        # the ExternallyCanceled signal is used to stop this worker. Without this, Python's default
        # behavior prints the full trace and Exception context to stderr, cluttering the logs.
        sys.unraisablehook = lambda _: None
        sys.excepthook = lambda _, __, ___: None
        with Path(os.devnull).open("w") as devnull:
            sys.stderr = devnull

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
                return (index, task())
            except ExternallyCanceled:
                pass
            except BaseException as ex:
                errors_queue.put(serialize_exception_chain(ex))

            return (index, None)

    async def _run_stage[T](
        self,
        stage: Stage[T],
        errors_queue: Queue[SerializedExceptionChain],
        pool: ProcessPoolExecutor,
        results: dict[int, T | None],
    ) -> None:
        done = anyio.Event()

        cancel_signal = self._manager.Event()
        try:
            async with anyio.create_task_group() as tg:
                tg.start_soon(self._poll_errors, errors_queue, done)

                async with anyio.create_task_group() as tg2:
                    for idx, task in enumerate(stage):

                        async def _task(task: StageTask[T] = task, idx: int = idx) -> None:
                            future = pool.submit(self._wrap, task, idx, errors_queue, cancel_signal)
                            idx_val, val = await self._wait_for_future_result(future)
                            results[idx_val] = val

                        tg2.start_soon(_task)

                done.set()
        except BaseException:
            cancel_signal.set()
            raise

    async def execute[T](self, stages: list[Stage[T]]) -> list[list[T | None]]:
        errors_queue: Queue[SerializedExceptionChain] = self._manager.Queue()
        all_results: list[list[T | None]] = []

        with ProcessPoolExecutor(
            max_workers=self.max_workers, initializer=logger.reinstall
        ) as pool:
            for stage in stages:
                results: dict[int, T | None] = {}
                await self._run_stage(stage, errors_queue, pool, results)
                all_results.append([results[i] for i in range(len(results))])

        return all_results

    @staticmethod
    async def _poll_errors(
        errors_queue: Queue[SerializedExceptionChain], done: anyio.Event
    ) -> None:
        while not done.is_set():
            with contextlib.suppress(Empty):
                raise rebuild_exception_chain(errors_queue.get_nowait())

            await anyio.sleep(0.01)
