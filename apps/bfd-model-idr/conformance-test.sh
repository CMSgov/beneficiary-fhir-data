#!/usr/bin/env bash
set -Eeuo pipefail

# @option	--resource=''
# @flag		--all
eval "$(argc --argc-eval "$0" "$@")"

if [[ -v argc_all ]]; then
	all_resources="$(uv run resources.py --resource all)"
	echo "$all_resources" | jq -r '.[].name' | while read -r name; do
		./conformance-test.sh --resource "$name" || true
	done
	exit 0
fi

# shellcheck disable=SC2154 # argc variables are generated externally
choice="$(uv run resources.py --resource "$argc_resource")"
output="$(echo "$choice" | jq -r ".output")"
uv run conformance_test.py "$output"
