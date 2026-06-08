from __future__ import annotations

from collections import OrderedDict
from collections.abc import Sequence
from dataclasses import dataclass
from datetime import datetime
from multiprocessing import Manager, Process
from queue import Queue

import anyio
import anyio.from_thread
import anyio.to_thread
import psycopg_pool
from anyio.streams.memory import MemoryObjectSendStream
from loguru import logger
from loguru._logger import Logger
from psycopg import sql
from psycopg_pool.abc import ACT

from db_utils import get_connection_string
from load_partition import LoadPartition
from model.base_model import DbType, IdrBaseModel, LoadMode, T
from settings import PER_BATCH_MAX_CONNECTIONS, PER_BATCH_MIN_CONNECTIONS
from timer import Timer

type LastUpdatedQueue = Queue[LastUpdatedBatch | None]


_WORKER_START_SIGNAL = 0


@dataclass
class LastUpdatedBatch:
    model: type[IdrBaseModel]
    partition: LoadPartition
    keys: list[DbType]
    timestamp: datetime

    @classmethod
    def from_result_set(
        cls, data: Sequence[T], partition: LoadPartition, timestamp: datetime
    ) -> LastUpdatedBatch:
        if not data:
            raise ValueError("data must not be empty")

        model_type = type(data[0])
        key = model_type.last_updated_timestamp_col()
        if not key:
            raise ValueError("model type of data does not support last updated")

        return LastUpdatedBatch(
            model=model_type,
            partition=partition,
            keys=list(OrderedDict.fromkeys(getattr(x, key) for x in data)),
            timestamp=timestamp,
        )


async def _handle_batch(
    batch: LastUpdatedBatch,
    key: str,
    pool: psycopg_pool.AsyncConnectionPool[ACT],
    limiter: anyio.CapacityLimiter,
) -> None:
    async with limiter, pool.connection() as conn, conn.cursor() as cur:
        logger.info(
            "Executing last_updated for {}-{} with {} key(s)",
            batch.model.table(),
            batch.partition.name,
            len(batch.keys),
        )
        last_updated_set_clause = sql.SQL(", ").join(
            t"{col:i} = {batch.timestamp}" for col in batch.model.last_updated_date_column()
        )
        schema, table = batch.model.last_updated_date_table().split(".", 1)
        full_table = sql.Identifier(schema, table)
        last_updated_timer = Timer("last_updated_bg", batch.model, batch.partition)
        last_updated_timer.start()
        await cur.execute(
            t"""
            UPDATE {full_table:i} u
            SET {last_updated_set_clause:q}
            WHERE u.{key:i} = ANY({batch.keys});
            """,
        )
        last_updated_timer.stop()


def _queue_reader(
    batch_queue: LastUpdatedQueue,
    send_stream: MemoryObjectSendStream[LastUpdatedBatch | None],
) -> None:
    while True:
        batch = batch_queue.get()  # blocks here — fine, it's a thread
        anyio.from_thread.run(send_stream.send, batch)

        if batch is None:
            break


async def _run_bridge(
    batch_queue: LastUpdatedQueue,
    send_stream: MemoryObjectSendStream[LastUpdatedBatch | None],
) -> None:
    async with send_stream:
        await anyio.to_thread.run_sync(_queue_reader, batch_queue, send_stream)


async def _worker_main(
    batch_queue: LastUpdatedQueue, start_queue: Queue[int], load_mode: LoadMode
) -> None:
    send_stream, receive_stream = anyio.create_memory_object_stream[LastUpdatedBatch | None](
        PER_BATCH_MAX_CONNECTIONS
    )
    limiter = anyio.CapacityLimiter(PER_BATCH_MAX_CONNECTIONS)

    async with (
        anyio.create_task_group() as tg,
        psycopg_pool.AsyncConnectionPool(
            conninfo=get_connection_string(load_mode),
            min_size=PER_BATCH_MIN_CONNECTIONS,
            max_size=PER_BATCH_MAX_CONNECTIONS,
            timeout=600,  # See loader.py for explanation on timeout length
        ) as pool,
    ):
        await pool.wait()

        tg.start_soon(_run_bridge, batch_queue, send_stream)

        start_queue.put(_WORKER_START_SIGNAL)
        logger.info(
            "last_updated worker setup with max concurrency of {}", PER_BATCH_MAX_CONNECTIONS
        )

        async with receive_stream:
            async for batch in receive_stream:
                if batch is None:
                    break

                if batch.model.last_updated_date_table() and (
                    key := batch.model.last_updated_timestamp_col()
                ):
                    tg.start_soon(_handle_batch, batch, key, pool, limiter)


def _worker_start_async(
    batch_queue: LastUpdatedQueue,
    start_queue: Queue[int],
    load_mode: LoadMode,
    root_logger: Logger,
) -> None:
    root_logger.reinstall()
    anyio.run(_worker_main, batch_queue, start_queue, load_mode)


def start_last_updated_worker(
    load_mode: LoadMode, root_logger: Logger
) -> tuple[Process, LastUpdatedQueue]:
    manager = Manager()
    batch_queue: LastUpdatedQueue = manager.Queue()
    start_queue: Queue[int] = manager.Queue()

    logger.info("Starting last_updated worker process...")
    worker = Process(
        target=_worker_start_async,
        args=(batch_queue, start_queue, load_mode, root_logger),
    )
    worker.start()

    _ = start_queue.get(timeout=10)  # We don't care about the value, just that there IS a value

    logger.info("last_updated worker process signaled startup")

    return (worker, batch_queue)
