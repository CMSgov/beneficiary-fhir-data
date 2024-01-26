#!/bin/bash
set -e

# verify base64 encoded PKCS#12 file
#
function verify_encoded_p12 {
    rm -f foo.p12
    cat "$1" |base64 --decode > foo.p12
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
    rm -f "$3-bluebutton-appserver-keystore.jks"
    rm -f "$3-bluebutton-appserver-keystore.pfx"
    rm -f "$3-bluebutton-appserver-keystore.pfx.b64"
    rm -f "$3-bluebutton-appserver-public-cert.pem"

    echo "Generating $3 keystore..."
    keytool -genkeypair -alias server -keyalg RSA -keysize 4096 -dname "$1" \
        -ext "$2" \
        -validity 730 -keypass changeit -keystore "$3-bluebutton-appserver-keystore.jks" -storepass changeit

    # This is definitely silly since this script is rather old and outdated, but I've opted to
    # include the command to convert the JKS keystores just in case someone decides to run this
    # again
    echo "Converting JKS keystore to PKCS#12..."
    keytool -importkeystore \
     -srckeystore "$3-bluebutton-appserver-keystore.jks" \
     -destkeystore "$3-bluebutton-appserver-keystore.pfx" \
     -srcstoretype jks \
     -deststoretype pkcs12 \
     -srcstorepass changeit \
     -deststorepass changeit \
     -srcalias server \
     -destalias server \
     -srckeypass changeit \
     -destkeypass changeit \
     -noprompt

    echo "Removing intermediary JKS keystore..."
    rm -f "$3-bluebutton-appserver-keystore.jks"

    echo "Extracting public cert..."
    keytool -export -keystore "$3-bluebutton-appserver-keystore.pfx" -alias server -storepass changeit -file "$3-bluebutton-appserver-public-cert.pem" -rfc
    
    echo "creating base64 encoded version of PKCS#12 file..."
    cat "$3-bluebutton-appserver-keystore.pfx" | base64 > "$3-bluebutton-appserver-keystore.pfx.b64"

    verify_encoded_p12 "$3-bluebutton-appserver-keystore.pfx.b64"
}

# Prod
gen_keystore "cn=bfd.cms.gov" \
    "san=dns:bfd.cms.gov,dns:prod.bfd.cms.gov" \
    "prod"

# Prod-SBX
gen_keystore "cn=prod-sbx.bfd.cms.gov" \
    "san=dns:prod-sbx.bfd.cms.gov" \
    "prod-sbx"

# Test
gen_keystore "cn=test.bfd.cms.gov" \
    "san=dns:test.bfd.cms.gov" \
    "test"

# Update SSM params in prod.yaml - bfd/prod/server/server_keystore_base64
# cd ops/terraform/env/mgmt/base_config
# ./scripts/edit-yaml.sh prod
