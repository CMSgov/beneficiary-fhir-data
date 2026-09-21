#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(path=$(realpath "$0") && dirname "$path")
readonly SCRIPT_DIR

function do_load() {
	BFD_DB_USERNAME="$BFD_LOCAL_DB_USERNAME" \
		BFD_DB_PASSWORD="$BFD_LOCAL_DB_PASSWORD" \
		BFD_DB_ENDPOINT="localhost" \
		IDR_ENABLE_DATE_PARTITIONS=0 \
		uv run idr-pipeline \
		--source postgres \
		--load-mode synthetic \
		--load-type initial \
		--seed-from "${1}"
}

if [[ -d "$1/0" ]]; then
	echo "Loading batches in $1..."
	for batch_dir in "$1"/*/; do
		echo "Loading batch $batch_dir..."
		do_load "$batch_dir"
		echo "Done loading batch $batch_dir"
	done
	echo "Done loading all batches"
else
	echo "Loading full tables from $1..."
	do_load "$1"
	echo "Done loading full tables from $1"
fi
