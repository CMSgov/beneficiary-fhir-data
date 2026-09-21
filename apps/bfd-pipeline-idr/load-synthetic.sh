#!/usr/bin/env bash
set -euo pipefail

# @option	--load-mode[local|synthetic]
# @flag		--truncate
eval "$(argc --argc-eval "$0" "$@")"

if [ "$argc_load_mode" = "synthetic" ]; then
	echo "here"
	read -p "Are you sure you want to overwrite the data in ${BFD_ENV}? [yn] " -n 1 -r
	echo # (optional) move to a new line
	if ! [[ $REPLY =~ ^[Yy]$ ]]; then
		echo 'exiting'
		exit 0
	fi
fi

source ./apps/bfd-pipeline-idr/load-idr-credentials.sh --load-mode="$argc_load_mode"
if [ "$1" = "synthetic" ]; then
	source ./apps/bfd-pipeline-idr/load-bfd-credentials.sh
fi
args=('--load-type' 'initial' '--source' 'snowflake' "--load-mode=$argc_load_mode")
if [ -n "${argc_truncate+set}" ]; then
	args+=('--truncate')
fi
IDR_ENABLE_DATE_PARTITIONS=0 uv run idr-pipeline "${args[@]}"
