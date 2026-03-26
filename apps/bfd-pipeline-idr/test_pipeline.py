import os
import shutil
import subprocess

from collections.abc import Generator
from datetime import datetime, timedelta
from pathlib import Path
from typing import cast

import psycopg
import pytest
from psycopg.rows import DictRow, dict_row
from testcontainers.core.config import testcontainers_config  # type: ignore

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from load_synthetic import load_from_csv
from logger_config import configure_logger
from pipeline import run
from settings import bfd_test_date

# ryuk throws a 500 or 404 error for some reason
# seems to have issues with podman https://github.com/testcontainers/testcontainers-python/issues/753
testcontainers_config.ryuk_disabled = True

configure_logger()


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
            f"-Dflyway.password={postgres.password} "
            "-Duser.timezone=UTC",
            cwd="../bfd-db-migrator-ng",
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
            # Info level logs obscure the error output when running tests
            # so we want to override this unless the calling process has set this explicitly
            os.environ.setdefault("IDR_LOG_LEVEL", "warning")
            os.environ["BFD_DB_ENDPOINT"] = info.host
            os.environ["BFD_DB_PORT"] = str(info.port)
            os.environ["BFD_DB_NAME"] = info.dbname
            os.environ["BFD_DB_USERNAME"] = info.user
            os.environ["BFD_DB_PASSWORD"] = info.password
            os.environ["IDR_BATCH_SIZE"] = "100000"
            os.environ["IDR_FORCE_LOAD_PROGRESS"] = "1"
            os.environ["BFD_TEST_DATE"] = "2025-06-15"
        yield postgres

def test_pipeline(setup_db: PostgresContainer) -> None:
    conn = cast(
        psycopg.Connection[DictRow],
        psycopg.connect(setup_db.get_connection_url(), row_factory=dict_row),  # type: ignore
    )

    conn.commit()

    run("synthetic")

    cur = conn.execute("select * from idr.beneficiary order by bene_sk")
    assert cur.rowcount == 28
    rows = cur.fetchmany(2)

    assert rows[0]["bene_sk"] == 10464258
    assert rows[0]["bene_mbi_id"] == "2ZT2XU2EN18"
    assert rows[1]["bene_sk"] == 16666900
    assert rows[1]["bene_mbi_id"] == "5B88XK5JN88"

    cur = conn.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
    assert cur.rowcount == 24
    rows = cur.fetchmany(1)
    assert rows[0]["bene_mbi_id"] == "1BC3JG0FM51"

    cur = conn.execute("select max(last_ts) as max_ts from idr.load_progress")
    
    date_adv = datetime.strftime(cur.fetchone()["max_ts"] + timedelta(days=1), "%Y-%m-%d")

    conn.execute(
        """
        UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry
        SET bene_mbi_id = '1S000000000', idr_insrt_ts=%(timestamp)s, idr_updt_ts=%(timestamp)s
        WHERE bene_sk = 10464258
        """,
        {"timestamp": date_adv},
    )
    conn.commit()

    os.environ['BFD_TEST_DATE'] = date_adv

    run("synthetic")

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

    cur = conn.execute("select * from idr.contract_pbp_number order by cntrct_pbp_sk")
    assert cur.rowcount == 10
    rows = cur.fetchmany(1)
    assert rows[0]["cntrct_pbp_sk"] == 16513335503

    cur = conn.execute("select * from idr.contract_pbp_contact order by cntrct_pbp_sk")
    assert cur.rowcount == 3
    rows = cur.fetchmany(4)
    assert rows[0]["cntrct_pbp_sk"] == 307963392254
    assert rows[2]["cntrct_pbp_sk"] == 940319838486
    # only a future record exists for this contract
    assert rows[2]["cntrct_pbp_bgn_dt"].strftime("%Y-%m-%d") == "2026-12-01"

    cur = conn.execute("select * from idr.beneficiary_ma_part_d_enrollment order by bene_sk")
    assert cur.rowcount == 3
    rows = cur.fetchmany(1)
    assert rows[0]["bene_sk"] == 353816020

    cur = conn.execute("select * from idr.beneficiary_ma_part_d_enrollment_rx order by bene_sk")
    assert cur.rowcount == 2
    rows = cur.fetchmany(1)
    assert rows[0]["bene_sk"] == 353816020

    cur = conn.execute("select * from idr.beneficiary_low_income_subsidy order by bene_sk")
    assert cur.rowcount == 2
    rows = cur.fetchmany(1)
    assert rows[0]["bene_sk"] == 353816020

    cur = conn.execute("select * from idr.claim_institutional_ss where clm_uniq_id = 8244064276500")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_institutional_nch order by clm_uniq_id")
    assert cur.rowcount == 51
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 113370100080

    cur = conn.execute("select * from idr.claim_institutional_ss order by clm_uniq_id")
    assert cur.rowcount == 21
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 580550863030

    cur = conn.execute("select * from idr.claim_professional_nch order by clm_uniq_id")
    assert cur.rowcount == 33
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 797757725380

    cur = conn.execute("select * from idr.claim_professional_ss order by clm_uniq_id")
    assert cur.rowcount == 1
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 4991490559710

    cur = conn.execute("select * from idr.claim_rx order by clm_uniq_id")
    assert cur.rowcount == 19
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 166776396279

    cur = conn.execute("select * from idr.claim_item_institutional_nch order by clm_uniq_id")
    assert cur.rowcount == 795
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 113370100080

    cur = conn.execute("select * from idr.claim_item_institutional_ss order by clm_uniq_id")
    assert cur.rowcount == 334
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 580550863030

    cur = conn.execute("select * from idr.claim_item_professional_nch order by clm_uniq_id")
    assert cur.rowcount == 442
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 119855147698

    cur = conn.execute("select * from idr.claim_item_professional_ss order by clm_uniq_id")
    assert cur.rowcount == 1
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 4991490559710
