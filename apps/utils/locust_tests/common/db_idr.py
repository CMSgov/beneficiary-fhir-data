from typing import List, LiteralString
import os

import psycopg


def _execute(uri: str, query: LiteralString) -> List:
    """
    Execute a PSQL select statement and return its results
    """
    if uri == "":
        uri = f"user={os.environ["PGUSER"]} password={os.environ["PGPASSWORD"]} host={os.environ["PGHOST"]} dbname={os.environ["PGDATABASE"]}"
    conn = None
    results = []
    try:
        with psycopg.connect(uri) as conn:
            with conn.cursor() as cursor:

                cursor.execute(query)
                results = cursor.fetchall()
    except Exception as ex:
        print("Error creating database connection", ex)
    finally:
        if conn:
            conn.close()

    return results


def get_regression_bene_sks(uri: str, table_sample_pct=None) -> List[str]:
    """Retrieves a random list of beneficiary IDs

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = "SELECT bene_sk FROM idr.beneficiary limit 1000"
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_bene_mbis(uri: str, table_sample_pct=None) -> List[str]:
    """Retrieves a random list list of MBIs

    Args:
        uri (str): Database URI

    Returns:
        List[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = "SELECT bene_mbi_id FROM idr.beneficiary limit 1000"
    return [str(r[0]) for r in _execute(uri, bene_query)]
