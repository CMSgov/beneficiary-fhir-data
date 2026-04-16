"""Utility module for executing database queries."""

import logging
import os

import psycopg

LIMIT = 100000
"""Global limit on the number of records to return"""
REGRESSION_BENE_RANGE_START = -10000000019999
"""Beginning of the beneficiary range used by the Regression Suite in each environment for
consistent data"""
REGRESSION_BENE_RANGE_END = -10000000000001
"""End of the beneficiary range used by the Regression Suite in each environment for
consistent data"""

logger = logging.getLogger()


def _execute(uri: str, query: str) -> list:
    """Execute a PSQL select statement and return its results."""
    if uri == "":
        uri = f"user={os.environ['PGUSER']} password={os.environ['PGPASSWORD']} \
            host={os.environ['PGHOST']} dbname={os.environ['PGDATABASE']}"
    conn = None
    results = []
    try:
        with psycopg.connect(uri) as conn, conn.cursor() as cursor:
            # The execute method here uses additional type validation to prevent SQL injections,
            # but this unfortunately doesn't support templated strings like we're using here,
            # so we have to bypass the type check
            cursor.execute(query)  # type: ignore
            results = cursor.fetchall()
    except Exception as ex:
        logger.error("Error creating database connection: %s", ex)
    finally:
        if conn:
            conn.close()

    return results


def _get_regression_query(select_query: str) -> str:
    return " ".join(
        [
            select_query,
            (
                f'WHERE "bene_id" BETWEEN {REGRESSION_BENE_RANGE_START} AND'
                f" {REGRESSION_BENE_RANGE_END}"
            ),
            'ORDER BY "bene_id" ASC',
            f"LIMIT {LIMIT}",
        ]
    )


def get_regression_bene_ids(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Retrieve a list of beneficiary IDs within the range of 20,000 contiguous synthetic
    beneficiaries that exist in each environment. Returned list is sorted in ascending order.

    Args:
        uri (str): Database URI

    Returns:
        list[str]: A list of synthetic beneficiary IDs used for the regression suites
    """
    bene_query = _get_regression_query('SELECT "bene_id" FROM ccw.beneficiaries')
    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_regression_hashed_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Retrieve a list of hashed MBIs within the range of 20,000 contiguous synthetic
    beneficiaries that exist in each environment. Returned list is sorted in ascending order.

    Args:
        uri (str): Database URI

    Returns:
        list[str]: A list of synthetic hashed MBIs used for the regression suites
    """
    mbi_query = _get_regression_query('SELECT "mbi_hash" FROM ccw.beneficiaries')
    return [str(r[0]) for r in _execute(uri, mbi_query)]


def get_regression_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Retrieve a list of MBIs within the range of 20,000 contiguous synthetic
    beneficiaries that exist in each environment. Returned list is sorted in ascending order.

    Args:
        uri (str): Database URI

    Returns:
        list[str]: A list of synthetic MBIs used for the regression suites
    """
    mbi_query = _get_regression_query('SELECT "mbi_num" FROM ccw.beneficiaries')
    return [str(r[0]) for r in _execute(uri, mbi_query)]


def get_regression_contract_ids(
    uri: str,
    table_sample_pct: float | None = None,
) -> list[dict[str, str]]:
    """Retrieve a list of contract IDs within the range of 20,000 contiguous synthetic
    beneficiaries that exist in each environment. Returned list is sorted in ascending order, and
    any empty values are excluded.

    Args:
        uri (str): Database URI

    Returns:
        list[Dict[str, str]]: A list of dicts with 3 keys, "id", "month", and "year", corresponding
        to the contract ID, the contract month, and contract year, respectively
    """
    contract_id_query = _get_regression_query(
        'SELECT "partd_contract_number_id", "year_month" FROM ccw.beneficiary_monthly'
    )

    return [
        {
            "id": str(result[0]),
            "month": f"{result[1].month:02}",
            "year": str(result[1].year),
        }
        for result in _execute(uri, contract_id_query)
        if result[0]
    ]


def get_regression_pac_hashed_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Return a list of MBI hashes within the set of static, synthetic PAC data.

    Args:
        uri (str): The database connection string

    Returns:
        list[str]: A list of MBI hashes
    """
    claims_mbis_query = (
        r"select hash from rda.mbi_cache where regexp_like(mbi, '\dS.+') order by hash limit 300"
    )
    return [str(r[0]) for r in _execute(uri, claims_mbis_query)]


def get_regression_pac_mbis(uri: str, table_sample_pct: float | None = None) -> list[str]:
    """Return a list of MBI within the set of static, synthetic PAC data.

    Args:
        uri (str): The database connection string

    Returns:
        list[str]: A list of MBI
    """
    claims_mbis_query = (
        r"select mbi from rda.mbi_cache where regexp_like(mbi, '\dS.+') order by mbi limit 300"
    )
    return [str(r[0]) for r in _execute(uri, claims_mbis_query)]


def get_bene_ids(uri: str, table_sample_pct: float | None = None) -> list:
    """Return a list of bene IDs from the adjudicated beneficiary table."""
    if table_sample_pct is None:
        table_sample_text = ""
    else:
        table_sample_text = f"TABLESAMPLE SYSTEM ({table_sample_pct}) "

    bene_query = f'SELECT "bene_id" FROM ccw.beneficiaries {table_sample_text} LIMIT {LIMIT}'

    return [str(r[0]) for r in _execute(uri, bene_query)]


def get_hashed_mbis(uri: str, table_sample_pct: float | None = None) -> list:
    """Return a list of unique hashed MBIs from the adjudicated beneficiary table."""
    if table_sample_pct is None:
        table_sample_text = ""
    else:
        table_sample_text = f"TABLESAMPLE SYSTEM ({table_sample_pct}) "

    # Handle possible hash collisions from duplicate hashes in beneficiaries_history
    # by only taking MBI hashes that are distinct in both beneficiaries _and_
    # benficiaries_history
    mbi_query = (
        f"SELECT beneficiaries.mbi_hash FROM ccw.beneficiaries {table_sample_text} "
        "    INNER JOIN ccw.beneficiaries_history "
        "        ON beneficiaries.mbi_hash = beneficiaries_history.mbi_hash "
        "    GROUP BY beneficiaries.mbi_hash "
        "    HAVING count(beneficiaries_history.mbi_hash) = 1 "
        f"LIMIT {LIMIT}"
    )

    return [str(r[0]) for r in _execute(uri, mbi_query)]


def get_mbis(uri: str, table_sample_pct: float | None = None) -> list:
    """Return a list of unique MBIs from the adjudicated beneficiary table."""
    if table_sample_pct is None:
        table_sample_text = ""
    else:
        table_sample_text = f"TABLESAMPLE SYSTEM ({table_sample_pct}) "

    # Handle possible collisions from duplicates in beneficiaries_history
    # by only taking MBIs that are distinct in both beneficiaries _and_
    # benficiaries_history
    mbi_query = (
        f"SELECT beneficiaries.mbi_num FROM ccw.beneficiaries {table_sample_text} "
        "    INNER JOIN ccw.beneficiaries_history "
        "        ON beneficiaries.mbi_num = beneficiaries_history.mbi_num "
        "    GROUP BY beneficiaries.mbi_num "
        "    HAVING count(beneficiaries_history.mbi_num) = 1 "
        f"LIMIT {LIMIT}"
    )

    return [str(r[0]) for r in _execute(uri, mbi_query)]


def get_contract_ids(uri: str, table_sample_pct: float | None = None) -> list:
    """
    Return a list of contract id / reference year pairs from the beneficiary
    table.
    """
    if table_sample_pct is None:
        table_sample_text = ""
    else:
        table_sample_text = f"TABLESAMPLE SYSTEM ({table_sample_pct}) "

    contract_id_query = (
        'SELECT "partd_contract_number_id", "year_month" '
        "FROM ccw.beneficiary_monthly "
        f"{table_sample_text}"
        f"LIMIT {LIMIT}"
    )

    unfiltered_contracts = [
        {
            "id": str(result[0]) if result[0] else None,
            "month": f"{result[1].month:02}",
            "year": str(result[1].year),
        }
        for result in _execute(uri, contract_id_query)
    ]

    return [contract for contract in unfiltered_contracts if contract["id"]]


def get_pac_mbis(uri: str) -> list:
    """
    Return a list of unique MBIs that represent a diverse set of FISS and MCS
    claims over a range of claim statuses.

    We anticipate that fields will have a mixture of blank vs non-blank values based on the status
    codes received.

    By selecting MBIs that are related to claims with varying status codes, we can get a good
    mixture of claim data elements, better testing our FHIR transformers' ability to
    correctly render them.
    """
    per_status_max = int(LIMIT / 40)  # Based on ~40 distinct status values between FISS/MCS

    sub_select_day_age = 30

    """
    selects FISS records only from the last N days
    """
    fiss_sub_query = (
        "select * from rda.fiss_claims where last_updated > current_date - interval"
        f" '{sub_select_day_age}' day and mbi_id is not null "
    )

    """
    Selects FISS rows partitioned by status
    """
    fiss_partition_sub_query = (
        "select fiss.*, ROW_NUMBER() "
        "   over (partition by fiss.curr_status order by fiss.last_updated desc) "
        f"from ({fiss_sub_query}) as fiss "
    )

    """
    Selects the mbi ids from N of each FISS status
    """
    fiss_mbi_sub_query = (
        "select fiss_partition.mbi_id "
        f"from ({fiss_partition_sub_query}) fiss_partition "
        f"where fiss_partition.row_number <= {per_status_max} "
    )

    """
    selects MCS records only from the last N days
    """
    mcs_sub_query = (
        "select * from rda.mcs_claims where last_updated > current_date - interval"
        f" '{sub_select_day_age}' day and mbi_id is not null "
    )

    """
    Selects MCS rows partitioned by status
    """
    mcs_partition_sub_query = (
        "select mcs.*, ROW_NUMBER() "
        "   over (partition by mcs.idr_status_code order by mcs.last_updated desc) "
        f"from ({mcs_sub_query}) as mcs "
    )

    """
    Selects the mbi ids from N of each MCS status
    """
    mcs_mbi_sub_query = (
        "select mcs_partition.mbi_id "
        f"from ({mcs_partition_sub_query}) mcs_partition "
        f"where mcs_partition.row_number <= {per_status_max} "
    )

    """
    Selects the distinct mbis from both fiss and mcs subqueries
    """
    distinct_type_status_mbis = (
        "select distinct type_status.mbi_id "
        f"from ({fiss_mbi_sub_query} union {mcs_mbi_sub_query}) as type_status "
    )

    mbi_query = (
        # Get up to N distinct MBI hashes
        "select mbi.mbi "
        "from ( "
        # Subquery sorts by source to weight 'filler' MBIs last
        "	select union_select.mbi_id "
        "	from ( "
        # Select up to N of the newest claims for each distinct FISS and MCS status value
        "		select src.mbi_id, 1 as source_order "
        f"		from ({distinct_type_status_mbis}) src "
        "		union "
        # Select whatever MBIs as filler
        "		select distinct mbi.mbi_id, 2 as source_order "
        "		from ( "
        "           select recent_mbis.* "
        "           from rda.mbi_cache as recent_mbis "
        f"		    where recent_mbis.mbi_id not in ({distinct_type_status_mbis}) "
        "           order by recent_mbis.last_updated desc "
        "       ) as mbi "
        f"		limit {LIMIT} "
        "	) as union_select "
        "	order by union_select.source_order "
        ") sources "
        "left join rda.mbi_cache as mbi on mbi.mbi_id = sources.mbi_id "
        f"limit {LIMIT}"
    )

    # intentionally reversing the query results, as the important mbis to test
    # will be at the beginning of the result set and BFDUserBase will pop items
    # off of the end of the list
    return [str(r[0]) for r in reversed(_execute(uri, mbi_query))]


def get_pac_mbis_smoketest(uri: str) -> list[str]:
    """Get the top LIMIT MBI from the rda table's MBI cache for use with the PACA smoketests.

    Args:
        uri (str): The database connection string

    Returns:
        list[str]: A list of MBIs
    """
    smoketest_mbi_query = f"select mbi from rda.mbi_cache limit {LIMIT}"

    return [str(r[0]) for r in _execute(uri, smoketest_mbi_query)]
