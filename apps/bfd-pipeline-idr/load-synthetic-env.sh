#!/usr/bin/env bash

set -e

db_cluster="bfd-${BFD_ENV}-aurora-cluster"
username="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/username --with-decryption --query "Parameter.Value" --output text)"
password="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/db/password --with-decryption --query "Parameter.Value" --output text)"
endpoint="$(aws rds describe-db-clusters --db-cluster-identifier $db_cluster --query "DBClusters[0].Endpoint" --output text)"

script_dir=$(path=$(realpath "$0") && dirname "$path")

PGPASSWORD="$password" psql "host=$endpoint port=5432 dbname=fhirdb user=$username" -f "$script_dir/mock-idr.sql"
BFD_DB_USERNAME="$username" BFD_DB_PASSWORD="$password" BFD_DB_ENDPOINT="$endpoint" uv run load_synthetic.py "$1"
BFD_DB_USERNAME="$username" BFD_DB_PASSWORD="$password" BFD_DB_ENDPOINT="$endpoint" IDR_LOAD_TYPE=initial IDR_ENABLE_PARTITIONS=0 uv run pipeline.py synthetic
