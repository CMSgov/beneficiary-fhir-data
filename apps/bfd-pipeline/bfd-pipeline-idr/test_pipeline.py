import os
import sys
import time
from datetime import UTC, datetime
from pathlib import Path
from typing import cast

import psycopg
import ray
from psycopg import Connection
from psycopg.rows import DictRow, dict_row
from testcontainers.core.config import testcontainers_config  # type: ignore

# https://github.com/testcontainers/testcontainers-python/issues/305
from testcontainers.postgres import PostgresContainer  # type: ignore

from load_synthetic import load_from_csv
from pipeline import main

# ryuk throws a 500 or 404 error for some reason
# seems to have issues with podman https://github.com/testcontainers/testcontainers-python/issues/753
testcontainers_config.ryuk_disabled = True


def setup_postgres(conn: Connection[DictRow]) -> None:
    with Path("./mock-idr.sql").open() as f:
        conn.execute(f.read())  # type: ignore
    conn.commit()
    with Path("./bfd.sql").open() as f:
        conn.execute(f.read())  # type: ignore
    conn.commit()

    load_from_csv(conn, "./test_samples1")  # type: ignore
    conn.commit()


def test_pipeline() -> None:
    with PostgresContainer("postgres:16", driver="") as postgres:
        conn = psycopg.connect(postgres.get_connection_url())
        setup_postgres(conn)  # type: ignore
        conn.close()

        dsn_conn = psycopg.connect(postgres.get_connection_url())
        info = dsn_conn.info
        os.environ["BFD_DB_ENDPOINT"] = info.host
        os.environ["BFD_DB_PORT"] = str(info.port)
        os.environ["BFD_DB_NAME"] = info.dbname
        os.environ["BFD_DB_USERNAME"] = info.user
        os.environ["BFD_DB_PASSWORD"] = info.password
        dsn_conn.close()

        os.environ["PARALLELISM"] = "2"
        os.environ["IDR_BATCH_SIZE"] = "100000"
        os.environ["IDR_LOAD_BENES"] = "true"
        os.environ["IDR_LOAD_CLAIMS"] = "true"

        sys.argv = ["pipeline.py", "synthetic"]
        main()

        with (
            cast(
                psycopg.Connection[DictRow],
                psycopg.connect(postgres.get_connection_url(), row_factory=dict_row),  # type: ignore
            ) as conn,
            conn.cursor() as cur,
        ):
            cur.execute("select * from idr.beneficiary order by bene_sk")
            assert cur.rowcount == 25
            rows = cur.fetchmany(2)
            assert rows[0]["bene_sk"] == 10464258  # type: ignore
            assert rows[0]["bene_mbi_id"] == "2ZT2XU2EN18"  # type: ignore
            assert rows[1]["bene_sk"] == 16666900  # type: ignore
            assert rows[1]["bene_mbi_id"] == "5B88XK5JN88"  # type: ignore

            cur.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
            assert cur.rowcount == 21
            rows = cur.fetchmany(1)
            assert rows[0]["bene_mbi_id"] == "1BC3JG0FM51"  # type: ignore

            cur.execute("select * from idr.beneficiary_mbi_id order by bene_mbi_id")
            assert cur.rowcount == 21
            rows = cur.fetchmany(1)
            assert rows[0]["bene_mbi_id"] == "1BC3JG0FM51"  # type: ignore

            # Wait for system time to advance enough to update the timestamp
            time.sleep(0.05)
            cur.execute(
                """
                UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry
                SET bene_mbi_id = '1S000000000', idr_insrt_ts=%(timestamp)s, 
                idr_updt_ts=%(timestamp)s
                WHERE bene_sk = 10464258
                """,
                {"timestamp": datetime.now(UTC)},
            )
            conn.commit()
            ray.shutdown()  # type: ignore
            main()

            cur.execute("select * from idr.beneficiary order by bene_sk")
            rows = cur.fetchmany(2)
            assert rows[0]["bene_mbi_id"] == "1S000000000"  # type: ignore
            assert rows[1]["bene_mbi_id"] == "5B88XK5JN88"  # type: ignore

            cur.execute(
                "select * from idr.beneficiary where bene_kill_cred_cd != '' order by bene_sk"
            )
            assert cur.rowcount == 5
            rows = cur.fetchmany(1)
            assert rows[0]["bene_sk"] == 174441863  # type: ignore

            cur.execute("select * from idr.beneficiary_third_party order by bene_sk")
            assert cur.rowcount == 4
            rows = cur.fetchmany(1)
            assert rows[0]["bene_sk"] == 16666900  # type: ignore

            cur.execute("select * from idr.beneficiary_status order by bene_sk")
            assert cur.rowcount == 15
            rows = cur.fetchmany(1)
            assert rows[0]["bene_sk"] == 10464258  # type: ignore

            cur.execute("select * from idr.beneficiary_entitlement order by bene_sk")
            assert cur.rowcount == 30
            rows = cur.fetchmany(1)
            assert rows[0]["bene_sk"] == 10464258  # type: ignore

            cur.execute("select * from idr.beneficiary_entitlement_reason order by bene_sk")
            assert cur.rowcount == 15
            rows = cur.fetchmany(1)
            assert rows[0]["bene_sk"] == 10464258  # type: ignore

            cur.execute("select * from idr.beneficiary_dual_eligibility order by bene_sk")
            assert cur.rowcount == 4
            rows = cur.fetchmany(1)
            assert rows[0]["bene_sk"] == 47347082  # type: ignore

            cur.execute("select * from idr.beneficiary_overshare_mbi order by bene_mbi_id")
            assert cur.rowcount == 2
            rows = cur.fetchmany(2)
            assert rows[0]["bene_mbi_id"] == "5OH0K85GU23"  # type: ignore
            assert rows[1]["bene_mbi_id"] == "6LM1C27GV22"  # type: ignore

            cur.execute("select * from idr.claim order by clm_uniq_id")
            assert cur.rowcount == 144
            rows = cur.fetchmany(1)
            assert rows[0]["clm_uniq_id"] == 113370100080  # type: ignore
            assert rows[0]["clm_nrln_ric_cd"] == "W"  # type: ignore

            cur.execute("select * from idr.claim_institutional order by clm_uniq_id")
            assert cur.rowcount == 74
            rows = cur.fetchmany(1)
            assert rows[0]["clm_uniq_id"] == 113370100080  # type: ignore

            cur.execute("select * from idr.claim_date_signature order by clm_dt_sgntr_sk")
            assert cur.rowcount == 144
            rows = cur.fetchmany(1)
            assert rows[0]["clm_dt_sgntr_sk"] == 2334117069  # type: ignore

            cur.execute("select * from idr.claim_professional order by clm_uniq_id")
            assert cur.rowcount == 86
            rows = cur.fetchmany(1)
            assert rows[0]["clm_uniq_id"] == 113370100080  # type: ignore

            cur.execute("select * from idr.claim_item order by clm_uniq_id")
            assert cur.rowcount == 1624
            rows = cur.fetchmany(1)
            assert rows[0]["clm_uniq_id"] == 113370100080  # type: ignore

            cur.execute("select * from idr.claim_line_institutional order by clm_uniq_id")
            assert cur.rowcount == 612
            rows = cur.fetchmany(1)
            assert rows[0]["clm_uniq_id"] == 113370100080  # type: ignore

            cur.execute("select * from idr.claim_line_professional order by clm_uniq_id")
            assert cur.rowcount == 281
            rows = cur.fetchmany(1)
            assert rows[0]["clm_uniq_id"] == 797757725380  # type: ignore

            cur.execute("select * from idr.claim_ansi_signature order by clm_ansi_sgntr_sk")
            assert cur.rowcount == 12072
            rows = cur.fetchmany(1)
            assert rows[0]["clm_ansi_sgntr_sk"] == 0  # type: ignore

            # TODO: add these back when contract data is added
            # cur = conn.execute("select * from idr.beneficiary_election_period_usage")
            # rows = cur.fetchall()
            # assert len(rows) == 1
            # assert rows[0]["cntrct_pbp_sk"] == 1

            # cur = conn.execute("select * from idr.contract_pbp_number")
            # rows = cur.fetchall()
            # assert len(rows) == 1
            # assert rows[0]["cntrct_pbp_sk"] == 1
