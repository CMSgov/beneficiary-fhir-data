#!/bin/sh

##
# This script was used to generate the test client keypair stored in this directory.
##

#serverAlias=server
#serverCommonName=server.example.com
clientAlias=client-test
clientCommonName=client-test

# Generate the server keypair into a temp keystore.
#keytool -genkeypair -alias "${serverAlias}" -keyalg RSA -keysize 4096 -dname cn="${serverCommonName}" -validity 3650 -keypass changeit -keystore tmp-keystore.jks -storepass changeit

# Export the server keypair to the standard PKCS12 format, then extract the cert and key from there.
#keytool -importkeystore -srckeystore tmp-keystore.jks -srcstorepass changeit -srcalias "${serverAlias}" -destkeystore tmp-keystore.p12 -deststorepass changeit -deststoretype PKCS12 -destkeypass changeit -noprompt
#openssl pkcs12 -in tmp-keystore.p12 -passin pass:changeit -nokeys -out server.crt.pem
#openssl pkcs12 -in tmp-keystore.p12 -passin pass:changeit -nodes -nocerts -out server.key.pem
#chmod u=rw,g=,o= server.key.pem
#rm tmp-keystore.p12

# Generate the client keypair into a temp keystore.
keytool -genkeypair -alias "${clientAlias}" -keyalg RSA -keysize 4096 -dname cn="${clientCommonName}" -validity 3650 -keypass changeit -keystore tmp-keystore.jks -storepass changeit

# Extract the combined certificate and private key to a standard unencrypted PEM file.
keytool -importkeystore -srckeystore tmp-keystore.jks -destkeystore "${clientAlias}.p12" -deststoretype PKCS12 -srcstorepass changeit -deststorepass changeit -srcAlias "${clientAlias}" -destalias "${clientAlias}" -destkeypass changeit -noprompt
keytool -exportcert -rfc -alias "${clientAlias}" -file "${clientAlias}-certificate.pem" -keystore tmp-keystore.jks -storepass changeit
openssl pkcs12 -in "${clientAlias}.p12" -passin "pass:changeit" -nodes -out "${clientAlias}-keypair.pem"

# Remove the temp keystores.
rm "${clientAlias}.p12"
rm tmp-keystore.jks

