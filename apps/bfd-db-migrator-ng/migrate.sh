#!/usr/bin/env bash

set -Eeou pipefail

export TZ=UTC

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

# $BFD_ENV, then first arg, then "local"
bfd_env="${BFD_ENV:-"${1:-"local"}"}"
readonly bfd_env

env_username="bfd"
env_password="InsecureLocalDev"
env_db_endpoint="jdbc:postgresql://localhost:5432/fhirdb"
if [[ $bfd_env =~ (prod|sandbox|test)$ ]]; then
    env_username=${DB_USERNAME:-"$(
        aws ssm get-parameter \
            --name "/bfd/$bfd_env/migrator/sensitive/db/username" \
            --with-decryption \
            --query "Parameter.Value" \
            --output text
    )"}
    env_password=${DB_PASSWORD:-"$(
        aws ssm get-parameter \
            --name "/bfd/$bfd_env/migrator/sensitive/db/password" \
            --with-decryption \
            --query "Parameter.Value" \
            --output text
    )"}
    db_cluster="bfd-$bfd_env-aurora-cluster"
    env_db_endpoint=${DB_ENDPOINT:-"jdbc:postgresql://$(
        aws rds describe-db-clusters \
            --db-cluster-identifier "$db_cluster" \
            --query "DBClusters[0].Endpoint" \
            --output text
    ):5432/fhirdb"}
fi

username="${DB_USERNAME:-"$env_username"}"
password="${DB_PASSWORD:-"$env_password"}"
db_endpoint="${DB_ENDPOINT:-"$env_db_endpoint"}"
(
    cd "$SCRIPT_DIR"
    mvn flyway:migrate \
        "-Dflyway.schemas=idr" \
        "-Dflyway.url=$db_endpoint" \
        "-Dflyway.user=$username" \
        "-Dflyway.password=$password" \
        "-Dflyway.callbackLocations=filesystem:$SCRIPT_DIR/callbacks"
)
