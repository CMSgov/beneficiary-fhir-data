import logging
import os
from collections.abc import Iterator
from datetime import UTC, datetime

import psycopg

from constants import DEFAULT_MIN_DATE
from model import LoadProgress, T
from timer import Timer

temp_table_timer = Timer("temp_table")
copy_timer = Timer("copy")
insert_timer = Timer("insert")
commit_timer = Timer("commit")

logger = logging.getLogger(__name__)


def print_timers() -> None:
    temp_table_timer.print_results()
    copy_timer.print_results()
    insert_timer.print_results()
    commit_timer.print_results()


def get_connection_string() -> str:
    port = os.environ.get("BFD_DB_PORT") or "5432"
    dbname = os.environ.get("BFD_DB_NAME") or "idr"
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
        insert_cols = list(model.insert_keys())
        insert_cols.sort()
        cols_str = ", ".join(insert_cols)
        unique_key = model.unique_key()

        update_set = ", ".join([f"{v}=EXCLUDED.{v}" for v in insert_cols if v not in unique_key])
        timestamp = datetime.now(UTC)
        table = model.table()
        # trim the schema from the table name to create the temp table
        # (temp tables can't be created with an explicit schema set)
        temp_table = table.split(".")[1] + "_temp"
        with self.conn.cursor() as cur:
            cur.execute(
                f"""
                    INSERT INTO idr.load_progress(
                        table_name, 
                        last_ts, 
                        batch_start_ts, 
                        batch_complete_ts)
                    VALUES(%(table)s, '{DEFAULT_MIN_DATE}', %(start_ts)s, '{DEFAULT_MIN_DATE}')
                    ON CONFLICT (table_name) DO UPDATE 
                    SET batch_start_ts = EXCLUDED.batch_start_ts
                    """,
                {
                    "table": table,
                    "start_ts": batch_start,
                },
            )
            data_loaded = False
            num_rows = 0

            # load each batch in a separate transaction
            for results in fetch_results:
                data_loaded = True
                logger.info("loading next %s results", len(results))
                num_rows += len(results)
                # Load each batch into a temp table
                # This is necessary because we want to use COPY to quickly
                # transfer everything into Postgres, but COPY can't handle
                # constraint conflicts natively.
                #
                # Note that temp tables don't use WAL so that helps with throughput as well.
                #
                # For simplicity's sake, we'll create our temp tables using the existing schema and
                # just drop the columns we need to ignore.
                temp_table_timer.start()
                cur.execute(
                    f"CREATE TEMPORARY TABLE {temp_table} (LIKE {table}) ON COMMIT DROP"  # type: ignore
                )
                immutable = len(model.update_timestamp_col()) == 0
                meta_keys = (
                    ["bfd_created_ts"] if immutable else ["bfd_created_ts", "bfd_updated_ts"]
                )
                # Created/updated columns don't need to be loaded from the source.
                exclude_cols = model.computed_keys() + meta_keys
                for col in exclude_cols:
                    cur.execute(f"ALTER TABLE {temp_table} DROP COLUMN {col}")  # type: ignore
                temp_table_timer.stop()

                # Use COPY to load the batch into Postgres.
                # COPY has a number of optimizations that make bulk loading more efficient
                # than a bunch of INSERTs.
                # The entire operation is performed in a single statement, resulting in
                # fewer network round-trips, less WAL activity, and less context switching.

                # Even though we need to move the data from the temp table in the next step,
                # it should still be faster than alternatives.
                copy_timer.start()
                with cur.copy(f"COPY {temp_table} ({cols_str}) FROM STDIN") as copy:  # type: ignore
                    for row in results:
                        model_dump = row.model_dump()
                        copy.write_row([model_dump[k] for k in insert_cols])
                copy_timer.stop()

                if len(results) > 0:
                    # For immutable tables, we may still be attempting to re-load some data
                    # due to a batch cancellation.
                    # In these cases, we can assume any conflicting rows have already been loaded so
                    # "DO NOTHING" is appropriate here.
                    on_conflict = (
                        "DO NOTHING"
                        if immutable
                        else f"DO UPDATE SET {update_set}, bfd_updated_ts=%(timestamp)s"
                    )
                    timestamp_placeholders = ",".join("%(timestamp)s" for _ in meta_keys)
                    # Upsert into the main table
                    insert_timer.start()
                    cur.execute(
                        f"""
                        INSERT INTO {table}({cols_str}, {",".join(meta_keys)})
                        SELECT {cols_str},{timestamp_placeholders} FROM {temp_table}
                        ON CONFLICT ({",".join(unique_key)}) {on_conflict}
                        """,  # type: ignore
                        {"timestamp": timestamp},
                    )
                    insert_timer.stop()

                    last = results[len(results) - 1].model_dump()
                    # Some tables that contain reference data (like contract info) may not have the
                    # normal IDR timestamps.
                    # For now we won't support incremental refreshes for those tables
                    batch_timestamp_cols = model.batch_timestamp_col(
                        progress is None or progress.is_historical()
                    )
                    if len(batch_timestamp_cols) > 0:
                        min_timestamp = min([last[col] for col in batch_timestamp_cols])
                        cur.execute(
                            """
                            UPDATE idr.load_progress
                            SET last_ts = %(last_ts)s
                            WHERE table_name = %(table)s
                            """,
                            {
                                "table": table,
                                "last_ts": min_timestamp,
                            },
                        )
                commit_timer.start()
                self.conn.commit()
                commit_timer.stop()
            # Mark the batch as completed
            cur.execute(
                """
                UPDATE idr.load_progress
                SET batch_complete_ts = NOW()
                WHERE table_name = %(table)s
                """,
                {"table": table},
            )
            self.conn.commit()
        logger.info("loaded %s rows", num_rows)
        return data_loaded
