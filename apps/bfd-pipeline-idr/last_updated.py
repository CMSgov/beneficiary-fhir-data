import os
from collections import OrderedDict
from collections.abc import Sequence
from dataclasses import dataclass
from datetime import datetime
from itertools import batched, chain
from math import ceil
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
from psycopg.errors import DeadlockDetected, InFailedSqlTransaction
from psycopg_pool.abc import ACT

from db_utils import get_connection_string, to_pg_integer
from load_partition import LoadPartition
from model.base_model import DbType, IdrBaseModel, LoadMode, T
from settings import PER_BATCH_MAX_CONNECTIONS, PER_BATCH_MIN_CONNECTIONS
from timer import Timer

type LastUpdatedQueue = Queue[LastUpdatedBatch | None]


_WORKER_START_SIGNAL = 0
_MAX_LAST_UPDATED_CHUNKS_PER_BATCH = 100
_MINIMUM_LAST_UPDATED_CHUNK_SIZE = 100


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
    key_col: str,
    pool: psycopg_pool.AsyncConnectionPool[ACT],
    limiter: anyio.CapacityLimiter,
    send_stream: MemoryObjectSendStream[LastUpdatedBatch | None],
) -> None:
    num_keys = len(batch.keys)
    keys_per_chunk = ceil(num_keys / _MINIMUM_LAST_UPDATED_CHUNK_SIZE)
    num_chunks = min(keys_per_chunk, _MAX_LAST_UPDATED_CHUNKS_PER_BATCH)
    chunk_size = max(keys_per_chunk, _MINIMUM_LAST_UPDATED_CHUNK_SIZE)
    logger.info(
        "Executing last_updated for {}-{} with {} key(s) in {} chunk(s) of {} key(s) each",
        batch.model.table(),
        batch.partition.name,
        num_keys,
        num_chunks,
        chunk_size,
    )
    last_updated_set_clause = sql.SQL(", ").join(
        t"{col:i} = {batch.timestamp}" for col in batch.model.last_updated_date_column()
    )
    schema, table = batch.model.last_updated_date_table().split(".", 1)
    full_table = sql.Identifier(schema, table)
    model_hash = to_pg_integer(hash(batch.model.last_updated_date_table()))
    chunks = {
        idx: list(chunk) for idx, chunk in enumerate(batched(batch.keys, chunk_size, strict=False))
    }
    chunk_results: dict[int, bool] = {}

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
                    WHERE u.{key_col:i} = ANY({chunks[chunk_id]});
                    """,
                )
                chunk_results[chunk_id] = True
            except DeadlockDetected, InFailedSqlTransaction:
                await conn.rollback()
                chunk_results[chunk_id] = False

    async with limiter:
        last_updated_timer = Timer("last_updated_bg", batch.model, batch.partition)
        last_updated_timer.start()
        async with anyio.create_task_group() as tg:
            for chunk_id in chunks:
                tg.start_soon(update_chunk, chunk_id)
        last_updated_timer.stop()

        failed_chunks = [k for k, v in chunk_results.items() if not v]
        if failed_chunks:
            logger.warning(
                "{} key chunks failed to be updated for {}-{}, resubmitting them to queue",
                len(failed_chunks),
                batch.model.table(),
                batch.partition.name,
            )
            await send_stream.send(
                LastUpdatedBatch(
                    batch.model,
                    batch.partition,
                    sorted(chain.from_iterable(chunks[id] for id in failed_chunks)),
                    batch.timestamp,
                )
            )


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
                    tg.start_soon(_handle_batch, batch, key, pool, limiter, send_stream)


def _worker_start_async(
    batch_queue: LastUpdatedQueue,
    start_queue: Queue[int],
    load_mode: LoadMode,
    root_logger: Logger,
) -> None:
    root_logger.reinstall()
    try:
        anyio.run(_worker_main, batch_queue, start_queue, load_mode)
    except Exception:
        logger.opt(exception=True).error("last_updated worker process died:")
        os._exit(1)  # Force an exit because the pipeline should stop if the background worker does


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
