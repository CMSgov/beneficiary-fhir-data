#!/usr/bin/env bash

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/pipeline/sensitive/idr_username --with-decryption --query "Parameter.Value" --output text)"
export IDR_USERNAME
IDR_PASSWORD="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/pipeline/sensitive/idr_password --with-decryption --query "Parameter.Value" --output text)"
export IDR_PASSWORD
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/pipeline/sensitive/idr_account --with-decryption --query "Parameter.Value" --output text)"
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/pipeline/sensitive/idr_warehouse --with-decryption --query "Parameter.Value" --output text)"
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/pipeline/sensitive/idr_database --with-decryption --query "Parameter.Value" --output text)"
export IDR_DATABASE
IDR_SCHEMA="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/pipeline/sensitive/idr_schema --with-decryption --query "Parameter.Value" --output text)"
export IDR_SCHEMA

BFD_DB_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/rda-pipeline/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
export BFD_DB_USERNAME
BFD_DB_PASSWORD="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/rda-pipeline/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"
export BFD_DB_PASSWORD

db_cluster="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/common/nonsensitive/rds_cluster_identifier --with-decryption --query "Parameter.Value" --output text)"
BFD_DB_ENDPOINT="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"
export BFD_DB_ENDPOINT
