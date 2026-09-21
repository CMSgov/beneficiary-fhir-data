#!/usr/bin/env bash

set -euo pipefail

docker cp "$1" "$BFD_LOCAL_DB_CONTAINER":/docker-entrypoint-initdb.d/script.sql
docker exec -u postgres "$BFD_LOCAL_DB_CONTAINER" psql fhirdb "$BFD_LOCAL_DB_USERNAME" -f docker-entrypoint-initdb.d/script.sql
