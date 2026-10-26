#!/usr/bin/env bash
set -Eeuo pipefail

# @option	--tablesample=10
# @option	--limit=300
# @option	--concurrency=10
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

(
	cd "$SCRIPT_DIR"
	if [ "$BFD_ENV" = "local" ]; then
		source "$SCRIPT_DIR/../scripts/local-constants.sh"
		# shellcheck disable=SC2154 # argc variables are generated externally
		uv run app/main.py \
			--hostname localhost:8080 \
			--security-labels "$SCRIPT_DIR/../../bfd-server-ng/src/main/resources/security_labels.yml" \
			--tablesample "$argc_tablesample" \
			--limit "$argc_limit" \
			--concurrency "$argc_concurrency"
	else
		source "$SCRIPT_DIR/../scripts/load-dns-names.sh" # source this first so that env is set
		source "$SCRIPT_DIR/../scripts/load-bfd-credentials.sh"
		source "$SCRIPT_DIR/../scripts/load-server-certs.sh"

		host_cert=()
		if [[ $BFD_ENV == "test" || $BFD_ENV == "sandbox" || $BFD_ENV == "prod" ]]; then
			host_cert=("--host-cert" "$BFD_PUBLIC_CERT")
		fi
		# shellcheck disable=SC2154
		uv run app/main.py \
			--hostname "${BFD_V3_LB_DNSNAME:-"$BFD_ENV.fhirv3.bfd.cmscloud.local"}" \
			--security-labels "$SCRIPT_DIR/../../bfd-server-ng/src/main/resources/security_labels.yml" \
			--samhsa-cert "$BFD_SAMHSA_CLIENT_CERT" \
			--samhsa-cert-key "$BFD_SAMHSA_CLIENT_KEY" \
			--no-samhsa-cert "$BFD_CLIENT_CERT" \
			--no-samhsa-cert-key "$BFD_CLIENT_KEY" \
			--tablesample "$argc_tablesample" \
			--limit "$argc_limit" \
			--concurrency "$argc_concurrency" \
			"${host_cert[@]}"
	fi
)
