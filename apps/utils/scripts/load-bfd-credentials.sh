#!/usr/bin/env bash
set -Eeuo pipefail

BFD_DB_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
export BFD_DB_USERNAME
BFD_DB_PASSWORD="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"
export BFD_DB_PASSWORD

db_cluster="bfd-${BFD_ENV}-aurora-cluster"
BFD_DB_ENDPOINT="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"
export BFD_DB_ENDPOINT
