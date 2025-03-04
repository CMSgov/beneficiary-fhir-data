#!/bin/bash

mkdir -p "$(dirname "$TRUSTSTORE_PATH")"

if [ -e "$TRUSTSTORE_PATH" ]; then
  rm "$TRUSTSTORE_PATH"
fi

while read -r cert_data; do
  keytool -import \
    -alias "$(jq -r '.alias' <<<"$cert_data")" \
    -noprompt \
    -keystore "$TRUSTSTORE_PATH" \
    -storetype PKCS12 \
    -storepass changeit \
    <<<"$(jq -r '.pubkey' <<<"$cert_data")"
done < <(jq -c '.[]' <<<"$CERTS_JSON")
