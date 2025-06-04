from datetime import datetime, timezone
from typing import cast
import psycopg
import time
from psycopg.rows import dict_row, DictRow
import pytest

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from load_synthetic import load_from_csv
from pipeline import run_pipeline
from extractor import PostgresExtractor


@pytest.fixture(scope="session", autouse=True)
def psql_url():
    with PostgresContainer("postgres:16", driver="") as postgres:
        psql_url = postgres.get_connection_url()
        conn = psycopg.connect(psql_url)

        conn.execute(open("./mock-idr.sql", "r").read())  # type: ignore
        conn.commit()
        conn.execute(open("./bfd.sql", "r").read())  # type: ignore
        conn.commit()

        load_from_csv(conn, "./sample_data")

        yield psql_url


class TestPipeline:
    def test_pipeline(self, psql_url: str):
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)
        conn = cast(psycopg.Connection[DictRow], psycopg.connect(psql_url, row_factory=dict_row))  # type: ignore
        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        assert cur.rowcount == 3
        rows = cur.fetchmany(3)

        assert rows[0]["bene_sk"] == 181968400
        assert rows[0]["bene_mbi_id"] == "8Z73WV0QC20"
        assert rows[1]["bene_sk"] == 405764107
        assert rows[1]["bene_mbi_id"] == "8Z73WV0QC20"

        cur = conn.execute("select * from idr.beneficiary_history order by bene_sk")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 405764107
        assert rows[0]["bene_mbi_id"] == "8Z73WV0QC20"

        cur = conn.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["bene_mbi_id"] == "8Z73WV0QC20"

        # Wait for system time to advance enough to update the timestamp
        time.sleep(0.05)
        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_bene
            SET bene_mbi_id = '1S000000000', idr_trans_efctv_ts=%(timestamp)s
            WHERE bene_sk = 181968400
        """,
            {"timestamp": datetime.now(timezone.utc)},
        )
        conn.commit()
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)

        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        rows = cur.fetchmany(2)
        assert rows[0]["bene_mbi_id"] == "1S000000000"
        assert rows[1]["bene_mbi_id"] == "8Z73WV0QC20"

        cur = conn.execute("select * from idr.claim")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 1071939711295
        assert rows[0]["clm_nrln_ric_cd"] == "V"

        cur = conn.execute("select * from idr.claim_institutional")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 1071939711295

        cur = conn.execute("select * from idr.claim_date_signature")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_dt_sgntr_sk"] == 322823692141

        cur = conn.execute("select * from idr.claim_value")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 1071939711295

        cur = conn.execute("select * from idr.claim_line")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 1071939711295

        cur = conn.execute("select * from idr.claim_line_institutional")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 1071939711295

        cur = conn.execute("select * from idr.claim_ansi_signature")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_ansi_sgntr_sk"] == 1

        cur = conn.execute("select * from idr.claim_procedure")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 1071939711295

        # TODO: add these back when synthetic coverage data is available
        # cur = conn.execute("select * from idr.beneficiary_third_party")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["bene_sk"] == 1

        # cur = conn.execute("select * from idr.beneficiary_status")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["bene_sk"] == 1

        # cur = conn.execute("select * from idr.beneficiary_entitlement")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["bene_sk"] == 1

        # cur = conn.execute("select * from idr.beneficiary_entitlement_reason")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["bene_sk"] == 1

        # cur = conn.execute("select * from idr.beneficiary_election_period_usage")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["cntrct_pbp_sk"] == 1

        # cur = conn.execute("select * from idr.contract_pbp_number")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["cntrct_pbp_sk"] == 1
