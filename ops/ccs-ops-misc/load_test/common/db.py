# -*- coding: utf-8 -*-
"""Utility module for executing database queries.
"""

from typing import List

import psycopg2


LIMIT = 100000  # global limit on the number of records to return


def _execute(uri: str, query: str) -> List:
    """
    Execute a PSQL select statement and return its results
    """

    conn = None
    try:
        with psycopg2.connect(uri) as conn:
            with conn.cursor() as cursor:
                cursor.execute(query)
                results = cursor.fetchall()
    finally:
        conn.close()

    return results


def get_bene_ids(uri: str, table_sample_pct: float = None) -> List:
    """
    Return a list of bene IDs from the adjudicated beneficiary table
    """

    if table_sample_pct is None:
        table_sample_text = ''
    else:
        table_sample_text = f'TABLESAMPLE SYSTEM ({table_sample_pct}) '

    bene_query = (
        f'SELECT "bene_id" FROM "beneficiaries" {table_sample_text} LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_hashed_mbis(uri: str, table_sample_pct: float = None) -> List:
    """
    Return a list of unique hashed MBIs from the adjudicated beneficiary table
    """

    if table_sample_pct is None:
        table_sample_text = ''
    else:
        table_sample_text = f'TABLESAMPLE SYSTEM ({table_sample_pct}) '

    bene_query = (
        f'SELECT "mbi_hash" FROM "beneficiaries" {table_sample_text} WHERE "mbi_hash" IS NOT NULL '
        f'LIMIT {LIMIT}'
    )

    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_contract_ids(uri: str) -> List:
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


def get_pac_hashed_mbis(uri: str) -> List:
    """
    Return a list of unique hashed MBIs that represent a diverse set of FISS and MCS
    claims over a range of claim statuses.

    We anticipate that fields will have a mixture of blank vs non-blank values based on the status codes received.

    By selecting MBIs that are related to claims with varying status codes, we can get a good mixture of claim data
    elements, better testing our FHIR transformers' ability to correctly render them.
    """
    per_status_max = int(LIMIT / 40)    # Based on ~40 distinct status values between FISS/MCS

    sub_select_day_age = 30

    """
    selects FISS records only from the last N days
    """
    fiss_sub_query = (
        'select * '
        'from rda.fiss_claims '
        f'where last_updated > current_date - interval \'{sub_select_day_age}\' day and mbi_id is not null '
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
        f'where fiss_partition.row_number <= {per_status_max} '
    )

    """
    selects MCS records only from the last N days
    """
    mcs_sub_query = (
        'select * '
        'from rda.mcs_claims '
        f'where last_updated > current_date - interval \'{sub_select_day_age}\' day and mbi_id is not null '
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
        f'where mcs_partition.row_number <= {per_status_max} '
    )

    """
    Selects the distinct mbis from both fiss and mcs subqueries
    """
    distinct_type_status_mbis = (
        'select distinct type_status.mbi_id '
        f'from ({fiss_mbi_sub_query} union {mcs_mbi_sub_query}) as type_status '
    )

    mbi_query = (
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

    return [str(r[0]) for r in _execute(uri, mbi_query)]
