import logging
import os
from typing import LiteralString

import psycopg

logger = logging.getLogger()


def _execute(uri: str, query: LiteralString) -> list:
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


# table_sample_pct is required for the interface even though it's unused here
def get_regression_bene_sks(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = "SELECT bene_sk FROM idr.valid_beneficiary limit 1000"
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
    bene_query = """
        SELECT b. bene_sk
        FROM idr.valid_beneficiary b
        join idr.beneficiary_third_party tp on b.bene_sk = tp.bene_sk
        where b. bene_sk = b. bene_xref_efctv_sk and tp.bene_tp_type_cd  = 'A'
        limit 1000
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
    bene_query = """
        SELECT b. bene_sk
        FROM idr.valid_beneficiary b
        join idr.beneficiary_third_party tp on b.bene_sk = tp.bene_sk
        where b. bene_sk = b. bene_xref_efctv_sk and tp.bene_tp_type_cd  = 'B' limit 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_bene_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list list of MBIs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = "SELECT bene_mbi_id FROM idr.valid_beneficiary limit 1000"
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_claim_ids(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list list of clam Ids.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    # TODO: Update this with latest psycopg templated strings to reduce duplication
    claim_query = """
    SELECT clm_uniq_id FROM (
        SELECT clm_uniq_id FROM idr.claim_rx
        JOIN idr.valid_beneficiary
            ON claim_rx.bene_sk = valid_beneficiary.bene_sk
        WHERE idr_ltst_trans_flg = 'Y'
        LIMIT 200
        ) t1
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_institutional_nch.clm_uniq_id FROM idr.claim_institutional_nch
            JOIN idr.valid_beneficiary
                ON claim_institutional_nch.bene_sk = valid_beneficiary.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_institutional_nch
                WHERE claim_item_institutional_nch.clm_uniq_id = claim_institutional_nch.clm_uniq_id
            ) AND idr_ltst_trans_flg = 'Y'
            LIMIT 200
        ) t2
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_institutional_ss.clm_uniq_id FROM idr.claim_institutional_ss
            JOIN idr.valid_beneficiary
                ON claim_institutional_ss.bene_sk = valid_beneficiary.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_institutional_ss
                WHERE claim_item_institutional_ss.clm_uniq_id = claim_institutional_ss.clm_uniq_id
            ) AND idr_ltst_trans_flg = 'Y'
            LIMIT 200
        ) t3
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_professional_nch.clm_uniq_id FROM idr.claim_professional_nch
            JOIN idr.valid_beneficiary
                ON claim_professional_nch.bene_sk = valid_beneficiary.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_professional_nch
                WHERE claim_item_professional_nch.clm_uniq_id = claim_professional_nch.clm_uniq_id
            ) AND idr_ltst_trans_flg = 'Y'
            LIMIT 200
        ) t4
    UNION ALL
    SELECT clm_uniq_id FROM (
            SELECT DISTINCT claim_professional_ss.clm_uniq_id FROM idr.claim_professional_ss
            JOIN idr.valid_beneficiary
                ON claim_professional_ss.bene_sk = valid_beneficiary.bene_sk
            WHERE EXISTS (
                SELECT 1 FROM idr.claim_item_professional_ss
                WHERE claim_item_professional_ss.clm_uniq_id = claim_professional_ss.clm_uniq_id
            ) AND idr_ltst_trans_flg = 'Y'
            LIMIT 200
        ) t5;
    """
    return [str(r[0]) for r in set(x for x in _execute(uri, claim_query))]
