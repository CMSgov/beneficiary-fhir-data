#!/usr/bin/env bash
set -Eeuo pipefail

# @option --clm-uniq-id!
# @option --source-directory!
# @option --profile-type[=CMS|Basis|Regular]
# @option --output-directory!
eval "$(argc --argc-eval "$0" "$@")"

cd "$(dirname "$(realpath "$0")")"

clm_id=${argc_clm_uniq_id:?}
out=${argc_output_directory:?}/$clm_id

# generate-eob-sample only takes one dir, so find the one with this claim
# shellcheck disable=SC2154 # argc variables are generated externally
clm_file=$(grep -rlE --include=SYNTHETIC_CLM.csv -- "(^|,)$clm_id(,|$)" "$argc_source_directory" | head -1 || true)
[[ -n $clm_file ]] || {
	echo "claim $clm_id not found in $argc_source_directory" >&2
	exit 1
}
src=$(dirname "$clm_file")
echo "using $src"

rm -rf "$out" && mkdir -p "$out"
uv run generate-eob-sample --clm-uniq-id "$clm_id" --source-directory "$src" --output-directory "$out"
input=$(find "$out" -maxdepth 1 -name 'EOB-*-Sample.json' | head -1)

resource="EOB - Base"
[[ $(jq -r .resourceType "$input") == ExplanationOfBenefit-Pharmacy ]] && resource="EOB - Pharmacy"

# use the same provider history the server loads
# shellcheck disable=SC2154
SYNTHETIC_PRVDR_HSTRY=$src/SYNTHETIC_PRVDR_HSTRY.csv ./fhir-transform.sh \
	--resource "$resource" \
	--profile-type "$argc_profile_type" \
	--input "$input" \
	--output "$out/ExplanationOfBenefit.json"

echo "wrote $out/ExplanationOfBenefit.json"
