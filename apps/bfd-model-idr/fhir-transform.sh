#!/usr/bin/env bash
set -euo pipefail

# @option	--input
# @option	--profile-type[Basis|Regular|CMS]
# @option	--output
# @option	--resource
# @option	--map
eval "$(argc --argc-eval "$0" "$@")"

MATCHBOX_SERVER="http://localhost:18080/matchboxv3"

filename=$(basename -- "$argc_map")
map_name="${filename%.*}"

input_file="$argc_input"
if [[ $argc_input == *"EOB-"* ]]; then
	echo "augmenting input file"
	# shellcheck disable=SC2154 # argc variables are generated externally
	uv run augment_sample_resources.py "$argc_input" "$argc_profile_type"
	input_file="out/temporary-sample.json"
fi

compiled_map_path="StructureMaps/BFD-$map_name-StructureMap.json"
sushi_resources=$(find ./sushi/fsh-generated/resources/*.json | xargs -I {} echo " -ig {}" | tr -d '\n')
# shellcheck disable=SC2154,SC2046 # We want word splitting here
java -jar validator_cli.jar "$input_file" -output "$argc_output" -transform "$argc_resource" \
	-version 4.0.1 -ig "$compiled_map_path" -ig hl7.fhir.us.carin-bb#2.2.0 $(uv run map_imports.py "$compiled_map_path") \
	$sushi_resources -tx "$MATCHBOX_SERVER/tx"
