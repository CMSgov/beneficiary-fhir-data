#!/usr/bin/env bash
set -Eeuo pipefail

# @option	--source-env
# @option	--seed-from
# @flag		--truncate
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

if [ -v argc_source_env ]; then
	if [ "$BFD_ENV" != "local" ]; then
		echo "--source-env can only be used when BFD_ENV is 'local'"
		exit 1
	fi
	source_env="$argc_source_env"
else
	source_env="$BFD_ENV"
fi

if [ "$BFD_ENV" != "local" ]; then
	read -p "Are you sure you want to overwrite the data in ${BFD_ENV}? [yn] " -n 1 -r
	echo # (optional) move to a new line
	if ! [[ $REPLY =~ ^[Yy]$ ]]; then
		echo 'exiting'
		exit 0
	fi
fi

if [ "$source_env" != "local" ]; then
	BFD_ENV="$source_env" source "$SCRIPT_DIR/../utils/scripts/load-idr-credentials.sh" synthetic
fi
if [ "$BFD_ENV" = "local" ]; then
	source "$SCRIPT_DIR/../utils/scripts/local-constants.sh"
	export BFD_DB_USERNAME="$BFD_LOCAL_DB_USERNAME"
	export BFD_DB_PASSWORD="$BFD_LOCAL_DB_PASSWORD"
	export BFD_DB_ENDPOINT="localhost"
else
	source "$SCRIPT_DIR/../utils/scripts/load-bfd-credentials.sh"
fi

args=("--load-type=initial" "--load-mode=synthetic")
if [ "$source_env" = "local" ]; then
	args+=("--source=postgres")
else
	args+=("--source=snowflake")
fi

if [ -v argc_truncate ]; then
	args+=('--truncate')
fi

if [ -v argc_seed_from ]; then
	args+=("--seed-from=$argc_seed_from")
fi

(cd "$SCRIPT_DIR" && IDR_ENABLE_DATE_PARTITIONS=0 LOGURU_COLORIZE=YES uv run idr-pipeline "${args[@]}")
