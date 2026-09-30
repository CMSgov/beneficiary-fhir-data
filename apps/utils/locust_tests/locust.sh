#!/usr/bin/env bash
set -Eeou pipefail

# @flag 	--headless
eval "$(argc --argc-eval "$0" "$@")"

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source "$SCRIPT_DIR/../scripts/load-bfd-credentials.sh"
headless=""
if [[ -v argc_headless ]]; then
	headless="--headless"
fi
(
	cd "$SCRIPT_DIR"
	tmpdir="${TMPDIR:-'/tmp'}"
	PGUSER="$BFD_DB_USERNAME" PGPASSWORD="$BFD_DB_PASSWORD" PGHOST="$BFD_DB_ENDPOINT" PGDATABASE=fhirdb \
		uv run locust -f v3/regression_suite.py \
		--client-cert-path="$tmpdir/${BFD_ENV}_combined.pem" \
		--host=https://test.fhirv3.bfd.cmscloud.local \
		$headless
)
