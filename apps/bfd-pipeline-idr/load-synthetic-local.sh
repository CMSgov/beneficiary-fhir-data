#!/usr/bin/env bash

set -e

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_username --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_USERNAME
export IDR_USERNAME
IDR_PRIVATE_KEY="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_private_key --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_PRIVATE_KEY
export IDR_PRIVATE_KEY
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_account --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_ACCOUNT
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_warehouse --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_WAREHOUSE
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_database --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_DATABASE
export IDR_DATABASE
IDR_SCHEMA="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_schema --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_SCHEMA
export IDR_SCHEMA

args=('--load-type' 'initial' '--source' 'snowflake' '--load-mode' 'local')
if [[ -n "$1" ]]; then
  args+=('--seed-from' "$1")
fi

IDR_ENABLE_DATE_PARTITIONS=0 uv run idr-pipeline "${args[@]}"
