#!/usr/bin/env bash
set -Eeuo pipefail

# @option		--profiles=""
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

if [ "$BFD_ENV" != "local" ]; then
	profiles="deployed,console-log,"
else
	profiles=""
fi

(
	cd "$SCRIPT_DIR"
	# shellcheck disable=SC2154 # argc variables are generated externally
	mvn clean spring-boot:run -Dspring-boot.run.profiles="$profiles$argc_profiles"
)
