#!/bin/bash
set -e

# verify base64 encoded PKCS#12 file
#
function verify_encoded_p12 {
  rm -f foo.p12
  base64 --decode >foo.p12 <"$1"
  echo " "
  echo "verifying $1 keystore file..."
  openssl pkcs12 -in foo.p12 -password pass:changeit -nodes | openssl x509 -noout -enddate
  rm -f foo.p12
  echo "-------------------------------"
  echo " "
}

# Generate a keystore and a public key pem file
#
function gen_keystore {
  echo "begin processing of $3 environment...."
  rm -f "$3-keystore.pfx"
  rm -f "$3-keystore.pfx.b64"
  rm -f "$3-public-cert.pem"

  echo "Generating $3 keystore..."
  keytool -genkeypair \
    -alias server \
    -keyalg RSA \
    -keysize 4096 \
    -dname "$1" \
    -ext "$2" \
    -validity 730 \
    -keypass changeit \
    -keystore "$3-keystore.pfx" \
    -storepass changeit

  echo "Extracting public cert..."
  keytool -export -keystore "$3-keystore.pfx" -alias server -storepass changeit -file "$3-public-cert.pem" -rfc

  echo "creating base64 encoded version of PKCS#12 file..."
  cat "$3-keystore.pfx" | base64 >"$3-keystore.pfx.b64"

  verify_encoded_p12 "$3-keystore.pfx.b64"
}

# Prod
gen_keystore "cn=prod.fhir.bfd.cmscloud.local" \
  "san=dns:prod.fhir.bfd.cmscloud.local,dns:prod.bfd.cms.gov" \
  "prod"

# Prod-SBX
gen_keystore "cn=prod-sbx.fhir.bfd.cmscloud.local" \
  "san=dns:prod-sbx.fhir.bfd.cmscloud.local,dns:prod-sbx.bfd.cms.gov" \
  "prod-sbx"

# Test
gen_keystore "cn=test.fhir.bfd.cmscloud.local" \
  "san=dns:test.fhir.bfd.cmscloud.local,dns:test.bfd.cms.gov" \
  "test"

echo
echo "REMEMBER: "
echo "* Store the contents of each <env>-keystore.pfx.b64 in SSM configuration under the /bfd/<env>/server/sensitive/server_keystore_base64 path"
echo "* Store the contents of each <env>-public-cert.pem in SSM configuration under the /bfd/<env>/server/sensitive/server_keystore_public_cert path"
