
import sys
import boto3
import botocore
from botocore.config import Config

boto_config = Config(region_name="us-east-1")
s3_client = boto3.client('s3', config=boto_config)

def rename_generated_folders(args):
    '''
    Script for fixing the synthetic future data folders to have a different
    seconds place, so they are unique from likely existing future data in each
    environment. This script is a bit rough as it's somewhat of a short-term
    hack until the generation script is fixed to handle generating things
    correctly in the first place.
    '''
    ## What we should use for the seconds amount for renamed files
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
            if prefix.endswith('12:00:00Z/'):
                keys.append(prefix)
        
        numResults += len(contents)
        try:
            token = response['NextContinuationToken']
        except KeyError:
            break
    
    print("Replacing key seconds with " + rename_seconds)
    
    ## set up the new names
    updatedKeys = []
    for key in keys:
        updatedKeys.append(key.replace('00Z/', rename_seconds + 'Z/'))
        
    s3 = boto3.resource('s3')
    for key in keys:
        old_key = key
        new_key = updatedKeys.pop(0)
        
        ## Original idea was to use boto3 to copy the old key to the new one
        ## However despite trying a number of things, couldnt get it to find the key
        ## url encoding the key didnt work, copy_object nor copy seems to work (all give 404 finding the key)
        
        #print("Replacing " + old_key + " with " + new_key)
        #copy_source = {'Bucket': bucket, 'Key': old_key}
        #print("Preparing copy from " + str(copy_source))
        #s3_client.copy(Bucket=bucket, CopySource=copy_source, Key=new_key)
        #s3.Object('my_bucket','old_file_key').delete()
        
        ## for sanity's sake, just print out the commands to move them and we'll use the CLI on the command line
        print("aws s3 --recursive mv s3://" + bucket + "/" + old_key + " s3://" + bucket + "/" + new_key)

## Runs the program via run args when this file is run
if __name__ == "__main__":
    rename_generated_folders(sys.argv[1:])