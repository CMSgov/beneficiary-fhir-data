#!/usr/bin/env bash

set -euo pipefail

# @option	--map
# @option	--resource
eval "$(argc --argc-eval "$0" "$@")"

MATCHBOX_SERVER="http://localhost:18080/matchboxv3"

# shellcheck disable=SC2154 # argc variables are generated externally
filename=$(basename -- "$argc_map")
map_name="${filename%.*}"

compiled_map_path="StructureMaps/BFD-$map_name-StructureMap.json"
# shellcheck disable=SC2154
java -jar validator_cli.jar -version 4.0.1 -ig "$argc_map" -compile "$argc_resource" \
	-output "$compiled_map_path" -tx "$MATCHBOX_SERVER/tx"
