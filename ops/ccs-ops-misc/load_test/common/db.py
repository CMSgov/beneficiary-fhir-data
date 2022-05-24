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

    sub_select_day_age = 30

    """
    selects FISS records only from the last N days
    """
    fiss_sub_query = (
        'select * '
        'from rda.fiss_claims '
        f'where last_updated > current_date - interval \'{sub_select_day_age}\' day '
    )

    """
    Selects FISS rows partitioned by status
    """
    fiss_partition_sub_query = (
        'select fiss.*, ROW_NUMBER() '
        '   over (partition by fiss.curr_status order by fiss.last_updated desc) '
        f'from ({fiss_sub_query}) as fiss '
    )

    """
    Selects the mbi ids from N of each FISS status
    """
    fiss_mbi_sub_query = (
        'select fiss_partition.mbi_id '
        f'from ({fiss_partition_sub_query}) fiss_partition '
        f'where fiss_partition.row_number <= {per_status_max} and fiss_partition.mbi_id is not null '
    )

    """
    selects MCS records only from the last N days
    """
    mcs_sub_query = (
        'select * '
        'from rda.mcs_claims '
        f'where last_updated > current_date - interval \'{sub_select_day_age}\' day '
    )

    """
    Selects MCS rows partitioned by status
    """
    mcs_partition_sub_query = (
        'select mcs.*, ROW_NUMBER() '
        '   over (partition by mcs.idr_status_code order by mcs.last_updated desc) '
        f'from ({mcs_sub_query}) as mcs '
    )

    """
    Selects the mbi ids from N of each MCS status
    """
    mcs_mbi_sub_query = (
        'select mcs_partition.mbi_id '
        f'from ({mcs_partition_sub_query}) mcs_partition '
        f'where mcs_partition.row_number <= {per_status_max} and mcs_partition.mbi_id is not null '
    )

    """
    Selects the distinct mbis from both fiss and mcs subqueries
    """
    distinct_type_status_mbis = (
        'select distinct type_status.mbi_id '
        f'from ({fiss_mbi_sub_query} union {mcs_mbi_sub_query}) as type_status '
    )

    beneQuery = (
        # Get up to N distinct MBI hashes
        'select mbi.hash '
        'from ( '
        # Subquery sorts by source to weight 'filler' MBIs last
        '	select union_select.mbi_id '
        '	from ( '
        # Select up to N of the newest claims for each distinct FISS and MCS status value
        '		select src.mbi_id, 1 as source_order '
        f'		from ({distinct_type_status_mbis}) src '
        '		union '
        # Select whatever MBIs as filler
        '		select distinct mbi.mbi_id, 2 as source_order '
        '		from ( '
        '           select recent_mbis.* '
        '           from rda.mbi_cache as recent_mbis '   
        f'		    where recent_mbis.mbi_id not in ({distinct_type_status_mbis}) '
        '           order by recent_mbis.last_updated desc '
        '       ) as mbi '
        f'		limit {LIMIT} '
        '	) as union_select '
        '	order by union_select.source_order '
        ') sources '
        'left join rda.mbi_cache as mbi on mbi.mbi_id = sources.mbi_id '
        f'limit {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, beneQuery)]
