#!/usr/bin/env bash

set -e

if [ "${BFD_ENV}" = "synthetic" ]; then
    PARAM_USER="synthetic_env_username"
    PARAM_KEY="synthetic_env_private_key"
    PARAM_ACCT="synthetic_env_account"
    PARAM_WH="synthetic_env_warehouse"
    PARAM_DB="synthetic_env_database"
    PARAM_SCHEMA="synthetic_env_schema"
else
    PARAM_USER="idr_username"
    PARAM_KEY="idr_private_key"
    PARAM_ACCT="idr_account"
    PARAM_WH="idr_warehouse"
    PARAM_DB="idr_database"
    PARAM_SCHEMA="idr_schema"

    BFD_DB_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
    export BFD_DB_USERNAME
    BFD_DB_PASSWORD="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"
    export BFD_DB_PASSWORD

    db_cluster="bfd-${BFD_ENV}-aurora-cluster"
    BFD_DB_ENDPOINT="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"
    export BFD_DB_ENDPOINT
fi

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${PARAM_USER} --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_USERNAME
export IDR_USERNAME
IDR_PRIVATE_KEY="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${PARAM_KEY} --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_PRIVATE_KEY
export IDR_PRIVATE_KEY
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${PARAM_ACCT} --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_ACCOUNT
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${PARAM_WH} --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_WAREHOUSE
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${PARAM_DB} --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_DATABASE
export IDR_DATABASE
IDR_SCHEMA="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/${PARAM_SCHEMA} --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_SCHEMA
export IDR_SCHEMA

# TODO: remove these at some point
# useful for testing the initial claim load
export IDR_MIN_CLAIM_NCH_TRANSACTION_DATE=2014-06-30
export IDR_MIN_CLAIM_SS_TRANSACTION_DATE=2021-03-01
export IDR_LOAD_TYPE=initial
export IDR_PARTITION_TYPE=day
export IDR_LATEST_CLAIMS=0
export IDR_ENABLE_DATE_PARTITIONS=0
export IDR_MAX_TASKS=100
