#!/usr/bin/env bash

set -e

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

script_dir=$(path=$(realpath "$0") && dirname "$path")

docker cp "$script_dir/mock-idr.sql" bfd-idr-db:/docker-entrypoint-initdb.d/mock-idr.sql

echo
echo Creating schema.

docker exec -u postgres bfd-idr-db psql fhirdb bfd -f docker-entrypoint-initdb.d/mock-idr.sql
"$script_dir/../bfd-db-migrator-ng/migrate-local.sh"

uv sync
BFD_DB_ENDPOINT=localhost BFD_DB_USERNAME=bfd BFD_DB_PASSWORD=InsecureLocalDev uv run load_synthetic.py "$1" 
docker exec -u postgres bfd-idr-db psql fhirdb bfd -c "VACUUM FULL ANALYZE"
BFD_DB_ENDPOINT=localhost BFD_DB_USERNAME=bfd BFD_DB_PASSWORD=InsecureLocalDev IDR_LOAD_TYPE=initial IDR_ENABLE_PARTITIONS=0 uv run pipeline.py local

echo
echo Schema created successfully.
