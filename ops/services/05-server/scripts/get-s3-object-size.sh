#!/bin/bash
#######################################
# This script retrieves the size of an S3 object, returning null if the object does not exist.
#
# This is intended for use in terraform as an external data script.
#
# Requires jq and the awscli
#
# Terraform query format:
# {
#   "bucket": "<S3 bucket name>",
#   "s3_key": "<S3 object key>"
# }
#######################################

set -Eeou pipefail

# Read the Terraform query from stdin using cat.
tf_query="$(cat -)"
readonly tf_query

bucket="$(jq -r '.bucket' <<<"$tf_query")"
readonly bucket

s3_key="$(jq -r '.s3_key' <<<"$tf_query")"
readonly s3_key

aws s3api get-object-attributes --bucket "$bucket" \
  --key "$s3_key" \
  --object-attributes "ObjectSize" 2>/dev/null |
  jq '{ObjectSize: (.ObjectSize | tostring) }' ||
  jq -n '{ObjectSize: null}'
