#
# Script for taking the results of a synthea run and splitting the claims from future dates into their own loads.
# This will allow us to have those loads queued up in the Synthetic/Incoming folder to load as if they were updates. 
#
# The script does the following:
#
# 1. Using threads, reads each file and stores its lines into memory
# 2. Splits up the lines based on date, per file;
#   a. Any line with a date less than or equal to todays date remains in the "current" bucket of line data for that file
#   b. Any future lines get placed in a dictionary based on the week they fall into. The dictionary is keyed off the 2 Wednesdays past the line's date (~2 weeks ahead). This is to simulate file processing time in prod for future loads.
# 3. After all files are read and split, the counts are validated to ensure the lines the file started with match the total of all the split lines added together, per file
# 4. After successful validation, folders are made for every week that was required for any file by looking at the dictionary keys. These folders are placed in /bfd/output automatically
# 5. Manifests are made for each of the future folders, using the same timestamp as the folder for the manifest, adding only the files that had lines for that date
# 6. Files are generated in each weeks' folder for each file that had lines for that week, plus header
# 7. The "current" data, which is now the original data minus the future data, overwrites the original files in the synthea dir/bfd/output
#
# Note that the future files will be loaded on the day specified by load_day and at the time specified in the manifest_date_format.
#
# Args:
# 1: file system location of synthea folder, for finding the output result to split the files for
#
# Example runstring: python3 ./split-future-claims.py ~/Git/synthea
#
#

import os
import sys
import fileinput
import shlex
import concurrent.futures

import datetime

## Set the manifest (and thus load time) to early in the morning weds (4am PST/7am EST)
manifest_date_format = "%Y-%m-%dT12:00:00Z"
lines_per_thread = 500000
failed_validation = False
## helps understand the days of the week related to calendar date calculation
MON, TUE, WED, THU, FRI, SAT, SUN = range(7)
load_day = WED

def split_future_synthea_load(args):
    """
    Reads in each non-bene output file and splits any lines with a future date 
    into weekly folders for future consumption.
    The original files are then overwritten without the future lines.
    """
    
    synthea_folder_filepath = args[0]
    if not synthea_folder_filepath.endswith('/'):
        synthea_folder_filepath = synthea_folder_filepath + "/"
    synthea_output_filepath = synthea_folder_filepath + "output/bfd/"
    
    ## Take each file and start scanning through the lines, noting the dates for each line and moving them into the appropriate folder/file depending on which week they fall into
    files = ['carrier.csv','pde.csv','dme.csv', 'hha.csv', 'hospice.csv', 'inpatient.csv', 'outpatient.csv', 'snf.csv']
    today = datetime.date.today()
    
    threads = [] ## keep track of threads we spawn to wait on
    with concurrent.futures.ThreadPoolExecutor() as executor:
        for file_path in files:
            if os.path.isfile(synthea_output_filepath + file_path):
                print(f"Creating thread for {file_path}")
                future = executor.submit(split_future_from_file, file_path, synthea_output_filepath, today)
                threads.append(future)
            else:
                print(f"Could not find path for {synthea_output_filepath}{file_path}")
            
    concurrent.futures.wait(threads, timeout=None, return_when='ALL_COMPLETED')
    print("All files done processing.")
    
    ## Before writing, check if our data validation failed
    if failed_validation:
        print("Returning with exit code 1")
        sys.exit(1)
    
    ## Get the results of processing the files
    file_data_tuples = []
    for future in threads:
        file_data_tuples.append(future.result())
        
    ## Make shared week folders to hold files and manifests all at once to avoid threading issues later
    create_week_folders_and_manifests(file_data_tuples, synthea_output_filepath, today)
        
    ## Get the file headers (remove the trailing newline that gets added)
    headers = {}
    for file_path in files:
        full_path = synthea_output_filepath + file_path
        if os.path.isfile(full_path):
            with open(full_path) as file:
                headers[file_path] = file.readline()
            
    ## write the file data to the right place
    for file_data_tuple in file_data_tuples:
        ## first return item in the tuple is the file
        file_name = file_data_tuple[0]
        ## second tuple item is the dict with weeks in it
        file_dict = file_data_tuple[1]
        for week_key in file_dict.keys():
            if week_key == today.strftime(manifest_date_format):
                ## write today's files to overwrite the originals
                write_path = f"{synthea_output_filepath}{file_name}"
            else:
                write_path = f"{synthea_output_filepath}{week_key}/{file_name}"
            ## Add header to data
            data = [headers[file_name]] + file_dict[week_key]
            with open(write_path, 'w') as f:
                f.writelines(line for line in data)
            
    print("Finished writing all files")    
    
def create_week_folders_and_manifests(file_data_tuples, synthea_output_filepath, today):
    '''
    Creates the future week folders and manifests based on the weeks found in the master list 
    of file data (specifically looking at the weeks and included filenames).
    
    These new folders will be created in the specified synthea path in the output/bfd location,
    and will be named after the END day of the week they represent so that they get loaded at the end of their week.
    '''
    
    ## get what week folders we need, which files will be in them, and make all the folders + manifests
    weeks_files = {}
    for file_data_tuple in file_data_tuples:
        ## first return item in the tuple is the file
        file_name = file_data_tuple[0]
        ## second tuple item is the dict with weeks in it
        file_dict = file_data_tuple[1]
        ## cycle through this file's weeks and add to the final list of weeks
        for week_key in file_dict.keys():
            if week_key not in weeks_files.keys():
                weeks_files[week_key] = [file_name]
            ## If the week exists, add this file to the list of files needed in that week's manifest if it doesnt exist
            elif file_name not in weeks_files[week_key]:
                weeks_files[week_key].append(file_name)
    
    print(f"Final weeks/files: {weeks_files}")
    
    ## Create folders and manifests
    for week in weeks_files.keys():
        week_folder_path = synthea_output_filepath + week
        if not week == today.strftime(manifest_date_format) and not os.path.exists(week_folder_path):
            ## Skip today's date since we'll replace those files in place
            os.mkdir(week_folder_path)
            create_manifest(week_folder_path, weeks_files[week], week)
    

def create_manifest(path, file_list, timestamp):
    '''
    Creates a new manifest at the given location with the given list of files and timestamp.
    '''
    with open(path + "/0_manifest.xml", "w") as manifest:
        manifest_lines = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        manifest_lines += f'<dataSetManifest xmlns="http://cms.hhs.gov/bluebutton/api/schema/ccw-rif/v10" timestamp="{timestamp}" sequenceId="0" syntheticData="true">\n'
        for file in file_list:
            type = file.rstrip('.csv').upper()
            manifest_lines += f'  <entry name="{file}" type="{type}"/>\n'
        manifest_lines += f'</dataSetManifest>'
        manifest.write(manifest_lines)
    
def split_future_from_file(file_path, synthea_output_filepath, today):
    '''
    For the file at the given path, reads through the lines in the file
    and splits out the dates that are in the future into their own datasets.
    
    Returns a tuple which represents the file name (first tuple) and dictionary (second tuple) composed of 
    week timestamp the data belongs to (key) and list of lines that belong to the week (value).
    Example return: ('carrier.csv', {'2022-10-11T12:00:00Z': [<line2 data>, <line4 data>...], '2022-12-28T12:00:00Z': [<line12 data>, <line15 data>...]})
    '''
    full_path = synthea_output_filepath + file_path
    ## Get file length
    with open(full_path) as file:
        lines = file.readlines()
        header = lines[0] ## save the header
        lines = lines[1:] ## remove the header, makes life easier later
        num_lines = len(lines)
    
    ## Divide and round up to get the number of threads needed using some arcane syntax
    num_threads = (-(-num_lines//lines_per_thread))
    
    thread_results = [] ## Store list of thread results
    threads = [] ## keep track of threads we spawn to wait on
    with concurrent.futures.ThreadPoolExecutor() as executor:
        for i in range(num_threads):
            line_start = lines_per_thread * i
            line_end = lines_per_thread * (i+1)
            if line_end > num_lines:
                line_end = num_lines
            future = executor.submit(split_future_lines, full_path, header, lines[line_start:line_end], today)
            threads.append(future)
    
    ## Wait for all the threads to complete before continuing
    concurrent.futures.wait(threads, timeout=None, return_when='ALL_COMPLETED')
    
    for future in threads:
        thread_results.append(future.result())
    
    ## When all threads are done, merge the dicts
    final_dict = {}
    for result_dict in thread_results:
        if len(final_dict.keys()) == 0:
            final_dict = result_dict
        else:
            for key in result_dict.keys():
                if key in final_dict.keys():
                    final_dict[key] = final_dict[key] + result_dict[key]
                else:
                    final_dict[key] = result_dict[key]
                    
    ## Make sure our line counts match up, print an error if they dont
    validate_final_lines(file_path, num_lines, final_dict)
    
    return (file_path, final_dict)


def validate_final_lines(file_name, original_count, final_dict):
    '''
    Validates that for the given file name, the lines in all the week data after
    splitting equals the original line count (minus the header) to ensure the line
    split adds up.
    '''
    line_count_total = 0
    for week in final_dict.keys():
        line_count_total = line_count_total + len(final_dict[week])
        
    if not original_count == line_count_total:
        print(f"(Validation Failure) File {file_name} split into {line_count_total} lines, expected {original_count}")
        failed_validation = True
    else:
        print(f"(Validation Success) File {file_name} correctly split into {line_count_total} lines over {len(final_dict.keys())} weeks")

def split_future_lines(file_path, header, lines, today):
    """
    For the given file lines, grab the column that holds the claim date
    and compare it to the current date; if its in the future, find the week
    that it belongs to and add it to a dictionary that holds lines using that
    week as a key. If it is not in the future, use the current datestring as a key
    and put the line there.
    
    Returns a dictionary where the key is the datestring of the week the line(s) belong to,
    and the value is a list of the lines belonging to that week.
    Example return: {'2022-10-11T12:00:00Z': [<line2 data>, <line4 data>...], '2022-12-28T12:00:00Z': [<line12 data>, <line15 data>...]}
    """
    
    date_index = get_field_index(header, "NCH_WKLY_PROC_DT")
    if date_index == -1:
        date_index = get_field_index(header, "PD_DT")
    if date_index == -1:
        print(f"ERROR: Unable to find date field index in file {file_path}")
    failed_validation = True
    
    ## Set up dict to hold the files to write (current + future)
    week_data = {}
        
    for line in lines:
        line_date = line.split('|')[date_index]
        parsed_date = datetime.datetime.strptime(line_date, "%d-%b-%Y").date()
        if parsed_date > today:
            target_weds = get_target_wednesday(parsed_date)
            if target_weds.strftime(manifest_date_format) not in week_data.keys():
                week_data[target_weds.strftime(manifest_date_format)] = []
            week_data[target_weds.strftime(manifest_date_format)].append(line)
            
        else:
            ## Line was not a future line, so add it to today's load
            if today.strftime(manifest_date_format) not in week_data.keys():
                week_data[today.strftime(manifest_date_format)] = []
            week_data[today.strftime(manifest_date_format)].append(line)
                
    return week_data
    
def get_target_wednesday(checked_date):
    '''
    Gets the next wednesday 2 weeks from the given date. (This is a specifically chosen
    amount of time that simulates data processing time to be more prod-like.)
    '''
    ## Check how many days are between now and weds (possibly negative offset)
    days_until_weds_offset = load_day - checked_date.weekday()
    ## go 2 week forward + offset until following weds
    days_ahead = 14 + days_until_weds_offset
    return checked_date + datetime.timedelta(days_ahead)
    
def get_field_index(header, field):
    """
    Gets the index from the header for the specified field.
    If unable to find, returns -1.
    """
    lines = header.split('|')
    index = 0
    for line in lines:
        if line == field:
            return index
        index = index + 1
    
    return -1


## Runs the program via run args when this file is run
if __name__ == "__main__":
    split_future_synthea_load(sys.argv[1:])
