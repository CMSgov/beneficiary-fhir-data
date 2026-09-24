#!/usr/bin/env bash
set -euo pipefail

# @option	--resource=''
# @option	--profile-type[=CMS|Basis|Regular]
# @flag 	--all
eval "$(argc --argc-eval "$0" "$@")"

MATCHBOX_SERVER="http://localhost:18080/matchboxv3"

if [[ -v argc_all ]]; then
	all_resources="$(uv run resources.py --resource all)"
	echo "$all_resources" | jq -r '.[].name' | while read -r name; do
		# shellcheck disable=SC2154 # argc variables are generated externally
		./fhir-transform.sh --resource "$name" --profile-type "$argc_profile_type"
	done
	exit 0
fi

# shellcheck disable=SC2154
choice="$(uv run resources.py --resource "$argc_resource")"

map="$(echo "$choice" | jq -r ".map")"
resource="$(echo "$choice" | jq -r ".resource")"
input="$(echo "$choice" | jq -r ".sample")"
output="$(echo "$choice" | jq -r ".output")"
type="$(echo "$choice" | jq -r ".type")"

filename=$(basename -- "$map")
map_name="${filename%.*}"

input_file="$input"

if [[ "$type" == "EOB" || "$type" == "EOB Pharmacy" ]]; then
	echo "augmenting input file"
	# shellcheck disable=SC2154
	uv run augment_sample_resources.py "$input" "$argc_profile_type"
	input_file="out/temporary-sample.json"
fi

compiled_map_path="StructureMaps/BFD-$map_name-StructureMap.json"
sushi_resources=$(find ./sushi/fsh-generated/resources/*.json -print0 | xargs -0 -I {} echo " -ig {}" | tr -d '\n')
# shellcheck disable=SC2154,SC2046 # We want word splitting here
java -jar validator_cli.jar "$input_file" -output "$output" -transform "$resource" \
	-version 4.0.1 -ig "$compiled_map_path" -ig hl7.fhir.us.carin-bb#2.2.0 $(uv run map_imports.py "$compiled_map_path") \
	$sushi_resources -tx "$MATCHBOX_SERVER/tx"
