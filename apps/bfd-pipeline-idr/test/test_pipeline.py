import multiprocessing
import os
import shutil
import subprocess
import sys
from collections.abc import Generator
from concurrent.futures import ThreadPoolExecutor
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import cast
from uuid import uuid4

import psycopg
import pytest
from loguru import logger
from psycopg import Connection, sql
from psycopg.rows import DictRow, TupleRow, dict_row
from testcontainers.core.config import testcontainers_config  # type: ignore

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from idr_pipeline import Executor, run
from idr_pipeline.extractor import PostgresExecutor
from idr_pipeline.load_events import IdrJobLoadEvent, IdrJobType
from idr_pipeline.load_partition import LoadType
from idr_pipeline.load_synthetic import load_from_csv
from idr_pipeline.logger_config import configure_logger
from idr_pipeline.model.base_model import LoadMode, Source
from idr_pipeline.parallel_executor import MultiprocessingExecutor, MultithreadingExecutor
from idr_pipeline.pydantic_utils import fields
from idr_pipeline.settings import SETTINGS

# ryuk throws a 500 or 404 error for some reason
# seems to have issues with podman https://github.com/testcontainers/testcontainers-python/issues/753
testcontainers_config.ryuk_disabled = True

# Forces runners to use spawn instead of the default fork when running tests
multiprocessing.set_start_method("spawn", force=True)


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
            cwd=Path(__file__).parent.parent.joinpath("../bfd-db-migrator-ng"),
            shell=True,
            capture_output=True,
            check=True,
        )
    except subprocess.CalledProcessError as ex:
        print(ex.output)
        raise


def _get_executor() -> Executor:
    # Only enable the multithreading executor if a debugger is attached
    # This makes debugging much simpler, but it is also a lot slower
    # So we only want to enable it when necessary
    if "pydevd" in sys.modules:
        return MultithreadingExecutor(SETTINGS.max_tasks)
    return MultiprocessingExecutor(SETTINGS.max_tasks)


def _do_test_pipeline(conn: Connection[DictRow], load_type: LoadType) -> None:
    run(Source.POSTGRES, LoadMode.SYNTHETIC, load_type, _get_executor())

    cur = conn.execute("select * from idr.beneficiary order by bene_sk")
    assert cur.rowcount == 29
    rows = cur.fetchmany(2)

    assert rows[0]["bene_sk"] == 10464258
    assert rows[0]["bene_mbi_id"] == "2ZT2XU2EN18"
    assert rows[1]["bene_sk"] == 16666900
    assert rows[1]["bene_mbi_id"] == "5B88XK5JN88"

    cur = conn.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
    assert cur.rowcount == 24
    rows = cur.fetchmany(1)
    assert rows[0]["bene_mbi_id"] == "1BC3JG0FM51"

    # Xref with valid kill_cred_cd should be included
    cur = conn.execute("select * from idr.beneficiary where bene_sk = 174441863")
    rows = cur.fetchmany(1)
    assert rows[0]["bene_xref_efctv_sk"] == 629529363

    # Xref with no valid entry in v2_bene_xref should not be included
    cur = conn.execute("select * from idr.beneficiary where bene_sk = 353816021")
    rows = cur.fetchmany(1)
    assert rows[0]["bene_xref_efctv_sk"] == 353816021

    cur = conn.execute("select * from idr.prior_auth order by mbi_num")
    assert cur.rowcount == 21
    rows = cur.fetchmany(1)
    assert rows[0]["mbi_num"] == "1OX4Y88RV68"

    cur = conn.execute("select * from idr.prior_auth_item order by mbi_num")
    assert cur.rowcount == 64
    rows = cur.fetchmany(1)
    assert rows[0]["mbi_num"] == "1OX4Y88RV68"

    # Seed stale non-Part-D parent claims so the prune job has rows to delete.
    # CSVs cover item pruning because stale non-Part-D parents do not load.
    if load_type == LoadType.INCREMENTAL:
        claim_table = sql.Identifier("idr", "claim_institutional_nch")
        cur = conn.execute(
            "select * from idr.claim_institutional_nch where clm_uniq_id = 113370100080"
        )
        assert cur.rowcount == 1
        row = cur.fetchone()
        assert row is not None

        stale_institutional_nch_parent_claim = dict(row)
        stale_institutional_nch_parent_claim["clm_uniq_id"] = 999999434801
        stale_institutional_nch_parent_claim["clm_ltst_clm_ind"] = "N"
        stale_institutional_nch_parent_claim["clm_type_cd"] = 40

        columns = sql.SQL(", ").join(
            sql.Identifier(k) for k in stale_institutional_nch_parent_claim
        )
        values = sql.SQL(", ").join(stale_institutional_nch_parent_claim.values())

        conn.execute(
            t"""
            INSERT INTO {claim_table:i}
            (
                {columns:q}
            )
            VALUES
            (
                {values:q}
            )
            """
        )

        claim_table = sql.Identifier("idr", "claim_professional_nch")
        cur = conn.execute(
            "select * from idr.claim_professional_nch where clm_uniq_id = 119855147698"
        )
        assert cur.rowcount == 1
        row = cur.fetchone()
        assert row is not None

        stale_professional_nch_parent_claim = dict(row)
        stale_professional_nch_parent_claim["clm_uniq_id"] = 999999434802
        stale_professional_nch_parent_claim["clm_ltst_clm_ind"] = "N"
        stale_professional_nch_parent_claim["clm_type_cd"] = 81

        columns = sql.SQL(", ").join(sql.Identifier(k) for k in stale_professional_nch_parent_claim)
        values = sql.SQL(", ").join(stale_professional_nch_parent_claim.values())

        conn.execute(
            t"""
            INSERT INTO {claim_table:i}
            (
                {columns:q}
            )
            VALUES
            (
                {values:q}
            )
            """
        )

        claim_table = sql.Identifier("idr", "claim_institutional_ss")
        cur = conn.execute(
            "select * from idr.claim_institutional_ss where clm_uniq_id = 123359318723"
        )
        assert cur.rowcount == 1
        row = cur.fetchone()
        assert row is not None

        stale_institutional_ss_parent_claim = dict(row)
        stale_institutional_ss_parent_claim["clm_uniq_id"] = 999999434800
        stale_institutional_ss_parent_claim["clm_ltst_clm_ind"] = "N"
        stale_institutional_ss_parent_claim["clm_type_cd"] = 2081

        columns = sql.SQL(", ").join(sql.Identifier(k) for k in stale_institutional_ss_parent_claim)
        values = sql.SQL(", ").join(stale_institutional_ss_parent_claim.values())

        conn.execute(
            t"""
            INSERT INTO {claim_table:i}
            (
                {columns:q}
            )
            VALUES
            (
                {values:q}
            )
            """
        )

        claim_table = sql.Identifier("idr", "claim_professional_ss")
        cur = conn.execute(
            "select * from idr.claim_professional_ss where clm_uniq_id = 4991490559710"
        )
        assert cur.rowcount == 1
        row = cur.fetchone()
        assert row is not None

        stale_professional_ss_parent_claim = dict(row)
        stale_professional_ss_parent_claim["clm_uniq_id"] = 999999434803
        stale_professional_ss_parent_claim["clm_ltst_clm_ind"] = "N"
        stale_professional_ss_parent_claim["clm_type_cd"] = 2800

        columns = sql.SQL(", ").join(sql.Identifier(k) for k in stale_professional_ss_parent_claim)
        values = sql.SQL(", ").join(stale_professional_ss_parent_claim.values())

        conn.execute(
            t"""
            INSERT INTO {claim_table:i}
            (
                {columns:q}
            )
            VALUES (
                {values:q}
            )
            """
        )
        conn.commit()

    cur = conn.execute("select max(last_ts) as max_ts from idr.load_progress")
    row = cur.fetchone()
    assert row is not None
    max_ts = cast(datetime, row["max_ts"])
    datetime_now = max_ts + timedelta(days=1)
    _advance_time(datetime_now)

    conn.execute(
        f"""
        UPDATE {SETTINGS.idr_bene_history_table}
        SET bene_mbi_id = '1S000000000', idr_insrt_ts=%(timestamp)s, idr_updt_ts=%(timestamp)s
        WHERE bene_sk = 10464258
        """,  # type: ignore
        {"timestamp": datetime_now},
    )
    conn.commit()

    run(Source.POSTGRES, LoadMode.SYNTHETIC, load_type, _get_executor())

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

    if load_type == LoadType.INITIAL:
        cur = conn.execute("select * from idr.beneficiary_ma_part_d_enrollment order by bene_sk")
        assert cur.rowcount == 4
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 353816020
    else:
        cur = conn.execute("select * from idr.beneficiary_ma_part_d_enrollment order by bene_sk")
        assert cur.rowcount == 3
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 353816020

    if load_type == LoadType.INITIAL:
        cur = conn.execute("select * from idr.beneficiary_ma_part_d_enrollment_rx order by bene_sk")
        assert cur.rowcount == 3
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 353816020
    else:
        cur = conn.execute("select * from idr.beneficiary_ma_part_d_enrollment_rx order by bene_sk")
        assert cur.rowcount == 2
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 353816020

    lis_cmbnd_query = "select * from idr.beneficiary_low_income_subsidy_cmbnd order by bene_sk"
    if load_type == LoadType.INITIAL:
        cur = conn.execute(lis_cmbnd_query)
        assert cur.rowcount == 3
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 353816020
    else:
        cur = conn.execute(lis_cmbnd_query)
        assert cur.rowcount == 2
        rows = cur.fetchmany(1)
        assert rows[0]["bene_sk"] == 353816020

    cur = conn.execute("select * from idr.claim_institutional_ss where clm_uniq_id = 8244064276500")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_institutional_nch order by clm_uniq_id")
    assert cur.rowcount == 63
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == -9879437343384

    # Stale non-Part-D parent claims do not remain in the final claim tables
    cur = conn.execute("select * from idr.claim_institutional_nch where clm_uniq_id = 999999434801")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_professional_nch where clm_uniq_id = 999999434802")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_institutional_ss where clm_uniq_id = 999999434800")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_professional_ss where clm_uniq_id = 999999434803")
    assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_professional_nch order by clm_uniq_id")
    assert cur.rowcount == 56
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == -8309297293881

    cur = conn.execute("select * from idr.claim_professional_ss order by clm_uniq_id")
    assert cur.rowcount == 1
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == 4991490559710

    cur = conn.execute("select * from idr.claim_rx order by clm_uniq_id")
    assert cur.rowcount == 27
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == -8797257401798

    cur = conn.execute("select * from idr.claim_item_institutional_nch order by clm_uniq_id")
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 996
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 995
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == -9879437343384

    # Items for stale non-Part-D claims are pruned on incremental loads
    cur = conn.execute(
        "select * from idr.claim_item_institutional_nch where clm_uniq_id = 999999434801"
    )
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 1
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 0

    cur = conn.execute(
        "select * from idr.claim_item_professional_nch where clm_uniq_id = 999999434802"
    )
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 1
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 0

    cur = conn.execute(
        "select * from idr.claim_item_institutional_ss where clm_uniq_id = 999999434800"
    )
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 1
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 0

    cur = conn.execute(
        "select * from idr.claim_item_professional_ss where clm_uniq_id = 999999434803"
    )
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 1
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 0

    cur = conn.execute("select * from idr.claim_item_professional_nch order by clm_uniq_id")
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 504
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 503
    rows = cur.fetchmany(1)
    assert rows[0]["clm_uniq_id"] == -8309297293881

    cur = conn.execute("select * from idr.claim_item_professional_ss order by clm_uniq_id")
    if load_type == LoadType.INITIAL:
        assert cur.rowcount == 2
    elif load_type == LoadType.INCREMENTAL:
        assert cur.rowcount == 1

    cur = conn.execute(
        "select * from idr.claim_item_professional_ss where clm_uniq_id = 4991490559710"
    )
    assert cur.rowcount == 1

    conn.commit()

    # Phase 1 SS (PAC) claims older than 60 days will be pruned on incremental loads
    if load_type == LoadType.INITIAL:
        cur = conn.execute("select * from idr.claim_institutional_ss order by clm_uniq_id")
        assert cur.rowcount == 21
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 123359318723

        cur = conn.execute("select * from idr.claim_item_institutional_ss order by clm_uniq_id")
        assert cur.rowcount == 328
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 123359318723

    else:
        make_it_stale_ts = datetime.now(UTC) + timedelta(days=60)
        _advance_time(make_it_stale_ts)
        run(Source.POSTGRES, LoadMode.SYNTHETIC, LoadType.INCREMENTAL, _get_executor())
        cur = conn.execute("select * from idr.claim_institutional_ss order by clm_uniq_id")
        assert cur.rowcount == 9
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 849348853948

        cur = conn.execute("select * from idr.claim_item_institutional_ss order by clm_uniq_id")
        assert cur.rowcount == 151
        rows = cur.fetchmany(1)
        assert rows[0]["clm_uniq_id"] == 849348853948

    # Test incremental loading logic involving 'source_load_events' if we're testing incremental
    # mode
    if load_type == LoadType.INCREMENTAL:
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
        _advance_time(ss_clm_ts)
        run(Source.POSTGRES, LoadMode.SYNTHETIC, load_type, _get_executor())

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
        _advance_time(nch_load_job.event_time)
        run(Source.POSTGRES, LoadMode.SYNTHETIC, load_type, _get_executor())

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
        _advance_time(ss_load_job.event_time)
        run(Source.POSTGRES, LoadMode.SYNTHETIC, load_type, _get_executor())

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


def _do_test_prior_auth_update_and_delete(conn: Connection[DictRow], load_type: LoadType) -> None:
    cur = conn.execute(
        "select * from idr.prior_auth where mbi_num = '7ZM6HW2AT68' and utn = '-OTENCJLOQRAKA'"
    )
    assert cur.rowcount == 1
    rows = cur.fetchmany(6)
    assert rows[0]["mbi_num"] == "7ZM6HW2AT68"
    original_updated_ts = rows[0]["bfd_updated_ts"]
    original_name = rows[0]["name"]

    cur = conn.execute(
        "select * from idr.prior_auth where mbi_num = '5OH0K85GU23' and utn = '-SC21YQR4UY4LI'"
    )
    assert cur.rowcount == 1
    row = cur.fetchone()
    assert row is not None

    prauc_table = sql.Identifier("cms_edp_view_cvm_prau_prd", "prauc")
    conn.execute(
        t"""
        UPDATE {prauc_table:i}
        SET name = 'BITE AID PHARMACY'
        WHERE mbi_num = '7ZM6HW2AT68'
        AND utn = '-OTENCJLOQRAKA'
        """
    )

    conn.execute(
        t"""
        DELETE FROM {prauc_table:i}
        WHERE mbi_num = '5OH0K85GU23'
        AND utn = '-SC21YQR4UY4LI'
        """
    )
    conn.commit()

    _advance_time(datetime.now() + timedelta(days=1))
    run(Source.POSTGRES, LoadMode.SYNTHETIC, load_type, _get_executor())

    # verify that updated rows by upstream were updated
    cur = conn.execute(
        "select * from idr.prior_auth where mbi_num = '7ZM6HW2AT68' and utn = '-OTENCJLOQRAKA'"
    )
    assert cur.rowcount == 1
    updated_row = cur.fetchone()
    assert updated_row is not None
    assert updated_row["name"] != original_name
    assert updated_row["bfd_updated_ts"] > original_updated_ts

    # verify that deleted rows by upstream were deleted in header and item level for prior auth
    cur = conn.execute(
        "select * from idr.prior_auth where mbi_num = '5OH0K85GU23' and utn = '-SC21YQR4UY4LI'"
    )
    assert cur.rowcount == 0

    cur = conn.execute(
        "select * from idr.prior_auth_item where mbi_num = '5OH0K85GU23' and utn = '-SC21YQR4UY4LI'"
    )
    assert cur.rowcount == 0

    # verify that untouched rows by upstream were not updated
    cur = conn.execute(
        "select * from idr.prior_auth where mbi_num = '7ZM6HW2AT68' and utn = '-RVUOWAUT5V5QZ'"
    )
    rows = cur.fetchmany(2)
    assert rows[0]["bfd_updated_ts"] < updated_row["bfd_updated_ts"]


def _advance_time(timestamp: datetime) -> None:
    os.environ["BFD_TEST_DATE"] = timestamp.isoformat()


def _do_legacy_npi_type_update(conn: Connection[DictRow]) -> None:
    run(Source.POSTGRES, LoadMode.SYNTHETIC, LoadType.INITIAL, _get_executor())

    cur = conn.execute("select max(last_ts) as max_ts from idr.load_progress")
    row = cur.fetchone()
    assert row is not None
    latest_time = cast(datetime, row["max_ts"]) + timedelta(days=1)
    _advance_time(latest_time)

    conn.execute("truncate table idr.load_progress")
    conn.commit()

    run(Source.POSTGRES, LoadMode.SYNTHETIC, LoadType.INITIAL, _get_executor())

    old_update_ts = datetime.fromisoformat("2023-04-02").replace(tzinfo=UTC)

    cur = conn.execute("select * from idr.claim_rx where clm_uniq_id = -1784862973911")
    row = cur.fetchone()
    assert row is not None
    assert row["prvdr_prscrbng_prvdr_npi_num"] == "1789655200"
    assert row["prvdr_prsbng_id_qlfyr_cd"] == "01"
    assert row["bfd_prvdr_prscrbng_npi_type"] == 1
    assert row["bfd_updated_ts"] == old_update_ts
    assert row["bfd_claim_updated_ts"] == old_update_ts

    cur = conn.execute("select max(bfd_claim_updated_ts) as max_claim_updated_ts from idr.claim_rx")
    row = cur.fetchone()
    assert row is not None
    assert row["max_claim_updated_ts"] >= latest_time

    cur = conn.execute("select * from idr.claim_rx where clm_uniq_id = -6260496095505")
    row = cur.fetchone()
    assert row is not None
    assert row["prvdr_prscrbng_prvdr_npi_num"] == "1820038259"
    assert row["prvdr_prsbng_id_qlfyr_cd"] == "01"
    assert row["bfd_prvdr_prscrbng_npi_type"] is None
    assert row["bfd_updated_ts"] == latest_time
    assert row["bfd_claim_updated_ts"] == latest_time

    cur = conn.execute("select * from idr.claim_institutional_ss where clm_uniq_id = 580550863030")
    row = cur.fetchone()
    assert row is not None
    assert row["prvdr_othr_prvdr_npi_num"] == "1320757457"
    assert row["clm_othr_fed_prvdr_spclty_cd"] == "93"
    assert row["bfd_prvdr_othr_npi_type"] == 1
    assert row["bfd_updated_ts"] == old_update_ts
    assert row["bfd_claim_updated_ts"] == old_update_ts

    cur = conn.execute(
        "select * from idr.claim_item_professional_nch where clm_uniq_id = -8309297293881 "
        "and prvdr_rndrng_prvdr_npi_num = '2658486156'"
    )
    row = cur.fetchone()
    assert row is not None
    assert row["clm_rndrg_fed_prvdr_spclty_cd"] == ""
    assert row["bfd_prvdr_rndrng_npi_type"] == 1
    assert row["bfd_updated_ts"] == latest_time
    # verify that bfd_claim_updated_ts on claim header table was updated as a result
    cur = conn.execute(
        "select * from idr.claim_professional_nch where clm_uniq_id = -8309297293881 "
    )
    row = cur.fetchone()
    assert row is not None
    assert row["bfd_claim_updated_ts"] == latest_time

    cur = conn.execute(
        "select * from idr.claim_item_professional_nch where clm_uniq_id = -8309297293881 "
        "and prvdr_rndrng_prvdr_npi_num = '1820038259'"
    )
    row = cur.fetchone()
    assert row is not None
    assert row["clm_rndrg_fed_prvdr_spclty_cd"] == ""
    assert row["bfd_prvdr_rndrng_npi_type"] is None
    assert row["bfd_updated_ts"] == old_update_ts
    # bfd_claim_updated_ts updated in claim -8309297293881 header still since a different
    # npi_type (prvdr_rndrg_prvdr_npi_num) was actually updated. See above.

    cur = conn.execute(
        "select * from idr.claim_professional_nch where clm_uniq_id = -8309297293881"
    )
    row = cur.fetchone()
    assert row is not None
    assert row["prvdr_srvc_prvdr_npi_num"] == "1819676937"
    assert row["bfd_prvdr_srvc_npi_type"] == 2
    assert row["bfd_updated_ts"] == latest_time
    assert row["bfd_claim_updated_ts"] == latest_time


def _reset_db(
    conn: psycopg.Connection[TupleRow], sample_path: Path, postgres: PostgresContainer
) -> None:
    conn.execute(
        """
        DO $$ DECLARE
            r RECORD;
        BEGIN
            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'idr') LOOP
                EXECUTE 'DROP TABLE idr.' || quote_ident(r.tablename) || ' CASCADE';
            END LOOP;

            FOR r IN (
                SELECT tablename FROM pg_tables WHERE schemaname = 'cms_vdm_view_mdcr_prd'
            ) LOOP
                EXECUTE 'DROP TABLE cms_vdm_view_mdcr_prd.'
                    || quote_ident(r.tablename)
                    || ' CASCADE';
            END LOOP;

            FOR r IN (
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'cms_edp_view_cvm_prau_prd'
            ) LOOP
                EXECUTE 'DROP TABLE cms_edp_view_cvm_prau_prd.'
                    || quote_ident(r.tablename)
                    || ' CASCADE';
            END LOOP;
        END $$;
        """
    )
    conn.commit()

    with Path(__file__).parent.parent.joinpath("./mock-idr.sql").open() as f:
        conn.execute(f.read())  # type: ignore
    conn.commit()

    _run_migrator(postgres)
    load_from_csv(PostgresExecutor(conn), sample_path)  # type: ignore


def _setup_pipeline_environment() -> None:
    # Info level logs obscure the error output when running tests
    # so we want to override this unless the calling process has set this explicitly
    os.environ.setdefault("IDR_LOG_LEVEL", "warning")
    # Prevent user-defined environment variables from overriding the defaults
    os.environ["IDR_BATCH_SIZE"] = "100000"
    os.environ["IDR_TEST_MODE"] = "1"
    os.environ["IDR_MAX_TASKS"] = "4"
    os.environ["BFD_TEST_DATE"] = "2023-04-02"
    os.environ["IDR_PER_BATCH_MIN_CONNECTIONS"] = "1"
    os.environ["IDR_PER_BATCH_MAX_CONNECTIONS"] = "1"
    os.environ["IDR_MIN_CLAIM_NCH_TRANSACTION_DATE"] = SETTINGS.min_claim_nch_transaction_date
    os.environ["IDR_MIN_CLAIM_SS_TRANSACTION_DATE"] = SETTINGS.min_claim_ss_transaction_date
    os.environ["IDR_ENABLE_NPI_TYPE_BACKFILL"] = "1"


def _setup_db_config(info: psycopg.ConnectionInfo) -> None:
    os.environ["BFD_DB_ENDPOINT"] = info.host
    os.environ["BFD_DB_PORT"] = str(info.port)
    os.environ["BFD_DB_NAME"] = info.dbname
    os.environ["BFD_DB_USERNAME"] = info.user
    os.environ["BFD_DB_PASSWORD"] = info.password


@pytest.fixture(scope="module")
def postgres_db() -> Generator[tuple[PostgresContainer, str]]:
    with PostgresContainer("postgres:16", driver="") as postgres:
        conninfo = postgres.get_connection_url()
        yield postgres, conninfo


def _test_pipeline_load(postgres_db: tuple[PostgresContainer, str], load_type: LoadType) -> None:
    _setup_pipeline_environment()
    configure_logger()
    postgres, conninfo = postgres_db
    with psycopg.connect(conninfo=conninfo, row_factory=dict_row) as conn:  # pyright: ignore[reportArgumentType]
        sample_dir = Path(__file__).parent.parent.joinpath("./test_samples1")
        _reset_db(conn, sample_dir, postgres)
        _setup_db_config(conn.info)
        _do_test_pipeline(cast(Connection[DictRow], conn), load_type)
        _do_test_prior_auth_update_and_delete(cast(Connection[DictRow], conn), load_type)
    logger.remove()


def _test_load_progress_concurrent(conn: Connection[DictRow]) -> None:
    cur = conn.execute(
        "select max_run_ts from idr.load_progress where job_id = 1 and max_run_ts is not null"
    )
    rows = cur.fetchmany(1)
    assert len(rows) == 0
    cur = conn.execute(
        "select * from idr.load_progress where job_id = 2 and max_run_ts is not null"
    )
    rows_2 = cur.fetchmany(1)
    assert len(rows_2) == 1
    job_row = rows_2[0]
    partition: str = job_row["batch_partition"]
    table: str = job_row["table_name"]
    cur = conn.execute(
        """select * from idr.load_progress where job_id = 1 
        and batch_partition = %(batch_partition)s 
        and table_name = %(table_name)s
    """,
        {
            "batch_partition": partition,
            "table_name": table,
        },
    )
    rows = cur.fetchmany(1)
    assert job_row["max_run_ts"] == rows[0]["last_ts"]
    # test table counts
    cur = conn.execute(
        "select DISTINCT table_name from idr.load_progress where job_id = 1"
        " EXCEPT "
        "select DISTINCT table_name from idr.load_progress where job_id = 2"
    )
    rows = cur.fetchmany(1)
    cur = conn.execute("select count(*) as row_count from idr.load_progress where job_id = 2")
    assert len(rows) == 0


def test_initial_pipeline_load(postgres_db: tuple[PostgresContainer, str]) -> None:
    _test_pipeline_load(postgres_db, LoadType.INITIAL)


def test_incremental_pipeline_load(postgres_db: tuple[PostgresContainer, str]) -> None:
    _test_pipeline_load(postgres_db, LoadType.INCREMENTAL)


def test_legacy_npi_type_pipeline_update(postgres_db: tuple[PostgresContainer, str]) -> None:
    _setup_pipeline_environment()
    configure_logger()
    postgres, conninfo = postgres_db
    with psycopg.connect(conninfo=conninfo, row_factory=dict_row) as conn:  # pyright: ignore[reportArgumentType]
        sample_dir = Path(__file__).parent.parent.joinpath("./test_samples1")
        _reset_db(conn, sample_dir, postgres)
        _setup_db_config(conn.info)
        _do_legacy_npi_type_update(cast(Connection[DictRow], conn))
    logger.remove()


def run_1() -> None:
    run(Source.POSTGRES, LoadMode.SYNTHETIC, LoadType.INCREMENTAL, _get_executor())


def run_2() -> None:
    run(Source.POSTGRES, LoadMode.SYNTHETIC, LoadType.INITIAL, _get_executor(), 2)


def test_concurrent_pipeline_load(postgres_db: tuple[PostgresContainer, str]) -> None:
    _setup_pipeline_environment()
    postgres, conninfo = postgres_db
    with psycopg.connect(conninfo=conninfo, row_factory=dict_row) as conn:  # pyright: ignore[reportArgumentType]
        sample_dir = Path(__file__).parent.parent.joinpath("./test_samples1")
        _reset_db(conn, sample_dir, postgres)
        _setup_db_config(conn.info)
        run(Source.POSTGRES, LoadMode.SYNTHETIC, LoadType.INITIAL, _get_executor())
        with ThreadPoolExecutor(max_workers=2) as executor:
            futures = [
                executor.submit(run_1),
                executor.submit(run_2),
            ]

            # This will now correctly catch any real pipeline failures
            for future in futures:
                future.result()

        _test_load_progress_concurrent(conn=cast(Connection[DictRow], conn))
