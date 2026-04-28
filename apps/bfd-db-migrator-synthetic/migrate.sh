#!/usr/bin/env bash

set -Eeou pipefail

export TZ=UTC

SCRIPT_DIR="$(path=$(realpath "$0") && dirname "$path")"
readonly SCRIPT_DIR

IDR_USERNAME="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_username --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_USERNAME
export IDR_USERNAME
IDR_PRIVATE_KEY="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_private_key --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_PRIVATE_KEY
export IDR_PRIVATE_KEY
IDR_ACCOUNT="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_account --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_ACCOUNT
export IDR_ACCOUNT
IDR_WAREHOUSE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_warehouse --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_WAREHOUSE
export IDR_WAREHOUSE
IDR_DATABASE="$(aws ssm get-parameter --name /bfd/${BFD_ENV}/idr-pipeline/sensitive/synthetic_env_database --with-decryption --query "Parameter.Value" --output text)"
readonly IDR_DATABASE
export IDR_DATABASE

TEMP_KEY_FILE="${TMPDIR:-'/tmp'}/idr_private.p8"

echo "${IDR_PRIVATE_KEY}" > "${TEMP_KEY_FILE}"
chmod 600 "${TEMP_KEY_FILE}"

(
    cd "$SCRIPT_DIR"
    mvn flyway:migrate \
        "-Dflyway.url=jdbc:snowflake://$IDR_ACCOUNT.snowflakecomputing.com/?db=${IDR_DATABASE}&warehouse=${IDR_WAREHOUSE}&role=SERVICE_USER&JDBC_QUERY_RESULT_FORMAT=JSON&authenticator=snowflake_jwt&private_key_file=${TEMP_KEY_FILE}" \
        "-Dflyway.user=$IDR_USERNAME" \
        "-Dflyway.callbackLocations=filesystem:$SCRIPT_DIR/callbacks"
) || rm "${TEMP_KEY_FILE}" # ensure private key gets deleted on failure

rm "${TEMP_KEY_FILE}"
