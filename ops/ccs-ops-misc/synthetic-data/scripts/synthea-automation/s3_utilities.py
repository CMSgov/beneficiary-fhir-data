import sys
import boto3
from botocore.config import Config
from pathlib import Path


boto_config = Config(region_name="us-east-1")
s3 = boto3.client('s3', config=boto_config)

mitre_synthea_bucket = "bfd-synthea"
bfd_synthea_bucket = "bfd-test-synthea-etl-577373831711"
bfd_synthea_characteristic_file = "end_state/end_state.properties"

code_map_files = [
    'betos_code_map.json',
    'condition_code_map.json',
    'dme_code_map.json',
    'drg_code_map.json',
    'hcps_code_map.json',
    'medication_code_map.json',
    'snf_pdpm_code_map.json',
    'snf_pps_code_map.json',
    'snf_rev_cntr_code_map.json'
    ]

def download_map_files(target_dir):
    for file_name in code_map_files:
        file_path = Path.joinpath(target_dir, file_name)
        file_path.parent.mkdir(parents=True, exist_ok=True)
        try:
            s3.Bucket(mitre_synthea_bucket).download_file(file_name, str(file_path))
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                print("The object does not exist.")
            else:
                raise

def download_characteristic_file(target_dir) -> str:
    file_path = Path.joinpath(target_dir, os.path.basename(bfd_synthea_characteristic_file))
    try:
        s3.Bucket(bfd_synthea_bucket).download_file(bfd_synthea_characteristic_file, str(file_path))
        return str(file_path)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            print("The object does not exist.")
        else:
            raise

def upload_characteristic_file(file_name):
    try:
        s3.Bucket(bfd_synthea_bucket).upload_file(
            file_name, bfd_synthea_bucket, bfd_synthea_characteristic_file)
    except botocore.exceptions.ClientError as e:
        if e.response['Error']['Code'] == "404":
            print("The object does not exist.")
        else:
            raise

def main(args):
    target = args[1] if len(args) > 1 else "./"
    op = args[2] if len(args) > 2 else "download_csv"
    if op == "download_csv":
        download_map_files(target)
    else:
        if op == "download_prop":
            rslt = upload_characteristic_file(target)
            return rslt
        else:
            if op == "upload_prop":
                upload_characteristic_file(target)
            else:
                return 1

if __name__ == "__main__":
    main(sys.argv[1:])