import time
from collections.abc import Generator
from datetime import UTC, datetime, timedelta
from pathlib import Path
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
def psql_url() -> Generator[str]:
    with PostgresContainer("postgres:16", driver="") as postgres:
        psql_url = postgres.get_connection_url()
        conn = psycopg.connect(psql_url)

        with Path("./mock-idr.sql").open() as f:
            conn.execute(f.read())  # type: ignore
        conn.commit()
        with Path("./bfd.sql").open() as f:
            conn.execute(f.read())  # type: ignore
        conn.commit()

        load_from_csv(conn, "./test_samples1")

        yield psql_url


class TestPipeline:
    def test_pipeline(self, psql_url: str) -> None:
        conn = cast(psycopg.Connection[DictRow], psycopg.connect(psql_url, row_factory=dict_row))  # type: ignore
        datetime_now = datetime.now(UTC)

        # Update dates to CURRENT_DATE before pipeline.py
        # Reason: PAC data older than 60 days is filtered by coalescing
        # (idr_updt_ts, idr_insrt_ts, clm_idr_ld_dt). Synthetic data has
        # outdated idr_updt_ts, idr_insrt_ts, and clm_idr_ld_dt values.
        # Update all values to current dates then change specific dates
        # to older than 60 days to test the functionality.
        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm
            SET idr_insrt_ts=%(timestamp)s,
                idr_updt_ts=%(timestamp)s,
                clm_idr_ld_dt=%(today)s
            """,
            {
                "timestamp": datetime_now,
                "today": datetime_now.date(),
            },
        )

        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm
            SET idr_updt_ts=%(none)s, idr_insrt_ts=%(timestamp)s
            WHERE clm_uniq_id = 1128619260039
            """,
            {"none": None, "timestamp": datetime_now - timedelta(days=65)},
        )

        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm
            SET idr_updt_ts=%(timestamp)s
            WHERE clm_uniq_id = 123359318723
            """,
            {"timestamp": datetime_now - timedelta(days=65)},
        )

        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm
            SET idr_insrt_ts=%(timestamp)s
            WHERE clm_uniq_id = 9844382563835
            """,
            {"timestamp": None},
        )

        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm
            SET idr_updt_ts=%(timestamp)s
            WHERE clm_uniq_id = 6919983105596
            """,
            {"timestamp": None},
        )
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)

        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        assert cur.rowcount == 25
        rows = cur.fetchmany(2)

        assert rows[0]["bene_sk"] == 10464258
        assert rows[0]["bene_mbi_id"] == "2ZT2XU2EN18"
        assert rows[1]["bene_sk"] == 16666900
        assert rows[1]["bene_mbi_id"] == "5B88XK5JN88"

        cur = conn.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
        assert cur.rowcount == 21
        rows = cur.fetchmany(1)
        assert rows[0]["bene_mbi_id"] == "1BC3JG0FM51"

        # Wait for system time to advance enough to update the timestamp
        time.sleep(0.05)
        conn.execute(
            """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry
            SET bene_mbi_id = '1S000000000', idr_insrt_ts=%(timestamp)s, idr_updt_ts=%(timestamp)s
            WHERE bene_sk = 10464258
            """,
            {"timestamp": datetime.now(UTC)},
        )
        conn.commit()
        run_pipeline(PostgresExtractor(psql_url, 100_000), psql_url)

        cur = conn.execute("select * from idr.beneficiary order by bene_sk")
        rows = cur.fetchmany(2)
        assert rows[0]["bene_mbi_id"] == "1S000000000"
        assert rows[1]["bene_mbi_id"] == "5B88XK5JN88"

        cur = conn.execute(
            "select * from idr.beneficiary where bene_kill_cred_cd != '' order by bene_sk"
        )
        assert cur.rowcount == 5
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 174441863

        cur = conn.execute("select * from idr.beneficiary_third_party order by bene_sk")
        assert cur.rowcount == 4
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 16666900

        cur = conn.execute("select * from idr.beneficiary_status order by bene_sk")
        assert cur.rowcount == 15
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 10464258

        cur = conn.execute("select * from idr.beneficiary_entitlement order by bene_sk")
        assert cur.rowcount == 30
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 10464258

        cur = conn.execute("select * from idr.beneficiary_entitlement_reason order by bene_sk")
        assert cur.rowcount == 15
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 10464258

        cur = conn.execute("select * from idr.beneficiary_dual_eligibility order by bene_sk")
        assert cur.rowcount == 4
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 47347082

        cur = conn.execute("select * from idr.beneficiary_overshare_mbi order by bene_mbi_id")
        assert cur.rowcount == 2
        rows = cur.fetchmany(2)
        assert rows[0]["bene_mbi_id"] == "5OH0K85GU23"
        assert rows[1]["bene_mbi_id"] == "6LM1C27GV22"

        cur = conn.execute("select * from idr.claim order by clm_uniq_id")
        assert cur.rowcount == 142
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 113370100080
        assert rows[0]["clm_nrln_ric_cd"] == "W"

        cur = conn.execute("select * from idr.claim_institutional order by clm_uniq_id")
        assert cur.rowcount == 53
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 113370100080

        cur = conn.execute("select * from idr.claim_date_signature order by clm_dt_sgntr_sk")
        assert cur.rowcount == 123
        rows = cur.fetchmany(1)
        assert rows[0]["clm_dt_sgntr_sk"] == 2334117069

        cur = conn.execute("select * from idr.claim_professional order by clm_uniq_id")
        assert cur.rowcount == 86
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 113370100080

        cur = conn.execute("select * from idr.claim_item order by clm_uniq_id")
        assert cur.rowcount == 1286
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 113370100080

        cur = conn.execute("select * from idr.claim_line_institutional order by clm_uniq_id")
        assert cur.rowcount == 433
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 113370100080

        cur = conn.execute("select * from idr.claim_line_professional order by clm_uniq_id")
        assert cur.rowcount == 281
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 797757725380

        cur = conn.execute("select * from idr.claim_ansi_signature order by clm_ansi_sgntr_sk")
        assert cur.rowcount == 12072
        rows = cur.fetchmany(1)
        assert rows[0]["clm_ansi_sgntr_sk"] == 0

        # TODO: add these back when contract data is added
        # cur = conn.execute("select * from idr.beneficiary_election_period_usage")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["cntrct_pbp_sk"] == 1

        # cur = conn.execute("select * from idr.contract_pbp_number")
        # rows = cur.fetchall()
        # assert len(rows) == 1
        # assert rows[0]["cntrct_pbp_sk"] == 1
