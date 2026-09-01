import functools
import itertools
import operator
from collections.abc import Awaitable, Callable, Iterator, Sequence
from datetime import UTC, datetime
from typing import Any, Generic, cast, override

import anyio
import psycopg
import psycopg_pool
from loguru import logger
from psycopg.abc import Params, QueryNoTemplate
from psycopg.errors import DeadlockDetected, InFailedSqlTransaction
from psycopg.rows import DictRow, dict_row
from psycopg_pool.abc import ACT

from .batch_worker import LoadingBatch, LoadingBatchWorkerClient
from .constants import DEFAULT_JOB_ID, DEFAULT_MIN_DATE
from .db_utils import get_connection_string
from .load_partition import LoadPartition, LoadType
from .model.base_model import DbType, IdrBaseModel, LoadMode, T
from .model.load_progress import LoadProgress
from .settings import SETTINGS
from .timer import Timer


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
        job_id: int,
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
            job_id,
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
        job_id: int,
    ) -> bool:
        async with psycopg_pool.AsyncConnectionPool(
            conninfo=get_connection_string(load_mode),
            min_size=SETTINGS.per_batch_min_connections,
            max_size=SETTINGS.per_batch_max_connections,
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
            loader_cls = FullSyncBatchLoader if model.should_delete_missing() else BatchLoader
            return await loader_cls(
                fetch_results,
                model,
                pool,
                job_start,
                partition,
                progress,
                load_type,
                load_mode,
                worker_client,
                job_id,
            ).load()


class BatchLoader(Generic[T]):  # noqa: UP046
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
        job_id: int,
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
        self.batch_start = resolve_test_date(load_mode)
        self.insert_cols = list(model.insert_keys())
        self.insert_cols.sort()
        self.meta_keys = (
            ["bfd_created_ts"] if model.is_immutable() else ["bfd_created_ts", "bfd_updated_ts"]
        )
        last_updated_col = self.model.last_updated_date_column()
        if last_updated_col:
            target_updated_date_table = self.model.last_updated_date_table()
            current_table = self.model.table()
            if current_table == target_updated_date_table:
                self.meta_keys.append(last_updated_col[0])
        self.cols_str = ", ".join(self.insert_cols)
        self.meta_keys_str = ", ".join(self.meta_keys)
        self.ordered_pkeys = model.ordered_pkeys()
        self.primary_keys_str = ", ".join(self.ordered_pkeys)
        update_set = [v for v in self.insert_cols if v not in self.ordered_pkeys]
        update_set_str = ", ".join([f"{v}=EXCLUDED.{v}" for v in update_set])
        on_conflict_where_clause = (
            f"WHERE ({', '.join(f't.{v}' for v in update_set)}) IS "
            f"DISTINCT FROM ({', '.join(f'EXCLUDED.{v}' for v in update_set)})"
        )
        self.npi_type_backfill_map = model.npi_type_backfill_compare_cols()
        self.npi_type_backfill_enabled = SETTINGS.enable_npi_type_backfill_compare and bool(
            self.npi_type_backfill_map
        )
        self.temp_only_cols = (
            list(self.npi_type_backfill_map) if self.npi_type_backfill_enabled else []
        )
        self.insert_and_temp_cols = self.insert_cols + self.temp_only_cols
        self.insert_and_temp_cols_str = ", ".join(self.insert_and_temp_cols)

        if self.npi_type_backfill_enabled:
            npi_type_real_cols = set(self.npi_type_backfill_map.values())
            non_npi_cols = [c for c in update_set if c not in npi_type_real_cols]
            other_changed_clause = (
                " OR ".join(f"t.{c} IS DISTINCT FROM EXCLUDED.{c}" for c in non_npi_cols)
                if non_npi_cols
                else "FALSE"
            )
            pkey_join = " AND ".join(f"tmp.{k} = EXCLUDED.{k}" for k in self.ordered_pkeys)
            legacy_versus_real_npi_types_check = " OR ".join(
                f"COALESCE(tmp.{legacy_col}, 0) IS DISTINCT FROM COALESCE(EXCLUDED.{real_col}, 0)"
                for legacy_col, real_col in self.npi_type_backfill_map.items()
            )
            npi_type_changed_clause = f"""
                EXISTS (
                        SELECT 1 
                        FROM "{{temp_tablename}}" tmp
                        WHERE {pkey_join} 
                        AND ({legacy_versus_real_npi_types_check})
                    )
            """
            updated_ts_expr = f"""
                CASE
                    WHEN {npi_type_changed_clause} THEN %(timestamp)s
                    WHEN ({other_changed_clause}) THEN %(timestamp)s
                    ELSE t.bfd_updated_ts
                END
            """

            on_conflict_where_clause = f"""
                WHERE (
                    ({", ".join(f"t.{v}" for v in update_set)}) 
                    IS DISTINCT FROM 
                    ({", ".join(f"EXCLUDED.{v}" for v in update_set)})
                )
                OR {npi_type_changed_clause}
            """

        else:
            updated_ts_expr = "%(timestamp)s"
            on_conflict_where_clause = (
                f"WHERE ({', '.join(f't.{v}' for v in update_set)}) IS "
                f"DISTINCT FROM ({', '.join(f'EXCLUDED.{v}' for v in update_set)})"
            )
        # For immutable tables, we may still be attempting to re-load some data
        # due to a batch cancellation.
        # In these cases, we can assume any conflicting rows have already been loaded so
        # "DO NOTHING" is appropriate here.
        # Additionally, if there are no extra columns to update, we can skip it.
        self.on_conflict_clause = (
            "DO NOTHING"
            if model.is_immutable() or not update_set
            else (
                f"DO UPDATE SET {update_set_str}, bfd_updated_ts={updated_ts_expr} "
                f"{on_conflict_where_clause}"
            )
        )
        # Used in _upsert so that relevant primary/last updated timestamp columns are returned for
        # rows that are actually updated during the upsert so that last updated can be ran for
        # just rows with changes during the load
        self.updated_keys_returning_str = ", ".join(
            {
                col
                for col in [
                    *self.ordered_pkeys,
                    self.model.last_updated_timestamp_col(),
                ]
                if col
            }
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
        self.load_mode = load_mode
        self.enable_load_progress = should_track_load_progress(load_mode)
        self.job_id = job_id

    async def load(self) -> bool:
        timestamp = resolve_test_date(self.load_mode)

        self.full_load_timer.start()
        async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
            await self._record_batch_start(conn, cur, commit=True)

        batch_num = 1

        async def _process_batch(results: list[T]) -> None:
            nonlocal batch_num
            self.full_batch_timer.start()
            logger.info(
                "{}-{}-{}: loading next {} results concurrently {} row(s) at a time",
                self.table,
                self.partition.name,
                batch_num,
                len(results),
                SETTINGS.per_batch_concurrent_rows,
            )
            self.sort_batch_timer.start()
            results.sort(key=operator.attrgetter(*self.ordered_pkeys))
            self.sort_batch_timer.stop()

            self.insert_batch_timer.start()
            updated_keys: list[dict[str, DbType]] = []

            async def _store_updated_keys(
                load_func: Callable[[], Awaitable[list[dict[str, DbType]]]],
                updated_rows: list[dict[str, DbType]] = updated_keys,
            ) -> None:
                updated_rows.extend(await load_func())

            async with anyio.create_task_group() as tg:
                for idx, chunk in enumerate(
                    itertools.batched(results, SETTINGS.per_batch_concurrent_rows, strict=False)
                ):

                    async def _wrap_batch_chunk(
                        idx: int = idx, chunk: Sequence[T] = chunk
                    ) -> list[dict[str, DbType]]:
                        return await self._load_batch_chunk(idx, chunk, timestamp)

                    tg.start_soon(
                        _store_updated_keys,
                        _wrap_batch_chunk,
                        name=f"{self.table}-{self.partition.name}-{idx}",
                    )
            self.insert_batch_timer.stop()
            logger.info(
                "{}-{}-{}: upserted {} new/changed row(s) out of {}",
                self.table,
                self.partition.name,
                batch_num,
                len(updated_keys),
                len(results),
            )

            cur_batch = LoadingBatch(
                batch_num,
                self.model,
                self.partition,
                self.progress,
                cast(list[IdrBaseModel], results),
                updated_keys,
                timestamp,
            )
            if self.model.last_updated_date_table():
                self.worker_client.do_last_updated(cur_batch, self.enable_load_progress)
            elif self.enable_load_progress:
                self.worker_client.do_load_progress(cur_batch)

            batch_num += 1
            self.full_batch_timer.stop()

        num_rows = await self._stage_all_batches(_process_batch)
        data_loaded = num_rows > 0

        # Wait until the background worker signals that all pending loading tasks are completed
        # for the current partition before marking it totally complete
        self.worker_client.wait_until_done(self.model, self.partition)

        async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
            await self._mark_batch_complete(cur)
            await conn.commit()

        self.full_load_timer.stop()
        logger.info("{}-{}: finished processing {} rows", self.table, self.partition.name, num_rows)
        return data_loaded

    async def _load_batch_chunk(
        self, batch_num: int, chunk: Sequence[T], timestamp: datetime
    ) -> list[dict[str, DbType]]:
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
                        return await self._upsert(cur, full_temp_table, timestamp)
                except DeadlockDetected, InFailedSqlTransaction:
                    await conn.rollback()

                    if attempt == max_attempts - 1:
                        raise

                    await anyio.sleep(0.01)

        return []

    async def _insert_batch_start(self, cur: psycopg.AsyncCursor) -> None:
        logger.info("loader insert job_id {}", self.job_id)
        sql = f"""
        INSERT INTO idr.load_progress(
            table_name,
            last_ts,
            last_id,
            batch_partition,
            job_start_ts,
            batch_start_ts,
            batch_complete_ts,
            job_id,
            max_run_ts)
        VALUES(
            %(table)s,
            '{DEFAULT_MIN_DATE}',
            0,
            %(partition)s,
            %(job_start_ts)s,
            %(batch_start_ts)s,
            '{DEFAULT_MIN_DATE}',
            %(job_id)s,
        """

        if self.job_id == DEFAULT_JOB_ID:
            sql += """
                null
            """
        else:
            sql += """
            (SELECT last_ts 
             FROM idr.load_progress
             WHERE job_id = %(default_job_id)s
               AND table_name = %(table)s
               AND batch_partition = %(partition)s
            )
            """

        sql += """
        )
        ON CONFLICT (table_name, batch_partition, job_id) DO UPDATE
        SET
            job_start_ts = EXCLUDED.job_start_ts,
            batch_start_ts = EXCLUDED.batch_start_ts
        """

        if self.job_id != DEFAULT_JOB_ID:
            sql += """
            ,max_run_ts = (SELECT last_ts 
                         FROM idr.load_progress
                         WHERE job_id = %(job_id)s
                           AND table_name = %(table)s
                           AND batch_partition = %(partition)s
                        )
            """

        await self._update_load_progress(
            cur,
            sql,
            {
                "table": self.table,
                "partition": self.partition.name,
                "job_start_ts": self.job_start,
                "batch_start_ts": self.batch_start,
                "job_id": self.job_id,
                "default_job_id": DEFAULT_JOB_ID,
            },
        )

    async def _mark_batch_complete(self, cur: psycopg.AsyncCursor) -> None:
        await self._update_load_progress(
            cur,
            """
            UPDATE idr.load_progress
            SET batch_complete_ts = NOW()
            WHERE table_name = %(table)s AND batch_partition = %(batch_partition)s 
                    AND job_id = %(job_id)s
            """,
            {"table": self.table, "batch_partition": self.partition.name, "job_id": self.job_id},
        )

    async def _setup_temp_table(
        self,
        cur: psycopg.AsyncCursor[Any],
        suffix: str | None = None,
        copy_primary_key: bool = False,
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
        copy_primary_key_option = (
            f", PRIMARY KEY ({self.primary_keys_str})" if copy_primary_key else ""
        )
        await cur.execute(
            f'CREATE TEMPORARY TABLE "{full_tablename}" '  # type: ignore
            f"(LIKE {self.table} {copy_primary_key_option}) ON COMMIT DROP"
        )
        # Created/updated columns don't need to be loaded from the source.
        for col in self.meta_keys:
            await cur.execute(f'ALTER TABLE "{full_tablename}" DROP COLUMN {col}')  # type: ignore

        for col in self.temp_only_cols:
            await cur.execute(f'ALTER TABLE "{full_tablename}" ADD COLUMN {col} integer')  # type: ignore

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
    ) -> list[dict[str, DbType]]:
        # Upsert into the main table
        if self.model.should_replace():
            # Delete before inserting since we've specified that the data should be
            # replaced rather than merged.
            # Note that this is executed within a transaction,
            # so consumers won't see an empty table.
            await cur.execute(f"DELETE FROM {self.table}")  # type: ignore
        await cur.execute("SET LOCAL synchronous_commit TO OFF")

        params: dict[str, DbType] = {"timestamp": timestamp}
        on_conflict_clause = self.on_conflict_clause.format(temp_tablename=temp_tablename)

        await cur.execute(
            f'''
            INSERT INTO {self.table} AS t ({self.cols_str}, {self.meta_keys_str})
            SELECT {self.cols_str}, {self.timestamp_placeholders} FROM "{temp_tablename}"
            ON CONFLICT ({self.primary_keys_str}) {on_conflict_clause}
            RETURNING {self.updated_keys_returning_str}
            ''',  # type: ignore
            params,
        )

        return await cur.fetchall()

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
            f'COPY "{temp_tablename}" ({self.insert_and_temp_cols_str}) FROM STDIN'  # type: ignore
        ) as copy:
            for row in data:
                await copy.write_row(
                    [_remove_null_bytes(getattr(row, k)) for k in self.insert_and_temp_cols]
                )

    async def _record_batch_start(
        self, conn: psycopg.AsyncConnection, cur: psycopg.AsyncCursor[Any], commit: bool
    ) -> None:
        self.progress_start_timer.start()
        await self._insert_batch_start(cur)
        if commit:
            await conn.commit()
        self.progress_start_timer.stop()

    def _next_batch(self) -> list[T] | None:
        self.idr_query_timer.start()
        results = next(self.fetch_results, None)
        self.idr_query_timer.stop()
        return results

    async def _stage_all_batches(self, process_batch: Callable[[list[T]], Awaitable[None]]) -> int:
        num_rows = 0

        while True:
            # We unfortunately need to use a while true loop here since we need to wrap the
            # iterator with the timer calls.
            self.idr_query_timer.start()
            results = next(self.fetch_results, None)
            self.idr_query_timer.stop()
            if not results:
                break

            num_rows += len(results)
            await process_batch(results)

        return num_rows


class FullSyncBatchLoader(BatchLoader[T]):
    @override
    async def load(self) -> bool:
        timestamp = datetime.now(UTC)
        self.full_load_timer.start()
        data_loaded = False

        async with self.pool.connection() as conn, conn.cursor(binary=True) as cur:
            await self._record_batch_start(conn, cur, commit=False)
            full_temp_table = await self._setup_temp_table(cur, "full_temp", copy_primary_key=True)

            num_rows = await self._stage_all_batches(
                functools.partial(self._copy_data, cur, full_temp_table)
            )
            data_loaded = num_rows > 0
            logger.info(
                "{}-{}: staged {} row(s) for full sync",
                self.table,
                self.partition.name,
                num_rows,
            )

            self.insert_batch_timer.start()
            updated_keys = await self._upsert(cur, full_temp_table, timestamp)
            deleted_count = await self._delete_missing(cur, full_temp_table)
            self.insert_batch_timer.stop()

            logger.info(
                "{}-{}: upserted {} new/changed row(s), deleted {} row(s) no longer present "
                "upstream",
                self.table,
                self.partition.name,
                len(updated_keys),
                deleted_count,
            )
            await self._mark_batch_complete(cur)

        self.full_load_timer.stop()
        logger.info(
            "{}-{}: finished full sync",
            self.table,
            self.partition.name,
        )
        return data_loaded

    async def _delete_missing(self, cur: psycopg.AsyncCursor[Any], temp_tablename: str) -> int:
        if self.load_mode != LoadMode.PROD and not SETTINGS.test_mode:
            return 0
        # We have to exclude our synthetic data that also exists in prod from deletion. We also want
        # to do this for synthetic loads, except for our pipeline tests
        synthetic_data_filter = self.model.synthetic_data_filter()
        synthetic_where_clause = (
            f"WHERE {synthetic_data_filter}"
            if synthetic_data_filter and self.load_mode != LoadMode.SYNTHETIC
            else ""
        )
        result = await cur.execute(  # type: ignore
            f'''
            DELETE FROM {self.table}
            WHERE ({self.primary_keys_str}) IN (
                SELECT {self.primary_keys_str} FROM {self.table}
                {synthetic_where_clause}
                EXCEPT
                SELECT {self.primary_keys_str} FROM "{temp_tablename}"
            )
            '''  # type: ignore
        )
        return result.rowcount  # type: ignore


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
    # Whether to read/write load progress, which is disabled for synthetic and testing loads.
    return load_mode == LoadMode.PROD or SETTINGS.test_mode


def resolve_test_date(load_mode: LoadMode) -> datetime:
    test_date = SETTINGS.bfd_test_date()

    if test_date and load_mode != LoadMode.PROD:
        return test_date
    return datetime.now(UTC)
