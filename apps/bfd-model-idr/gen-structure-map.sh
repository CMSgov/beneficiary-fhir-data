#!/usr/bin/env bash

set -Eeuo pipefail

# @option	--type=''
# @flag 	--all
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
source "$SCRIPT_DIR/../utils/scripts/local-constants.sh"

if [[ -v argc_all ]]; then
	(
		cd "$SCRIPT_DIR"
		all_resources="$(uv run resources.py --type all)"

		echo "$all_resources" | jq -r '.[]' | while read -r name; do
			./gen-structure-map.sh --type "$name"
		done
	)
	exit 0
fi

(
	cd "$SCRIPT_DIR"
	# shellcheck disable=SC2154 # argc variables are generated externally
	choice="$(uv run resources.py --type "$argc_type")"
	map="$(echo "$choice" | jq -r ".map")"
	resource="$(echo "$choice" | jq -r ".resource")"

	filename=$(basename -- "$map")
	map_name="${filename%.*}"

	compiled_map_path="StructureMaps/BFD-$map_name-StructureMap.json"
	# shellcheck disable=SC2154
	java -jar validator_cli.jar -version 4.0.1 -ig "$map" -compile "$resource" \
		-output "$compiled_map_path" -tx "$MATCHBOX_SERVER/tx"

	echo "saved output to $compiled_map_path"
)
