#!/usr/bin/env bash
set -Eeuo pipefail

echo "Starting copying Snowflake synthetic data"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/../../utils/scripts/load-idr-credentials.sh" synthetic

(cd "$SCRIPT_DIR" && uv run copy-synthetic-data.py "$@")
