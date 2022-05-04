# -*- coding: utf-8 -*-
"""common.db

Utility module for executing database queries
"""
import psycopg2


LIMIT = 10000  # global limit on the number of records to return


def _execute(uri, query):
    """
    Execute a PSQL select statement and return its results
    """
    print('Collecting test data...')

    conn = None

    try:
        with psycopg2.connect(uri) as conn:
            with conn.cursor() as cursor:
                cursor.execute(query)
                results = cursor.fetchall()
    finally:
        conn.close()

    return results


def get_bene_ids(uri):
    """
    Return a list of bene IDs from the adjudicated beneficiary table
    """
    bene_query = (
        'SELECT "bene_id" '
        'FROM "beneficiaries" '
        # 'TABLESAMPLE SYSTEM (0.25) '
        'WHERE "rfrnc_yr" IS NOT NULL '
        f'LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_hashed_mbis(uri):
    """
    Return a list of unique hashed MBIs from the adjudicated beneficiary table
    """
    bene_query = (
        'SELECT "mbi_hash" '
        'FROM "beneficiaries" '
        # 'TABLESAMPLE SYSTEM (0.25) '
        'WHERE "mbi_hash" IS NOT NULL '
        f'LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_contract_ids(uri):
    """
    Return a list of contract id / reference year pairs from the beneficiary
    table
    """
    contract_data = []
    months = { '01': 'jan', '02': 'feb', '03': 'mar', '04': 'apr', '05': 'may',
        '06': 'jun', '07': 'jul', '08': 'aug', '09': 'sept', '10': 'oct',
        '11': 'nov', '12': 'dec' }
    for month_numeric, month_text in months.items():
        contract_id_query = (
            f'SELECT DISTINCT "ptd_cntrct_{month_text}_id", "rfrnc_yr" '
            'FROM "beneficiaries" '
            'WHERE "rfrnc_yr" IS NOT NULL '
            f'LIMIT {LIMIT}'
        )

        results = _execute(uri, contract_id_query)
        for result in results:
            contract_data.append({
                'id': str(result[0]),
                'year': str(result[1]),
                'month': month_numeric
            })

    return contract_data


def get_partially_adj_hashed_mbis(uri):
    """
    Return a list of unique hashed MBIs from the partially adjudicated
    FISS and MCS tables
    """
    mbi_query = (
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

    return [str(r[0]) for r in _execute(uri, mbi_query)]
