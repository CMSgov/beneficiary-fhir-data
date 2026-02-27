#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(path=$(realpath "$0") && dirname "$path")
readonly SCRIPT_DIR

DB_ENDPOINT=localhost
readonly DB_ENDPOINT

DB_USERNAME=bfd
readonly DB_USERNAME

DB_PASSWORD=InsecureLocalDev
readonly DB_PASSWORD

function do_load() {
  PGPASSWORD="$DB_PASSWORD" psql "host=$DB_ENDPOINT port=5432 dbname=fhirdb user=$DB_USERNAME" -f "$SCRIPT_DIR/mock-idr.sql"
  BFD_DB_USERNAME="$DB_USERNAME" BFD_DB_PASSWORD="$DB_PASSWORD" BFD_DB_ENDPOINT="$DB_ENDPOINT" uv run load_synthetic.py "$1"
  docker exec -u postgres bfd-idr-db psql fhirdb bfd -c "VACUUM FULL ANALYZE"
  BFD_DB_USERNAME="$DB_USERNAME" BFD_DB_PASSWORD="$DB_PASSWORD" BFD_DB_ENDPOINT="$DB_ENDPOINT" IDR_LOAD_TYPE=initial IDR_ENABLE_DATE_PARTITIONS=0 uv run pipeline.py synthetic
}

image=postgres:16.6
max_connections=500
docker pull $image

docker rm -f bfd-idr-db
docker volume rm -f bfd-idr-db

docker run \
  -d \
  --name 'bfd-idr-db' \
  -e 'POSTGRES_USER=bfd' \
  -e 'POSTGRES_PASSWORD=InsecureLocalDev' \
  -p '5432:5432' \
  -v 'bfd-idr-db:/var/lib/postgresql/data' \
  --shm-size=1g \
  $image \
  postgres -N $max_connections

echo
echo Waiting for port 5432 to become available.
sleep 2
docker exec bfd-idr-db timeout 15 bash -c 'until echo > /dev/tcp/localhost/5432; do sleep 1; done'

echo
echo Creating database
docker exec bfd-idr-db createdb --host localhost --username bfd --owner bfd fhirdb

echo
echo Database created successfully.

docker cp "$SCRIPT_DIR/mock-idr.sql" bfd-idr-db:/docker-entrypoint-initdb.d/mock-idr.sql

echo
echo Creating schema.

docker exec -u postgres bfd-idr-db psql fhirdb bfd -f docker-entrypoint-initdb.d/mock-idr.sql
"$SCRIPT_DIR/../bfd-db-migrator-ng/migrate-local.sh"

echo
echo Schema created successfully.

uv sync

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

