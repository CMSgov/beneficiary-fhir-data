#!/usr/bin/env bash
set -euo pipefail

echo "Starting IDR Extract"
echo "BFD_ENV: ${BFD_ENV}"

source ../utils/scripts/load-idr-credentials.sh synthetic

EXPORT_FILE_DIR="../bfd-model-idr/out"
readonly EXPORT_FILE_DIR
export EXPORT_FILE_DIR

uv run idr-extract "$@"
