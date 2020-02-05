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

    echo "Extracting public cert..."
    keytool -export -keystore "$3-bluebutton-appserver-keystore.jks" -storetype jks -alias server -storepass changeit -file "$3-bluebutton-appserver-public-cert.pem" -rfc 
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
ansible-vault encrypt --ask-vault-pass prod-bluebutton-appserver-keystore.jks prod-sbx-bluebutton-appserver-keystore.jks test-bluebutton-appserver-keystore.jks
