# -*- coding: utf-8 -*-
"""common.db

Utility module for executing database queries
"""
import psycopg2


LIMIT = 100000  # global limit on the number of records to return


def _execute(uri, query):
    """
    Execute a PSQL select statement and return its results
    """
    print('Collecting test data...')

    with psycopg2.connect(uri) as conn:
        with conn.cursor() as cursor:
            cursor.execute(query)
            results = cursor.fetchall()
            print(f'Returned {len(results)} results from the database for the test.')

    return results


def get_bene_ids(uri):
    """
    Return a list of bene IDs from the adjudicated beneficiary table
    """
    beneQuery = (
        'SELECT "bene_id" '
        'FROM "beneficiaries" '
        # 'TABLESAMPLE SYSTEM (0.25) '  # when data is cleaned up, use this to access random benes
        # there are tons of benes with null ref year which return 404 if we use them
        'WHERE "rfrnc_yr" IS NOT NULL '
        f'LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, beneQuery)]


def get_hashed_mbis(uri):
    """
    Return a list of unique hashed MBIs from the adjudicated beneficiary table
    """
    beneQuery = (
        'SELECT "mbi_hash" '
        'FROM "beneficiaries" '
        'WHERE "mbi_hash" IS NOT NULL '
        f'LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, beneQuery)]


def get_partially_adj_hashed_mbis(uri):
    """
    Return a list of unique hashed MBIs from the partially adjudicated
    FISS and MCS tables
    """
    beneQuery = (
        'SELECT DISTINCT f."mbiHash" AS hash '
        'FROM "pre_adj"."FissClaims" f '
        'WHERE f."mbi" IS NOT NULL '
        'UNION '
        'SELECT DISTINCT m."idrClaimMbiHash" AS hash '
        'FROM "pre_adj"."McsClaims" m '
        'WHERE m."idrClaimMbi" IS NOT NULL '
        'AND m."idrClaimMbi" NOT IN ( '
        '   SELECT DISTINCT "idrClaimMbi" '
        '   FROM "pre_adj"."McsClaims" '
        '   WHERE "idrStatusCode" IS NULL '
        '   AND "idrClaimMbi" IS NOT NULL '
        ') '
        'ORDER BY hash '
        f'LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, beneQuery)]
