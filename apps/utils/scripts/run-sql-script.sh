#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/local-db-constants.sh"
docker cp "$1" "$BFD_LOCAL_DB_CONTAINER":/docker-entrypoint-initdb.d/script.sql
docker exec -u postgres "$BFD_LOCAL_DB_CONTAINER" psql fhirdb "$BFD_LOCAL_DB_USERNAME" -f docker-entrypoint-initdb.d/script.sql
