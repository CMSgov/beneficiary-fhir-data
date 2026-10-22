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


def _sample_clause(table_sample_pct: float | None) -> Template:
    """Return a TABLESAMPLE clause for the given percent, or nothing if no percent is given."""
    return t"" if table_sample_pct is None else t"TABLESAMPLE SYSTEM({table_sample_pct:l})"


def _valid_bene_sql(table_sample_pct: float | None = None) -> Template:
    """Return the definition of the idr.valid_beneficiary view."""
    return t"""(
        SELECT b.*
        FROM idr.beneficiary b {_sample_clause(table_sample_pct):q}
        WHERE b.idr_ltst_trans_flg = 'Y'
            AND NOT EXISTS (
                SELECT 1
                FROM idr.beneficiary_overshare_mbi om
                WHERE om.bene_mbi_id = b.bene_mbi_id
            )
    )"""


def get_regression_bene_sks(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI
        table_sample_pct (float | None): The percentage of the beneficiaries table to sample

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    valid_bene = _valid_bene_sql(table_sample_pct)
    bene_query = t"""
        SELECT bene_sk
        FROM {valid_bene:q}
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_current_part_a_bene_sks(
    uri: str,
    table_sample_pct: float | None = None,
) -> list[str]:
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI
        table_sample_pct (float | None): The percentage of the beneficiaries table to sample

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    valid_bene = _valid_bene_sql(table_sample_pct)
    bene_query = t"""
        SELECT b.bene_sk
        FROM {valid_bene:q} b
        JOIN idr.beneficiary_third_party tp ON b.bene_sk = tp.bene_sk
        WHERE b.bene_sk = b.bene_xref_efctv_sk AND tp.bene_tp_type_cd = 'A'
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_current_part_b_bene_sks(
    uri: str,
    table_sample_pct: float | None = None,
) -> list[str]:
    """Retrieve a random list of beneficiary IDs.

    Args:
        uri (str): Database URI
        table_sample_pct (float | None): The percentage of the beneficiaries table to sample

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    valid_bene = _valid_bene_sql(table_sample_pct)
    bene_query = t"""
        SELECT b.bene_sk
        FROM {valid_bene:q} b
        JOIN idr.beneficiary_third_party tp ON b.bene_sk = tp.bene_sk
        WHERE b.bene_sk = b.bene_xref_efctv_sk AND tp.bene_tp_type_cd = 'B'
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_bene_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Retrieve a random list list of MBIs.

    Args:
        uri (str): Database URI
        table_sample_pct (float | None): The percentage of the beneficiaries table to sample

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    valid_bene = _valid_bene_sql(table_sample_pct)
    bene_query = t"""
        SELECT bene_mbi_id
        FROM {valid_bene:q}
        LIMIT 1000
    """
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_claim_ids(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Retrieve a random list list of claim Ids.

    Args:
        uri (str): Database URI
        table_sample_pct (float | None): The percentage of each claims table to sample

    Returns:
        List[str]: A list of valid, unique claim IDs from each claim table
    """
    limit = 200
    sampling = _sample_clause(table_sample_pct)

    claims_and_items = [
        ("claim_rx", None),
        ("claim_institutional_nch", "claim_item_institutional_nch"),
        ("claim_institutional_ss", "claim_item_institutional_ss"),
        ("claim_professional_nch", "claim_item_professional_nch"),
        ("claim_professional_ss", "claim_item_professional_ss"),
    ]

    branches: list[Template] = []
    for claims_table, items_table in claims_and_items:
        item_filter = (
            t"""
                AND EXISTS (
                    SELECT 1
                    FROM idr.{items_table:i}
                    WHERE {items_table:i}.clm_uniq_id = {claims_table:i}.clm_uniq_id
                )
            """
            if items_table
            else sql.SQL("")
        )
        branches.append(
            t"""
                SELECT clm_uniq_id FROM (
                    SELECT DISTINCT {claims_table:i}.clm_uniq_id
                    FROM idr.{claims_table:i} {sampling:q}
                    JOIN {_valid_bene_sql():q} AS b ON {claims_table:i}.bene_sk = b.bene_sk
                    WHERE {claims_table:i}.clm_ltst_clm_ind = 'Y'
                    {item_filter:q}
                    LIMIT {limit:l}
                )
            """
        )

    claim_query = t"{sql.SQL(' UNION ALL ').join(branches):q};"
    return list({str(r[0]) for r in _execute(uri, claim_query)})
