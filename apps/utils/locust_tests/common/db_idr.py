import logging
import os
from string.templatelib import Template

import psycopg
from psycopg import sql

logger = logging.getLogger()


def _execute(uri: str, query: Template) -> list:
    """Execute a PSQL select statement and return its results."""
    if uri == "":
        uri = f"""user={os.environ["PGUSER"]} password={os.environ["PGPASSWORD"]}
        host={os.environ["PGHOST"]} dbname={os.environ["PGDATABASE"]}"""
    conn = None
    results = []
    try:
        with psycopg.connect(uri) as conn, conn.cursor() as cursor:
            cursor.execute(query)
            results = cursor.fetchall()
    except Exception as ex:
        logger.error("Error creating database connection: %s", ex)
    finally:
        if conn:
            conn.close()

    return results


VALID_BENE_SQL = sql.SQL("""(
    SELECT b.*
    FROM idr.beneficiary b
    WHERE b.idr_ltst_trans_flg = 'Y'
        AND NOT EXISTS (
            SELECT 1
            FROM idr.beneficiary_overshare_mbi om
            WHERE om.bene_mbi_id = b.bene_mbi_id
        )
)""")


# table_sample_pct is required for the interface even though it's unused here
def get_regression_bene_sks(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = t"""
        SELECT bene_sk
        FROM {VALID_BENE_SQL:q}
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_current_part_a_bene_sks(
    uri: str,
    table_sample_pct: float | None = None,  # noqa: ARG001
) -> list[str]:
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = t"""
        SELECT b.bene_sk
        FROM {VALID_BENE_SQL:q} b
        JOIN idr.beneficiary_third_party tp ON b.bene_sk = tp.bene_sk
        WHERE b.bene_sk = b.bene_xref_efctv_sk AND tp.bene_tp_type_cd = 'A'
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_current_part_b_bene_sks(
    uri: str,
    table_sample_pct: float | None = None,  # noqa: ARG001
) -> list[str]:
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = t"""
        SELECT b.bene_sk
        FROM {VALID_BENE_SQL:q} b
        JOIN idr.beneficiary_third_party tp ON b.bene_sk = tp.bene_sk
        WHERE b.bene_sk = b.bene_xref_efctv_sk AND tp.bene_tp_type_cd = 'B'
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_bene_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list list of MBIs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = t"""
        SELECT bene_mbi_id
        FROM {VALID_BENE_SQL:q}
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_claim_ids(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list list of claim Ids.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    claim_query = t"""
    SELECT clm_uniq_id FROM (
        SELECT clm_uniq_id FROM idr.claim_rx
        JOIN {VALID_BENE_SQL:q} AS b
            ON claim_rx.bene_sk = b.bene_sk
        WHERE clm_ltst_clm_ind = 'Y'
        LIMIT 200
        ) t1
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_institutional_nch.clm_uniq_id FROM idr.claim_institutional_nch
            JOIN {VALID_BENE_SQL:q} AS b
                ON claim_institutional_nch.bene_sk = b.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_institutional_nch
                WHERE claim_item_institutional_nch.clm_uniq_id = claim_institutional_nch.clm_uniq_id
            ) AND clm_ltst_clm_ind = 'Y'
            LIMIT 200
        ) t2
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_institutional_ss.clm_uniq_id FROM idr.claim_institutional_ss
            JOIN {VALID_BENE_SQL:q} AS b
                ON claim_institutional_ss.bene_sk = b.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_institutional_ss
                WHERE claim_item_institutional_ss.clm_uniq_id = claim_institutional_ss.clm_uniq_id
            ) AND clm_ltst_clm_ind = 'Y'
            LIMIT 200
        ) t3
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_professional_nch.clm_uniq_id FROM idr.claim_professional_nch
            JOIN {VALID_BENE_SQL:q} AS b
                ON claim_professional_nch.bene_sk = b.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_professional_nch
                WHERE claim_item_professional_nch.clm_uniq_id = claim_professional_nch.clm_uniq_id
            ) AND clm_ltst_clm_ind = 'Y'
            LIMIT 200
        ) t4
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_professional_ss.clm_uniq_id FROM idr.claim_professional_ss
            JOIN {VALID_BENE_SQL:q} AS b
                ON claim_professional_ss.bene_sk = b.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_professional_ss
                WHERE claim_item_professional_ss.clm_uniq_id = claim_professional_ss.clm_uniq_id
            ) AND clm_ltst_clm_ind = 'Y'
            LIMIT 200
        ) t5;
    """
    return [str(r[0]) for r in set(x for x in _execute(uri, claim_query))]
