#!/bin/bash
set -e

# Generate a keystore and a public key pem file
#
function gen_keystore {
    rm -f "$3-bluebutton-appserver-keystore.jks"
    rm -f "$3-bluebutton-appserver-public-cert.pem"

    echo "Generating $3 keystore..."
    keytool -genkeypair -alias server -keyalg RSA -keysize 4096 -dname "$1" \
        -ext "$2" \
        -validity 730 -keypass changeit -keystore "$3-bluebutton-appserver-keystore.jks" -storepass changeit

    # This is definitely silly since this script is rather old and outdated, but I've opted to
    # include the command to convert the JKS keystores just in case someone decides to run this
    # again
    echo "Converting JKS keystore to PKCS12..."
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
}

# Prod
gen_keystore "cn=bfd.cms.gov" \
    "san=dns:prod.bfdcloud.net,dns:mct.prod.bfdcloud.net,dns:dpc.prod.bfdcloud.net,dns:bcda.prod.bfdcloud.net,dns:bb.prod.bfdcloud.net,dns:internal-pdcw10lb01-1951212262.us-east-1.elb.amazonaws.com,dns:bfd.cms.gov,dns:prod.bfd.cms.gov" \
    "prod"

# Prod-SBX
gen_keystore "cn=prod-sbx.bfd.cms.gov" \
    "san=dns:prod-sbx.bfdcloud.net,dns:mct.prod-sbx.bfdcloud.net,dns:dpc.prod-sbx.bfdcloud.net,dns:bcda.prod-sbx.bfdcloud.net,dns:bb.prod-sbx.bfdcloud.net,dns:internal-dpcwelb01-2074070868.us-east-1.elb.amazonaws.com,dns:prod-sbx.bfd.cms.gov,dns:fhir.backend.bluebutton.hhsdevcloud.us" \
    "prod-sbx"

# Test
gen_keystore "cn=test.bfd.cms.gov" \
    "san=dns:test.bfdcloud.net,dns:internal-tsbb10lb01-758855236.us-east-1.elb.amazonaws.com,dns:test.bfd.cms.gov" \
    "test"

# Encrypt
echo "About to encrypt using ansible vault..."
ansible-vault encrypt --ask-vault-pass prod-bluebutton-appserver-keystore.pfx prod-sbx-bluebutton-appserver-keystore.pfx test-bluebutton-appserver-keystore.pfx
