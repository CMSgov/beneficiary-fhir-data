import os
import random
import string
import time
import uuid
from collections import Counter, OrderedDict
from collections.abc import Awaitable, Callable
from dataclasses import dataclass, field
from datetime import UTC, date, datetime
from itertools import batched
from math import ceil
from multiprocessing import Manager, Process
from multiprocessing.managers import SyncManager
from queue import Queue
from threading import Event
from typing import Any, cast

import anyio
import anyio.from_thread
import anyio.to_thread
import psycopg_pool
from anyio.streams.memory import MemoryObjectSendStream
from loguru import logger
from loguru._logger import Logger
from psycopg import sql
from psycopg.errors import DeadlockDetected, InFailedSqlTransaction
from psycopg_pool.abc import ACT

from db_utils import to_pg_integer
from load_partition import LoadPartition
from model.base_model import DbType, IdrBaseModel
from model.load_progress import LoadProgress
from settings import PER_BATCH_MAX_CONNECTIONS, PER_BATCH_MIN_CONNECTIONS
from timer import Timer

_MAX_LAST_UPDATED_CHUNKS_PER_BATCH = 100
_MINIMUM_LAST_UPDATED_CHUNK_SIZE = 100


@dataclass
class LoadingBatch:
    model: type[IdrBaseModel]
    partition: LoadPartition
    progress: LoadProgress | None
    data: list[IdrBaseModel]
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
        if not batch.data:
            logger.debug("data must not be empty")
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
            keys=list(OrderedDict.fromkeys(getattr(x, target_table_key) for x in batch.data)),
            timestamp=batch.timestamp,
        )


@dataclass(eq=False)
class _UpdateLoadProgress(_LoadPartitionTask):
    last: IdrBaseModel
    last_id: str | int
    last_ts: datetime

    @classmethod
    def from_loading_batch(cls, batch: LoadingBatch) -> _UpdateLoadProgress | None:
        if not batch.data:
            logger.debug("data must not be empty")
            return None

        last = batch.data[len(batch.data) - 1]
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
    """A subprocess worker that pulls tasks from a shared queue."""

    def __init__(
        self,
        task_queue: Queue[_TaskSequence],
        started_event: Event,
        shutdown_event: Event,
        root_logger: Logger,
        conn_str: str,
    ) -> None:
        super().__init__()
        self.task_queue = task_queue
        self.started_event = started_event
        self.shutdown_event = shutdown_event
        self._logger = root_logger
        self._conn_str = conn_str

        self._running_tasks: set[_Task] = set()

    def run(self) -> None:
        self._logger.reinstall()
        try:
            anyio.run(self._worker_main)
        except Exception:
            logger.opt(exception=True).critical("LoadingBatchWorker process died:")
            os._exit(
                1
            )  # Force an exit because the pipeline should stop if the background worker does

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

            tg.start_soon(self._run_queue_bridge, task_send)

            # Signal back to the manager that we're alive and ready
            self.started_event.set()
            logger.info(
                "LoadingBatchWorker setup with max concurrency of {}", PER_BATCH_MAX_CONNECTIONS
            )

            async with task_receive:
                async for task_sequence in task_receive:
                    if not task_sequence:
                        continue

                    task_funcs: list[Callable[[], Awaitable[Any]]] = []
                    stop = False
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
                                task_funcs.append(
                                    lambda task=task: anyio.to_thread.run_sync(
                                        self._wait_for_completion, task
                                    )
                                )

                            case _StopWorker():
                                stop = True
                                break

                            case _:
                                pass

                    if stop:
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
    ) -> None:
        async with task_send:
            await anyio.to_thread.run_sync(self._queue_reader, task_send)

    def _queue_reader(
        self,
        task_send: MemoryObjectSendStream[_TaskSequence],
    ) -> None:
        while True:
            task = self.task_queue.get()  # blocks here — fine, it's a thread
            anyio.from_thread.run(task_send.send, task)

            if isinstance(task, _StopWorker):
                break

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
        model_hash = to_pg_integer(hash(task.target_table))
        chunks = {
            idx: list(chunk)
            for idx, chunk in enumerate(batched(task.keys, chunk_size, strict=False))
        }
        chunks_complete: dict[int, bool] = {idx: False for idx in chunks}

        async def update_chunk(chunk_id: int) -> None:
            async with pool.connection() as conn, conn.cursor() as cur:
                try:
                    await cur.execute(
                        sql.SQL("\n").join(
                            sql.SQL("SELECT pg_advisory_xact_lock({}, {});").format(
                                model_hash, to_pg_integer(hash(x))
                            )
                            for x in chunks[chunk_id]
                        ),
                        prepare=False,
                        binary=False,
                    )
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
            while incomplete_chunks := [
                id for id, complete in chunks_complete.items() if not complete
            ]:
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
                        attempt,
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

    def _wait_for_completion(self, task: _WaitForPartitionComplete) -> None:
        while any(
            task
            for task in self._running_tasks
            if isinstance(task, _LoadPartitionTask)
            and task.partition.name == task.partition.name
            and task.model == task.model
        ):
            anyio.from_thread.check_cancelled()
            time.sleep(0.05)  # poll interval

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
        self._started_event: Event = self._manager.Event()
        self._shutdown_event: Event = self._manager.Event()
        self._worker: _LoadingBatchWorker | None = None
        self._conn_str = conn_str

    @property
    def client(self) -> LoadingBatchWorkerClient:
        return LoadingBatchWorkerClient(self._task_queue, self._manager.address)

    def start(self) -> None:
        """Spawn the worker process and wait until it signals readiness."""
        if self._worker is not None and self._worker.is_alive():
            return

        self._started_event.clear()
        self._shutdown_event.clear()

        self._worker = _LoadingBatchWorker(
            task_queue=self._task_queue,
            started_event=self._started_event,
            shutdown_event=self._shutdown_event,
            root_logger=cast(Logger, logger),
            conn_str=self._conn_str,
        )
        self._worker.start()

        # Block until the worker signals it has started
        self._started_event.wait()

        logger.info("LoadingBatchWorker signaled startup")

    def cleanup(self, timeout: float = 5.0) -> None:
        """Gracefully shut down the worker process and release resources."""
        if self._worker is None:
            return

        # 1. Signal via the Event (fast path)
        self._shutdown_event.set()

        # 2. Also push a sentinel so the worker exits even if it's
        #    blocked in queue.get() — two-pronged approach
        self._task_queue.put([_StopWorker()])

        # 3. Join with timeout
        self._worker.join(timeout=timeout)

        if self._worker.is_alive():
            logger.warning("Worker did not exit gracefully, terminating.")
            self._worker.terminate()
            self._worker.join(timeout=1.0)

        logger.info("LoadingBatchWorker cleanup complete")
