#
# Script for validating via a quick smoke test that a synthea load successfully made it into the database by ensuring that 
# values from the load files are in the database, and the correct number of benes were loaded. 
#
# Args:
# 1: bene id start (inclusive, taken from previous end state properties / synthea properties file)
# 2: bene id end (exclusive, taken from new output end state properties)
# 3: file system location of synthea folder
# 4: which environments to check, should be a single comma separated string consisting of test,sbx,prod or any combo of the three (example "test,sbx,prod" or "test")
#
# Example runstring: python3 ./validate-synthea-load.py -10000004009994 -10000004010094 ~/Git/synthea-2 "test,sbx"
#
# Requires psycopg2 and boto3 installed
#

import sys
import psycopg2
import fileinput
import shlex

import ssmutil
from pathlib import Path

def validate_synthea_load(args):
    """
    Validates key data from the load files and number of benes expected were loaded into the database.
    """
    
    bene_id_start = args[0]
    bene_id_end = args[1]
    synthea_folder_filepath = args[2]
    envs = args[3].split(',')
    
    if not synthea_folder_filepath.endswith('/'):
        synthea_folder_filepath = synthea_folder_filepath + "/"
    synthea_output_folder = synthea_folder_filepath + "output/bfd/"
    
    test_db_string = ssmutil.get_ssm_db_string("test")
    prod_sbx_db_string = ssmutil.get_ssm_db_string("prod-sbx")
    prod_string = ssmutil.get_ssm_db_string("prod")
    
    ## Sanity check the tables and make sure the last line of each synthea file exists in the corresponding table
    print(f"Reading data from synthea output files for validation...")
    table_ids = {}
    if Path(synthea_output_folder + "carrier.csv").is_file():
        table_ids['carrier'] = get_bene_id_from_last_file_line(synthea_output_folder + "carrier.csv")
    if Path(synthea_output_folder + "dme.csv").is_file():
        table_ids['dme'] = get_bene_id_from_last_file_line(synthea_output_folder + "dme.csv")
    if Path(synthea_output_folder + "hha.csv").is_file():
        table_ids['hha'] = get_bene_id_from_last_file_line(synthea_output_folder + "hha.csv")
    if Path(synthea_output_folder + "hospice.csv").is_file():
        table_ids['hospice'] = get_bene_id_from_last_file_line(synthea_output_folder + "hospice.csv")
    if Path(synthea_output_folder + "inpatient.csv").is_file():
        table_ids['inpatient'] = get_bene_id_from_last_file_line(synthea_output_folder + "inpatient.csv")
    if Path(synthea_output_folder + "outpatient.csv").is_file():
        table_ids['outpatient'] = get_bene_id_from_last_file_line(synthea_output_folder + "outpatient.csv")
    if Path(synthea_output_folder + "pde.csv").is_file():
        table_ids['pde'] = get_bene_id_from_last_file_line(synthea_output_folder + "pde.csv")
    if Path(synthea_output_folder + "snf.csv").is_file():
        table_ids['snf'] = get_bene_id_from_last_file_line(synthea_output_folder + "snf.csv")

    expected_benes = int(bene_id_start) - int(bene_id_end)
    print(f"Expecting {expected_benes}")
    
    test_validation_result = True
    prod_sbx_validation_result = True
    prod_validation_result = True
    num_run = 0
    if "test" in envs:
        print("Running validations for test...")
        test_validation_result = check_data_loaded(bene_id_start, bene_id_end, expected_benes, table_ids, test_db_string)
        num_run = num_run + 1
    if "prd-sbx" in envs:
        print("Running validations for prod-sbx...")
        prod_sbx_validation_result = check_data_loaded(bene_id_start, bene_id_end, expected_benes, table_ids, prod_sbx_db_string)
        num_run = num_run + 1
    if "prod" in envs:
        print("Running validations for prod...")
        prod_validation_result = check_data_loaded(bene_id_start, bene_id_end, expected_benes, table_ids, prod_string)
        num_run = num_run + 1
        
    if not num_run == len(envs):
        print(f"(Validation Failure) Unknown environment found in {envs}")
        print("Returning with exit code 1")
        sys.exit(1)
    
    if not (test_validation_result and prod_sbx_validation_result and prod_validation_result):
        print("Failed validation, not all data loaded successfully")
        print("Returning with exit code 1")
        sys.exit(1)
    
    print("Returning with exit code 0 (No errors)")
    sys.exit(0)
    
def get_bene_id_from_last_file_line(file_path):
    """
    Grabs the bene id from the specified file's last line.
    If the file has only a header or no lines, returns -1.
    """
    
    lines = []
    with open(file_path) as file:
        lines = file.readlines()
        ## if the file had only the header or nothing, we have nothing to return
        if len(lines) < 2:
            return -1
        last_line = lines[-1]
        return last_line.split('|')[get_bene_id_index(lines[0])]
        
    return -1;
    
def get_bene_id_index(header):
    """
    Gets the index from the header for the bene_id.
    If unable to find, returns -1.
    """
    lines = header.split('|')
    index = 0
    for line in lines:
        if line == 'BENE_ID':
            return index
        index = index + 1
    
    print("    Could not find bene id index!")
    return -1

def check_data_loaded(bene_id_start, bene_id_end, expected_benes, table_ids, db_string):
    """
    Checks that the database to ensure the proper number of benes have been loaded,
    and that the last line in each load file exists in its specified table.
    Since the load files are roughly loaded in order, this should be a decent smoke
    test for if the data in those files successfully loaded.
    """
    
    ## This will be updated with each validation; if False it will stay false
    total_result = True
    
    query = f"select count(*) from beneficiaries where bene_id <= {bene_id_start} and bene_id > {bene_id_end}"
    result = _execute_single_count_query(db_string, query)
    if not result == expected_benes:
        print(f"(Validation Failure) expected {expected_benes} but only found {result} between bene_ids {bene_id_start} and {bene_id_end}")
    total_result = result == expected_benes
    
    ## Check each table to make sure the id within exists
    if "carrier" in table_ids:
        total_result = check_table_for_bene_id(table_ids["carrier"], "carrier_claims", db_string) and total_result
    if "dme" in table_ids:
        total_result = check_table_for_bene_id(table_ids["dme"], "dme_claims", db_string) and total_result
    if "hha" in table_ids:
        total_result = check_table_for_bene_id(table_ids["hha"], "hha_claims", db_string) and total_result
    if "inpatient" in table_ids:
        total_result = check_table_for_bene_id(table_ids["inpatient"], "inpatient_claims", db_string) and total_result
    if "hospice" in table_ids:
        total_result = check_table_for_bene_id(table_ids["hospice"], "hospice_claims", db_string) and total_result
    if "outpatient" in table_ids:
        total_result = check_table_for_bene_id(table_ids["outpatient"], "outpatient_claims", db_string) and total_result
    if "pde" in table_ids:
        total_result = check_table_for_bene_id(table_ids["pde"], "partd_events", db_string) and total_result
    if "snf" in table_ids:
        total_result = check_table_for_bene_id(table_ids["snf"], "snf_claims", db_string) and total_result
    
    return total_result
    
def check_table_for_bene_id(bene_id, table, db_string):
    """
    Checks the specified database table for the specified bene_id.
    """
    
    if not bene_id == -1:
        query = f"select count(*) from {table} where bene_id = {bene_id}"
        result = _execute_single_count_query(db_string, query)
        if result == 0:
            print(f"(Validation Failure) Did not find expected bene_id {bene_id} in {table} table")
        else:
            print(f"(Validation Success) Bene_id {bene_id} found in {table} table")
        return result > 0
    ## If the file had no rows and this returned no bene id (-1), pass the check for this table
    print(f"(Validation Success) No rows generated for {table} table")
    return True
    
def _execute_single_count_query(uri: str, query: str):
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

    if len(results) <= 0 and len(results[0]) <= 0:
        return 0

    #we want just a single number; the count, so extract this from the results set
    return results[0][0]
    

## Runs the program via run args when this file is run
if __name__ == "__main__":
    validate_synthea_load(sys.argv[1:])
