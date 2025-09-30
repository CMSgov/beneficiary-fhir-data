#!/bin/bash
#######################################
# This script generates a Java truststore file from a set of certificates provided as a JSON string
# and saves it at the specified path.
#
# This is intended for use in terraform as the "local-exec" provisioner of a "null_resource"
# resource.
#
# Requires jq and the JDK (for keytool)
#
# Arguments:
#   $1 maps to the path to the truststore file to be generated.
#   $2 maps to a JSON string containing certificate data in the following format:
#     {
#       "alias": "<certificate alias>",
#       "pubkey": "<base64 encoded public key>"
#     }
#   Each certificate in the JSON string will be added to the truststore with its corresponding alias.
#######################################

set -Eeou pipefail

truststore_path="$1"
readonly truststore_path

certs_json="$2"
readonly certs_json

if [[ "$(jq '(. | length >= 0) and (.[0] | type == "object") and all(. | has("alias") and has("pubkey"))' <<<"$certs_json")" != "true" ]]; then
  echo "Invalid JSON format for certificates"
  exit 1
fi

mkdir -p "$(dirname "$truststore_path")"

if [ -e "$truststore_path" ]; then
  rm "$truststore_path"
fi

certs_list="$(jq -c '.[]' <<<"$certs_json")"
while read -r cert_data; do
  keytool -import \
    -alias "$(jq -r '.alias' <<<"$cert_data")" \
    -noprompt \
    -keystore "$truststore_path" \
    -storetype PKCS12 \
    -storepass changeit \
    <<<"$(jq -r '.pubkey' <<<"$cert_data")"
done <<<"$certs_list"
