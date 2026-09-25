#!/usr/bin/env bash
set -Eeuo pipefail

image=postgres:16.6
max_connections=500

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/local-db-constants.sh"

docker pull $image

docker rm -f "$BFD_LOCAL_DB_CONTAINER"
docker volume rm -f "$BFD_LOCAL_DB_CONTAINER"

docker run \
	-d \
	--name "$BFD_LOCAL_DB_CONTAINER" \
	-e "POSTGRES_USER=$BFD_LOCAL_DB_USERNAME" \
	-e "POSTGRES_PASSWORD=$BFD_LOCAL_DB_PASSWORD" \
	-p '5432:5432' \
	-v "$BFD_LOCAL_DB_CONTAINER:/var/lib/postgresql/data" \
	$image \
	postgres -N $max_connections

echo
echo Waiting for port 5432 to become available.
sleep 2
docker exec "$BFD_LOCAL_DB_CONTAINER" timeout 15 bash -c 'until echo > /dev/tcp/localhost/5432; do sleep 1; done'

echo
echo Creating database
docker exec "$BFD_LOCAL_DB_CONTAINER" createdb --host localhost --username "$BFD_LOCAL_DB_USERNAME" --owner "$BFD_LOCAL_DB_USERNAME" fhirdb

echo
echo Database created successfully.
