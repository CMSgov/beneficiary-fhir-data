#!/usr/bin/env bash
set -Eeuo pipefail

# @arg 		resource
# @arg		query
# @flag		--samhsa
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/load-server-certs.sh"

if [[ -v argc_samhsa ]]; then
	cert="$BFD_SAMHSA_CLIENT_CERT"
	key="$BFD_SAMHSA_CLIENT_KEY"
else
	cert="$BFD_CLIENT_CERT"
	key="$BFD_CLIENT_KEY"
fi

# shellcheck disable=SC2154 # argc variables are generated externally
curl --compressed --cacert "$BFD_PUBLIC_CERT" --cert "$cert" --key "$key" \
	"https://$BFD_ENV.fhirv3.bfd.cmscloud.local/v3/fhir/$argc_resource?$argc_query" | jq
