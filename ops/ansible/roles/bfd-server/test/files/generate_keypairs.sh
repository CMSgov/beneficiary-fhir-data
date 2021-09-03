#!/bin/sh

##
# This script was used to generate the keypairs stored in this directory.
##

#serverAlias=server
#serverCommonName=server.example.com
clientAlias=client-foo
clientCommonName=client-foo.example.com

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

# Extract the client certificate to a separate file.
keytool -exportcert -rfc -alias "${clientAlias}" -file client-foo.crt.pem -keystore tmp-keystore.jks -storepass changeit

# Remove the temp keystore.
rm tmp-keystore.jks

