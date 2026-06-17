import contextlib
import os
import random
import signal
import string
import threading
import uuid
from collections import Counter, OrderedDict
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from datetime import UTC, date, datetime
from itertools import batched
from math import ceil
from multiprocessing import Manager, Process
from multiprocessing.managers import SyncManager
from queue import Empty, Queue
from threading import Event
from types import FrameType
from typing import Any, Never, cast

import anyio
import psycopg_pool
from anyio.abc import TaskStatus
from anyio.streams.memory import MemoryObjectSendStream
from loguru import logger
from loguru._logger import Logger
from psycopg import sql
from psycopg.errors import DeadlockDetected, InFailedSqlTransaction
from psycopg_pool.abc import ACT

from load_partition import LoadPartition
from model.base_model import DbType, IdrBaseModel
from model.load_progress import LoadProgress
from parallel_executor import ExternallyCanceled
from settings import PER_BATCH_MAX_CONNECTIONS, PER_BATCH_MIN_CONNECTIONS
from timer import Timer

_MAX_LAST_UPDATED_CHUNKS_PER_BATCH = 100
_MINIMUM_LAST_UPDATED_CHUNK_SIZE = 100


@dataclass
class LoadingBatch:
    batch_num: int
    model: type[IdrBaseModel]
    partition: LoadPartition
    progress: LoadProgress | None
    all_rows: list[IdrBaseModel]
    changed_keys: list[dict[str, DbType]]
    timestamp: datetime


type _TaskSequence = list[_Task]


@dataclass(eq=False)
class _Task:
    _guid: uuid.UUID = field(init=False, repr=False)

    def __post_init__(self) -> None:
        self._guid = uuid.uuid4()

    def __hash__(self) -> int:
        return hash(self._guid)

    def __eq__(self, value: object, /) -> bool:
        if not isinstance(value, _Task):
            return False

        return self._guid == value._guid


@dataclass(eq=False)
class _LoadPartitionTask(_Task):
    model: type[IdrBaseModel]
    partition: LoadPartition


@dataclass(eq=False)
class _DoLastUpdated(_LoadPartitionTask):
    target_table: str
    target_table_key: str
    keys: list[DbType]
    timestamp: datetime

    @classmethod
    def from_loading_batch(cls, batch: LoadingBatch) -> _DoLastUpdated | None:
        # _DoLastUpdated looks at the changed keys of the loading batch because we do not want to
        # update parent table columns for rows that had no changes in their data
        if not batch.changed_keys:
            logger.debug("changed_rows must not be empty")
            return None

        target_table = batch.model.last_updated_date_table()
        target_table_key = batch.model.last_updated_timestamp_col()
        if not target_table_key or not target_table:
            logger.warning("Model type of data does not support last updated")
            return None

        return _DoLastUpdated(
            model=batch.model,
            target_table=target_table,
            target_table_key=target_table_key,
            partition=batch.partition,
            keys=list(OrderedDict.fromkeys(x[target_table_key] for x in batch.changed_keys)),
            timestamp=batch.timestamp,
        )


@dataclass(eq=False)
class _UpdateLoadProgress(_LoadPartitionTask):
    last: IdrBaseModel
    last_id: str | int
    last_ts: datetime

    @classmethod
    def from_loading_batch(cls, batch: LoadingBatch) -> _UpdateLoadProgress | None:
        # _UpdateLoadProgress looks at _all_ rows because the tracking is for us, and even though
        # not all rows may have actually had relevant changes we still want to track that they've
        # been processed
        if not batch.all_rows:
            logger.debug("data must not be empty")
            return None

        last = batch.all_rows[len(batch.all_rows) - 1]
        # Some tables that contain reference data (like contract info) may not have the
        # normal IDR timestamps.
        # For now we won't support incremental refreshes for those tables
        batch_timestamp_cols = batch.model.batch_timestamp_col(
            batch.progress is None or batch.progress.is_historical()
        )
        update_cols = batch.model.update_timestamp_col()
        if not batch_timestamp_cols:
            logger.warning(f"No batch timestamp columns for {batch.model.table()}")
            return None

        max_timestamp = max(
            [
                cls._convert_date(getattr(last, col))
                for col in [*batch_timestamp_cols, *update_cols]
                if getattr(last, col) is not None
            ]
        )
        batch_id_col = batch.model.batch_id_col()
        batch_id: str | int = getattr(last, batch_id_col) if batch_id_col else 0

        return _UpdateLoadProgress(
            model=batch.model,
            partition=batch.partition,
            last=last,
            last_id=batch_id,
            last_ts=max_timestamp,
        )

    @staticmethod
    def _convert_date(date_field: date | datetime) -> datetime:
        if type(date_field) is datetime:
            return date_field.replace(tzinfo=UTC)
        return datetime.combine(date_field, datetime.min.time()).replace(tzinfo=UTC)


@dataclass(eq=False)
class _WaitForPartitionComplete(_Task):
    model: type[IdrBaseModel]
    partition: LoadPartition
    done_event: Event


@dataclass(eq=False)
class _StopWorker(_Task):
    pass


class _LoadingBatchWorker(Process):
    def __init__(
        self,
        task_queue: Queue[_TaskSequence],
        errors_queue: Queue[BaseException],
        started_signal: Event,
        cancel_signal: Event,
        root_logger: Logger,
        conn_str: str,
    ) -> None:
        super().__init__()
        self.task_queue = task_queue
        self.errors_queue = errors_queue
        self.started_signal = started_signal
        self._cancel_signal = cancel_signal
        self._logger = root_logger
        self._conn_str = conn_str

        self._running_tasks: set[_Task] = set()

    def run(self) -> None:
        self._logger.reinstall()

        def _watch_for_parent_cancel(stop_signal: Event) -> None:
            stop_signal.wait()
            os.kill(os.getpid(), signal.SIGUSR1)

        def sigusr1_handler(signum: int, frame: FrameType | None) -> Never:  # noqa: ARG001
            raise ExternallyCanceled("Externally canceled, interrupting")

        signal.signal(signal.SIGUSR1, sigusr1_handler)
        threading.Thread(
            target=lambda: _watch_for_parent_cancel(self._cancel_signal), daemon=True
        ).start()

        try:
            anyio.run(self._worker_main)
        except BaseException as ex:
            self.errors_queue.put(ex)

    async def _worker_main(self) -> None:
        task_send, task_receive = anyio.create_memory_object_stream[_TaskSequence](
            PER_BATCH_MAX_CONNECTIONS
        )
        limiter = anyio.CapacityLimiter(PER_BATCH_MAX_CONNECTIONS)

        async with (
            anyio.create_task_group() as tg,
            psycopg_pool.AsyncConnectionPool(
                conninfo=self._conn_str,
                min_size=PER_BATCH_MIN_CONNECTIONS,
                max_size=PER_BATCH_MAX_CONNECTIONS,
                timeout=600,  # See loader.py for explanation on timeout length
            ) as pool,
        ):
            await pool.wait()

            stop = anyio.Event()
            tg.start_soon(self._run_queue_bridge, task_send, stop)

            # Signal back to the manager that we're alive and ready
            self.started_signal.set()
            logger.info(
                "LoadingBatchWorker setup with max concurrency of {}", PER_BATCH_MAX_CONNECTIONS
            )

            async with task_receive:
                async for task_sequence in task_receive:
                    if not task_sequence:
                        continue

                    task_funcs: list[Callable[[], Awaitable[Any]]] = []
                    logger.info(
                        "Task sequence received: {}",
                        " -> ".join(type(x).__name__ for x in task_sequence),
                    )
                    for task in task_sequence:
                        match task:
                            case _DoLastUpdated():
                                task_funcs.append(
                                    lambda task=task: self._do_last_updated(task, pool, limiter)
                                )

                            case _UpdateLoadProgress():
                                task_funcs.append(
                                    lambda task=task: self._do_load_progress(task, pool, limiter)
                                )

                            case _WaitForPartitionComplete():
                                task_funcs.append(lambda task=task: self._wait_for_completion(task))

                            case _StopWorker():
                                stop.set()
                                logger.info(
                                    "{} received, {} shutting down",
                                    _StopWorker.__name__,
                                    _LoadingBatchWorker.__name__,
                                )
                                break

                            case _:
                                pass

                    if stop.is_set():
                        break

                    if task_funcs:
                        tg.start_soon(self._run_task_sequence, task_sequence, task_funcs)

    async def _run_task_sequence[T](
        self, tasks: _TaskSequence, funcs: list[Callable[[], Awaitable[T]]]
    ) -> list[T]:
        def str_tasks() -> str:
            return ", ".join(
                f"{k}: {v}"
                for k, v in Counter(type(obj).__name__ for obj in self._running_tasks).items()
            )

        # This id is computed just for the log so that it's easier to correlate a given sequence of
        # asynchronous tasks in the log
        sequence_id = "".join(random.choices(string.ascii_letters + string.digits, k=8))
        self._running_tasks.update(tasks)
        logger.info(
            "Starting new task sequence (id {}): {}; {} task(s) now running: {}",
            sequence_id,
            " -> ".join(type(x).__name__ for x in tasks),
            len(self._running_tasks),
            str_tasks(),
        )
        results = [await x() for x in funcs]
        self._running_tasks.difference_update(tasks)
        logger.info(
            "Completed task sequence (id {}): {}; {} task(s) still running: {}",
            sequence_id,
            " -> ".join(type(x).__name__ for x in tasks),
            len(self._running_tasks),
            str_tasks(),
        )
        return results

    async def _run_queue_bridge(
        self,
        task_send: MemoryObjectSendStream[_TaskSequence],
        stop: anyio.Event,
    ) -> None:
        async with task_send:
            while not stop.is_set():
                with contextlib.suppress(Empty):
                    await task_send.send(self.task_queue.get_nowait())
                await anyio.sleep(0)

    async def _do_last_updated(
        self,
        task: _DoLastUpdated,
        pool: psycopg_pool.AsyncConnectionPool[ACT],
        limiter: anyio.CapacityLimiter,
    ) -> None:
        num_keys = len(task.keys)
        keys_per_chunk = ceil(num_keys / _MINIMUM_LAST_UPDATED_CHUNK_SIZE)
        num_chunks = min(keys_per_chunk, _MAX_LAST_UPDATED_CHUNKS_PER_BATCH)
        chunk_size = max(keys_per_chunk, _MINIMUM_LAST_UPDATED_CHUNK_SIZE)
        logger.debug(
            "Executing last_updated for {}-{} with {} key(s) in {} chunk(s) of {} key(s) each",
            task.model.table(),
            task.partition.name,
            num_keys,
            num_chunks,
            chunk_size,
        )
        last_updated_set_clause = sql.SQL(", ").join(
            t"{col:i} = {task.timestamp}" for col in task.model.last_updated_date_column()
        )
        schema, table = task.target_table.split(".", 1)
        full_table = sql.Identifier(schema, table)
        chunks = {
            idx: list(chunk)
            for idx, chunk in enumerate(batched(task.keys, chunk_size, strict=False))
        }
        chunks_complete: dict[int, bool] = {idx: False for idx in chunks}

        async def update_chunk(chunk_id: int) -> None:
            async with pool.connection() as conn, conn.cursor() as cur:
                try:
                    await cur.execute(
                        t"""
                        UPDATE {full_table:i} u
                        SET {last_updated_set_clause:q}
                        WHERE u.{task.target_table_key:i} = ANY({chunks[chunk_id]});
                        """,
                    )
                    chunks_complete[chunk_id] = True
                except DeadlockDetected, InFailedSqlTransaction:
                    await conn.rollback()
                    chunks_complete[chunk_id] = False

        async with limiter:
            last_updated_timer = Timer("last_updated_bg", task.model, task.partition)
            last_updated_timer.start()
            attempt = 1
            max_attempts = 15
            while incomplete_chunks := [
                id for id, complete in chunks_complete.items() if not complete
            ]:
                if attempt > max_attempts:
                    raise RuntimeError(
                        f"Last updated failed for {task.model.table()}-{task.partition.name} after "
                        f"{max_attempts} attempts"
                    )

                logger.debug(
                    "Updating {} incomplete last_updated chunks for {}-{}, attempt #{}",
                    len(incomplete_chunks),
                    task.target_table,
                    task.target_table_key,
                    attempt,
                )
                if attempt > 1:
                    logger.warning(
                        "Failed to do last_updated for {}-{} (updating {}-{}) after {} attempt(s); "
                        "{} failing chunks remaining",
                        task.model.table(),
                        task.partition.name,
                        task.target_table,
                        task.target_table_key,
                        attempt - 1,
                        len(incomplete_chunks),
                    )
                async with anyio.create_task_group() as tg:
                    for chunk_id in incomplete_chunks:
                        tg.start_soon(update_chunk, chunk_id)

                attempt += 1
            last_updated_timer.stop()

    async def _do_load_progress(
        self,
        task: _UpdateLoadProgress,
        pool: psycopg_pool.AsyncConnectionPool[ACT],
        limiter: anyio.CapacityLimiter,
    ) -> None:
        logger.debug(
            "Running load progress update for {}-{}", task.model.table(), task.partition.name
        )
        load_progress_bg_timer = Timer("load_progress_bg", task.model, task.partition)
        async with limiter, pool.connection() as conn, conn.cursor() as cur:
            load_progress_bg_timer.start()
            await cur.execute(t"""
                UPDATE {sql.Identifier("idr", "load_progress"):i}
                SET
                    last_ts = {task.last_ts},
                    last_id = {task.last_id}
                WHERE
                    table_name = {task.model.table()}
                    AND batch_partition = {task.partition.name}
                """)
            load_progress_bg_timer.stop()

    async def _wait_for_completion(self, task: _WaitForPartitionComplete) -> None:
        while any(
            running
            for running in self._running_tasks
            if isinstance(running, _LoadPartitionTask)
            and running.partition.name == task.partition.name
            and running.model == task.model
        ):
            await anyio.sleep(0)

        logger.debug(
            "{}-{} has no tasks remaining, done event: {}",
            task.model.table(),
            task.partition.name,
            task.done_event.__hash__(),
        )

        task.done_event.set()


class LoadingBatchWorkerClient:
    def __init__(self, task_queue: Queue[_TaskSequence], manager_address: Any) -> None:  # noqa: ANN401
        self._task_queue = task_queue
        self._manager_address = manager_address

    def do_last_updated(self, batch: LoadingBatch, and_load_progress: bool | None = None) -> None:
        if and_load_progress is None:
            and_load_progress = True

        last_updated = _DoLastUpdated.from_loading_batch(batch)
        load_progress = _UpdateLoadProgress.from_loading_batch(batch) if and_load_progress else None

        self._task_queue.put([x for x in [last_updated, load_progress] if x])

    def do_load_progress(self, batch: LoadingBatch) -> None:
        self._task_queue.put([x for x in [_UpdateLoadProgress.from_loading_batch(batch)] if x])

    def wait_until_done(self, model: type[IdrBaseModel], partition: LoadPartition) -> None:
        manager = SyncManager(address=self._manager_address)
        manager.connect()
        done_event = manager.Event()
        self._task_queue.put([_WaitForPartitionComplete(model, partition, done_event)])
        done_event.wait()


class LoadingBatchWorkerManager:
    def __init__(self, conn_str: str) -> None:
        self._manager = Manager()
        self._task_queue: Queue[_TaskSequence] = self._manager.Queue()
        self._started_signal: Event = self._manager.Event()
        self._worker: _LoadingBatchWorker | None = None
        self._conn_str = conn_str

    @property
    def client(self) -> LoadingBatchWorkerClient:
        return LoadingBatchWorkerClient(self._task_queue, self._manager.address)

    async def start(self, stop: anyio.Event, task_status: TaskStatus | None = None) -> None:
        if self._worker is not None and self._worker.is_alive():
            return

        self._started_signal.clear()
        errors_queue: Queue[BaseException] = self._manager.Queue()
        cancel_signal = self._manager.Event()

        self._worker = _LoadingBatchWorker(
            task_queue=self._task_queue,
            errors_queue=errors_queue,
            started_signal=self._started_signal,
            cancel_signal=cancel_signal,
            root_logger=cast(Logger, logger),
            conn_str=self._conn_str,
        )
        self._worker.start()

        # Block until the worker signals it has started
        self._started_signal.wait()

        logger.info("LoadingBatchWorker signaled startup")

        if task_status:
            task_status.started()

        async def watch_queue() -> None:
            while not stop.is_set():
                with contextlib.suppress(Empty):
                    errors = errors_queue.get_nowait()
                    raise errors

                await anyio.sleep(0.01)

        async with anyio.create_task_group() as tg:
            try:
                tg.start_soon(watch_queue)
            except BaseException:
                cancel_signal.set()
                raise

    def cleanup(self, timeout: float = 5.0) -> None:
        if self._worker is None:
            return

        self._task_queue.put([_StopWorker()])

        self._worker.join(timeout=timeout)

        if self._worker.is_alive():
            logger.warning("Worker did not exit gracefully, terminating.")
            self._worker.terminate()
            self._worker.join(timeout=1.0)

        logger.info("LoadingBatchWorker cleanup complete")
