import sys
import psycopg2
import re
import csv
from pathlib import Path

def generate_characteristics_file(args):
    """
    Generates a beneficiary characteristics file for a given
    synthea load, and exports it as a csv.
    """
    
    bene_id_start = args[0]
    bene_id_end = args[1]
    db_string = args[2]
    output_path = args[3]
    
    header = ['Beneficiary Id','MBI Unhashed','Part D Contract Number','Carrier Claims Total','DME Claims Total','HHA Claims Total','Hospice Claims Total','Inpatient Claims Total','Outpatient Claims Total','SNF Claims Total','Part D Events Total']
    
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
    except BaseException as err:
        print(f"Unexpected error while running queries: {err}")
        print("Returning with exit code 1")
        sys.exit(1)
    
    ## synthesize data into final rows
    final_data_rows = put_data_into_final_rows(bene_data, carrier_data, dme_data, hha_data, hospice_data, inpatient_data, outpatient_data, snf_data, pde_data)
    
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
        f" FROM public.beneficiaries WHERE bene_id <= {bene_id_start} and bene_id > {bene_id_end} order by bene_id desc"
        
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
    
    query = "SELECT t1.bene_id, count(t2.*)"\
            " FROM public.beneficiaries as t1"\
            f" LEFT JOIN public.{table_name} as t2"\
            " ON t1.bene_id=t2.bene_id"\
            f" WHERE t1.bene_id <= {bene_id_start} and t1.bene_id > {bene_id_end}"\
            " GROUP BY t1.bene_id"\
            " ORDER BY t1.bene_id desc;"\
            
    print(f"Starting query for {table_name} count...");
    raw_query_response = _execute_query(db_string, query)
    rows = len(raw_query_response)
    print(f"Got {table_name} counts for {rows} benes.");
    # put the entries in a dict for faster lookup later
    dict_response = {}
    for entry in raw_query_response:
        dict_response[entry[0]] = entry[1]
    
    return dict_response

def put_data_into_final_rows(bene_data, carrier_data, dme_data, hha_data, hospice_data, inpatient_data, outpatient_data, snf_data, pde_data):
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
        carrier_count = carrier_data[bene_id]
        dme_count = dme_data[bene_id]
        hha_count = hha_data[bene_id]
        hospice_count = hospice_data[bene_id]
        inpatient_count = inpatient_data[bene_id]
        outpatient_count = outpatient_data[bene_id]
        snf_count = snf_data[bene_id]
        pde_count = pde_data[bene_id]
        final_rows.append([bene_id, mbi, contracts, carrier_count, dme_count, hha_count, hospice_count, inpatient_count, outpatient_count, snf_count, pde_count])
        
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

## Runs the program via run args when this file is run
if __name__ == "__main__":
    # 3 args:
    # arg1: bene id starting point for the load to check. The characteristics file generation will assume everything from this point on (counting down, as the synthea bene ids are negative) is new in the db. This can be mined from the synthea properties file used for the generation
    # arg2: bene id ending point; this should be the number from end-state.properties for bene_id
    # arg2: db string for target environment DB, in this format: postgres://<dbName>:<db-pass>@<aws db url>:5432/fhirdb
    # arg3: file system location to output the characteristics file
    """
    Notes:
    - Will overwrite any characteristics.csv at output location, assuming queries succeed
    """
    generate_characteristics_file(sys.argv[1:])
