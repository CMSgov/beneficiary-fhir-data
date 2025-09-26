#!/usr/bin/env bash
#
# Sync a simple synthea-generated dataset from s3://bfd-mgmt-synthea to an ETL bucket.
#
# **NOTE** This only works for single-datasets and needs enhancement to support
#          recurring datasets.
shopt -s expand_aliases
set -euo pipefail

usage="$(basename "$0") <target_bucket> <dataset>

Example:
  ./load-dataset.sh bfd-3460-prod-etl20240603193725757700000001 generated-2024-08-06_12-07-34/

Sync a simple synthea-generated dataset from s3://bfd-mgmt-synthea to an ETL bucket.
This script provides an interactive validation preview (dryrun) that requires user-input to execute the sync.

Requires 'awscli'. Preference toward GNU 'awk' and GNU 'sed'.
See output of 'aws s3 ls s3://bfd-mgmt-synthea/generated/' for valid <dataset> inputs."

# Display usage string if there aren't exactly 2 arguments
if [ "$#" -ne 2 ]; then
    echo "$usage"
    exit 1
fi

# Gently nudge toward GNU utilities...
if [ "$(uname)" = 'Darwin' ]; then
    command -v gawk >/dev/null && alias "awk=gawk" || echo "'gawk' not found; try 'brew install gawk'"
    command -v gsed >/dev/null && alias "sed=gsed" || echo "'gsed' not found; try 'brew install gnu-sed'"
fi

target_bucket="$1"
dataset="${2//\/}" # Remove any potential '/' characters from the dataset
source_bucket="bfd-mgmt-synthea"

# The manifest_timestamp depends on manifest.xml structure of the <dataManifest xmlns="[..]" timestamp="[..]" [..]> key
manifest_timestamp="$(aws s3 cp "s3://${source_bucket}/generated/${dataset}/output/bfd/manifest.xml" - | grep timestamp | awk '{print $3}' | sed s/timestamp=// | tr -d '"')"

echo "Previewing load of dataset ${dataset} with timestamp of ${manifest_timestamp} FROM ${source_bucket} TO ${target_bucket} ..."

aws s3 sync "s3://${source_bucket}/generated/${dataset}/output/bfd/" \
    "s3://${target_bucket}/Synthetic/Incoming/${manifest_timestamp}" \
    --exclude "end_state*" \
    --exclude "missing_codes.csv" \
    --exclude "npi.tsv" \
    --exclude "export_summary.csv" \
    --dryrun

echo

read -p "Does the preview above look correct (y/n)? " -n 1 -r

echo

if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo "Loading..."
    aws s3 sync "s3://${source_bucket}/generated/${dataset}/output/bfd/" \
    "s3://${target_bucket}/Synthetic/Incoming/${manifest_timestamp}" \
    --exclude "end_state*" \
    --exclude "missing_codes.csv" \
    --exclude "npi.tsv" \
    --exclude "export_summary.csv"

    # Rename manifest to begin load...
    aws s3 mv "s3://${target_bucket}/Synthetic/Incoming/${manifest_timestamp}/manifest.xml" \
        "s3://${target_bucket}/Synthetic/Incoming/${manifest_timestamp}/0_manifest.xml";
else
    echo "Preview rejected. Doing nothing and exiting."
fi
