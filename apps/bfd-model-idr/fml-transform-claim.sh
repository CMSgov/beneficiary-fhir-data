#!/usr/bin/env bash
set -Eeuo pipefail

# @option	--clm-uniq-id!
# @option	--source-directory!
# @option	--profile-type[=CMS|Basis|Regular]
# @option	--output-directory!
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"

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

	# provider history needs to come from the same data the server loads
	# shellcheck disable=SC2154
	SYNTHETIC_PRVDR_HSTRY="$source_dir/SYNTHETIC_PRVDR_HSTRY.csv" ./fhir-transform.sh \
		--resource "$resource_name" \
		--profile-type "$argc_profile_type" \
		--input "$input" \
		--output "$output_dir/ExplanationOfBenefit.json"

	echo "saved output to $output_dir/ExplanationOfBenefit.json"
)
