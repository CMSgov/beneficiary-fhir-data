import os
import shutil
import subprocess
from collections.abc import Generator
from datetime import datetime, timedelta
from pathlib import Path
from typing import cast
from uuid import uuid4

import psycopg
import pytest
from psycopg import sql
from psycopg.rows import DictRow, dict_row
from testcontainers.core.config import testcontainers_config  # type: ignore

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from constants import IDR_BENE_HISTORY_TABLE, PAC_PHASE_1_MAX, PAC_PHASE_1_MIN
from load_events import IdrJobLoadEvent, IdrJobType
from load_partition import LoadType
from load_synthetic import load_from_csv
from logger_config import configure_logger
from model.base_model import LoadMode
from pipeline import run
from pydantic_utils import fields
from settings import LOAD_TYPE

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
            os.environ["BFD_TEST_DATE"] = "2023-04-02"
        yield postgres


def test_pipeline(setup_db: PostgresContainer) -> None:
    conn = cast(
        psycopg.Connection[DictRow],
        psycopg.connect(setup_db.get_connection_url(), row_factory=dict_row),  # type: ignore
    )

    conn.commit()

    run(LoadMode.SYNTHETIC)

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
    row = cur.fetchone()
    assert row is not None
    max_ts = cast(datetime, row["max_ts"])
    datetime_now = max_ts + timedelta(days=1)
    advance_time(datetime_now)

    conn.execute(
        f"""
        UPDATE {IDR_BENE_HISTORY_TABLE}
        SET bene_mbi_id = '1S000000000', idr_insrt_ts=%(timestamp)s, idr_updt_ts=%(timestamp)s
        WHERE bene_sk = 10464258
        """,
        {"timestamp": datetime_now},
    )
    conn.commit()

    run(LoadMode.SYNTHETIC)

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
    assert cur.rowcount == 7
    rows = cur.fetchmany(7)
    assert rows[0]["cntrct_pbp_sk"] == 130640088184
    assert rows[6]["cntrct_pbp_sk"] == 940319838486
    # only a future record exists for this contract
    assert rows[6]["cntrct_pbp_bgn_dt"].strftime("%Y-%m-%d") == "2026-12-01"

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
    assert rows[0]["clm_uniq_id"] == 123359318723

    cur = conn.execute("select * from idr.claim_professional_nch order by clm_uniq_id")
    assert cur.rowcount == 51
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 119855147698

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
    assert cur.rowcount == 327
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 123359318723

    cur = conn.execute("select * from idr.claim_item_professional_nch order by clm_uniq_id")
    assert cur.rowcount == 442
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 119855147698

    cur = conn.execute("select * from idr.claim_item_professional_ss order by clm_uniq_id")
    assert cur.rowcount == 1
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 4991490559710

    conn.commit()

    # Test incremental loading logic involving 'source_load_events' if we're testing incremental
    # mode
    if LOAD_TYPE == LoadType.INCREMENTAL:
        # First, pretend that loading ./test_samples1 was the result of loading _all_ possible jobs
        # by inserting load events with completion times of datetime_now + 1hr for all types
        idr_jobs_table = sql.Identifier("idr", "source_load_events")
        cur = conn.execute("select max(last_ts) as max_ts from idr.load_progress")
        row = cur.fetchone()
        assert row is not None
        datetime_now = cast(datetime, row["max_ts"])
        load_1_complete_time = datetime_now + timedelta(hours=1)
        load_jobs = [
            IdrJobLoadEvent(
                id=uuid4(),
                job_type=job_type,
                job_message="SUCCESSFUL",
                event_time=datetime_now,
                completion_time=load_1_complete_time,
            ).model_dump(by_alias=True)
            for job_type in IdrJobType
        ]
        for job in load_jobs:
            conn.execute(
                t"""
                INSERT INTO {idr_jobs_table:i} (
                    {sql.SQL(", ").join(sql.Identifier(k) for k in job):q}
                )
                VALUES (
                    {sql.SQL(", ").join(job.values()):q}
                )
                """
            )
        conn.commit()

        # To simulate a new CLMNCH and FISS load, get a known NCH claim and re-insert it with an
        # updated insert timestamp and ID into the relevant institutional claim staging tables (CLM
        # and CLM_INSTNL)
        staging_clm_table = sql.Identifier("cms_vdm_view_mdcr_prd", "v2_mdcr_clm")
        cur = conn.execute(
            t"""
            SELECT * from {staging_clm_table:i}
            WHERE {"clm_uniq_id":i} = {"0113370100080"}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        nch_clm_row = cur.fetchmany(1)[0]
        nch_clm_ts = load_1_complete_time + timedelta(hours=1)
        nch_clm_row["clm_uniq_id"] = (
            "9999999999998"  # This clm_uniq_id does not exist in ./test_samples1
        )
        nch_clm_row["clm_num_sk"] = 2
        nch_clm_row["idr_insrt_ts"] = nch_clm_ts
        nch_clm_row["idr_updt_ts"] = nch_clm_ts
        conn.execute(
            t"""
            INSERT INTO {staging_clm_table:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in nch_clm_row):q}
            )
            VALUES (
                {sql.SQL(", ").join(nch_clm_row.values()):q}
            )
            """
        )
        conn.commit()
        staging_clm_instnl_table = sql.Identifier("cms_vdm_view_mdcr_prd", "v2_mdcr_clm_instnl")
        cur = conn.execute(
            t"""
            SELECT * from {staging_clm_instnl_table:i}
            WHERE {"clm_dt_sgntr_sk":i} = {"876776550714"}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        nch_clm_instnl_row = cur.fetchmany(1)[0]
        nch_clm_instnl_row["clm_num_sk"] = 2
        nch_clm_instnl_row["idr_insrt_ts"] = nch_clm_ts
        nch_clm_instnl_row["idr_updt_ts"] = nch_clm_ts
        cur = conn.execute(
            t"""
            INSERT INTO {staging_clm_instnl_table:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in nch_clm_instnl_row):q}
            )
            VALUES (
                {sql.SQL(", ").join(nch_clm_instnl_row.values()):q}
            )
            """
        )
        conn.commit()

        # Do it again for a known shared-systems claim
        cur = conn.execute(
            t"""
            SELECT * from {staging_clm_table:i}
            WHERE {"clm_uniq_id":i} = {"849348853948"}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        ss_clm_row = cur.fetchmany(1)[0]
        ss_clm_ts = load_1_complete_time + timedelta(hours=1)
        ss_clm_row["clm_uniq_id"] = (
            "9999999999999"  # This clm_uniq_id does not exist in ./test_samples1
        )
        ss_clm_row["clm_num_sk"] = 2
        ss_clm_row["idr_insrt_ts"] = ss_clm_ts
        ss_clm_row["idr_updt_ts"] = ss_clm_ts
        conn.execute(
            t"""
            INSERT INTO {staging_clm_table:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in ss_clm_row):q}
            )
            VALUES (
                {sql.SQL(", ").join(ss_clm_row.values()):q}
            )
            """
        )
        conn.commit()
        cur = conn.execute(
            t"""
            SELECT * from {staging_clm_instnl_table:i}
            WHERE {"clm_dt_sgntr_sk":i} = {"246326234188"}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        ss_clm_instnl_row = cur.fetchmany(1)[0]
        ss_clm_instnl_row["clm_num_sk"] = 2
        ss_clm_instnl_row["idr_insrt_ts"] = ss_clm_ts
        ss_clm_instnl_row["idr_updt_ts"] = ss_clm_ts
        cur = conn.execute(
            t"""
            INSERT INTO {staging_clm_instnl_table:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in ss_clm_instnl_row):q}
            )
            VALUES (
                {sql.SQL(", ").join(ss_clm_instnl_row.values()):q}
            )
            """
        )
        conn.commit()

        # Simulate running the pipeline in the middle of an "ongoing load" (NCH + SS claims being
        # added)
        advance_time(ss_clm_ts)
        run(LoadMode.SYNTHETIC)

        # Check to make sure the NCH claim was not loaded as no corresponding event should exist
        # in source_load_events nor has it been 24 hours since the last load of NCH data
        nch_table = sql.Identifier("idr", "claim_institutional_nch")
        cur = conn.execute(
            t"""
            SELECT * FROM {nch_table:i}
            WHERE {"clm_uniq_id":i} = {nch_clm_row["clm_uniq_id"]}
            """
        )
        conn.commit()
        assert cur.rowcount == 0

        # _Now_ insert an event into source_load_events indicating that the NCH load job was
        # completed
        nch_load_job = IdrJobLoadEvent(
            id=uuid4(),
            job_type=IdrJobType.NCH,
            job_message="SUCCESSFUL",
            event_time=nch_clm_ts + timedelta(hours=1),
        )
        nch_job_dict = nch_load_job.model_dump(by_alias=True)
        conn.execute(
            t"""
            INSERT INTO {idr_jobs_table:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in nch_job_dict):q}
            )
            VALUES (
                {sql.SQL(", ").join(nch_job_dict.values()):q}
            )
            """
        )
        conn.commit()

        # Run the Pipeline with the NCH event having been inserted indicating that there is NCH
        # data to load
        advance_time(nch_load_job.event_time)
        run(LoadMode.SYNTHETIC)

        # Check for the NCH claim in the v3 idr schema
        cur = conn.execute(
            t"""
            SELECT * FROM {nch_table:i}
            WHERE {"clm_uniq_id":i} = {nch_clm_row["clm_uniq_id"]}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert str(rows[0]["clm_uniq_id"]) == str(nch_clm_row["clm_uniq_id"])

        # Confirm the NCH load event has a completion time
        cur = conn.execute(
            t"""
            SELECT * FROM {idr_jobs_table:i}
            WHERE {fields(IdrJobLoadEvent).id:i} = {nch_load_job.id}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        updated_nch_job = IdrJobLoadEvent.model_validate(cur.fetchmany(1)[0], by_alias=True)
        assert updated_nch_job.completion_time
        assert updated_nch_job.completion_time >= nch_clm_ts

        # Check that the SS claim was _not_ loaded since its load job has not "yet" completed
        ss_table = sql.Identifier("idr", "claim_institutional_ss")
        cur = conn.execute(
            t"""
            SELECT * FROM {ss_table:i}
            WHERE {"clm_uniq_id":i} = {ss_clm_row["clm_uniq_id"]}
            """
        )
        conn.commit()
        assert cur.rowcount == 0

        # _Now_ insert an event into source_load_events indicating that the FISS load job was
        # completed
        ss_load_job = IdrJobLoadEvent(
            id=uuid4(),
            job_type=IdrJobType.FISS,
            job_message="SUCCESSFUL",
            event_time=ss_clm_ts + timedelta(hours=1.5),
        )
        ss_job_dict = ss_load_job.model_dump(by_alias=True)
        conn.execute(
            t"""
            INSERT INTO {idr_jobs_table:i} (
                {sql.SQL(", ").join(sql.Identifier(k) for k in ss_job_dict):q}
            )
            VALUES (
                {sql.SQL(", ").join(ss_job_dict.values()):q}
            )
            """
        )
        conn.commit()

        # Run one last time now that the FISS "job" has completed and the SS claim can be loaded
        advance_time(ss_load_job.event_time)
        run(LoadMode.SYNTHETIC)

        # Check for the SS claim in the v3 idr schema
        cur = conn.execute(
            t"""
            SELECT * FROM {ss_table:i}
            WHERE {"clm_uniq_id":i} = {ss_clm_row["clm_uniq_id"]}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        rows = cur.fetchmany(1)
        assert str(rows[0]["clm_uniq_id"]) == str(ss_clm_row["clm_uniq_id"])

        # Confirm the SS load event has a completion time
        cur = conn.execute(
            t"""
            SELECT * FROM {idr_jobs_table:i}
            WHERE {fields(IdrJobLoadEvent).id:i} = {ss_load_job.id}
            """
        )
        conn.commit()
        assert cur.rowcount == 1
        updated_ss_job = IdrJobLoadEvent.model_validate(cur.fetchmany(1)[0], by_alias=True)
        assert updated_ss_job.completion_time
        assert updated_ss_job.completion_time >= ss_clm_ts

        advance_time(datetime_now - timedelta(days=60))
        run(LoadMode.SYNTHETIC)

        claim_prof_ss = sql.Identifier("idr", "claim_professional_ss")

        date_minus_sixty = os.environ["BFD_TEST_DATE"]
        cur = conn.execute(
            t"""
            SELECT clm_uniq_id FROM {claim_prof_ss:i}
            WHERE clm_type_cd BETWEEN {PAC_PHASE_1_MIN} AND {PAC_PHASE_1_MAX} 
            AND bfd_updated_ts <= {date_minus_sixty};
            """
        )
        conn.commit()
        assert cur.rowcount == 0

        cur = conn.execute(
            t"""
            SELECT clm_uniq_id FROM {ss_table:i}
            WHERE clm_type_cd BETWEEN {PAC_PHASE_1_MIN} AND {PAC_PHASE_1_MAX} 
            AND bfd_updated_ts <= {date_minus_sixty};
            """
        )
        conn.commit()
        assert cur.rowcount == 0


def advance_time(timestamp: datetime) -> None:
    new_time = timestamp + timedelta(minutes=1)
    os.environ["BFD_TEST_DATE"] = new_time.isoformat()
