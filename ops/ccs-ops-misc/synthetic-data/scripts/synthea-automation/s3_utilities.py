import sys
import os
import boto3
import botocore
from botocore.config import Config
from pathlib import Path


boto_config = Config(region_name="us-east-1")
s3_client = boto3.client('s3', config=boto_config)

mitre_synthea_bucket = "bfd-synthea"
bfd_synthea_bucket = "bfd-test-synthea-etl-577373831711"
bfd_synthea_characteristic_file = "end_state/characteristics.csv"

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

def download_map_files(target_dir):
    for fn in code_map_files:
        output_fn = target_dir if target_dir.endswith('/') else target_dir + "/"
        output_fn = output_fn + fn
        print(f"file_path: {output_fn}")
        try:
            s3_client.download_file(mitre_synthea_bucket, fn, output_fn)
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                print(f"The object does not exist: {fn}")
            else:
                raise

def download_characteristic_file(target_dir) -> str:
    base_name = os.path.basename(bfd_synthea_characteristic_file)
    output_fn = target_dir if target_dir.endswith('/') else target_dir + "/"
    output_fn = output_fn + base_name
    print(f"download_characteristic_file, output_fn: {output_fn}")
    try:
        s3_client.download_file(bfd_synthea_bucket, bfd_synthea_characteristic_file, output_fn)
        return output_fn
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            print("The object does not exist.")
        else:
            raise

def upload_characteristic_file(file_name):
    print(f"upload_characteristic_file, file_name: {file_name}")
    try:
        s3_client.upload_file(file_name, bfd_synthea_bucket, bfd_synthea_characteristic_file)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            print("The object does not exist.")
        else:
            raise

def main(args):
    target = args[0] if len(args) > 0 else "./"
    op = args[1] if len(args) > 1 else "download_csv"
    if op == "download_csv":
        download_map_files(target)
    else:
        if op == "download_prop":
            rslt = download_characteristic_file(target)
            return rslt
        else:
            if op == "upload_prop":
                upload_characteristic_file(target)
            else:
                return 1

if __name__ == "__main__":
    main(sys.argv[1:])