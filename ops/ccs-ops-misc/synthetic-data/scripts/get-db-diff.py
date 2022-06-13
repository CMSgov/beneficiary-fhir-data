import sys
import psycopg2

db1_string = ""
db2_string = ""


def check_db(args):
    """
    Compares the counts in one db vs another; useful for checking counts 
    for data loads against a clone of an environment.
    """
    db1_string = args[0]
    db2_string = args[1]
    db_items = count_all_tables_in_db(db1_string)
    db_items2 = count_all_tables_in_db(db2_string)

    for key in db_items2:
        item1 = db_items[key][0][0]
        item2 = db_items2[key][0][0]
        diff = item2 - item1
        print(key, ': ' + str(item2) + " (+" + str(diff) + ")")


def count_all_tables_in_db(db_string):
    """
    Gets the table counts of each table for the given database
    and returns them in a dict.
    """
    db_items = {}
    
    db_items["bene_count"] = _execute(db_string, "select count(*) from beneficiaries")
    db_items["bene_history_count"] = _execute(db_string, "select count(*) from beneficiaries_history")
    db_items["bene_monthly_count"] = _execute(db_string, "select count(*) from beneficiary_monthly")
    db_items["carrier_count"] = _execute(db_string, "select count(*) from carrier_claims_new")
    db_items["carrier_lines_count"] = _execute(db_string, "select count(*) from carrier_claim_lines_new")
    db_items["dme_count"] = _execute(db_string, "select count(*) from dme_claims_new")
    db_items["dme_lines_count"] = _execute(db_string, "select count(*) from dme_claim_lines_new")
    db_items["hha_count"] = _execute(db_string, "select count(*) from hha_claims_new")
    db_items["hha_lines_count"] = _execute(db_string, "select count(*) from hha_claim_lines_new")
    db_items["hospice_count"] = _execute(db_string, "select count(*) from hospice_claims_new")
    db_items["hospice_lines_count"] = _execute(db_string, "select count(*) from hospice_claim_lines_new")
    db_items["inpatient_count"] = _execute(db_string, "select count(*) from inpatient_claims_new")
    db_items["inpatient_lines_count"] = _execute(db_string, "select count(*) from inpatient_claim_lines_new")
    db_items["outpatient_count"] = _execute(db_string, "select count(*) from outpatient_claims")
    db_items["outpatient_lines_count"] = _execute(db_string, "select count(*) from outpatient_claim_lines")
    db_items["snf_count"] = _execute(db_string, "select count(*) from snf_claims_new")
    db_items["snf_lines_count"] = _execute(db_string, "select count(*) from snf_claim_lines_new")
    db_items["pde_count"] = _execute(db_string, "select count(*) from partd_events")
    
    return db_items
    
def _execute(uri: str, query: str):
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
    

## Runs the program via run args when this file is run
if __name__ == "__main__":
    """
    Arg1 should be original db connection string, arg2 should be
    new db's connection string.
    """
    check_db(sys.argv[1:])
