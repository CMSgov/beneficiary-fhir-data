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

    conn = None

    try:
        with psycopg2.connect(uri) as conn:
            with conn.cursor() as cursor:
                cursor.execute(query)
                results = cursor.fetchall()
                print(f'Returned {len(results)} results from the database for the test.')
    finally:
        conn.close()

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
    Return a list of unique hashed MBIs that represent a diverse set of FISS and MCS
    claims over a range of claim statuses.
    """
    per_status_max = int(LIMIT / 40)    # Assuming ~40 distinct status values between FISS/MCS
    per_status_limit = min(50, per_status_max)
    per_claim_max = int(LIMIT / 10)     # Arbitrary
    per_claim_limit = min(1000, per_claim_max)

    beneQuery = (
        # Get up to N distinct MBI hashes
        'select distinct mbi.hash '
        'from rda.mbi_cache as mbi '
        'where mbi.mbi_id in ( '
        # Subquery sorts by source to weight 'filler' MBIs last
        '	select union_select.mbi_id '
        '	from ( '
        # Select up to 50 claims for each distinct MCS status value
        '		select src.mbi_id, 1 as source_order '
        '		from ('
        '			select mcs.*, ROW_NUMBER() over (partition by mcs.idr_status_code order by mcs.mbi_id) '
        '           from rda.mcs_claims as mcs '
        '			) src '
        f'		where src.row_number <= {per_status_limit} '
        '		union '
        # Select up to 50 claims for each distinct FISS status value
        '		select src.mbi_id, 2 as source_order '
        '		from ( '
        '			select fiss.*, ROW_NUMBER() over (partition by fiss.curr_status order by fiss.mbi_id) '
        '           from rda.fiss_claims as fiss '
        '			) src '
        f'		where src.row_number <= {per_status_limit} '
        '		union '
        # Select up to 1000 whatever claims from MCS
        '		select src.mbi_id, 3 as source_order '
        f'		from (select mcs.* from rda.mcs_claims as mcs limit {per_claim_limit}) as src '
        '		union '
        # Select all the FISS claims as filler
        '		select src.mbi_id, 4 as source_order '
        f'		from (select fiss.* from rda.fiss_claims as fiss limit {per_claim_limit}) as src '
        '		union '
        # Select whatever MBIs as filler
        '		select mbi.mbi_id, 5 as source_order '
        f'		from rda.mbi_cache as mbi limit {LIMIT}'
        '	) as union_select '
        '	order by union_select.source_order '
        ') '
        f'limit {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, beneQuery)]
