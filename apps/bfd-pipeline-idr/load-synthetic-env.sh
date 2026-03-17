#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(path=$(realpath "$0") && dirname "$path")
readonly SCRIPT_DIR

DB_CLUSTER="bfd-${BFD_ENV}-aurora-cluster"
readonly DB_CLUSTER

DB_USERNAME="$(aws ssm get-parameter --name "/bfd/${BFD_ENV}/idr-pipeline/sensitive/db/username" --with-decryption --query "Parameter.Value" --output text)"
readonly DB_USERNAME

DB_PASSWORD="$(aws ssm get-parameter --name "/bfd/${BFD_ENV}/idr-pipeline/sensitive/db/password" --with-decryption --query "Parameter.Value" --output text)"
readonly DB_PASSWORD

DB_ENDPOINT="$(aws rds describe-db-clusters --db-cluster-identifier "$DB_CLUSTER" --query "DBClusters[0].Endpoint" --output text)"
readonly DB_ENDPOINT

function do_load() {
  PGPASSWORD="$DB_PASSWORD" psql "host=$DB_ENDPOINT port=5432 dbname=fhirdb user=$DB_USERNAME" -f "$SCRIPT_DIR/mock-idr.sql"
  BFD_DB_USERNAME="$DB_USERNAME" BFD_DB_PASSWORD="$DB_PASSWORD" BFD_DB_ENDPOINT="$DB_ENDPOINT" uv run load_synthetic.py "$1"
  BFD_DB_USERNAME="$DB_USERNAME" BFD_DB_PASSWORD="$DB_PASSWORD" BFD_DB_ENDPOINT="$DB_ENDPOINT" IDR_LOAD_TYPE=initial IDR_ENABLE_DATE_PARTITIONS=0 uv run pipeline.py synthetic
}

if [[ -d "$1/0" ]]; then
  echo "Loading batches in $1..."
  for batch_dir in "$1"/*/; do
    echo "Loading batch $batch_dir..."
    do_load "$batch_dir"
    echo "Done loading batch $batch_dir"
  done
  echo "Done loading all batches"
else
  echo "Loading full tables from $1..."
  do_load "$1"
  echo "Done loading full tables from $1"
fi
