import itertools
import operator
from collections.abc import Iterator, Sequence
from datetime import UTC, date, datetime

import anyio
import psycopg
import psycopg_pool
from loguru import logger
from psycopg import sql
from psycopg.abc import Params, QueryNoTemplate
from psycopg.errors import DeadlockDetected, InFailedSqlTransaction
from psycopg_pool.abc import ACT

from constants import DEFAULT_MIN_DATE
from db_utils import get_connection_string, to_pg_integer
from last_updated import LastUpdatedBatch, LastUpdatedQueue
from load_partition import LoadPartition, LoadType
from model.base_model import DbType, LoadMode, T
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
        last_updated_queue: LastUpdatedQueue | None,
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
            last_updated_queue,
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
        last_updated_queue: LastUpdatedQueue | None,
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
                last_updated_queue,
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
        last_updated_queue: LastUpdatedQueue | None,
    ) -> None:
        self.pool = pool
        self.fetch_results = fetch_results
        self.model = model
        self.last_updated_queue = last_updated_queue
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
        self.last_updated_set_clause = ", ".join(
            f"{col} = %(timestamp)s" for col in self.model.last_updated_date_column()
        )
        self.timestamp_placeholders = ", ".join("%(timestamp)s" for _ in self.meta_keys)

        self.batch_timestamp_cols = model.batch_timestamp_col(
            progress is None or progress.is_historical()
        )
        self.partition = partition
        self.progress = progress
        self.progress_start_timer = Timer("progress_start", model, partition)
        self.idr_query_timer = Timer("idr_query", model, partition)
        self.insert_batch_timer = Timer("insert_batch", model, partition)
        self.sort_batch_timer = Timer("sort_batch", model, partition)
        self.last_updated_queue_timer = Timer("last_updated_queue", model, partition)
        self.full_batch_timer = Timer("full_batch", model, partition)
        self.full_load_timer = Timer("full_load", model, partition)
        self.load_type = load_type
        self.enable_load_progress = should_track_load_progress(load_mode)

    async def load(
        self,
    ) -> bool:
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
            async with anyio.create_task_group() as tg:
                for idx, part in enumerate(
                    itertools.batched(results, PER_BATCH_CONCURRENT_ROWS, strict=False)
                ):
                    tg.start_soon(
                        self._load_batch_part,
                        idx,
                        part,
                        timestamp,
                        name=f"{self.table}-{self.partition.name}-{idx}",
                    )
            self.insert_batch_timer.stop()

            async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
                if (
                    self.last_updated_queue is not None
                    and self.model.last_updated_date_table()
                    # This clause ensures that tables like idr.beneficiary or any of the claims
                    # tables do not unnecessarily submit additional last updated updates to the
                    # queue since the upsert already sets their last updated columns
                    and self.model.last_updated_date_table() != self.model.table()
                ):
                    self.last_updated_queue_timer.start()
                    self.last_updated_queue.put(
                        LastUpdatedBatch.from_result_set(results, self.partition, timestamp)
                    )
                    self.last_updated_queue_timer.stop()

                await self._calculate_load_progress(cur, results)
            self.full_batch_timer.stop()

        async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
            await self._mark_batch_complete(cur)
            await conn.commit()

        self.full_load_timer.stop()
        logger.info("{}-{}: loaded {} rows", self.table, self.partition.name, num_rows)
        return data_loaded

    async def _load_batch_part(
        self, batch_num: int, batch_part: Sequence[T], timestamp: datetime
    ) -> None:
        async with self.pool.connection() as conn:
            max_attempts = 15
            for attempt in range(max_attempts):
                try:
                    async with conn.cursor(binary=True) as cur:
                        full_temp_table = await self._setup_temp_table(
                            cur, f"{self.partition.name}_{batch_num}"
                        )

                        await self._copy_data(cur, full_temp_table, batch_part)

                        if self.last_updated_queue and (
                            timestamp_pkey_col := self.model.last_updated_timestamp_col()
                        ):
                            await self._advisory_locks(cur, batch_part, timestamp_pkey_col)

                        # Upsert into the main table
                        await self._upsert(cur, full_temp_table, timestamp)
                        break
                except DeadlockDetected, InFailedSqlTransaction:
                    await conn.rollback()

                    if attempt == max_attempts - 1:
                        raise

                    await anyio.sleep(0.01)

            await conn.commit()

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

    async def _setup_temp_table(self, cur: psycopg.AsyncCursor, suffix: str | None = None) -> str:
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

    async def _calculate_load_progress(self, cur: psycopg.AsyncCursor, results: list[T]) -> None:
        last = results[len(results) - 1].model_dump()
        # Some tables that contain reference data (like contract info) may not have the
        # normal IDR timestamps.
        # For now we won't support incremental refreshes for those tables
        batch_timestamp_cols = self.model.batch_timestamp_col(
            self.progress is None or self.progress.is_historical()
        )
        update_cols = self.model.update_timestamp_col()
        if batch_timestamp_cols:
            max_timestamp = max(
                [
                    _convert_date(last[col])
                    for col in [*self.batch_timestamp_cols, *update_cols]
                    if last[col] is not None
                ]
            )
            batch_id_col = self.model.batch_id_col()
            batch_id = last[batch_id_col] if batch_id_col else 0
            await self._update_load_progress(
                cur,
                """
                UPDATE idr.load_progress
                SET
                    last_ts = %(last_ts)s,
                    last_id = %(last_id)s
                WHERE
                    table_name = %(table)s
                    AND batch_partition = %(partition)s
                """,
                {
                    "table": self.table,
                    "partition": self.partition.name,
                    "last_ts": max_timestamp,
                    "last_id": batch_id,
                },
            )

    async def _update_load_progress(
        self, cur: psycopg.AsyncCursor, query: QueryNoTemplate, params: Params | None
    ) -> None:
        if self.enable_load_progress:
            await cur.execute(query, params)  # type: ignore
            await cur.connection.commit()

    async def _advisory_locks(
        self, cur: psycopg.AsyncCursor, data: Sequence[T], timestamp_pkey_col: str
    ) -> None:
        # The pg_advisory_xact_lock accepts only "integer" types, not even bigint. Hence,
        # to_integer and hash
        model_hash = to_pg_integer(hash(self.model.last_updated_date_table()))
        await cur.execute(
            sql.SQL("\n").join(
                sql.SQL("SELECT pg_advisory_xact_lock({}, {});").format(
                    model_hash, to_pg_integer(hash(getattr(x, timestamp_pkey_col)))
                )
                for x in data
            ),
            prepare=False,
            binary=False,
        )

    async def _upsert(
        self, cur: psycopg.AsyncCursor, temp_tablename: str, timestamp: datetime
    ) -> None:
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
            ''',  # type: ignore
            {"timestamp": timestamp},
        )

    async def _copy_data(
        self, cur: psycopg.AsyncCursor, temp_tablename: str, data: Sequence[T]
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


def _convert_date(date_field: date | datetime) -> datetime:
    if type(date_field) is datetime:
        return date_field.replace(tzinfo=UTC)
    return datetime.combine(date_field, datetime.min.time()).replace(tzinfo=UTC)


def should_track_load_progress(load_mode: LoadMode) -> bool:
    # Whether to read/write load progress, which is diabled for synthetic and testing loads.
    return load_mode == LoadMode.PROD or force_load_progress()
