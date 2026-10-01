#!/usr/bin/env bash
set -Eeou pipefail

# @flag 	--headless
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/../scripts/load-bfd-credentials.sh"
source "$SCRIPT_DIR/../scripts/load-server-certs.sh"
headless=""
if [[ -v argc_headless ]]; then
	headless="--headless"
fi
(
	cd "$SCRIPT_DIR"
	uv run locust -f v3/regression_suite.py \
		--client-cert-path="$BFD_COMBINED_CERT" \
		--host="https://$BFD_ENV.fhirv3.bfd.cmscloud.local" \
		$headless
)
