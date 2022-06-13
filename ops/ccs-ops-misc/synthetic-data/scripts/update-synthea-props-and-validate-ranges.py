import sys
import psycopg2
import re
from pathlib import Path

def update_and_validate(args):
    
    skip_validation = True if len(args) > 4 and args[4] == "True" else False
    end_state_properties_file = Path(args[0]).read_text()
    db_string = args[3]
    generated_benes = args[2]
    synthea_prop_filepath = args[1]
    
    print("Previous End State file: " + args[0])
    print(f"DB String: {db_string}")
    print("Output props file: " + args[1])
    print(f"Number generated: {generated_benes}")
    print(f"Skip Validation: {skip_validation}")
    
    #Validate the ranges - number to be generated
    ranges_good = True if skip_validation else check_ranges(end_state_properties_file, generated_benes, db_string)
    if ranges_good == True:
        update_synthea_props(end_state_properties_file, synthea_prop_filepath)
        print("Updated synthea properties")
        return 0
    else:
        print("Failed validation, not updating synthea properties")
        return 1

def check_ranges(properties_file, number_of_benes_to_generate, db_string):
    
    ranges_good = True
    
    clm_id_start = re.findall("exporter.bfd.clm_id_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from carrier_claims_new where clm_id <= {clm_id_start}"
    result = _execute(db_string, query)[0][0]
    if result > 0:
        print(f"Carrier claims id range invalid, {result} values past starting id {clm_id_start}")
        ranges_good = False
    else:
        print("Carrier claims id range is valid")
        
    bene_id_start = re.findall("exporter.bfd.bene_id_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    bene_id_end = int(bene_id_start) - int(number_of_benes_to_generate)
    query = f"select count(*) from beneficiaries where bene_id::bigint <= {bene_id_start} and bene_id::bigint > {bene_id_end}"
    result = _execute(db_string, query)[0][0]
    if result > 0:
        print(f"Beneficiary id range invalid, {result} values in this range")
        ranges_good = False
    else:
        print("Beneficiary id range is valid")
        
    clm_grp_id_start = re.findall("exporter.bfd.clm_grp_id_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    ## this is the end of the batch 1 relocated ids; should be nothing between the generated start and this
    clm_grp_id_end = '-99999831003'
    query = f"select count(*) from carrier_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute(db_string, query)[0][0]
    
    query = f"select count(*) from inpatient_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    query = f"select count(*) from outpatient_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    query = f"select count(*) from snf_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    query = f"select count(*) from dme_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    query = f"select count(*) from hha_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    query = f"select count(*) from hospice_claims_new where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    query = f"select count(*) from partd_events where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = result + _execute(db_string, query)[0][0]
    
    if result > 0:
        print(f"Carrier claims group id potential conflict, {result} values past start value {clm_grp_id_start} across all tables")
        ranges_good = False
    else:
        print("Carrier claims group id range is valid")
        
    pde_id_start = re.findall("exporter.bfd.pde_id_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from partd_events where pde_id::bigint <= {pde_id_start}"
    result = _execute(db_string, query)[0][0]
    if result > 0:
        print(f"PDE id potential conflict, {result} values after start {pde_id_start}")
        ranges_good = False
    else:
        print("PDE id range is valid")
    
    carr_clm_ctrl_num_start = re.findall("exporter.bfd.carr_clm_cntl_num_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from carrier_claims_new where carr_clm_cntl_num::bigint <= {carr_clm_ctrl_num_start}"
    result = _execute(db_string, query)[0][0]
    if result > 0:
        print(f"carr_clm_cntl_num id range invalid, {result} values past starting id {carr_clm_ctrl_num_start}")
        ranges_good = False
    else:
        print("carr_clm_cntl_num range is valid")
    
    ## Check fi_num_start in all the tables it exists in
    fi_num_start = re.findall("exporter.bfd.fi_doc_cntl_num_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from outpatient_claims_new where fi_doc_clm_cntl_num::bigint <= {fi_num_start}"
    result = _execute(db_string, query)[0][0]
    
    fi_num_start = re.findall("exporter.bfd.fi_doc_cntl_num_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from inpatient_claims_new where fi_doc_clm_cntl_num::bigint <= {fi_num_start}"
    result = result + _execute(db_string, query)[0][0]
    
    fi_num_start = re.findall("exporter.bfd.fi_doc_cntl_num_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from hha_claims_new where fi_doc_clm_cntl_num::bigint <= {fi_num_start}"
    result = result + _execute(db_string, query)[0][0]
    
    fi_num_start = re.findall("exporter.bfd.fi_doc_cntl_num_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from snf_claims_new where fi_doc_clm_cntl_num::bigint <= {fi_num_start}"
    result = result + _execute(db_string, query)[0][0]
    
    fi_num_start = re.findall("exporter.bfd.fi_doc_cntl_num_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from hospice_claims_new where fi_doc_clm_cntl_num::bigint <= {fi_num_start}"
    result = result + _execute(db_string, query)[0][0]
    
    if result > 0:
        print(f"fi_doc_cntl_num id range invalid, {result} values past starting id {fi_num_start} across all tables")
        ranges_good = False
    else:
        print("fi_doc_cntl_num range is valid")
        
    hicn_start = re.findall("exporter.bfd.hicn_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from beneficiaries where hicn_unhashed = \'{hicn_start}\'"
    result = _execute(db_string, query)[0][0]
    if result > 0:
        print(f"Start HICN {hicn_start} exists in DB")
        ranges_good = False
    else:
        print("Start HICN does not exist in DB")
        
    mbi_start = re.findall("exporter.bfd.mbi_start.*$",properties_file,re.MULTILINE)[0].split("=")[1]
    query = f"select count(*) from beneficiaries where mbi_num = \'{mbi_start}\'"
    result = _execute(db_string, query)[0][0]
    if result > 0:
        print(f"Start MBI {mbi_start} exists in DB")
        ranges_good = False
    else:
        print("Start MBI does not exist in DB")
        
    return ranges_good


def update_synthea_props(end_state_file_data, synthea_props_file_location):
    
    return False
    

    
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
    # 4 args:
    # arg1: previous end state properties
    # arg2: number of items to be generated
    # arg3: location of synthea properties file to write to
    # arg4: db string for target environment DB
    # arg5: (optional) skip validation, useful if re-generating a bad batch
    update_and_validate(sys.argv[1:])
