#!/usr/bin/env bash

# Allowed values: prod, prod-sbx, test
env="${env:-test}";
SSM_PATH="/bfd/${env}/server/client_certs"

# name of the PKCS12 truststore and assoc'd pswd
truststore_name="bluebutton-appserver-truststore.pfx"
truststore_pswd="changeit"
rm -f ./$truststore_name
rm -f ./*.pem

for key in $(aws ssm get-parameters-by-path --path "${SSM_PATH}" --region=us-east-1 --query 'Parameters[].Name' --output text)
do \
base_fn="$(echo "${key}" |sed 's/.*\///')";
pem_fn="${base_fn}.pem";
echo "extracting SSM param key: ${key} to file: ${pem_fn}";
aws ssm get-parameter --name "${key}" |jq -r ".Parameter.Value" > "$pem_fn";
keytool -import -alias "${pem_fn}" -noprompt -file "${pem_fn}" -keystore ${truststore_name} -storetype PKCS12 -storepass ${truststore_pswd}
rm -f ./"${pem_fn}"
done

echo "PKCS12 truststore $truststore_name has been created and certificates added."
openssl pkcs12 -in "${truststore_name}" -passin "pass:${truststore_pswd}" -info