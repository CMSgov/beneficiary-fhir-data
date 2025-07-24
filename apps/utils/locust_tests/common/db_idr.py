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
    bene_query = "SELECT bene_sk FROM idr.beneficiary limit 1000"
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_bene_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:  # noqa: ARG001
    """Retrieve a random list list of MBIs.

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = "SELECT bene_mbi_id FROM idr.beneficiary limit 1000"
    return [str(r[0]) for r in _execute(uri, bene_query)]
