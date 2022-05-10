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
    per_status_max = int(LIMIT / 40)    # Based on 40 distinct status values between FISS/MCS

    """
    Selects rows partitioned into type/status combinations
    """
    partition_sub_query = (
        'select api.*, ROW_NUMBER() '
        '   over (partition by api.claim_type, api.claim_state order by api.received_date desc) '
        'from rda.rda_api_claim_message_meta_data as api'
    )

    """
    Selects only N distinct mbi ids from each type/status combination from the partition
    """
    status_sub_query = (
        'select distinct partition.mbi_id '
        f'from ({partition_sub_query}) partition '
        f'where partition.row_number <= {per_status_max}'
    )

    beneQuery = (
        # Get up to N distinct MBI hashes
        'select mbi.hash '
        'from ( '
        # Subquery sorts by source to weight 'filler' MBIs last
        '	select union_select.mbi_id '
        '	from ( '
        # Select up to N of the newest claims for each distinct FISS and MCS status value
        '		select src.mbi_id, 1 as source_order'
        f'		from ({status_sub_query}) src '
        '		union '
        # Select whatever MBIs as filler
        '		select distinct mbi.mbi_id, 2 as source_order '
        '		from rda.mbi_cache as mbi '
        f'		where mbi.mbi_id not in ({status_sub_query}) '
        f'		limit {LIMIT} '
        '	) as union_select '
        '	order by union_select.source_order '
        ') sources '
        'left join rda.mbi_cache as mbi on mbi.mbi_id = sources.mbi_id '
        f'limit {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, beneQuery)]
