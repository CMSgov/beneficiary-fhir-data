import logging
import os
from collections.abc import Iterator
from datetime import UTC, date, datetime
import pprint

import psycopg

from constants import DEFAULT_MIN_DATE
from model import DbType, LoadProgress, T
from timer import Timer

idr_query_timer = Timer("idr_query")
temp_table_timer = Timer("temp_table")
copy_timer = Timer("copy")
insert_timer = Timer("insert")
commit_timer = Timer("commit")

logger = logging.getLogger(__name__)


def get_connection_string() -> str:
    port = os.environ.get("BFD_DB_PORT") or "5432"
    dbname = os.environ.get("BFD_DB_NAME") or "fhirdb"
    return f"host={os.environ['BFD_DB_ENDPOINT']} port={port} dbname={dbname} \
        user={os.environ['BFD_DB_USERNAME']} password={os.environ['BFD_DB_PASSWORD']}"


class PostgresLoader:
    def __init__(
        self,
        connection_string: str,
    ) -> None:
        self.conn = psycopg.connect(connection_string)

    def run_sql(self, sql: str) -> None:
        self.conn.execute(sql)  # type: ignore
        self.conn.commit()

    def load(
        self,
        fetch_results: Iterator[list[T]],
        model: type[T],
        batch_start: datetime,
        progress: LoadProgress | None,
    ) -> bool:
        return BatchLoader(self.conn, fetch_results, model, batch_start, progress).load()


class BatchLoader:
    def __init__(
        self,
        conn: psycopg.Connection,
        fetch_results: Iterator[list[T]],
        model: type[T],
        batch_start: datetime,
        progress: LoadProgress | None,
    ) -> None:
        self.conn = conn
        self.fetch_results = fetch_results
        self.model = model
        self.table = model.table()
        self.temp_table = model.table().split(".")[1] + "_temp"
        self.batch_start = batch_start
        self.insert_cols = list(model.insert_keys())
        self.insert_cols.sort()
        self.cols_str = ", ".join(self.insert_cols)
        self.batch_timestamp_cols = model.batch_timestamp_col(
            progress is None or progress.is_historical()
        )
        self.progress = progress
        self.immutable = not model.update_timestamp_col()
        self.meta_keys = (
            ["bfd_created_ts"] if self.immutable else ["bfd_created_ts", "bfd_updated_ts"]
        )

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
                idr_query_timer.start()
                # We unfortunately need to use a while true loop here since we need to wrap the
                # iterator with the timer calls.
                results = next(self.fetch_results, None)
                idr_query_timer.stop(self.table)
                if results is None:
                    break

                data_loaded = True
                logger.info("loading next %s results", len(results))
                num_rows += len(results)

                temp_table_timer.start()
                self._setup_temp_table(cur)
                temp_table_timer.stop(self.table)

                copy_timer.start()
                self._copy_data(cur, results)
                copy_timer.stop(self.table)

                if results:
                    # Upsert into the main table
                    insert_timer.start()
                    self._merge(cur, timestamp)
                    insert_timer.stop(self.table)

                    self._calculate_load_progress(cur, results)

                commit_timer.start()
                self.conn.commit()
                commit_timer.stop(self.table)

            self._mark_batch_complete(cur)
            self.conn.commit()
        logger.info("loaded %s rows", num_rows)
        return data_loaded

    def _insert_batch_start(self, cur: psycopg.Cursor) -> None:
        cur.execute(
            f"""
            INSERT INTO idr.load_progress(
                table_name, 
                last_ts, 
                last_id,
                batch_start_ts, 
                batch_complete_ts)
            VALUES(%(table)s, '{DEFAULT_MIN_DATE}', 0, %(start_ts)s, '{DEFAULT_MIN_DATE}')
            ON CONFLICT (table_name) DO UPDATE 
            SET batch_start_ts = EXCLUDED.batch_start_ts
            """,
            {
                "table": self.table,
                "start_ts": self.batch_start,
            },
        )

    def _mark_batch_complete(self, cur: psycopg.Cursor) -> None:
        cur.execute(
            """
            UPDATE idr.load_progress
            SET batch_complete_ts = NOW()
            WHERE table_name = %(table)s
            """,
            {"table": self.table},
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

    def _calculate_load_progress(self, cur: psycopg.Cursor, results: list[T]) -> None:
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
            cur.execute(
                """
            UPDATE idr.load_progress
            SET last_ts = %(last_ts)s,
                last_id = %(last_id)s
            WHERE table_name = %(table)s
            """,
                {
                    "table": self.table,
                    "last_ts": max_timestamp,
                    "last_id": batch_id,
                },
            )

    def _merge(self, cur: psycopg.Cursor, timestamp: datetime) -> None:
        print("---MERGE---")
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
        print("----TABLE----")
        print(self.table)
        print("----COLS----")
        print(self.insert_cols)
        print("----META----")
        print(self.meta_keys)

        tracked_columns = [
            ("bene_sk", "idr.beneficiary_updates"),
            ("clm_uniq_id", "idr.claim_updates"),
        ]

        # Find the first column that exists in self.insert_cols
        match_pair = next(
            ((col, table) for col, table in tracked_columns if col in self.insert_cols),
            None
        )

        print("----MATCH PAIR----", match_pair)

        if match_pair:
            col, target_table = match_pair
            print("----COL----", col)
            print("----TARGET TABLE----", target_table)

            self._upsert_updates_from_temp(cur,target_table,col)

    def _upsert_updates_from_temp(
            self,
            cur: psycopg.Cursor,
            target_table: str,
            key_col: str,
    ) -> None:
        """
        Upsert the last updated timestamp from temp table into the target updates table.
        Falls back to the loader timestamp when no source timestamp exists.
        """
        print("----COLUMNS----", self.insert_cols)
        # Find the first column that contains "idr_updt_ts"
        match = next((col for col in self.insert_cols if "idr_updt_ts" in col), None)
        print("----MATCH----", match)

        if match:
            cur.execute(
                f"""
                INSERT INTO {target_table}({key_col}, last_updated)
                SELECT {key_col}, MAX({match})
                FROM {self.temp_table}
                WHERE {key_col} IS NOT NULL AND {match} IS NOT NULL
                GROUP BY {key_col}
                ON CONFLICT ({key_col}) DO UPDATE
                SET last_updated = GREATEST({target_table}.last_updated, EXCLUDED.last_updated)
                """,
                {},
            )

    def _copy_data(self, cur: psycopg.Cursor, results: list[T]) -> None:
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
