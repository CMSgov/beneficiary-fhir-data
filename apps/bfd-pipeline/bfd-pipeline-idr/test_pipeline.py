import time
from datetime import datetime, timezone, UTC
from typing import cast

import psycopg
import pytest
from psycopg.rows import DictRow, dict_row
from testcontainers.core.config import testcontainers_config  # type: ignore

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from extractor import PostgresExtractor
from load_synthetic import load_from_csv
from pipeline import run_pipeline

# ryuk throws a 500 or 404 error for some reason
# seems to have issues with podman https://github.com/testcontainers/testcontainers-python/issues/753
testcontainers_config.ryuk_disabled = True


@pytest.fixture(scope="session", autouse=True)
def psql_url():
    with PostgresContainer("postgres:16", driver="") as postgres:
        psql_url = postgres.get_connection_url()
        conn = psycopg.connect(psql_url)

        conn.execute(open("./mock-idr.sql", "r").read())  # type: ignore
        conn.commit()
        conn.execute(open("./bfd.sql", "r").read())  # type: ignore
        conn.commit()

        load_from_csv(conn, "./test_samples1")

        yield psql_url


class TestPipeline:
    def test_pipeline(self, psql_url: str):
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)
        conn = cast(psycopg.Connection[DictRow], psycopg.connect(psql_url, row_factory=dict_row))  # type: ignore
        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        assert cur.rowcount == 20
        rows = cur.fetchmany(2)

        assert rows[0]["bene_sk"] == 53965935
        assert rows[0]["bene_mbi_id"] == "3LQ6D75DA70"
        assert rows[1]["bene_sk"] == 70288544
        assert rows[1]["bene_mbi_id"] == "3BR5F18GJ10"

        cur = conn.execute("select * from idr.beneficiary_history order by bene_sk")
        assert cur.rowcount == 4
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 70288544
        assert rows[0]["bene_mbi_id"] == "3BR5F18GJ10"

        cur = conn.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
        assert cur.rowcount == 19
        rows = cur.fetchmany(1)
        assert rows[0]["bene_mbi_id"] == "1IH0YW0KT23"

        # Wait for system time to advance enough to update the timestamp
        time.sleep(0.05)
        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_bene
            SET bene_mbi_id = '1S000000000', idr_insrt_ts=%(timestamp)s
            WHERE bene_sk = 53965935
            """,
            {"timestamp": datetime.now(UTC)},
        )
        conn.commit()
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)

        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        rows = cur.fetchmany(2)
        assert rows[0]["bene_mbi_id"] == "1S000000000"
        assert rows[1]["bene_mbi_id"] == "3BR5F18GJ10"

        cur = conn.execute("select * from idr.beneficiary_third_party order by bene_sk")
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 445272343

        cur = conn.execute("select * from idr.beneficiary_status order by bene_sk")
        assert cur.rowcount == 15
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 53965935

        cur = conn.execute("select * from idr.beneficiary_entitlement order by bene_sk")
        assert cur.rowcount == 30
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 53965935

        cur = conn.execute("select * from idr.beneficiary_entitlement_reason order by bene_sk")
        assert cur.rowcount == 15
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 53965935

        cur = conn.execute("select * from idr.claim order by clm_uniq_id")
        assert cur.rowcount == 150
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 74294264116
        assert rows[0]["clm_nrln_ric_cd"] == "V"

        cur = conn.execute("select * from idr.claim_institutional order by clm_uniq_id")
        assert cur.rowcount == 150
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 74294264116

        cur = conn.execute("select * from idr.claim_date_signature order by clm_dt_sgntr_sk")
        assert cur.rowcount == 150
        rows = cur.fetchmany(1)
        assert rows[0]["clm_dt_sgntr_sk"] == 5123224512

        cur = conn.execute("select * from idr.claim_value order by clm_uniq_id")
        assert cur.rowcount == 136
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 191283812055

        cur = conn.execute("select * from idr.claim_line order by clm_uniq_id")
        assert cur.rowcount == 1160
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 74294264116

        cur = conn.execute("select * from idr.claim_line_institutional order by clm_uniq_id")
        assert cur.rowcount == 1160
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 74294264116

        cur = conn.execute("select * from idr.claim_ansi_signature order by clm_ansi_sgntr_sk")
        assert cur.rowcount == 12072
        rows = cur.fetchmany(1)
        assert rows[0]["clm_ansi_sgntr_sk"] == 0

        cur = conn.execute("select * from idr.claim_procedure order by clm_uniq_id, bfd_row_num")
        assert cur.rowcount == 2020
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 74294264116
        assert rows[0]["bfd_row_num"] == 1

        # TODO: add these back when contract data is added
        # cur = conn.execute("select * from idr.beneficiary_election_period_usage")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["cntrct_pbp_sk"] == 1

        # cur = conn.execute("select * from idr.contract_pbp_number")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["cntrct_pbp_sk"] == 1
