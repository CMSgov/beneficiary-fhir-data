import os
import shutil
import subprocess
import sys
import time
from collections.abc import Generator
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import cast

import psycopg
import pytest
import ray
from psycopg.rows import DictRow, dict_row
from testcontainers.core.config import testcontainers_config  # type: ignore

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from constants import DEFAULT_MAX_DATE
from load_synthetic import load_from_csv
from pipeline import main

# ryuk throws a 500 or 404 error for some reason
# seems to have issues with podman https://github.com/testcontainers/testcontainers-python/issues/753
testcontainers_config.ryuk_disabled = True


def _run_migrator(postgres: PostgresContainer) -> None:
    # Python recommends using an absolute path when running an executable
    # to avoid any ambiguity
    mvn = shutil.which("mvn") or "mvn"
    try:
        subprocess.run(
            f"{mvn} flyway:migrate "
            "-Dflyway.url="
            f"jdbc:postgresql://localhost:{postgres.get_exposed_port(5432)}/{postgres.dbname} "
            f"-Dflyway.user={postgres.username} "
            f"-Dflyway.password={postgres.password}",
            cwd="../../bfd-db-migrator-ng",
            shell=True,
            capture_output=True,
            check=True,
        )
    except subprocess.CalledProcessError as ex:
        print(ex.output)
        raise


@pytest.fixture(scope="module")
def setup_db() -> Generator[PostgresContainer]:
    with PostgresContainer("postgres:16", driver="") as postgres:
        with psycopg.connect(postgres.get_connection_url()) as conn:
            with Path("./mock-idr.sql").open() as f:
                conn.execute(f.read())  # type: ignore
            conn.commit()

            _run_migrator(postgres)
            load_from_csv(conn, "./test_samples1")  # type: ignore

            info = conn.info
            os.environ["BFD_DB_ENDPOINT"] = info.host
            os.environ["BFD_DB_PORT"] = str(info.port)
            os.environ["BFD_DB_NAME"] = info.dbname
            os.environ["BFD_DB_USERNAME"] = info.user
            os.environ["BFD_DB_PASSWORD"] = info.password
            os.environ["PARALLELISM"] = "2"
            os.environ["IDR_BATCH_SIZE"] = "100000"
            os.environ["IDR_LOAD_BENES"] = "true"
            os.environ["IDR_LOAD_CLAIMS"] = "true"
        yield postgres


def test_pipeline(setup_db: PostgresContainer) -> None:
    conn = cast(
        psycopg.Connection[DictRow],
        psycopg.connect(setup_db.get_connection_url(), row_factory=dict_row),  # type: ignore
    )
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
            SET idr_insrt_ts=%(none)s
            WHERE clm_uniq_id = 9844382563835
            """,
        {"none": None},
    )

    conn.execute(
        """
            UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm
            SET idr_updt_ts=%(none)s
            WHERE clm_uniq_id = 6919983105596
            """,
        {"none": None},
    )

    conn.commit()

    sys.argv = ["pipeline.py", "synthetic"]
    main()

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
    ray.shutdown()  # type: ignore
    main()

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

    cur = conn.execute("select * from idr.claim where clm_uniq_id = 8244064276500")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_institutional order by clm_uniq_id")
    assert cur.rowcount == 72
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 113370100080

    cur = conn.execute("select * from idr.claim_date_signature order by clm_dt_sgntr_sk")
    assert cur.rowcount == 142
    rows = cur.fetchmany(2)
    assert rows[0]["clm_dt_sgntr_sk"] == 2334117069
    assert rows[0]["clm_cms_proc_dt"] == datetime.strptime(DEFAULT_MAX_DATE, "%Y-%m-%d").date()
    assert rows[1]["clm_cms_proc_dt"] == datetime.strptime(DEFAULT_MAX_DATE, "%Y-%m-%d").date()

    cur = conn.execute("select * from idr.claim_professional order by clm_uniq_id")
    assert cur.rowcount == 86
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 113370100080

    cur = conn.execute("select * from idr.claim_item order by clm_uniq_id")
    assert cur.rowcount == 1590
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 113370100080

    cur = conn.execute("select * from idr.claim_line_institutional order by clm_uniq_id")
    assert cur.rowcount == 594
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
