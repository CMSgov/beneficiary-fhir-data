#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/local-db-constants.sh"
docker stop "$BFD_LOCAL_DB_CONTAINER" && docker rm "$BFD_LOCAL_DB_CONTAINER"
