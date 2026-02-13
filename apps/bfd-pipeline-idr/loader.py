import logging
from collections.abc import Iterator, Sequence
from datetime import UTC, date, datetime

import psycopg
from psycopg.abc import Params, Query

from constants import DEFAULT_MIN_DATE
from load_partition import LoadPartition, LoadType
from model import DbType, LoadMode, LoadProgress, T
from settings import (
    bfd_db_endpoint,
    bfd_db_name,
    bfd_db_password,
    bfd_db_port,
    bfd_db_username,
    force_load_progress,
)
from timer import Timer

logger = logging.getLogger(__name__)


def get_connection_string(load_mode: LoadMode) -> str:
    if load_mode == LoadMode.LOCAL:
        return "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"

    return f"host={bfd_db_endpoint()} port={bfd_db_port()} dbname={bfd_db_name()} \
        user={bfd_db_username()} password={bfd_db_password()}"


class PostgresLoader:
    def __init__(self, load_mode: LoadMode) -> None:
        connection_string = get_connection_string(load_mode)
        self.conn = psycopg.connect(connection_string)

    def run_sql(self, sql: str) -> None:
        self.conn.execute(sql)  # type: ignore
        self.conn.commit()

    def load(
        self,
        fetch_results: Iterator[Sequence[T]],
        model: type[T],
        job_start: datetime,
        partition: LoadPartition,
        progress: LoadProgress | None,
        load_type: LoadType,
        load_mode: LoadMode,
    ) -> bool:
        return BatchLoader(
            self.conn, fetch_results, model, job_start, partition, progress, load_type, load_mode
        ).load()

    def close(self) -> None:
        self.conn.close()


class BatchLoader:
    def __init__(
        self,
        conn: psycopg.Connection,
        fetch_results: Iterator[Sequence[T]],
        model: type[T],
        job_start: datetime,
        partition: LoadPartition,
        progress: LoadProgress | None,
        load_type: LoadType,
        load_mode: LoadMode,
    ) -> None:
        self.conn = conn
        self.fetch_results = fetch_results
        self.model = model
        self.table = model.table()
        self.temp_table = model.table().split(".")[1] + "_temp"
        self.job_start = job_start
        self.batch_start = datetime.now(UTC)
        self.insert_cols = list(model.insert_keys())
        self.insert_cols.sort()
        self.cols_str = ", ".join(self.insert_cols)
        self.batch_timestamp_cols = model.batch_timestamp_col(
            progress is None or progress.is_historical()
        )
        self.partition = partition
        self.progress = progress
        self.immutable = not model.update_timestamp_col()
        self.meta_keys = (
            ["bfd_created_ts"] if self.immutable else ["bfd_created_ts", "bfd_updated_ts"]
        )
        self.idr_query_timer = Timer("idr_query", model, partition)
        self.temp_table_timer = Timer("temp_table", model, partition)
        self.copy_timer = Timer("copy", model, partition)
        self.insert_timer = Timer("insert", model, partition)
        self.commit_timer = Timer("commit", model, partition)
        self.load_type = load_type
        self.enable_load_progress = load_mode == LoadMode.IDR or force_load_progress()

    def load(
        self,
    ) -> bool:
        timestamp = datetime.now(UTC)
        # trim the schema from the table name to create the temp table
        # (temp tables can't be created with an explicit schema set)

        with self.conn.cursor() as cur:
            self._insert_batch_start(cur)
            self.conn.commit()
            data_loaded = False
            num_rows = 0

            # load each batch in a separate transaction
            while True:
                self.idr_query_timer.start()
                # We unfortunately need to use a while true loop here since we need to wrap the
                # iterator with the timer calls.
                results = next(self.fetch_results, None)
                self.idr_query_timer.stop()
                if results is None:
                    break

                data_loaded = True
                logger.info("loading next %s results", len(results))
                num_rows += len(results)

                self.temp_table_timer.start()
                self._setup_temp_table(cur)
                self.temp_table_timer.stop()

                self.copy_timer.start()
                self._copy_data(cur, results)
                self.copy_timer.stop()

                if results:
                    # Upsert into the main table
                    self.insert_timer.start()
                    self._merge(cur, timestamp)
                    self.insert_timer.stop()

                    self._calculate_load_progress(cur, results)

                self.commit_timer.start()
                self.conn.commit()
                self.commit_timer.stop()

            self._mark_batch_complete(cur)
            self.conn.commit()
        logger.info("loaded %s rows", num_rows)
        return data_loaded

    def _insert_batch_start(self, cur: psycopg.Cursor) -> None:
        self._update_load_progress(
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

    def _mark_batch_complete(self, cur: psycopg.Cursor) -> None:
        self._update_load_progress(
            cur,
            """
            UPDATE idr.load_progress
            SET batch_complete_ts = NOW()
            WHERE table_name = %(table)s AND batch_partition = %(batch_partition)s
            """,
            {"table": self.table, "batch_partition": self.partition.name},
        )

    def _setup_temp_table(self, cur: psycopg.Cursor) -> None:
        # Load each batch into a temp table
        # This is necessary because we want to use COPY to quickly
        # transfer everything into Postgres, but COPY can't handle
        # constraint conflicts natively.
        #
        # Note that temp tables don't use WAL so that helps with throughput as well.
        #
        # For simplicity's sake, we'll create our temp tables using the existing schema and
        # just drop the columns we need to ignore.
        cur.execute(
            f"CREATE TEMPORARY TABLE {self.temp_table} (LIKE {self.table}) ON COMMIT DROP"  # type: ignore
        )
        # Created/updated columns don't need to be loaded from the source.
        exclude_cols = self.model.computed_keys() + self.meta_keys
        for col in exclude_cols:
            cur.execute(f"ALTER TABLE {self.temp_table} DROP COLUMN {col}")  # type: ignore

    def _calculate_load_progress(self, cur: psycopg.Cursor, results: Sequence[T]) -> None:
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
            self._update_load_progress(
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

    def _update_load_progress(
        self, cur: psycopg.Cursor, query: Query, params: Params | None
    ) -> None:
        if self.enable_load_progress:
            cur.execute(query, params)

    def _merge(self, cur: psycopg.Cursor, timestamp: datetime) -> None:
        unique_key = self.model.unique_key()
        update_set = ", ".join(
            [f"{v}=EXCLUDED.{v}" for v in self.insert_cols if v not in unique_key]
        )
        # For immutable tables, we may still be attempting to re-load some data
        # due to a batch cancellation.
        # In these cases, we can assume any conflicting rows have already been loaded so
        # "DO NOTHING" is appropriate here.
        # Additionally, if there are no extra columns to update, we can skip it.
        on_conflict = (
            "DO NOTHING"
            if self.immutable or not update_set
            else f"DO UPDATE SET {update_set}, bfd_updated_ts=%(timestamp)s"
        )
        timestamp_placeholders = ",".join("%(timestamp)s" for _ in self.meta_keys)

        # Upsert into the main table
        if self.model.should_replace():
            # Delete before inserting since we've specified that the data should be
            # replaced rather than merged.
            # Note that this is executed within a transaction,
            # so consumers won't see an empty table.
            cur.execute(f"DELETE FROM {self.table}")  # type: ignore
        cur.execute(
            f"""
            INSERT INTO {self.table}({self.cols_str}, {",".join(self.meta_keys)})
            SELECT {self.cols_str},{timestamp_placeholders} FROM {self.temp_table}
            ON CONFLICT ({",".join(unique_key)}) {on_conflict}
            """,  # type: ignore
            {"timestamp": timestamp},
        )

        if self.load_type == LoadType.INCREMENTAL and self.model.last_updated_date_table():
            key = self.model.last_updated_timestamp_col()
            last_updated_cols = self.model.last_updated_date_column()
            set_clause = ", ".join(f"{col} = %(timestamp)s" for col in last_updated_cols)
            # Locking rows to prevent Deadlocks when multiple nodes update this table
            cur.execute(
                f"""
                WITH locked AS (
                    SELECT {key}
                    FROM {self.model.last_updated_date_table()}
                    WHERE {key} IN (
                        SELECT {key} FROM {self.temp_table}
                    )
                    ORDER BY {key}
                    FOR UPDATE
                )
                UPDATE {self.model.last_updated_date_table()} u
                SET {set_clause}
                FROM locked l
                WHERE u.{key} = l.{key};
                """,  # type: ignore
                {"timestamp": timestamp},
            )

    def _copy_data(self, cur: psycopg.Cursor, results: Sequence[T]) -> None:
        # Use COPY to load the batch into Postgres.
        # COPY has a number of optimizations that make bulk loading more efficient
        # than a bunch of INSERTs.
        # The entire operation is performed in a single statement, resulting in
        # fewer network round-trips, less WAL activity, and less context switching.

        # Even though we need to move the data from the temp table in the next step,
        # it should still be faster than alternatives.
        with cur.copy(f"COPY {self.temp_table} ({self.cols_str}) FROM STDIN") as copy:  # type: ignore
            for row in results:
                model_dump = row.model_dump()
                copy.write_row([_remove_null_bytes(model_dump[k]) for k in self.insert_cols])


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
