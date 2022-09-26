import sys
import os
import boto3
import botocore
import fnmatch
import time
from botocore.config import Config
from botocore.exceptions import ClientError, WaiterError
from pathlib import Path

boto_config = Config(region_name="us-east-1")
s3_client = boto3.client('s3', config=boto_config)

# wait config for checking presence of manifest.xml in /Done folder
# check very 30 seconds
s3_wait_delay = 30
# willing to wait for up to 3 days:
#   1440 mins / day * 3 days == 4320
#   multiply times 2 since we'll try every 30 secs
s3_wait_max_retries = 8640

# Mitre S3 bucket
mitre_synthea_bucket = "bfd-synthea"

# generic FQN for persisting end_state.properties
end_state_props_file = "end_state/end_state.properties"

# list of proprietary Mitre mapping files that will be downloaded;
# needed to generate synthetic data.
code_map_files = [
    'betos_code_map.json',
    'condition_code_map.json',
    'dme_code_map.json',
    'drg_code_map.json',
    'hcpcs_code_map.json',
    'medication_code_map.json',
    'snf_pdpm_code_map.json',
    'snf_pps_code_map.json',
    'snf_rev_cntr_code_map.json',
    'external_codes.csv'
    ]

# Mitre shell script files that need to be downloaded; used to generated synthetic data.
code_script_files = [
    'national_bfd.sh',
    'national_bfd_v2.sh'
    ]

# Function to download proprietary Mitre mapping files from an S3 bucket; the files
# are necessary to generate synthetic data. The mapping filenames are listed in an
# array of filenames so this function loops over names in the code_map_files array.
#
# Param: target_dir : unix filesystem directory where downloaded S3 file(s) are written to.
# Raises a python exception if failure to download file.
def download_synthea_files(target_dir):
    for fn in code_map_files:
        output_fn = target_dir + fn
        try:
            s3_client.download_file(mitre_synthea_bucket, fn, output_fn)
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                print(f"The object does not exist: {fn}")
            else:
                raise

# Function to download proprietary Mitre shell script files from an S3 bucket; the scripts
# drive the synthea generation process. The scripts are listed in an array of filenames
# so this function loops over names in the code_script_files array. Since the files will
# be run as shell script(s), unix file permissions are set to enable them to be executed.
#
# Param: target_dir : unix filesystem directory where downloaded S3 file(s) are written to.
# Raises a python exception if failure to download file.
def download_synthea_scripts(target_dir):
    for fn in code_script_files:
        output_fn = target_dir + fn
        try:
            s3_client.download_file(mitre_synthea_bucket, fn, output_fn)
            os.chmod(os.fspath(output_fn), 0o744)
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                print(f"The object does not exist: {fn}")
            else:
                raise

# Function to download the Mitre BFD end_state.properties file from an S3 bucket;
# the file contains meta information of the previous run of the synthea data generation
# process. Maintaining state between runs allows synthea generation to not create
# redudnant data. 
#
# Param: target_dir : unix filesystem directory where downloaded S3 end_state.properties
# file is written to.
# Raises a python exception if failure to download file.
def download_end_state_props_file(target_dir):
    base_name = os.path.basename(end_state_props_file)
    output_fn = target_dir + base_name
    try:
        s3_client.download_file(mitre_synthea_bucket, end_state_props_file, output_fn)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            print("The object does not exist.")
        else:
            raise

# Function to upload a newly generated end_state.properties file from the synthea
# run, to the Mitre BFD S3 bucket. This file will be downloaded from the S3 bucket
# as a prerequisite to the next synthea generation run. 
#
# Param: file_name : unix filesystem filename of the end_state.properties file to upload
# to the Mitre BFD S3 bucket. 
# Raises a python exception if failure to upload file.
def upload_end_state_props_file(file_name):
    # Mitre FQN for storing end_state.properties file
    mitre_synthea_end_state = f"/end_state/{file_name}"
    try:
        s3_client.upload_file(file_name, mitre_synthea_bucket, mitre_synthea_end_state)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            print("The object does not exist.")
        else:
            raise

# Function to extract a Unix timestamp from the manifest.xml file that is
# generated during the synthea generation run. This file is a key component to
# BFD's ETL processing of RIF files; this script needs the timestamp to create an
# S3 folder name in the the BFD ETL /Incoming bucket. 
#
# Param: synthea_output_dir : unix filesystem directory that the synthea generation
# process  wrote its output files.
#
# Returns unix time as a string suitable for the BFD ETL pipeline discovery process
# or an empty string if unable to extract the string from the xml file.
def extract_timestamp_from_manifest(synthea_output_dir) -> str:
    if not os.path.exists(synthea_output_dir + 'manifest.xml'):
        return ""
    lines = []
    with open(synthea_output_dir + 'manifest.xml') as file:
        lines = file.readlines()
    for line in lines:
        if line.startswith("<dataSetManifest"):
            beg_ix = line.find('timestamp=')
            if beg_ix > 0:
                ## timestamp="yyyy-mm-ddThh:mi:ssZ"
                ts = line[beg_ix:beg_ix+31]
                lines = ts.split("\"")
                return lines[1] if len(lines) > 1 else ""

# Function to upload synthea generated CSV files to the BFD ETL /Incoming
# S3 bucket folder. It does this by getting a list of files in the synthea
# output folder and for those files with a .csv file extension, and pushing
# them to the appropriate folder specified by a parameter passed to the function.
# The logic also ignores the export_summary.csv file which is generated by syntha
# but not used by the BFD ETL pipeline.
#
# Param: synthea_output_dir : unix filesystem directory that the synthea generation
# process  wrote its output files.
# Param: s3_folder : BFD S3 Bucket/folder that the .CSV files will be uploaded to.
#
# Raises a python exception if failure to upload file.
def upload_rif_files(synthea_output_dir, s3_bucket, s3_folder):
    for fn in os.listdir(synthea_output_dir):
        ## ignore the export_summary.csv
        if fn.startswith("export_summary"):
            continue
        tmp_ext = fn.split('.')[1:]
        if len(tmp_ext) > 0 and tmp_ext[0] == 'csv':
            try:
                local_fn = synthea_output_dir + fn
                remote_fn = s3_folder + "/" + fn
                print(f"{local_fn} ==> S3: {s3_bucket} : {remote_fn}")
                s3_client.upload_file(local_fn, s3_bucket, remote_fn)
            except botocore.exceptions.ClientError as e:
                if e.response['Error']['Code'] == "404":
                    print("The object does not exist.")
                else:
                    raise

# Function to upload synthea generated manifest.xml file to the BFD ETL /Incoming
# S3 bucket folder. This occurs after successfully uploading beneficiary and
# claims .CSV files to the same S3 bucket; the reason for this is that when the
# BFD ETL process detects the manifest.xml file, it triggers processing of the
# .CSV file(s).
#
# Param: synthea_output_dir : unix filesystem directory that the synthea generation
# process  wrote its output files.
# Param: s3_folder : BFD S3 Bucket/folder that the .CSV files will be uploaded to.
#
# Raises a python exception if failure to upload file.
def upload_manifest_file(synthea_output_dir, s3_bucket, s3_folder):
    local_fn = synthea_output_dir + "manifest.xml"
    remote_fn = s3_folder + "/manifest.xml"
    
    if os.path.exists(local_fn):
        print(f"S3 upload: {local_fn}, bucket: {s3_bucket}, remote_fn: {remote_fn}")
        try:
            s3_client.upload_file(local_fn, s3_bucket, remote_fn)
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                print("The object does not exist.")
            else:
                raise
    else:
        raise f"Failed to find file: {local_fn}"

# Function to upload synthea generated manifest.xml file to the BFD ETL /Incoming
# S3 bucket folder. This occurs after successfully uploading beneficiary and
# claims .CSV files to the same S3 bucket; the reason for this is that when the
# BFD ETL process detects the manifest.xml file, it triggers processing of the
# .CSV file(s).
#
# Param: synthea_output_dir : unix filesystem directory that the synthea generation
# process  wrote its output files.
# Param: s3_folder : BFD S3 Bucket/folder that the .CSV files will be uploaded to.
#
# Raises a python exception if failure to upload file.
def wait_for_manifest_done(s3_bucket, s3_folder):
    key_name = s3_folder + "/manifest.xml"
    s3_resource = boto3.resource('s3', config=boto_config)

    # use AWS Waiter object to check for the mainfest.xml showing up in the appropriate
    # /Done folder.
    try:
        waiter = s3_client.get_waiter('object_exists')
        print(f"S3 waiting for manifest: {s3_bucket} : {key_name}")
        waiter.wait(Bucket=s3_bucket, Key=key_name,
                WaiterConfig={'Delay': s3_wait_delay, 'MaxAttempts': s3_wait_max_retries})
    except ClientError as e:
        raise Exception( "boto3 client error in wait_for_manifest_done: " + e.__str__())
    except WaiterError as e:
        print("boto3 client timed out: " + e.__str__())
        sys.exit(1)
    except Exception as e:
        raise Exception( "Unexpected error in wait_for_manifest_done: " + e.__str__())

# Function to upload synthea generated .CSV files generated in the BFD synthea
# output directory to the BFD ETL S3 bucket folder. 
#
# Param: synthea_output_dir : unix filesystem directory that the synthea generation
#        process  wrote its output files.
# Param: s3_folder : BFD S3 Bucket/folder that the .CSV files will be uploaded to.
#
# Raises a python exception if failure to upload file.
def upload_synthea_results(synthea_output_dir, s3_bucket):
    manifest_ts = extract_timestamp_from_manifest(synthea_output_dir)
    # need to extract the timestamp from the manifest.xml file. If we don't get
    # it, then raise an excetion (and exit).
    if len(manifest_ts) < 1:
        raise "Failed to extract timestamp from manifest"
    if len(s3_bucket) < 1:
        raise "Failed to provide BFD S3 bucket for ETL files"

    # using the timestamp just derived, upload all RIF (.csv) files to the S3 bucket/folder
    s3_folder = "Incoming/" + manifest_ts
    print(f"uploading RIF S3: {s3_bucket}, folder: {s3_folder}");
    upload_rif_files(synthea_output_dir, s3_bucket, s3_folder)

    # now upload the manifest.xml file to the S3 bucket/folder; this is done last after
    # all RIF files are uploaded because once the ETL process sees an /Incoming manifest
    # it will begin processing.
    upload_manifest_file(synthea_output_dir, s3_bucket, s3_folder)

    # now we wait....the ETL pipeline will move processed files to the Done/ folder; when it
    # has completed processing all RIF (.csv) files, it then moves the manifest.xml file to
    # the Done/ folder signifying job job completion so we'll wait for that to happen.
    s3_folder = "Done/" + manifest_ts
    wait_for_manifest_done(s3_bucket, s3_folder)

# Function to handle S3 processing for the synthea generation shell script.
# Requires at least 2 arguments from the invocation stack:
# args[0] : the output directory where the BFD synthea process writes RIF files.
# args[1] : an operation identifier to perform.
# args[2] : environment-driven (prod, test, or prod-sbx) AWS S3 bucket; only used
#           when uploading synthea-generated result files to appropriate S3 bucket.
def main(args):
    if len(args) < 2:
        raise Exception("ERROR, failed to provide required parameters!")

    target_dir = args[0]
    op = args[1]
    bfd_s3_bucket = args[2] if len(args) > 2 else ''

    if not target_dir.endswith('/'):
        target_dir = target_dir + "/"

    print(f"op: {op}, target_dir: {target_dir}, bfd_s3_bucket: {bfd_s3_bucket}")
    match op:
        case "download_file":
            download_synthea_files(target_dir)
        case "download_script":
            download_synthea_scripts(target_dir)
        case "download_prop":
            download_end_state_props_file(target_dir)
        case "upload_prop":
            upload_end_state_props_file(target_dir)
        case "upload_synthea_results":
            upload_synthea_results(target_dir, bfd_s3_bucket)
        case _:
            return 1


if __name__ == "__main__":
    main(sys.argv[1:])