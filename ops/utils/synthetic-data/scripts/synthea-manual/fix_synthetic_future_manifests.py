
import sys
import boto3
import botocore
from botocore.config import Config

boto_config = Config(region_name="us-east-1")
s3_client = boto3.client('s3', config=boto_config)

def rename_generated_manifest_timestamps(args):
    '''
    Script for fixing the manifests for each synthetic future data folder
    which has been renamed by rename_synthetic_future_folders_aws.py. The
    manifest needs to have the same timestamp as the folder, so this
    makes all the manifests line up with the folder timestamp. This script 
    is a bit rough as it's somewhat of a short-term
    hack until the generation script is fixed to handle generating things
    correctly in the first place.
    '''
    bucket = args[0]
    bucket_key = args[1]
    rename_seconds = args[2]
    
    keys = []
    
    token = False
    response = []
    numResults = 0
    while True:
        if token:
            response = s3_client.list_objects_v2(Bucket=bucket, Delimiter='/', Prefix=bucket_key, ContinuationToken=token)
        else:
            response = s3_client.list_objects_v2(Bucket=bucket, Delimiter='/', Prefix=bucket_key)
            
        contents = response['CommonPrefixes']
        for key in contents:
            prefix = key['Prefix']
            if prefix.endswith('12:00:' + rename_seconds + 'Z/'):
                keys.append(prefix)
        
        numResults += len(contents)
        try:
            token = response['NextContinuationToken']
        except KeyError:
            break
            
    print("Renaming manifest...")
    s3 = boto3.resource('s3')
    for key in keys:
        
        file = key + "0_manifest.xml"
        print("Adjusting " + file)
        obj = s3.Object(bucket, file)
        data = obj.get()['Body'].read().decode('utf-8') 
        data = data.replace('00Z', rename_seconds + "Z")
        s3_client.put_object(Body=data, Bucket=bucket, Key=file)
        print("Reaplced file " + file)
        

## Runs the program via run args when this file is run
if __name__ == "__main__":
    rename_generated_manifest_timestamps(sys.argv[1:])