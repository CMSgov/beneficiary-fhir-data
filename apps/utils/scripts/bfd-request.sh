#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/load-server-certs.sh"
curl --compressed --cacert "$BFD_PUBLIC_CERT" --cert "$BFD_CLIENT_CERT" --key "$BFD_CLIENT_KEY" \
	"https://$BFD_ENV.fhirv3.bfd.cmscloud.local/v3/fhir/$1?$2" | jq
