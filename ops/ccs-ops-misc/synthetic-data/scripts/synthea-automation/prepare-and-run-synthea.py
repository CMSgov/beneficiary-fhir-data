#
# Script for preparing a synthea repo for generating a number of
# beneficiary files. 
#
# Args:
# 1: previous end state properties file location
# 2: file system location of synthea folder
# 3: number of beneficiaries to be generated
# 4: which environments to load/validate, should be a single comma separated string consisting of test,sbx,prod or any combo of the three (example "test,sbx,prod" or "test")
# 5: number of months into the future that synthea should generate dates for
# 6: (optional) boolean to skip validation if True, useful if re-generating a bad batch, True or False, defaults to False
#
# Example runstring (with db usernames and passwords as an env variable DB_STRING): python3 prepare-and-run-synthea.py ~/end-state.properties ~/Git/synthea/ 2000 $DB_STRING "test,sbx"
#
# The script will enact the following steps:
#
# 1. Validate the paths and files for synthea needed are in place within the synthea directory (as passed in arg2)
#    - Also checks files and folders that must be written to are writable, and that some externally added files are readable
#    - Checks if the output folder exists, and creates one if not
# 2. Checks the supplied database has room to load the number of benes and related table data, so we dont waste time generating something that will have collisions in the target db
#    - This validation's checks will begin at the expected generation starting point for each field, as read from the end state properties file in arg1
#    - This is the validation that can be skipped by using optional arg6. There may be times where we are reloading a partially loaded synthea set that previously failed and are forcing a re-generation of data that will be loaded in idempotent mode (overwriting the existing db data).
# 3. Validates the output directory is empty
#    - If the output folder has data from a previous run, the output directory is renamed with a timestamp and a new empty output directory is created
#    - Since this check handles a non-empty output folder, this step wont fail unless there is an IO issue
# 4. Swaps the script's execution directory to the synthea directory supplied in arg3, since the national script is hardcoded to run other synthea code via relative paths
# 5. Run the synthea bfd-national shell script with the number of beneficiaries supplied in arg3
#    - Output of this run will be written to a timestamped log file in the synthea directory
#    - If this run fails (denoted by checking the output for text synthea outputs on a build failure) the synthea generation step will be considered a failure
#    - If arg5 is greater than 0, synthea will generate claim lines that extend up to the input number of months into the future
#
# Example runstring: python3 prepare-and-run-synthea.py ~/Documents/end-state.properties ~/Git/synthea 100 "sbx" True
#
# If any step of the above fails, a message describing the failure will be printed to stdout along with a standard message on a new line "Returning with exit code 1"
# If all steps succeed, the script will print to stdout "Returning with exit code 0 (No errors)"
#
# Note: If running locally, you will need to be connected to the VPN in order to successfully connect to the database
#
# Requires psycopg2 and boto3 installed
#

import sys
import psycopg2
import os
import time
import fileinput
import subprocess
import shlex

import ssmutil

def validate_and_run(args):
    """
    Validates (unless specified to skip) and then updates the
    synthea.properties file with the specified end state data.
    If validation is not skipped, and fails, the properties file
    will not be updated. Also cleans up the output directory.
    
    After the validation, cleanup, and updating are successful,
    runs the synthea national script.
    """
    
    end_state_file_path = args[0]
    synthea_folder_filepath = args[1]
    ## Script assumes trailing slash on this, so add it if not added
    if not synthea_folder_filepath.endswith('/'):
        synthea_folder_filepath = synthea_folder_filepath + "/"

    generated_benes = args[2]
    envs = args[3].split(',')
    future_months = int(args[4])
    skip_validation = True if len(args) > 5 and args[5].lower() == "true" else False
    synthea_prop_filepath = synthea_folder_filepath + "src/main/resources/synthea.properties"
    synthea_output_filepath = synthea_folder_filepath + "output/"
    
    print (f"Synthea folder file path: {synthea_folder_filepath}")
    print (f"Synthea folder prop path: {synthea_prop_filepath}")
    print (f"Synthea folder output   : {synthea_output_filepath}")
    
    found_all_paths = validate_file_paths(synthea_folder_filepath, synthea_prop_filepath, synthea_output_filepath, end_state_file_path)
    
    if found_all_paths == True:
        print("(Validation Success) Filepath check success")
    else:
        print("Failed file path check")
        print("Returning with exit code 1")
        sys.exit(1)
    
    end_state_properties_file = read_file_lines(end_state_file_path)
        
    ## Get DB Creds from param store
    test_db_string = ssmutil.get_ssm_db_string("test")
    prod_sbx_db_string = ssmutil.get_ssm_db_string("prod-sbx")
    prod_string = ssmutil.get_ssm_db_string("prod")
    
    #Validate the ranges - number to be generated
    test_validation_result = True
    prod_sbx_validation_result = True
    prod_validation_result = True
    num_run = 0
    if not skip_validation:
        if "test" in envs:
            print("Running validations for test...")
            test_validation_result = check_ranges(end_state_properties_file, generated_benes, test_db_string)
            num_run = num_run + 1
        if "prd-sbx" in envs:
            print("Running validations for prod-sbx...")
            prod_sbx_validation_result = check_ranges(end_state_properties_file, generated_benes, prod_sbx_db_string)
            num_run = num_run + 1
        if "prod" in envs:
            ## Note this one step takes a while (near 30 mins), due to checking for non-indexed fields on very big tables
            print("Running validations for prod...")
            prod_validation_result = check_ranges(end_state_properties_file, generated_benes, prod_string)
            num_run = num_run + 1
        
        if not num_run == len(envs):
            print(f"(Validation Failure) Unknown environment found in {envs}")
            print("Returning with exit code 1")
            sys.exit(1)
        
        if not (test_validation_result and prod_sbx_validation_result and prod_validation_result):
            print("Failed validation, not updating synthea properties")
            print("Returning with exit code 1")
            sys.exit(1)
    
    update_property_file(end_state_properties_file, synthea_prop_filepath)
    print("Updated synthea properties")
    
    clean_synthea_output(synthea_folder_filepath)
    
    ## National script expects we're in the synthea directory, so swap to that before running
    os.chdir(synthea_folder_filepath)
    run_success = run_synthea(synthea_folder_filepath, generated_benes, future_months)
    if not run_success:
        print("Synthea run finished with errors")
        print("Returning with exit code 1")
        sys.exit(1)
    
    print("Returning with exit code 0 (No errors)")
    sys.exit(0)

def run_synthea(synthea_folder_filepath, benes_to_generate, future_months):
    """
    Runs synthea using the national script and pipes the output
    to a log file.
    """
    
    logfile_path = f'{synthea_folder_filepath}synthea-' + time.strftime("%Y_%m_%d-%I_%M_%S_%p") + '.log'
    if future_months > 0:
        print(f'Running synthea ({synthea_folder_filepath}national_bfd_v2.sh) with {benes_to_generate} benes and {future_months} future months...')
        output = subprocess.check_output(shlex.split(f'{synthea_folder_filepath}national_bfd_v2.sh {benes_to_generate} {future_months}'), text=True, stderr=subprocess.STDOUT)
    else:
        print(f'Running synthea ({synthea_folder_filepath}national_bfd.sh) with {benes_to_generate} benes...')
        output = subprocess.check_output(shlex.split(f'{synthea_folder_filepath}national_bfd.sh {benes_to_generate}'), text=True, stderr=subprocess.STDOUT)
    with open(logfile_path, 'w') as f:
        f.write(output)
    
    print(f'Synthea run complete, log saved at {logfile_path}')
    
    ## Check output for synthea failure text as the success check
    return not 'FAILURE:' in output
    

def validate_file_paths(synthea_folder_filepath, synthea_prop_filepath, synthea_output_filepath, end_state_file_path):
    '''
    Validates that all paths needed for synthea setup can be found and
    applicable paths are writable before we continue.
    '''
    validation_passed = True
    
    if not os.path.exists(synthea_folder_filepath):
        print(f"(Validation Failure) Synthea folder filepath could not be found at {synthea_folder_filepath}")
        validation_passed = False
    else:
        if not os.path.exists(synthea_prop_filepath):
            print(f"(Validation Failure) Synthea properties file could not be found at {synthea_prop_filepath}")
            validation_passed = False
        elif not os.access(synthea_prop_filepath, os.W_OK):
            print(f"(Validation Failure) Synthea properties file is not writable (found at {synthea_prop_filepath})")
            validation_passed = False
        if not os.path.exists(synthea_output_filepath):
            print(f"(Validation Warning) Output directory ({synthea_output_filepath}) could not be found, creating it...")
            os.mkdir(synthea_output_filepath)
        if not os.access(synthea_output_filepath, os.W_OK):
            print(f"(Validation Failure) Synthea output directory is not writable (found at {synthea_output_filepath})")
            validation_passed = False
    
    if not os.path.exists(end_state_file_path):
        print(f"(Validation Failure) End state properties file could not be found at {end_state_file_path}")
        validation_passed = False
    
    if os.path.exists(synthea_folder_filepath):
        ## Validate we have the export files in place, and the national script
        export_filenames = ["condition_code_map.json", "dme_code_map.json", "hcpcs_code_map.json", "medication_code_map.json", "drg_code_map.json", "betos_code_map.json", "external_codes.csv"]
        for filename in export_filenames:
            export_file_loc = synthea_folder_filepath + "src/main/resources/export/" + filename
            if not os.path.exists(export_file_loc):
                print(f"(Validation Failure) Expected export file could not be found: {export_file_loc}")
                validation_passed = False
            elif not os.access(synthea_prop_filepath, os.R_OK):
                print(f"(Validation Failure) Export file {export_file_loc} not readable")
                validation_passed = False
        
        national_file_loc = synthea_folder_filepath + "national_bfd.sh"
        if not os.path.exists(national_file_loc):
            print(f"(Validation Failure) Expected national runfile ({national_file_loc}) could not be found")
            validation_passed = False
        elif not os.access(national_file_loc, os.X_OK):
            print(f"(Validation Failure) Synthea run file is not executable (found at {national_file_loc})")
            validation_passed = False
    
    return validation_passed

    
def clean_synthea_output(synthea_folder_filepath):
    """
    Prepares the output directory for a new run of the Synthea generation.
    If an output with files exists, rename the output directory and create
    a fresh one to put the new files in.
    """
    output_dir = synthea_folder_filepath + "output/"
    numFiles = len(os.listdir(output_dir))
    if numFiles > 0:
        ## create a copy of the output directory and make a new empty one
        timestr = time.strftime("%Y_%m_%d-%I_%M_%S_%p")
        new_filename = synthea_folder_filepath + "output-" + timestr
        os.rename(output_dir, new_filename)
        os.mkdir(output_dir)
        print("(Validation Warning) Synthea output had files, renamed old output directory and created fresh synthea output folder")
    else:
        print("(Validation Success) Synthea output folder empty")
    
def check_ranges(properties_file, number_of_benes_to_generate, db_string):
    """
    Checks that the synthea properties values to update to have no
    conflicts with the target database by checking there are no conflicting 
    existing items beyond the starting value for each field.
    """
    
    ## This will be updated with each validation; if False it will stay false
    overall_validation_result = True
    
    clm_id_start = get_props_value(properties_file, 'exporter.bfd.clm_id_start')
    query = f"select count(*) from carrier_claims where clm_id <= {clm_id_start}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_id_start, "carrier_claims", "clm_id", result, overall_validation_result)
        
    bene_id_start = get_props_value(properties_file, "exporter.bfd.bene_id_start")
    bene_id_end = int(bene_id_start) - int(number_of_benes_to_generate)
    query = f"select count(*) from beneficiaries where bene_id <= {bene_id_start} and bene_id > {bene_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(bene_id_start, "beneficiaries", "bene_id", result, overall_validation_result)
        
    clm_grp_id_start = get_props_value(properties_file, "exporter.bfd.clm_grp_id_start")
    ## this is the end of the batch 1 relocated ids; should be nothing between the generated start and this
    clm_grp_id_end = '-99999831003'
    query = f"select count(*) from carrier_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "carrier_claims", "clm_grp_id", result, overall_validation_result)
    
    query = f"select count(*) from inpatient_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "inpatient_claims", "clm_grp_id", result, overall_validation_result)

    query = f"select count(*) from outpatient_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "outpatient_claims", "clm_grp_id", result, overall_validation_result)
    
    query = f"select count(*) from snf_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "snf_claims", "clm_grp_id", result, overall_validation_result)
    
    query = f"select count(*) from dme_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "dme_claims", "clm_grp_id", result, overall_validation_result)
    
    query = f"select count(*) from hha_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "hha_claims", "clm_grp_id", result, overall_validation_result)
    
    query = f"select count(*) from hospice_claims where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "hospice_claims", "clm_grp_id", result, overall_validation_result)
    
    query = f"select count(*) from partd_events where clm_grp_id <= {clm_grp_id_start} and clm_grp_id > {clm_grp_id_end}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(clm_grp_id_start, "partd_events", "clm_grp_id", result, overall_validation_result)
    
    
    pde_id_start = get_props_value(properties_file, "exporter.bfd.pde_id_start")
    query = f"select count(*) from partd_events where pde_id::bigint <= {pde_id_start}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(pde_id_start, "partd_events", "pde_id", result, overall_validation_result)
    
    carr_clm_ctrl_num_start = get_props_value(properties_file, "exporter.bfd.carr_clm_cntl_num_start")
    query = f"select count(*) from carrier_claims where carr_clm_cntl_num::bigint <= {carr_clm_ctrl_num_start}"
    result = _execute_single_count_query(db_string, query)
    overall_validation_result = field_has_room_in_table(carr_clm_ctrl_num_start, "carrier_claims", "carr_clm_cntl_num", result, overall_validation_result)

    ## Check fi_num_start doesnt exist in all the tables it exists in (cant check range due to fi_num not being convertable to int in prod)
    fi_num_start = get_props_value(properties_file, "exporter.bfd.fi_doc_cntl_num_start")
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('outpatient_claims', 'fi_doc_clm_cntl_num', fi_num_start, db_string)
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('inpatient_claims', 'fi_doc_clm_cntl_num', fi_num_start, db_string)
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('hha_claims', 'fi_doc_clm_cntl_num', fi_num_start, db_string)
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('snf_claims', 'fi_doc_clm_cntl_num', fi_num_start, db_string)
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('hospice_claims', 'fi_doc_clm_cntl_num', fi_num_start, db_string)

    ## Since MBI and HICN are incremented in difficult-to-query ways, just check if it exists; if it doesnt exist the range should be fine
    hicn_start = get_props_value(properties_file, "exporter.bfd.hicn_start")
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('beneficiaries', 'hicn_unhashed', hicn_start, db_string)
        
    mbi_start = get_props_value(properties_file, "exporter.bfd.mbi_start")
    overall_validation_result = overall_validation_result and check_single_doesnt_exist('beneficiaries', 'mbi_num', mbi_start, db_string)
        
    ## Ensure no (synthetic) mbi_num has more than one bene_id associated with it from any previous loads
    ## this takes about 30 seconds in test as of this writing, and 2 seconds in the other envs, but this will increase as more data is added
    ## Takes the union of beneficiaries, bene_history, and medicare_beneid_history and checks if any mbi numbers resolve to more than one bene id, then returns the count
    query = 'select count(*) from ('\
            '  select count(*) bene_id_count from ('\
            '    select distinct bene_id, mbi_num '\
            '    from public.beneficiaries '\
            '    where bene_id < 0 and mbi_num IS NOT NULL '\
            '   union '\
            '    select distinct bene_id, mbi_num '\
            '    from public.beneficiaries_history '\
            '    where bene_id < 0 and mbi_num IS NOT NULL '\
            '   union '\
            '    select distinct bene_id, mbi_num '\
            '    from public.medicare_beneficiaryid_history '\
            '    where bene_id < 0 and mbi_num IS NOT NULL '\
            '  ) as foo '\
            '  group by mbi_num '\
            '  having count(*) > 1'\
            ') as s'
    result = _execute_single_count_query(db_string, query)
    ## There is one duplicate in the db for testing on purpose, so ignore that one
    if result > 1:
        print(f"(Validation Warning) {result} MBI(s) resolve to multiple bene_ids; a given mbi should only resolve to one bene_id (this needs cleanup to avoid errors using that data)")
    else:
        print("(Validation Success) No MBIs resolve to multiple bene_ids")
        
    return overall_validation_result
    
def check_single_doesnt_exist(table, field, value, db_string):
    query = f"select count(*) from {table} where {field} = \'{value}\'"
    result = _execute_single_count_query(db_string, query)
    if result > 0:
        print(f"(Validation Failure) Start {field} {value} exists in DB ({table})")
        return False
    else:
        print(f"(Validation Success) Start {field} does not exist in DB ({table})")
        return True

def get_props_value(list, starts_with):
    """
    Small helper function for getting a value from the property file
    for the line that starts with the given value.
    """
    return [x for x in list if x.startswith(starts_with)][0].split("=")[1]


def field_has_room_in_table(starting_value, table, field, query_result, overall_validation_result):
    """
    Checks if for a given starting value, for a specific table and field,
    the query that checked the range was 0 (meaning there were no rows with 
    values beyond the starting value). If the query was successful, return
    the overall success value for the entire validation across tables.
    """
    if query_result > 0:
        print(f"(Validation Failure) {table} : {field} has conflict with data to load, {query_result} rows beyond starting value {starting_value}")
        return False
    else:
        print(f"(Validation Success) {table} : {field} has clearance from starting value {starting_value}")
        return overall_validation_result
    

def update_property_file(end_state_file_lines, synthea_props_file_location):
    """
    Updates the synthea properties file to prepare
    for the next batch creation.
    """
    
    replacement_lines = []
    for line in end_state_file_lines:
        ## Avoid any accidental blank lines in the end state file;
        ## also, ignore any comment lines
        if len(line.strip()) > 0:
            if line[0] != '#':
                replacement_lines.append(line.split("="))
        
    for tuple in replacement_lines:
        replace_text = tuple[0] + "=" + tuple[1]
        replace_line_starting_with(synthea_props_file_location, tuple[0], replace_text)
    
    return False
    
def read_file_lines(file_path):
    """
    Reads file lines into an array.
    """
    lines = []
    with open(file_path) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
    
    return lines
    
def replace_line_starting_with(file_path, line_starts_with, replace_with):
    """
    Replaces lines starting with the given phrase with the replacement
    line for a given file, and saves the file with the new lines.
    """
    with open(file_path, 'r') as file:
        lines = file.readlines()
        
    new_lines = []
    for line in lines:
        if line.startswith(line_starts_with):
            new_lines.append(replace_with + "\n")
        else:
            new_lines.append(line)

    # and write everything back
    with open(file_path, 'w') as file:
        file.writelines( new_lines )
    
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
    validate_and_run(sys.argv[1:]) #get everything (slice) after the script name
