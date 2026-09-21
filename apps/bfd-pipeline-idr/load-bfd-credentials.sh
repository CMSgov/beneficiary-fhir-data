#!/usr/bin/env bash
set -euo pipefail

# @option --env <env>
eval "$(argc --argc-eval "$0" "$@")"

BFD_DB_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
BFD_DB_PASSWORD="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"

db_cluster="bfd-${BFD_ENV}-aurora-cluster"
BFD_DB_ENDPOINT="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"
