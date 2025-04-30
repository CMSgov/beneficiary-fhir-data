from datetime import datetime, timezone
import os
import time
import psycopg
from psycopg.rows import dict_row
import pytest
from testcontainers.postgres import PostgresContainer

from pipeline import run_pipeline
from extractor import PostgresExtractor


@pytest.fixture(scope="session", autouse=True)
def psql_url():
    with PostgresContainer("postgres:16", driver="") as postgres:
        psql_url = postgres.get_connection_url()
        conn = psycopg.connect(psql_url)

        conn.execute(open("./mock-idr.sql", "r").read())
        conn.commit()
        conn.execute(open("./bfd.sql", "r").read())
        conn.commit()
        yield psql_url


class TestPipeline:
    def test_pipeline(self, psql_url: str):
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)
        conn = psycopg.connect(psql_url, row_factory=dict_row)
        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        assert cur.rowcount == 2
        rows = cur.fetchmany(2)

        assert rows[0]["bene_sk"] == 1
        assert rows[0]["bene_mbi_id"] == "1S000000000"
        assert rows[1]["bene_sk"] == 2
        assert rows[1]["bene_mbi_id"] == "1S000000001"

        cur = conn.execute("select * from idr.beneficiary_history order by bene_sk")
        assert cur.rowcount == 1
        assert rows[0]["bene_sk"] == 1
        assert rows[0]["bene_mbi_id"] == "1S000000000"

        cur = conn.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
        assert cur.rowcount == 1
        assert rows[0]["bene_mbi_id"] == "1S000000000"

        # Wait for system time to advance enough to update the timestamp
        time.sleep(0.05)
        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_bene
            SET bene_mbi_id = '1S000000002', idr_trans_efctv_ts=%(timestamp)s
            WHERE bene_sk = 1
        """,
            {"timestamp": datetime.now(timezone.utc)},
        )
        conn.commit()
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)

        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        rows = cur.fetchmany(2)
        assert rows[0]["bene_mbi_id"] == "1S000000002"
        assert rows[1]["bene_mbi_id"] == "1S000000001"

        cur = conn.execute("select * from idr.beneficiary_election_period_usage")
        rows = cur.fetchall()
        assert len(rows) == 1
        assert rows[0]["cntrct_pbp_sk"] == 1

        cur = conn.execute("select * from idr.contract_pbp_number")
        rows = cur.fetchall()
        assert len(rows) == 1
        assert rows[0]["cntrct_pbp_sk"] == 1
