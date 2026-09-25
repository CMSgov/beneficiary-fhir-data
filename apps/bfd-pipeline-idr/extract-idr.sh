#!/usr/bin/env bash
set -Eeuo pipefail

echo "Starting IDR Extract"
echo "BFD_ENV: ${BFD_ENV}"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/../utils/scripts/load-idr-credentials.sh" synthetic

EXPORT_FILE_DIR="../bfd-model-idr/out"
readonly EXPORT_FILE_DIR
export EXPORT_FILE_DIR

(cd "$SCRIPT_DIR" && uv run idr-extract "$@")
