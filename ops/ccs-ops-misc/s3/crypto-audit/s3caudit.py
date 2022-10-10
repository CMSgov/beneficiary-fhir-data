#!/usr/bin/env python3
import sys
import boto3
import logging
import argparse
import functools

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
log = logging.getLogger(__name__)

# `max_depth` is the number of subdirectories to check- 0 checks all subdirectories, 1 checks top level objects only,
#   2 (the default) checks top level objects and objects in the first level of subdirectories, etc.
# `max_items` is the max number of objects to check in each subdirectory (0 checks all objects) default is 1000
# `prefix` (optional) is the prefix to filter objects by
# `unique_keys` is a set of unique keys found in the bucket during previous calls to this function
def check_objects(s3client, bucket_name, max_depth=2, max_items=1000, prefix=""):
    log.info(f"*** Validating {bucket_name} objects (max_depth={max_depth}, max_items={max_items}, prefix={prefix})")
    keys_used = set()
    # if unique_keys:
    #     keys_used = keys_used.union(unique_keys)

    # if sse is enabled, get the key id
    try:
        enc = s3client.get_bucket_encryption(Bucket=bucket_name)['ServerSideEncryptionConfiguration']['Rules'][0]
        enc = enc['ApplyServerSideEncryptionByDefault']
        if enc['SSEAlgorithm'].startswith('aws:kms'):
            bucket_key = enc['KMSMasterKeyID'].split('/')[-1]
        else:
            bucket_key = enc['SSEAlgorithm']
    except s3client.exceptions.ClientError as e:
        if e.response['Error']['Code'] == 'ServerSideEncryptionConfigurationNotFoundError':
            bucket_key = None
    except Exception as e:
        log.fatal("Unexpected error: %s" % e)
        sys.exit(1)
    p = s3client.get_paginator('list_objects_v2')
    page_config = {'MaxItems': max_items} if max_items > 0 else {}
    pages = p.paginate(Bucket=bucket_name, Delimiter='/', Prefix=prefix, PaginationConfig=page_config)
    for page in pages:
        if 'Contents' in page:
            for obj in page['Contents']:
                object = s3client.head_object(Bucket=bucket_name, Key=obj['Key'])
                if 'ServerSideEncryption' in object:
                    if object['ServerSideEncryption'] == 'aws:kms':
                        key_id = object['SSEKMSKeyId'].split('/')[-1]
                        keys_used = keys_used.union({key_id})
                        print("✓", end="", flush=True)
                    if object['ServerSideEncryption'] and object['ServerSideEncryption'] != 'aws:kms':
                        key_id = object['ServerSideEncryption'] # aws managed (ie AES256)
                        if key_id == bucket_key:
                            print("✓", end="", flush=True)
                        else:
                            print("o", end="", flush=True)
                            log.warning(
                                f"s3://{bucket_name}/{obj['Key']} encrypted with a different key ({key_id})")
                else:
                    print("x", end="", flush=True)
                    log.critical(f"s3://{bucket_name}/{obj['Key']} is not encrypted!")
        # check remaining subfolders up to max_depth, else print the summary
        if max_depth > 0 and 'CommonPrefixes' in page:
            for subfolder in page['CommonPrefixes']:
                keys_used = keys_used.union(
                    check_objects(s3client, bucket_name, max_depth - 1, max_items, subfolder['Prefix']))
    return keys_used


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--region', default='us-east-1', help='AWS region')
    bucket_options = parser.add_mutually_exclusive_group(required=False)
    bucket_options.add_argument('--bucket', help='Check a specific S3 bucket')
    bucket_options.add_argument('--bucket-prefix', default='', help='Check all buckets starting with this prefix')
    parser.add_argument('--ignore-bucket', action='append', default=[], help='Ignore specific S3 bucket(s)')
    object_limits = parser.add_argument_group('object checking limits')
    object_limits.add_argument('--max-depth', type=int, default=2, help='Max depth to check object encryption')
    object_limits.add_argument('--max-items', type=int, default=1000, help='Max number of items to check in each subdirectory')
    object_mutually_exclusive = parser.add_mutually_exclusive_group(required=False)
    object_mutually_exclusive.add_argument('--check-all-objects', action='store_true', help='Check all objects in the bucket, not just the first 1000')
    object_mutually_exclusive.add_argument_group(object_limits)
    parser.add_argument('--full-check-if-sse-disabled', action='store_true', help='Check all objects in the bucket if SSE is disabled')
    parser.add_argument('--full-check-if-no-cmk', action='store_true', help='Check all objects in the bucket if the bucket is not encrypted with a CMK')
    parser.add_argument('--disable-object-checking', action='store_true', help="Don't check objects, just check the bucket")
    args = parser.parse_args()
    region = args.region

    do_checks = True
    if args.disable_object_checking:
        do_checks = False
    if args.check_all_objects:
        obj_max_items = 0
        obj_max_depth = 0
    else:
        obj_max_depth = args.max_depth
        obj_max_items = args.max_items

    # cache boto clients and resoures
    boto3.Session.client = functools.cache(boto3.Session.client)
    boto3.Session.resource = functools.cache(boto3.Session.resource)

    # create our session clients
    session = boto3.Session(region_name=region)
    s3 = session.client('s3')
    kms = session.client('kms')

    # and cache repetitive/expensive client calls
    s3.get_bucket_encryption = functools.cache(s3.get_bucket_encryption)
    kms.describe_key = functools.cache(kms.describe_key)
    kms.list_aliases = functools.cache(kms.list_aliases)
    kms.get_key_rotation_status = functools.cache(kms.get_key_rotation_status)

    # get our buckets
    if args.bucket:
        buckets = [{'Name': args.bucket}]
    else:
        buckets = s3.list_buckets()['Buckets']
        # filter out ignored buckets
        buckets = [b['Name'] for b in buckets if b['Name'] not in args.ignore_bucket]

    # iterate over buckets
    for bucket in buckets:
        # but skip buckets that don't match the prefix (if set)
        if not bucket.startswith(args.bucket_prefix):
            log.info(f"Skipping {bucket} (doesn't match prefix)")
            continue

        # check bucket encryption
        msg = f"{bucket} sse "
        try:
            enc = s3.get_bucket_encryption(Bucket=bucket)['ServerSideEncryptionConfiguration']['Rules'][0]
            enc = enc['ApplyServerSideEncryptionByDefault']
            if enc['SSEAlgorithm'] == 'aws:kms':
                # sse enabled with a cmk, get the keys info
                key_id = enc['KMSMasterKeyID']
                aliases = ', '.join([a['AliasName'] for a in kms.list_aliases(KeyId=key_id)['Aliases']])
                kms_key = kms.describe_key(KeyId=key_id)['KeyMetadata']
                enabled = kms_key['KeyState']
                rotation = kms.get_key_rotation_status(KeyId=key_id)['KeyRotationEnabled']

                # log a warning if the key is disabled, else log the key info
                key_id = key_id.split('/')[-1]
                if enabled:
                    msg += f"enabled with cmk {key_id} (aliases: {aliases}) (rotation: {rotation})"
                    print(msg)
                    log.info(msg)
                else:
                    msg += f"enabled with a disabled cmk {key_id} (aliases: {aliases}) (rotation: {rotation})"
                    print(f"WARN: {msg}")
                    log.warning(msg)
            else:
                # sse is enabled with an aws managed key
                key_id = enc['SSEAlgorithm']
                msg += f"enabled with aws managed key ({key_id})"
                log.info(msg)
                if do_checks:
                    print(f"Checking '{bucket}'s objects ", end="", flush=True)
                    obj_max_depth = 0 if args.full_check_if_no_cmk else obj_max_depth
                    obj_max_items = 0 if args.full_check_if_no_cmk else obj_max_items
                    keys_used = check_objects(s3, bucket, obj_max_depth, obj_max_items)
                    print(" [done]")
                    # log a warning if objects were encrypted with something other than the bucket's default sse key
                    other_keys = keys_used.intersection({key_id})
                    if len(other_keys) > 0:
                        msg = f"{bucket} has objects encrypted with other keys! (found: {', '.join(other_keys)})"
                        log.warning(msg)
                        print(f"WARN: {msg}")
        except AttributeError as ae:
            log.fatal(ae)
            sys.exit(1)
        except s3.exceptions.ClientError as e:
            if e.response['Error']['Code'] == 'ServerSideEncryptionConfigurationNotFoundError':
                msg += "not enabled"
                log.critical(msg)
                print(f"CRIT: {msg}")
                if do_checks:
                    if args.full_check_if_sse_disabled:
                        check_objects(s3client=s3, bucket_name=bucket, max_depth=0, max_items=0)
                    else:
                        check_objects(s3client=s3, bucket_name=bucket, max_depth=obj_max_depth, max_items=obj_max_items)
            else:
                log.fatal(f'Unknown error: {e}')
                sys.exit(1)


if __name__ == '__main__':
    main()
