import sys
import os
import boto3
import botocore
import fnmatch
import time
from botocore.config import Config
from botocore.exceptions import ClientError
from pathlib import Path

boto_config = Config(region_name="us-east-1")
s3_client = boto3.client('s3', config=boto_config)

# Mitre S3 bucket
mitre_synthea_bucket = "bfd-synthea"
# generic FQN for persisting end_state.properties
end_state_props_file = "end_state/end_state.properties"

# BFD S3 root buckets for 3 environments
bfd_etl_s3_buckets = dict([
    ('prod', 'bfd-prod-etl-577373831711'),
    ('prod-sbx', 'bfd-prod-sbx-etl-577373831711'), 
    ('test', 'bfd-test-synthea-etl-577373831711')
])

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

# Mitre shell script files that will be downloaded; used to generated synthetic data.
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
        print(f"file_path: {output_fn}")
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
        print(f"download_synthea_scripts, file_path: {output_fn}")
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
    print(f"upload_end_state_props_file, file_name: {file_name}")
    # Mitre FQN for storing end_state.properties file
    mitre_synthea_end_state = "/end_state/end_state.properties"
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
    print(f"upload_rif_files, bucket: {s3_bucket}, remote_fn: {s3_folder}")
    for fn in os.listdir(synthea_output_dir):
        ## ignore the export_summary.csv
        if fn.startswith("export_summary"):
            continue
        tmp_ext = fn.split('.')[1:]
        if len(tmp_ext) > 0 and tmp_ext[0] == 'csv':
            try:
                local_fn = synthea_output_dir + fn
                remote_fn = s3_folder + "/" + fn
                print(f"upload_rif_files, local_fn: {local_fn}, remote_fn: {remote_fn}")
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
    remote_fn = s3_folder + "/" + "manifest.xml"
    
    if os.path.exists(local_fn):
        print(f"upload_manifest_file, local_fn: {local_fn}, bucket: {s3_bucket}, remote_fn: {remote_fn}")
        try:
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
def path_exists(s3_bucket, s3_resource, key_name, loop_cnt, max_tries):
    """Check to see if an object exists on S3"""
    cnt = loop_cnt
    print(f"S3 waiting for Bucket: {s3_bucket}, Key: {key_name}")

   try:
      waiter = s3_client.get_waiter('object_exists')
      waiter.wait(Bucket=s3_bucket, Key=key_name,
                  WaiterConfig={'Delay': 2, 'MaxAttempts': 5})
      print(f"Object exists: ' + bucket_name +'/'+key_name)
   except ClientError as e:
      raise Exception( "boto3 client error in path_exists: " + e.__str__())
   except Exception as e:
      raise Exception( "Unexpected error in path_exists: " + e.__str__())

#    while loop_cnt < max_tries:
#        try:
#            s3_resource.ObjectSummary(bucket_name=bfd_synthea_bucket, key=key_name).load()
#            return cnt
#        except botocore.exceptions.ClientError as e:
#            if e.response['Error']['Code'] == '404':
#                cnt += 1
#                continue
#            else:
#                raise Exception( "boto3 client error in wait_for_manifest_done: " + e.__str__())
    
def wait_for_manifest_done(s3_bucket, s3_folder):
    print(f"wait_for_manifest_done, S3 bucket: {s3_bucket}, S3 folder: {s3_folder}")
    s3_resource = boto3.resource('s3', config=boto_config)
    num_mins_3_days = 4320
    loop_cnt = 0
    key_name = s3_folder + "/"

    try:
      waiter = s3_client.get_waiter('object_exists')
      waiter.wait(Bucket=s3_bucket, Key = key_name,
                  WaiterConfig={
                     'Delay': 2, 'MaxAttempts': 5})
      print('Object exists: ' + bucket_name +'/'+key_name)
   except ClientError as e:
      raise Exception( "boto3 client error in path_exists: " + e.__str__())
   except Exception as e:
      raise Exception( "Unexpected error in path_exists: " + e.__str__())


    loop_cnt = path_exists(s3_resource, key_name, loop_cnt, num_mins_3_days)
    if loop_cnt >= num_mins_3_days:
        raise Exception(f"failed to detect S3 folder using key: {key_name}")
    else:
        key_name = s3_folder + "/manifest.xml"
        loop_cnt = path_exists(s3_resource, key_name, loop_cnt, num_mins_3_days)
        if loop_cnt >= num_mins_3_days:
            raise Exception(f"failed to detect manifest.xml using key: {key_name}")

def upload_synthea_results(synthea_output_dir, env):
    manifest_ts = extract_timestamp_from_manifest(synthea_output_dir)
    if len(manifest_ts) < 1:
        raise "Failed to extract timestamp from manifest"
    s3_bucket = bfd_etl_s3_buckets[env]
    if len(s3_bucket) < 1:
        raise "Invalid env parameter value"
    s3_folder = s3_bucket + "/Incoming/" + manifest_ts
    print(f"uploading RIF files to bucket: {s3_bucket}, folder: {s3_folder}");
    upload_rif_files(synthea_output_dir, s3_bucket, s3_folder)
    upload_rif_files(synthea_output_dir, s3_bucket, s3_folder)
    upload_manifest_file(synthea_output_dir, s3_bucket, s3_folder)

    s3_folder = s3_bucket + "/Done/" + manifest_ts
    wait_for_manifest_done(s3_bucket, s3_folder)

def main(args):
    env = args[0] if len(args) > 0 else "test"
    target = args[1] if len(args) > 1 else "./"
    if not target.endswith('/'):
        target = target + "/"
    op = args[2] if len(args) > 2 else "download_file"
    print(f"env: {env}, op: {op}, target_dir: {target}")
    match op:
        case "download_file":
            download_synthea_files(target)
        case "download_script":
            download_synthea_scripts(target)
        case "download_prop":
            download_end_state_props_file(target)
        case "upload_prop":
            upload_end_state_props_file(target)
        case "upload_synthea_results":
            upload_synthea_results(target, env)
        case _:
            return 1


if __name__ == "__main__":
    main(sys.argv[1:])