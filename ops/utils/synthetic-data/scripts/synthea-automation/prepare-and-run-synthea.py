#
# Script for preparing a synthea repo for generating a number of
# beneficiary files. 
#
# Args:
# 1: previous end state properties file location
# 2: file system location of synthea folder
# 3: number of beneficiaries to be generated
# 4: number of months into the future that synthea should generate dates for
#
# The script will enact the following steps:
#
# 1. Validate the paths and files for synthea needed are in place within the synthea directory (as passed in arg2)
#    - Also checks files and folders that must be written to are writable, and that some externally added files are readable
#    - Checks if the output folder exists, and creates one if not
# 2. Validates the output directory is empty
#    - If the output folder has data from a previous run, the output directory is renamed with a timestamp and a new empty output directory is created
#    - Since this check handles a non-empty output folder, this step wont fail unless there is an IO issue
# 3. Swaps the script's execution directory to the synthea directory supplied in arg3, since the national script is hardcoded to run other synthea code via relative paths
# 4. Run the synthea bfd-national shell script with the number of beneficiaries supplied in arg3
#    - Output of this run will be written to a timestamped log file in the synthea directory
#    - If this run fails (denoted by checking the output for text synthea outputs on a build failure) the synthea generation step will be considered a failure
#    - If arg5 is greater than 0, synthea will generate claim lines that extend up to the input number of months into the future
#
# Example runstring: python3 prepare-and-run-synthea.py ~/Documents/end-state.properties ~/Git/synthea 100 0
#
# If any step of the above fails, a message describing the failure will be printed to stdout along with a standard message on a new line "Returning with exit code 1"
# If all steps succeed, the script will print to stdout "Returning with exit code 0 (No errors)"
#
# Note: If running locally, you will need to be connected to the VPN in order to successfully connect to the database
#

import functools
import sys
import os
import time
import fileinput
import subprocess
import shlex

# See https://stackoverflow.com/a/35467658
# Forces prints to flush _immediately_, even if a subprocess is still executing
print = functools.partial(print, flush=True)  # pylint: disable=redefined-builtin

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
    future_months = int(args[3])
    
    contract_target = args[4]
    use_contract_target = args[5]
    
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
    
    
    ## If contract target is requested via use_contract_target, add a line to replace the synthea properties:
    ## exporter.bfd.partd_contract_start and exporter.bfd.partd_contract_count
    synthea_properties = end_state_properties_file.copy()
    if use_contract_target == "true":
        if len(contract_target) != 5:
            print(f"Given contract number must be 5 characters, received '{contract_target}'")
            print("Returning with exit code 1")
            sys.exit(1)
        print("Generating using partD contract: " + contract_target)
        synthea_properties.append("exporter.bfd.partd_contract_start=" + contract_target)
        synthea_properties.append("exporter.bfd.partd_contract_count=1")
    
    update_property_file(synthea_properties, synthea_prop_filepath)
    print("Updated synthea properties")
    
    clean_synthea_output(synthea_folder_filepath)
    
    ## National script expects we're in the synthea directory, so swap to that before running
    os.chdir(synthea_folder_filepath)
    run_success = run_synthea(synthea_folder_filepath, generated_benes, future_months)
    if not run_success:
        print("Synthea run finished with errors")
        print("Returning with exit code 1")
        sys.exit(1)
        
    new_end_state_properties_file = read_file_lines(synthea_output_filepath + "bfd/end_state.properties")
    update_manifest(synthea_output_filepath, end_state_properties_file, new_end_state_properties_file)
    print("Updated synthea manifest with end.state data")
    
    print("Returning with exit code 0 (No errors)")
    sys.exit(0)

def update_manifest(synthea_output_filepath, end_state_properties_file, new_end_state_properties_file):
    '''
    Updates the manifest with the end state property data needed to perform validation
    in the pipeline.
    '''
    
    manifest_file = synthea_output_filepath + "bfd/manifest.xml"
    timestamp = ''
    week_days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    
    ## Get the end state property data BEFORE synthea generation, the start ranges of each field
    end_state_data_start_ranges = []
    for line in end_state_properties_file:
        ## Avoid any accidental blank lines in the end state file;
        ## also, ignore any comment lines
        s3 = line[1:4]
        if len(line.strip()) > 0:
            if line[0] != '#':
                tuple = line.split("=")
                end_state_data_start_ranges.append(tuple)
            elif line[1:4] in week_days:
                ## Grab the line minus the comment hash
                timestamp = line[1:]
                
    ## Get select end state property data AFTER synthea generation, the end ranges of some fields
    end_state_data_end_ranges = []
    for line in new_end_state_properties_file:
        ## Avoid any accidental blank lines in the end state file;
        ## also, ignore any comment lines
        s3 = line[1:4]
        if len(line.strip()) > 0 and line[0] != '#':
            tuple = line.split("=")
            if tuple[0] == 'exporter.bfd.bene_id_start':
                bene_id_end = tuple[1]
            if tuple[0] == 'exporter.bfd.clm_id_start':
                clm_id_end = tuple[1]
            if tuple[0] == 'exporter.bfd.pde_id_start':
                pde_id_end = tuple[1]
    
    lines_to_add = []
    lines_to_add.append("<preValidationProperties>")
    for tuple in end_state_data_start_ranges:
        ## check tuple 1 for name, then add line in the manifest
        property_name = tuple[0].split(".bfd.")[1]
        property_value = tuple[1]
        lines_to_add.append(f"<{property_name}>{property_value}</{property_name}>")
        if property_name == "bene_id_start":
            bene_id_start = property_value
    lines_to_add.append(f"<bene_id_end>{bene_id_end}</bene_id_end>")
    lines_to_add.append(f"<clm_id_end>{clm_id_end}</clm_id_end>")
    lines_to_add.append(f"<pde_id_end>{pde_id_end}</pde_id_end>")
    lines_to_add.append(f"<generated>{timestamp}</generated>")
    lines_to_add.append("</preValidationProperties>")
    lines_to_add.append("</dataSetManifest>")
    write_string = '\n'.join(lines_to_add)
    
    replace_line_starting_with(manifest_file, "</dataSetManifest>", write_string)
    
    ## Do a printout to help the user know start/end bene id for characteristics file
    print(f"Bene id start: {bene_id_start}")
    print(f"Bene id end: {bene_id_end}")

def run_synthea(synthea_folder_filepath, benes_to_generate, future_months):
    """
    Runs synthea using the national script and pipes the output
    to a log file.
    """
    
    logfile_path = f'{synthea_folder_filepath}synthea-' + time.strftime("%Y_%m_%d-%I_%M_%S_%p") + '.log'
    synthea_failed = False
    if future_months > 0:
        print(f'Running synthea ({synthea_folder_filepath}national_bfd.sh) with {benes_to_generate} benes and {future_months} future months...')
        process_cmd = shlex.split(f'{synthea_folder_filepath}national_bfd.sh {benes_to_generate} {future_months}')
    else:
        print(f'Running synthea ({synthea_folder_filepath}national_bfd.sh) with {benes_to_generate} benes...')
        process_cmd = shlex.split(f'{synthea_folder_filepath}national_bfd.sh {benes_to_generate} 0')
    with subprocess.Popen(process_cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=1) as p, \
        open(logfile_path, 'w') as f:
        for line in p.stdout:
            print(line, end='')
            f.write(line)
            if 'FAILURE:' in line:
                synthea_failed = True
    
    print(f'Synthea run complete, log saved at {logfile_path}')
    
    ## Check output for synthea failure text as the success check
    return not synthea_failed
    

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
        export_filenames = [
            'betos_code_map.json',
            'condition_code_map.json',
            'dme_code_map.json',
            'drg_code_map.json',
            'hcpcs_code_map.json',
            'hha_rev_cntr_code_map.json',
            'hha_pps_pdgm_codes.csv',
            'hha_pps_case_mix_codes.csv',
            'hospice_rev_cntr_code_map.json',
            'inpatient_rev_cntr_code_map.json',
            'medication_code_map.json',
            'outpatient_rev_cntr_code_map.json',
            'snf_pdpm_code_map.json',
            'snf_pps_code_map.json',
            'snf_rev_cntr_code_map.json',
            'external_codes.csv',
            ]
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

def get_props_value(list, starts_with):
    """
    Small helper function for getting a value from the property file
    for the line that starts with the given value.
    """
    return [x for x in list if x.startswith(starts_with)][0].split("=")[1]    

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
    
## Runs the program via run args when this file is run
if __name__ == "__main__":
    validate_and_run(sys.argv[1:]) #get everything (slice) after the script name
