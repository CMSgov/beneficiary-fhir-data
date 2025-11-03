#!/usr/bin/python3
"""Create SQL scripts to delete synthetic data.
"""

import sys

# List of predefined Beneficiary IDs to delete and pattern of bene_ids to match in a NOT LIKE
# clause. Source: https://jira.cms.gov/browse/BFD-1686
BENE_IDS = {
    'test': {
        'ids': [ '-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212',
            '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223',
            '-400','-401', '-403', '-410', '-88880000000001' ],
        'pattern': '-%'
    },
    'prod-sbx': {
        'ids': [ '-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212',
            '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223',
            '-400' ],
        'pattern': '-%'
    },
    'prod': {
        'ids': [ '-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212',
            '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223',
            '-400' ],
        'pattern': None
    }
}

# Tables with a bene_id column. We make sure to run the beneficiaries table last so we don't
# violate foreign key constraints.
BASE_TABLES = ['beneficiaries_history', 'beneficiary_monthly',
    'beneficiaries_history_invalid_beneficiaries',
    'medicare_beneficiaryid_history_invalid_beneficiaries', 'medicare_beneficiaryid_history',
    'partd_events', 'beneficiaries']

# Tables supporting claims, where the first has a bene_id column and a clm_id column, and the
# second can be joined via clm_id.
CLAIMS_TABLES = [
    ('carrier_claims', 'carrier_claim_lines'),
    ('dme_claims', 'dme_claim_lines'),
    ('hha_claims', 'hha_claim_lines'),
    ('hospice_claims', 'hospice_claim_lines'),
    ('inpatient_claims', 'inpatient_claim_lines'),
    ('outpatient_claims', 'outpatient_claim_lines'),
    ('snf_claims', 'snf_claim_lines')
]

# Tables supporting claims, where the first has a BIGINT bene_id column and a clm_id column, and
# the second can be joined via clm_id.
NEW_CLAIMS_TABLES = [
    ('snf_claims_new', 'snf_claim_lines_new')
]


# Helpers


def make_bene_id_pattern(bene_ids: list[str], pattern: str, cast_id: bool = False) -> str:
    """Make the WHERE clause to handle bene_ids that are in the list or fit the pattern.
    """
    output = '  bene_id IN (\'' + '\', \''.join(bene_ids) + '\')'
    if pattern is not None:
        bene_id = 'CAST(bene_id AS CHARACTER VARYING(15))' if cast_id else 'bene_id'
        output += f"\n  OR {bene_id} NOT LIKE '{pattern}'"

    return output


def make_claims_sql(bene_id_clause: str, claims_table: str, claim_lines_table: str,
    is_count: bool) -> str:
    """Create SQL to count or delete from a claims table and its corresponding claim_lines table.

    We want to run the claims table after the claim_lines table, because we couldn't join / union
    if we don't.
    """
    # Generate SQL for Claims Lines
    output  = f"-- {claim_lines_table}\n\n"
    if is_count:
        output += f"SELECT COUNT(lines.*) AS {claim_lines_table}\n"
        output += f"FROM {claim_lines_table} AS lines\n"
        output += f"LEFT JOIN {claims_table} AS claims ON (claims.clm_id = lines.clm_id)\n"
        output += f"WHERE\n{bene_id_clause};\n\n\n"
    else:
        output += f"DELETE FROM {claim_lines_table} AS lines\n"
        output += f"USING {claims_table} AS claims\n"
        output +=  "WHERE\n"
        output +=  "  claims.clm_id = lines.clm_id\n"
        output +=  "  AND (\n"
        output += f"{bene_id_clause.replace('  ', '    ')}\n"
        output +=  "  );\n\n\n"

    # Generate SQL for Claims
    output += make_base_sql(bene_id_clause, claims_table, is_count)

    return output


def make_base_sql(bene_id_clause: str, base_table: str, is_count: bool) -> str:
    """Create SQL to count or delete from a table containing a bene_id clause.
    """
    output  = f"-- {base_table}\n\n"

    output += f"SELECT COUNT(*) AS {base_table}\n" if is_count else 'DELETE\n'
    output += f"FROM {base_table}\n"
    output += f"WHERE\n{bene_id_clause};\n\n"

    return output


def help_text() -> str:
    """Provide help text if the user does not provide valid arguments or asks for help.
    """
    return 'Usage: make_sql.py [count | delete] [test|prod-sbx|prod]'


# Main


def main(args: list):
    """Main function, called from the command line.
    """
    if not args or len(args) < 2:
        print(help_text(), file=sys.stderr)
        sys.exit()

    if args[0] == 'count':
        is_count = True
    elif args[0] == 'delete':
        is_count = False
    elif args[0] == 'help':
        print(help_text())
        sys.exit()
    else:
        print(help_text(), file=sys.stderr)
        sys.exit()

    try:
        bene_ids = BENE_IDS[args[1]]
    except KeyError:
        print(help_text(), file=sys.stderr)
        sys.exit()

    bene_id_pattern = make_bene_id_pattern(bene_ids['ids'], bene_ids['pattern'], False)
    new_bene_id_pattern = make_bene_id_pattern(bene_ids['ids'], bene_ids['pattern'], True)

    for claims_table, claim_lines_table in CLAIMS_TABLES:
        print(make_claims_sql(bene_id_pattern, claims_table, claim_lines_table, is_count))

    for claims_table, claim_lines_table in NEW_CLAIMS_TABLES:
        print(make_claims_sql(new_bene_id_pattern, claims_table, claim_lines_table, is_count))

    for base_table in BASE_TABLES:
        print(make_base_sql(bene_id_pattern, base_table, is_count))


## Runs the test via run args when this file is run
if __name__ == "__main__":
    main(sys.argv[1:])
