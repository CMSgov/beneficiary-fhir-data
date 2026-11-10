#!/usr/bin/env bash
set -Eeuo pipefail

# @option	--clm-uniq-id!
# @option	--source-directory!
# @option	--profile-type[=CMS|Basis|Regular]
# @option	--output-directory!
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
source "$SCRIPT_DIR/../utils/scripts/local-constants.sh"

(
	cd "$SCRIPT_DIR"
	# shellcheck disable=SC2154 # argc variables are generated externally
	clm_id="$argc_clm_uniq_id"
	# shellcheck disable=SC2154
	output_dir="$argc_output_directory/$clm_id"

	# the sample generator reads from a single directory, so find the one containing the claim
	# shellcheck disable=SC2154
	clm_file="$(grep -rlE --include=SYNTHETIC_CLM.csv -- "(^|,)$clm_id(,|$)" "$argc_source_directory" | head -1 || true)"
	if [[ -z "$clm_file" ]]; then
		echo "claim $clm_id not found in $argc_source_directory"
		exit 1
	fi
	source_dir="$(dirname "$clm_file")"
	echo "using claim data from $source_dir"

	rm -rf "$output_dir"
	mkdir -p "$output_dir"
	uv run generate-eob-sample --clm-uniq-id "$clm_id" --source-directory "$source_dir" --output-directory "$output_dir"
	input="$(find "$output_dir" -maxdepth 1 -name 'EOB-*-Sample.json' | head -1)"

	if [[ "$(jq -r ".resourceType" "$input")" == "ExplanationOfBenefit-Pharmacy" ]]; then
		resource_name="EOB - Pharmacy"
	else
		resource_name="EOB - Base"
	fi
	choice="$(uv run resources.py --resource "$resource_name")"
	map="$(echo "$choice" | jq -r ".map")"
	resource="$(echo "$choice" | jq -r ".resource")"
	type="$(echo "$choice" | jq -r ".type")"

	filename=$(basename -- "$map")
	map_name="${filename%.*}"
	compiled_map_path="StructureMaps/BFD-$map_name-StructureMap.json"
	if [[ ! -f "$compiled_map_path" || "$map" -nt "$compiled_map_path" ]]; then
		./gen-structure-map.sh --type "$type"
	fi

	echo "augmenting input file"
	filename=$(basename -- "$input")
	out_name="${filename%.*}"
	# provider history needs to come from the same data the server loads
	# shellcheck disable=SC2154
	SYNTHETIC_PRVDR_HSTRY="$source_dir/SYNTHETIC_PRVDR_HSTRY.csv" \
		uv run augment_sample_resources.py "$input" "$argc_profile_type"
	input_file="$output_dir/augmented.json"
	mv "out/${out_name}_temp.json" "$input_file"

	output="$output_dir/ExplanationOfBenefit.json"
	sushi_resources=$(find ./sushi/fsh-generated/resources/*.json -print0 | xargs -0 -I {} echo " -ig {}" | tr -d '\n')
	# shellcheck disable=SC2046,SC2086 # We want word splitting here
	java -jar validator_cli.jar "$input_file" -output "$output" -transform "$resource" \
		-version 4.0.1 -ig "$compiled_map_path" -ig hl7.fhir.us.carin-bb#2.2.0 $(uv run map_imports.py "$compiled_map_path") \
		$sushi_resources -tx "$MATCHBOX_SERVER/tx"

	echo "saved output to $output"
)
