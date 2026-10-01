#!/usr/bin/env bash
set -Eeuo pipefail

# @option	--tablesample=10
# @option	--limit=300
# @option	--concurrency=10
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/../scripts/load-bfd-credentials.sh"
source "$SCRIPT_DIR/../scripts/load-server-certs.sh"
(
	cd "$SCRIPT_DIR"
	# shellcheck disable=SC2154 # argc variables are generated externally
	uv run main.py \
		--hostname "$BFD_ENV.fhirv3.bfd.cmscloud.local" \
		--security-labels "$SCRIPT_DIR/../../bfd-server-ng/src/main/resources/security_labels.yml" \
		--samhsa-cert "$BFD_SAMHSA_CLIENT_CERT" \
		--samhsa-cert-key "$BFD_SAMHSA_CLIENT_KEY" \
		--no-samhsa-cert "$BFD_CLIENT_CERT" \
		--no-samhsa-cert-key "$BFD_CLIENT_KEY" \
		--host-cert "$BFD_PUBLIC_CERT" \
		--tablesample "$argc_tablesample" \
		--limit "$argc_limit" \
		--concurrency "$argc_concurrency"
)
