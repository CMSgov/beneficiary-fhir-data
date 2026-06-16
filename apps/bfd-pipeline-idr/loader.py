import itertools
import operator
from collections.abc import Awaitable, Callable, Iterator, Sequence
from datetime import UTC, datetime
from typing import Any, cast

import anyio
import psycopg
import psycopg_pool
from loguru import logger
from psycopg.abc import Params, QueryNoTemplate
from psycopg.errors import DeadlockDetected, InFailedSqlTransaction
from psycopg.rows import DictRow, dict_row
from psycopg_pool.abc import ACT

from batch_worker import LoadingBatch, LoadingBatchWorkerClient
from constants import DEFAULT_MIN_DATE
from db_utils import get_connection_string
from load_partition import LoadPartition, LoadType
from model.base_model import DbType, IdrBaseModel, LoadMode, T
from model.load_progress import LoadProgress
from settings import (
    PER_BATCH_CONCURRENT_ROWS,
    PER_BATCH_MAX_CONNECTIONS,
    PER_BATCH_MIN_CONNECTIONS,
    force_load_progress,
)
from timer import Timer


class PostgresLoader:
    def load(
        self,
        fetch_results: Iterator[list[T]],
        model: type[T],
        job_start: datetime,
        partition: LoadPartition,
        progress: LoadProgress | None,
        load_type: LoadType,
        load_mode: LoadMode,
        worker_client: LoadingBatchWorkerClient,
    ) -> bool:
        return anyio.run(
            self._async_load,
            fetch_results,
            model,
            job_start,
            partition,
            progress,
            load_type,
            load_mode,
            worker_client,
        )

    async def _async_load(
        self,
        fetch_results: Iterator[list[T]],
        model: type[T],
        job_start: datetime,
        partition: LoadPartition,
        progress: LoadProgress | None,
        load_type: LoadType,
        load_mode: LoadMode,
        worker_client: LoadingBatchWorkerClient,
    ) -> bool:
        async with psycopg_pool.AsyncConnectionPool(
            conninfo=get_connection_string(load_mode),
            min_size=PER_BATCH_MIN_CONNECTIONS,
            max_size=PER_BATCH_MAX_CONNECTIONS,
            # Testing both psycopg and asyncpg by introducing a Timer for the statement that
            # acquires a connection from either library's implementation of a pool showed that
            # the majority of the time spent was actually in acquiring a connection, _not_ the
            # upsert queries themselves. We were unable to determine why this was the case, and
            # there is little to no information online about this behavior. Thus, we need to
            # increase the pool timeout or some partitions will fail to load. It does not seem to
            # matter whether we use a pool or not, either
            # TODO: Investigate pool timeout further so that this can be removed
            timeout=600,
        ) as pool:
            await pool.wait()
            return await BatchLoader(
                fetch_results,
                model,
                pool,
                job_start,
                partition,
                progress,
                load_type,
                load_mode,
                worker_client,
            ).load()


class BatchLoader:
    def __init__(
        self,
        fetch_results: Iterator[list[T]],
        model: type[T],
        pool: psycopg_pool.AsyncConnectionPool[ACT],
        job_start: datetime,
        partition: LoadPartition,
        progress: LoadProgress | None,
        load_type: LoadType,
        load_mode: LoadMode,
        worker_client: LoadingBatchWorkerClient,
    ) -> None:
        self.pool = pool
        self.fetch_results = fetch_results
        self.model = model
        self.worker_client = worker_client
        self.table = model.table()
        # trim the schema from the table name to create the temp table
        # (temp tables can't be created with an explicit schema set)
        self.temp_table = model.table().split(".")[1] + "_temp"
        self.job_start = job_start
        self.batch_start = datetime.now(UTC)
        self.insert_cols = list(model.insert_keys())
        self.insert_cols.sort()
        self.immutable = not model.update_timestamp_col()
        self.meta_keys = (
            ["bfd_created_ts"] if self.immutable else ["bfd_created_ts", "bfd_updated_ts"]
        )
        self.cols_str = ", ".join(self.insert_cols)
        self.meta_keys_str = ", ".join(self.meta_keys)
        self.ordered_pkeys = model.ordered_pkeys()
        self.primary_keys_str = ", ".join(self.ordered_pkeys)
        self.update_set = [v for v in self.insert_cols if v not in model.ordered_pkeys()]
        self.update_set_str = ", ".join([f"{v}=EXCLUDED.{v}" for v in self.update_set])
        self.where_clause = (
            f"WHERE ({', '.join(f't.{v}' for v in self.update_set)}) IS "
            f"DISTINCT FROM ({', '.join(f'EXCLUDED.{v}' for v in self.update_set)})"
        )
        # For immutable tables, we may still be attempting to re-load some data
        # due to a batch cancellation.
        # In these cases, we can assume any conflicting rows have already been loaded so
        # "DO NOTHING" is appropriate here.
        # Additionally, if there are no extra columns to update, we can skip it.
        self.on_conflict_clause = (
            "DO NOTHING"
            if self.immutable or not self.update_set
            else (
                f"DO UPDATE SET {self.update_set_str}, bfd_updated_ts=%(timestamp)s "
                f"{self.where_clause}"
            )
        )
        self.timestamp_placeholders = ", ".join("%(timestamp)s" for _ in self.meta_keys)

        self.partition = partition
        self.progress = progress
        self.progress_start_timer = Timer("progress_start", model, partition)
        self.idr_query_timer = Timer("idr_query", model, partition)
        self.insert_batch_timer = Timer("insert_batch", model, partition)
        self.sort_batch_timer = Timer("sort_batch", model, partition)
        self.full_batch_timer = Timer("full_batch", model, partition)
        self.full_load_timer = Timer("full_load", model, partition)
        self.load_type = load_type
        self.enable_load_progress = should_track_load_progress(load_mode)

    async def load(self) -> bool:
        timestamp = datetime.now(UTC)

        self.full_load_timer.start()
        async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
            self.progress_start_timer.start()
            await self._insert_batch_start(cur)
            await conn.commit()
            self.progress_start_timer.stop()

        data_loaded = False
        num_rows = 0
        while True:
            self.idr_query_timer.start()
            # We unfortunately need to use a while true loop here since we need to wrap the
            # iterator with the timer calls.
            results = next(self.fetch_results, None)
            self.idr_query_timer.stop()
            if not results:
                break

            self.full_batch_timer.start()
            data_loaded = True
            logger.info(
                "{}-{}: loading next {} results concurrently {} row(s) at a time",
                self.table,
                self.partition.name,
                len(results),
                PER_BATCH_CONCURRENT_ROWS,
            )
            num_rows += len(results)

            self.sort_batch_timer.start()
            results.sort(key=operator.attrgetter(*self.ordered_pkeys))
            self.sort_batch_timer.stop()

            self.insert_batch_timer.start()
            updated_rows: list[IdrBaseModel] = []

            async def _store_updates(
                load_func: Callable[[], Awaitable[list[T]]], updated_rows: list[T] = updated_rows
            ) -> None:
                updated_rows.extend(await load_func())

            async with anyio.create_task_group() as tg:
                for idx, chunk in enumerate(
                    itertools.batched(results, PER_BATCH_CONCURRENT_ROWS, strict=False)
                ):

                    async def _wrap_batch_chunk(
                        idx: int = idx, chunk: Sequence[T] = chunk
                    ) -> list[T]:
                        return await self._load_batch_chunk(idx, chunk, timestamp)

                    tg.start_soon(
                        _store_updates,
                        _wrap_batch_chunk,
                        name=f"{self.table}-{self.partition.name}-{idx}",
                    )
            logger.info(
                "{}-{}: upserted {} new/changed row(s) out of {}",
                self.table,
                self.partition.name,
                len(updated_rows),
                len(results),
            )
            self.insert_batch_timer.stop()

            if (
                self.model.last_updated_date_table()
                # This clause ensures that tables like idr.beneficiary or any of the claims
                # tables do not unnecessarily submit additional last updated updates to the
                # queue since the upsert already sets their last updated columns
                and self.model.last_updated_date_table() != self.model.table()
            ):
                self.worker_client.do_last_updated(
                    LoadingBatch(
                        self.model,
                        self.partition,
                        self.progress,
                        cast(list[IdrBaseModel], results),
                        updated_rows,
                        timestamp,
                    ),
                    self.enable_load_progress,
                )
            elif self.enable_load_progress:
                self.worker_client.do_load_progress(
                    LoadingBatch(
                        self.model,
                        self.partition,
                        self.progress,
                        cast(list[IdrBaseModel], results),
                        updated_rows,
                        timestamp,
                    )
                )

            self.full_batch_timer.stop()

        # Wait until the background worker signals that all pending loading tasks are completed
        # for the current partition before marking it totally complete
        self.worker_client.wait_until_done(self.model, self.partition)

        async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
            await self._mark_batch_complete(cur)
            await conn.commit()

        self.full_load_timer.stop()
        logger.info("{}-{}: loaded {} rows", self.table, self.partition.name, num_rows)
        return data_loaded

    async def _load_batch_chunk(
        self, batch_num: int, chunk: Sequence[T], timestamp: datetime
    ) -> list[T]:
        async with self.pool.connection() as conn:
            max_attempts = 15
            for attempt in range(max_attempts):
                try:
                    async with conn.cursor(binary=True, row_factory=dict_row) as cur:
                        full_temp_table = await self._setup_temp_table(
                            cur, f"{self.partition.name}_{batch_num}"
                        )

                        await self._copy_data(cur, full_temp_table, chunk)

                        # Upsert into the main table
                        return await self._upsert(cur, full_temp_table, timestamp, chunk)
                except DeadlockDetected, InFailedSqlTransaction:
                    await conn.rollback()

                    if attempt == max_attempts - 1:
                        raise

                    await anyio.sleep(0.01)

        return []

    async def _insert_batch_start(self, cur: psycopg.AsyncCursor) -> None:
        await self._update_load_progress(
            cur,
            f"""
            INSERT INTO idr.load_progress(
                table_name,
                last_ts,
                last_id,
                batch_partition,
                job_start_ts,
                batch_start_ts,
                batch_complete_ts)
            VALUES(
                %(table)s,
                '{DEFAULT_MIN_DATE}',
                0,
                %(partition)s,
                %(job_start_ts)s,
                %(batch_start_ts)s,
                '{DEFAULT_MIN_DATE}'
            )
            ON CONFLICT (table_name, batch_partition) DO UPDATE
            SET
                job_start_ts = EXCLUDED.job_start_ts,
                batch_start_ts = EXCLUDED.batch_start_ts
            """,
            {
                "table": self.table,
                "partition": self.partition.name,
                "job_start_ts": self.job_start,
                "batch_start_ts": self.batch_start,
            },
        )

    async def _mark_batch_complete(self, cur: psycopg.AsyncCursor) -> None:
        await self._update_load_progress(
            cur,
            """
            UPDATE idr.load_progress
            SET batch_complete_ts = NOW()
            WHERE table_name = %(table)s AND batch_partition = %(batch_partition)s
            """,
            {"table": self.table, "batch_partition": self.partition.name},
        )

    async def _setup_temp_table(
        self, cur: psycopg.AsyncCursor[Any], suffix: str | None = None
    ) -> str:
        # Load each batch into a temp table
        # This is necessary because we want to use COPY to quickly
        # transfer everything into Postgres, but COPY can't handle
        # constraint conflicts natively.
        #
        # Note that temp tables don't use WAL so that helps with throughput as well.
        #
        # For simplicity's sake, we'll create our temp tables using the existing schema and
        # just drop the columns we need to ignore.
        full_tablename = f"{self.temp_table}_{suffix or ''}"
        await cur.execute(
            f'CREATE TEMPORARY TABLE "{full_tablename}" (LIKE {self.table}) '  # type: ignore
            "ON COMMIT DROP"
        )
        # Created/updated columns don't need to be loaded from the source.
        for col in self.meta_keys:
            await cur.execute(f'ALTER TABLE "{full_tablename}" DROP COLUMN {col}')  # type: ignore

        return full_tablename

    async def _update_load_progress(
        self, cur: psycopg.AsyncCursor[Any], query: QueryNoTemplate, params: Params | None
    ) -> None:
        if self.enable_load_progress:
            await cur.execute(query, params)  # type: ignore
            await cur.connection.commit()

    async def _upsert(
        self,
        cur: psycopg.AsyncCursor[DictRow],
        temp_tablename: str,
        timestamp: datetime,
        chunk: Sequence[T],
    ) -> list[T]:
        # Upsert into the main table
        if self.model.should_replace():
            # Delete before inserting since we've specified that the data should be
            # replaced rather than merged.
            # Note that this is executed within a transaction,
            # so consumers won't see an empty table.
            await cur.execute(f"DELETE FROM {self.table}")  # type: ignore
        await cur.execute("SET LOCAL synchronous_commit TO OFF")
        await cur.execute(
            f'''
            INSERT INTO {self.table} AS t ({self.cols_str}, {self.meta_keys_str})
            SELECT {self.cols_str}, {self.timestamp_placeholders} FROM "{temp_tablename}"
            ON CONFLICT ({self.primary_keys_str}) {self.on_conflict_clause}
            RETURNING {", ".join(self.model.ordered_pkeys())}
            ''',  # type: ignore
            {"timestamp": timestamp},
        )

        # We cannot type adapt (using TypeAdapter) the RETURNING (e.g. if we use RETURNING *) to
        # get the list of updated rows out of the original rows because some non-optional columns
        # are not inserted into the database. So, we just return the primary key values for
        # each updated row and match against the original list to get the list of updated rows
        updated_pkeys = await cur.fetchall()
        return [
            row
            for row in chunk
            if any(all(getattr(row, k) == v for k, v in pkey.items()) for pkey in updated_pkeys)
        ]

    async def _copy_data(
        self, cur: psycopg.AsyncCursor[Any], temp_tablename: str, data: Sequence[T]
    ) -> None:
        # Use COPY to load the batch into Postgres.
        # COPY has a number of optimizations that make bulk loading more efficient
        # than a bunch of INSERTs.
        # The entire operation is performed in a single statement, resulting in
        # fewer network round-trips, less WAL activity, and less context switching.

        # Even though we need to move the data from the temp table in the next step,
        # it should still be faster than alternatives.
        async with cur.copy(
            f'COPY "{temp_tablename}" ({self.cols_str}) FROM STDIN'  # type: ignore
        ) as copy:
            for row in data:
                await copy.write_row(
                    [_remove_null_bytes(getattr(row, k)) for k in self.insert_cols]
                )


def _remove_null_bytes(val: DbType) -> DbType:
    # Some IDR strings have null bytes.
    # Postgres doesn't allow these in text fields.
    # We can't use a UTF-8 validator here since technically these are valid UTF-8
    # and we can't use string.printable because that only contains ASCII fields
    # so neither of those validation techniques will remove null bytes
    # and still allow other valid UTF-8 characters.
    if type(val) is str:
        return val.replace("\x00", "")
    return val


def should_track_load_progress(load_mode: LoadMode) -> bool:
    # Whether to read/write load progress, which is diabled for synthetic and testing loads.
    return load_mode == LoadMode.PROD or force_load_progress()
