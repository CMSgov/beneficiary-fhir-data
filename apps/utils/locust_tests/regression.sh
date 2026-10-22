#!/usr/bin/env bash
set -Eeou pipefail

# @flag 	--headless
# @option --users=1
# @option --table-sample-percent
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/../scripts/load-dns-names.sh" # source this first so that env is set
source "$SCRIPT_DIR/../scripts/load-bfd-credentials.sh"
source "$SCRIPT_DIR/../scripts/load-server-certs.sh"
headless=""
if [[ -v argc_headless ]]; then
	headless="--headless"
fi
tablesample=()
if [[ -v argc_table_sample_percent ]]; then
	tablesample=("--table-sample-percent" "$argc_table_sample_percent")
fi
(
	cd "$SCRIPT_DIR"
	# shellcheck disable=SC2154 # argc variables are generated externally
	uv run locust -f v3/regression_suite.py \
		--client-cert-path="$BFD_SAMHSA_COMBINED_CERT" \
		--server-public-key="$BFD_PUBLIC_CERT" \
		--host="https://${BFD_V3_LB_DNSNAME:-"$BFD_ENV.fhirv3.bfd.cmscloud.local"}" \
		--users "$argc_users" \
		--spawn-rate "$argc_users" \
		"${tablesample[@]}" \
		$headless
)
