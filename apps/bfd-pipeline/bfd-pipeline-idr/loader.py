from datetime import datetime, timezone
from typing import Iterator, Optional, TypeVar
from constants import DEFAULT_DATE
from timer import Timer
import psycopg
from model import T


temp_table_timer = Timer("temp_table")
copy_timer = Timer("copy")
insert_timer = Timer("insert")
commit_timer = Timer("commit")


def print_timers():
    temp_table_timer.print_results()
    copy_timer.print_results()
    insert_timer.print_results()
    commit_timer.print_results()


class PostgresLoader:
    def __init__(
        self,
        connection_string: str,
        table: str,
        unique_key: list[str],
        exclude_keys: list[str],
        immutable: bool,
        batch_timestamp_col: Optional[str] = None,
    ):
        self.conn = psycopg.connect(connection_string)
        self.table = table
        self.unique_key = unique_key
        self.exclude_keys = exclude_keys
        self.immutable = immutable
        self.batch_timestamp_col = batch_timestamp_col

    def refresh_materialized_view(self, view_name: str):
        self.conn.execute(f"REFRESH MATERIALIZED VIEW CONCURRENTLY {view_name}")  # type: ignore
        self.conn.commit()

    def load(self, fetch_results: Iterator[list[T]], model: type[T]):
        insert_cols = list(model.model_fields.keys())
        insert_cols.sort()
        cols_str = ", ".join(insert_cols)

        update_set = ", ".join(
            [f"{v}=EXCLUDED.{v}" for v in insert_cols if not v in self.unique_key]
        )
        timestamp = datetime.now(timezone.utc)
        # trim the schema from the table name to create the temp table (temp tables can't be created with an explicit schema set)
        temp_table = self.table.split(".")[1] + "_temp"
        with self.conn.cursor() as cur:
            # load each batch in a separate transaction
            for results in fetch_results:
                # Load each batch into a temp table
                # This is necessary because we want to use COPY to quickly transfer everything into Postgres
                # but COPY can't handle constraint conflicts natively.
                # Note that temp tables don't use WAL so that helps with throughput as well.
                #
                # For simplicity's sake, we'll create our temp tables using the existing schema and just drop the columns we need to ignore
                temp_table_timer.start()
                cur.execute(
                    f"CREATE TEMPORARY TABLE {temp_table} (LIKE {self.table}) ON COMMIT DROP"  # type: ignore
                )
                meta_keys = (
                    ["bfd_created_ts"]
                    if self.immutable
                    else ["bfd_created_ts", "bfd_updated_ts"]
                )
                # Created/updated columns don't need to be loaded from the source.
                exclude_cols = self.exclude_keys + meta_keys
                for col in exclude_cols:
                    cur.execute(f"ALTER TABLE {temp_table} DROP COLUMN {col}")  # type: ignore
                temp_table_timer.stop()

                # Use COPY to load the batch into Postgres.
                # COPY has a number of optimizations that make bulk loading more efficient than a bunch of INSERTs.
                # The entire operation is performed in a single statement, resulting in fewer network round-trips,
                # less WAL activity, and less context switching.

                # Even though we need to move the data from the temp table in the next step, it should still be
                # faster than alternatives.
                copy_timer.start()
                with cur.copy(f"COPY {temp_table} ({cols_str}) FROM STDIN") as copy:  # type: ignore
                    for row in results:
                        model_dump = row.model_dump()
                        copy.write_row([model_dump[k] for k in insert_cols])
                copy_timer.stop()

                if len(results) > 0:
                    # For immutable tables, we may still be attempting to re-load some data due to a batch cancellation.
                    # In these cases, we can assume any conflicting rows have already been loaded so "DO NOTHING" is appropriate here.
                    on_conflict = (
                        "DO NOTHING"
                        if self.immutable
                        else f"DO UPDATE SET {update_set}, bfd_updated_ts=%(timestamp)s"
                    )
                    timestamp_placeholders = ",".join(
                        "%(timestamp)s" for _ in meta_keys
                    )
                    # Upsert into the main table
                    insert_timer.start()
                    cur.execute(
                        f"""
                        INSERT INTO {self.table}({cols_str}, {",".join(meta_keys)})
                        SELECT {cols_str},{timestamp_placeholders} FROM {temp_table}
                        ON CONFLICT ({",".join(self.unique_key)}) {on_conflict}
                        """,  # type: ignore
                        {"timestamp": timestamp},
                    )
                    insert_timer.stop()

                    last = results[len(results) - 1].model_dump()
                    # Some tables that contain reference data (like contract info) may not have the normal IDR timestamps
                    # For now we won't support incremental refreshes for those tables
                    if self.batch_timestamp_col:
                        last_timestamp = last[self.batch_timestamp_col]
                        cur.execute(
                            f"""
                            INSERT INTO idr.load_progress(table_name, last_ts, batch_completion_ts)
                            VALUES(%(table)s, %(last_ts)s, '{DEFAULT_DATE}')
                            ON CONFLICT (table_name) DO UPDATE SET last_ts = EXCLUDED.last_ts
                            """,
                            {
                                "table": self.table,
                                "last_ts": last_timestamp,
                            },
                        )
                # Mark the batch as completed
                cur.execute(
                    """
                    UPDATE idr.load_progress
                    SET batch_completion_ts = NOW()
                    WHERE table_name = %(table)s
                    """,
                    {"table": self.table},
                )
                commit_timer.start()
                self.conn.commit()
                commit_timer.stop()
