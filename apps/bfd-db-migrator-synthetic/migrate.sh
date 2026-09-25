#!/usr/bin/env bash
set -Eeou pipefail

export TZ=UTC

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

source ./apps/utils/scripts/load-idr-credentials.sh synthetic

TEMP_KEY_FILE="${TMPDIR:-'/tmp'}/idr_private.p8"

echo "${IDR_PRIVATE_KEY}" >"${TEMP_KEY_FILE}"
chmod 600 "${TEMP_KEY_FILE}"

(
	cd "$SCRIPT_DIR"
	mvn flyway:migrate \
		"-Dflyway.url=jdbc:snowflake://$IDR_ACCOUNT.snowflakecomputing.com/?db=${IDR_DATABASE}&warehouse=${IDR_WAREHOUSE}&role=${BFD_ENV}_SERVICE_USER&JDBC_QUERY_RESULT_FORMAT=JSON&authenticator=snowflake_jwt&private_key_file=${TEMP_KEY_FILE}" \
		"-Dflyway.user=$IDR_USERNAME"
) || rm "${TEMP_KEY_FILE}" # ensure private key gets deleted on failure

rm "${TEMP_KEY_FILE}"
