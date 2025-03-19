from datetime import datetime, timezone
from typing import Iterator, TypeVar
import psycopg
from pydantic import BaseModel

T = TypeVar("T", bound=BaseModel)


class PostgresLoader:
    def __init__(
        self,
        connection_string: str,
        table: str,
        temp_table: str,
        unique_key: list[str],
        sort_key: str,
        exclude_keys: list[str],
    ):
        self.conn = psycopg.connect(connection_string)
        self.table = table
        self.temp_table = temp_table
        self.unique_key = unique_key
        self.sort_key = sort_key
        self.exclude_keys = exclude_keys

    def refresh_materialized_view(self, view_name: str):
        self.conn.execute(f"REFRESH MATERIALIZED VIEW {view_name}")
        self.conn.commit()

    def load(self, fetch_results: Iterator[list[T]], model: type[T]):
        insert_cols = list(model.model_fields.keys())
        insert_cols.sort()
        cols_str = ", ".join(insert_cols)

        update_set = ", ".join(
            [f"{v}=EXCLUDED.{v}" for v in insert_cols if not v in self.unique_key]
        )
        timestamp = datetime.now(timezone.utc)
        with self.conn.cursor() as cur:
            # load each batch in a separate transaction
            for results in fetch_results:
                # Load each batch into a temp table
                # This is necessary because we want to use COPY to quickly transfer everything into Postgres
                # but COPY can't handle constraint conflicts natively.
                # Note that temp tables don't use WAL so that helps with throughput as well.
                #
                # For simplicity's sake, we'll create our temp tables using the existing schema and just drop the columns we need to ignore
                cur.execute(
                    f"CREATE TEMPORARY TABLE {self.temp_table} (LIKE {self.table}) ON COMMIT DROP"
                )
                # Created/updated columns don't need to be loaded from the source.
                exclude_cols = self.exclude_keys + [
                    "bfd_created_ts",
                    "bfd_updated_ts",
                ]
                for col in exclude_cols:
                    cur.execute(f"ALTER TABLE {self.temp_table} DROP COLUMN {col}")

                # Use COPY to load the batch into Postgres.
                # COPY has a number of optimizations that make bulk loading more efficient than a bunch of INSERTs.
                # The entire operation is performed in a single statement, resulting in fewer network round-trips,
                # less WAL activity, and less context switching.

                # Even though we need to move the data from the temp table in the next step, it should still be
                # faster than alternatives.
                with cur.copy(
                    f"COPY {self.temp_table} ({cols_str}) FROM STDIN"
                ) as copy:
                    for row in results:
                        model_dump = row.model_dump()
                        copy.write_row([model_dump[k] for k in insert_cols])
                if len(results) > 0:
                    # Upsert into the main table
                    cur.execute(
                        f"""
                        INSERT INTO {self.table}({cols_str}, bfd_created_ts, bfd_updated_ts)
                        SELECT {cols_str}, %(timestamp)s, %(timestamp)s FROM {self.temp_table}
                        ON CONFLICT ({",".join(self.unique_key)}) DO UPDATE SET {update_set}, bfd_updated_ts=%(timestamp)s
                        """,
                        {"timestamp": timestamp},
                    )

                    last = results[len(results) - 1].model_dump()
                    last_id = last[self.sort_key]
                    last_timestamp = last["idr_trans_efctv_ts"]
                    cur.execute(
                        f"""
                        INSERT INTO idr.load_progress(table_name, last_id, last_timestamp)
                        VALUES(%(table)s, %(last_id)s, %(last_timestamp)s)
                        ON CONFLICT (table_name) DO UPDATE SET last_id = EXCLUDED.last_id, last_timestamp = EXCLUDED.last_timestamp
                        """,
                        {
                            "table": self.table,
                            "last_id": last_id,
                            "last_timestamp": last_timestamp,
                        },
                    )
                self.conn.commit()
