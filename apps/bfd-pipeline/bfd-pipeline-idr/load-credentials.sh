#!/usr/bin/env bash

set -e

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_username --with-decryption --query "Parameter.Value" --output text)"
export IDR_USERNAME
IDR_PRIVATE_KEY="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_private_key --with-decryption --query "Parameter.Value" --output text)"
export IDR_PRIVATE_KEY
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_account --with-decryption --query "Parameter.Value" --output text)"
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_warehouse --with-decryption --query "Parameter.Value" --output text)"
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_database --with-decryption --query "Parameter.Value" --output text)"
export IDR_DATABASE
IDR_SCHEMA="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/idr_schema --with-decryption --query "Parameter.Value" --output text)"
export IDR_SCHEMA

BFD_DB_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
export BFD_DB_USERNAME
BFD_DB_PASSWORD="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"
export BFD_DB_PASSWORD

db_cluster="bfd-${BFD_ENV}-aurora-cluster"
BFD_DB_ENDPOINT="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"
export BFD_DB_ENDPOINT

export IDR_MIN_TRANSACTION_DATE=2024-07-01
export IDR_LOAD_TYPE=initial
export IDR_PARTITION_TYPE=day
export IDR_LATEST_CLAIMS=1
