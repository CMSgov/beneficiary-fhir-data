#!/bin/bash

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
  $image \
  postgres -N $max_connections

echo
echo Waiting for port 5432 to become available.
sleep 2
docker exec bfd-idr-db timeout 15 bash -c 'until echo > /dev/tcp/localhost/5432; do sleep 1; done'

echo
echo Creating database
docker exec bfd-idr-db createdb --host localhost --username bfd --owner bfd idr

echo
echo Database created successfully.

docker cp ./mock-idr.sql bfd-idr-db:/docker-entrypoint-initdb.d/mock-idr.sql
docker cp ./bfd.sql bfd-idr-db:/docker-entrypoint-initdb.d/bfd.sql

echo
echo Creating schema.

docker exec -u postgres bfd-idr-db psql idr bfd -f docker-entrypoint-initdb.d/mock-idr.sql
docker exec -u postgres bfd-idr-db psql idr bfd -f docker-entrypoint-initdb.d/bfd.sql

uv sync
BFD_DB_ENDPOINT=localhost BFD_DB_USERNAME=bfd BFD_DB_PASSWORD=InsecureLocalDev uv run python ./load_synthetic.py "$1"

echo
echo Schema created successfully.
