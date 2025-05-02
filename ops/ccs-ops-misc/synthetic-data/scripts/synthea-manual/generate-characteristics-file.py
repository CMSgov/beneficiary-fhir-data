#
# Script for creating a file that describes which bene ids were generated 
# and what claim types were associated with each bene id. 
# Will overwrite any characteristics.csv at output location, assuming queries succeed
#
# Args:
# 1: bene id start (inclusive, taken from previous end state properties / synthea properties file)
# 2: bene id end (exclusive, taken from new output end state properties)
# 3: file system location to write the characteristics file
# 4: which environment to check, should be a single value from the list of [test prd-sbx prod]
#
# Example runstring: python3 ./generate-characteristics-file.py -10000008009988 -10000010009985 ~/Documents/Test/ test
#
# Requires psycopg2 and boto3 installed
#

import sys
import psycopg2
import re
import csv
from pathlib import Path

import ssmutil

def generate_characteristics_file(args):
    """
    Generates a beneficiary characteristics file for a given
    synthea load, and exports it as a csv.
    """
    
    bene_id_start = args[0]
    bene_id_end = args[1]
    output_path = args[2] if args[2].endswith('/') else args[2] + "/"
    env = args[3]
    
    db_string = ""

    if "test" == env:
        db_string = ssmutil.get_ssm_db_string("test")
    elif "prd-sbx" == env:
        db_string = ssmutil.get_ssm_db_string("prd-sbx")
    elif "prod" == env:
        db_string = ssmutil.get_ssm_db_string("prod")
    else:
        print(f"(Validation Failure) Unknown environment string {env}")
        print("Returning with exit code 1")
        sys.exit(1)
    
    header = ['Beneficiary Id','MBI Unhashed','Part D Contract Number','Carrier Claims Total','DME Claims Total','HHA Claims Total','Hospice Claims Total','Inpatient Claims Total','Outpatient Claims Total','SNF Claims Total','Part D Events Total','FISS','MCS']
    
    ## get data for csv from db
    bene_data = {}
    carrier_data = {}
    dme_data = {}
    hha_data = {}
    hospice_data = {}
    inpatient_data = {}
    outpatient_data = {}
    snf_data = {}
    pde_data = {}
    
    try:
        ## bene data, 3 columns: bene id, unhashed mbi, concatenated contract numbers
        bene_data = get_bene_data(bene_id_start, bene_id_end, db_string)
        carrier_data = get_table_count("carrier_claims", bene_id_start, bene_id_end, db_string)
        dme_data = get_table_count("dme_claims", bene_id_start, bene_id_end, db_string)
        hha_data = get_table_count("hha_claims", bene_id_start, bene_id_end, db_string)
        hospice_data = get_table_count("hospice_claims", bene_id_start, bene_id_end, db_string)
        inpatient_data = get_table_count("inpatient_claims", bene_id_start, bene_id_end, db_string)
        outpatient_data = get_table_count("outpatient_claims", bene_id_start, bene_id_end, db_string)
        snf_data = get_table_count("snf_claims", bene_id_start, bene_id_end, db_string)
        pde_data = get_table_count("partd_events", bene_id_start, bene_id_end, db_string)
        fiss_data, mcs_data = get_rda_claim_count(db_string, bene_id_start, bene_id_end)
    except BaseException as err:
        print(f"Unexpected error while running queries: {err}")
        print("Returning with exit code 1")
        sys.exit(1)
    
    ## synthesize data into final rows
    final_data_rows = put_data_into_final_rows(bene_data, carrier_data, dme_data, hha_data, hospice_data, inpatient_data, outpatient_data, snf_data, pde_data, fiss_data, mcs_data )
    
    ## Write csv to filesystem + header
    filePath = output_path + 'characteristics.csv'
    print("Writing final csv...")
    try:
        with open(filePath, 'w') as f:
            writer = csv.writer(f)
            writer.writerow(header)
            writer.writerows(final_data_rows)
            num_rows = len(final_data_rows)
            print(f"Wrote out {num_rows} to {filePath}")
    except IOError as err:
        print(f"IOError while opening/writing csv: {err}")
        print("Returning with exit code 1")
        sys.exit(1)
    except BaseException as err:
        print(f"Unexpected error while opening/writing csv: {err}")
        print("Returning with exit code 1")
        sys.exit(1)
    
    print("Returning with exit code 0 (No errors)")
    sys.exit(0)
    

def get_bene_data(bene_id_start, bene_id_end, db_string):
    """
    Gets the initial data from the beneficiary table including the beneficiary id, 
    mbi, and a concatenated list of contract numbers.
    """
    
    query = f"SELECT bene_id, mbi_num, concat_ws(',', ptd_cntrct_jan_id, ptd_cntrct_feb_id,ptd_cntrct_mar_id,ptd_cntrct_apr_id,ptd_cntrct_may_id,ptd_cntrct_jun_id,"\
        f" ptd_cntrct_jul_id, ptd_cntrct_aug_id, ptd_cntrct_sept_id, ptd_cntrct_oct_id, ptd_cntrct_nov_id, ptd_cntrct_dec_id) as \"Part D Contract Number\""\
        f" FROM ccw.beneficiaries WHERE bene_id <= {bene_id_start} and bene_id > {bene_id_end} order by bene_id desc"
        
    print(f"Starting query for bene data...");
    raw_query_response = _execute_query(db_string, query)
    rows = len(raw_query_response)
    print(f"Got {rows} results from bene data query.");
    return raw_query_response
    
def get_table_count(table_name, bene_id_start, bene_id_end, db_string):
    """
    Gets the table count for each bene in the specified range for the specified
    database, and returns a dictionary with the bene id as the key and the
    table count as the value.
    """
    
    query = "SELECT bene_id, count(*)"\
            f" FROM ccw.{table_name}"\
            f" WHERE bene_id <= {bene_id_start} and bene_id > {bene_id_end}"\
            " GROUP BY bene_id"\
            " ORDER BY bene_id desc;"\
            
    print(f"Starting query for {table_name} count...");
    raw_query_response = _execute_query(db_string, query)
    rows = len(raw_query_response)
    print(f"Got {table_name} counts for {rows} benes.");
    # put the entries in a dict for faster lookup later
    dict_response = {}
    for entry in raw_query_response:
        dict_response[entry[0]] = entry[1]
    
    return dict_response

def put_data_into_final_rows(bene_data, carrier_data, dme_data, hha_data, hospice_data, inpatient_data, outpatient_data, snf_data, pde_data, fiss_data, mcs_data):
    """
    Takes the bene data and table counts and creates a list of rows that will
    be used in the final csv characteristics file.
    """
    final_rows = []
    
    print("Setting up final data rows...")
    for row in bene_data:
        bene_id = row[0]
        mbi = row[1]
        contracts = row[2]
        carrier_count = carrier_data[bene_id] if bene_id in carrier_data else 0
        dme_count = dme_data[bene_id] if bene_id in dme_data else 0
        hha_count = hha_data[bene_id] if bene_id in hha_data else 0
        hospice_count = hospice_data[bene_id] if bene_id in hospice_data else 0
        inpatient_count = inpatient_data[bene_id] if bene_id in inpatient_data else 0
        outpatient_count = outpatient_data[bene_id] if bene_id in outpatient_data else 0
        snf_count = snf_data[bene_id] if bene_id in snf_data else 0
        pde_count = pde_data[bene_id] if bene_id in pde_data else 0
        fiss_count = fiss_data.get(bene_id, 0)
        mcs_count = mcs_data.get(bene_id, 0)
        final_rows.append([bene_id, mbi, contracts, carrier_count, dme_count, hha_count, hospice_count, inpatient_count, outpatient_count, snf_count, pde_count, fiss_count , mcs_count])
        
    return final_rows

def _execute_query(uri: str, query: str):
    """
    Execute a PSQL select statement and return its results.
    """
    conn = None
    finalResults = []
    try:
        with psycopg2.connect(uri) as conn:
            with conn.cursor() as cursor:
                cursor.execute(query)
                finalResults = cursor.fetchall()
    finally:
        conn.close()

    return finalResults

def get_rda_claim_count(db_string, bene_id_start, bene_id_end):
    query_fiss = f"""
        SELECT b.bene_id, COUNT(fc.claim_id)
        FROM rda.fiss_claims fc
        JOIN rda.mbi_cache mca ON fc.mbi_id = mca.mbi_id
        JOIN ccw.beneficiaries b ON b.mbi_num = mca.mbi
        WHERE b.bene_id <= {bene_id_start} AND b.bene_id > {bene_id_end}
        GROUP BY b.bene_id
    """

    query_mcs = f"""
        SELECT b.bene_id, COUNT(mc.idr_clm_hd_icn)
        FROM rda.mcs_claims mc
        JOIN rda.mbi_cache mca ON mc.mbi_id = mca.mbi_id
        JOIN ccw.beneficiaries b ON b.mbi_num = mca.mbi
        WHERE b.bene_id <= {bene_id_start} AND b.bene_id > {bene_id_end}
        GROUP BY b.bene_id
    """

    fiss_counts = {row[0]: row[1] for row in _execute_query(db_string, query_fiss)}
    mcs_counts = {row[0]: row[1] for row in _execute_query(db_string, query_mcs)}

    return fiss_counts, mcs_counts



## Runs the program via run args when this file is run
if __name__ == "__main__":
    generate_characteristics_file(sys.argv[1:])
