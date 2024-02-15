#!/usr/bin/env bash
# Helper shim script used to translate Terraform's external data source's JSON "query" into
# environment variables and command-line arguments that the YAML decrypt script understands. Not
# intended for use outside of an external data source context

set -Eeuo pipefail

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

TF_QUERY="$(</dev/stdin)"
export TF_QUERY

# Takes in a Terraform input query, as JSON:
# {
#   "seed_env": "test",
#   "kms_key_alias": "...",
#   "env": "..."
# }
# and returns a newline-delimited string of key, value pairs:
# seed_env=test
# kms_key_alias=...
# env=...
TF_QUERY_AS_VARS=$(jq -r 'to_entries[] | "\(.key)=\(.value)"' <<<"$TF_QUERY")
readonly TF_QUERY_AS_VARS

SEED_ENV="$(jq -r '"\(.seed_env)"' <<<"$TF_QUERY")"
readonly SEED_ENV

KMS_KEY_ALIAS="$(jq -r '"\(.kms_key_alias)"' <<<"$TF_QUERY")"
readonly KMS_KEY_ALIAS

if [[ $SEED_ENV == "null" || $KMS_KEY_ALIAS == "null" ]]; then
  echo "kms_key_alias or seed_env unspecified"
  exit 1
fi

# Creates an array from the Terraform query input, where each element in the array is a key value
# string, i.e. "key=value"
vars=()
while read -r item; do
  vars+=("$item")
done <<<"$TF_QUERY_AS_VARS"

# Calls the decrypt helper script passing the original Terraform query input as environment
# variables. All extra variables passed to this script are treated as template values for the
# template strings in each .yaml file
env "${vars[@]}" "$SCRIPT_DIR"/read-and-decrypt-yaml.sh "$SEED_ENV" "$KMS_KEY_ALIAS"
