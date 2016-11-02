#!/bin/sh

##
# Generates the keypairs, keystores, and truststores for the Blue Button API 
# server and all clients.
# 
# 
# Make sure the constants at the top of this file are correct for your 
# environment and then run this scripts as follows:
#
#     $ /u01/bin/regenerate-certs-and-stores.sh
##

# Stop execution for any errors.
set -e

keyAlg='RSA'
keySize='4096'
validityDays=$((365 * 3))

envName='dev'
jbossDirectory='/u01/jboss/jboss-eap-7.0'
jbossSslDirectory="${jbossDirectory}/standalone/configuration"

serverFileKeystore="${jbossSslDirectory}/server-${envName}-keystore.jks"
serverFileTruststore="${jbossSslDirectory}/server-${envName}-truststore.jks"
serverFileCertPublic="${jbossSslDirectory}/server-${envName}-public.cer"
serverAlias='server'
serverPass='changeit'
serverDnsExternal='host1.external.example.com'
serverDnsInternal='host1.internal.example.com'
serverDnsElb='loadbalancer.aws.example.com'

clientEtlFileKeystore="${jbossSslDirectory}/client-${envName}-etl-keystore.jks"
clientEtlFileTruststore="${jbossSslDirectory}/client-${envName}-etl-truststore.jks"
clientEtlFileCertPublic="${jbossSslDirectory}/client-${envName}-etl-public.cer"
clientEtlAlias="client-${envName}-etl"
clientEtlPass='changeit'
clientEtlDnsInternal='host2.internal.example.com'

clientDemoFileKeystore="${jbossSslDirectory}/client-${envName}-demo-keystore.jks"
clientDemoFileCertPublic="${jbossSslDirectory}/client-${envName}-demo-public.cer"
clientDemoFilePfx="${jbossSslDirectory}/client-${envName}-demo.pfx"
clientDemoAlias="client-${envName}-demo"
clientDemoPass='changeit'
clientDemoDnsInternal='host3.internal.example.com'

#if [[ -f "${serverFileKeystore}" ]] || [[ -f "${serverFileCsr}" ]] || [[ -f "${clientEtlFileKeystore}" ]] || [[ -f "${clientEtlFileCsr}" ]]; then
#	>&2 echo "Error: files already exist!"
#	exit 1
#fi

rm -f "${serverFileKeystore}" "${serverFileCertPublic}" "${serverFileTruststore}" \
	"${clientEtlFileKeystore}" "${clientEtlFileCertPublic}" "${clientEtlFileTruststore}" \
	"${clientDemoFileKeystore}" "${clientDemoFileCertPublic}" "${clientDemoFileTruststore}"

keytool -genkeypair -alias "${serverAlias}" -keyalg "${keyAlg}" -keysize "${keySize}" -dname "cn=${serverDnsExternal}" -ext "san=dns:${serverDnsInternal},dns:${serverDnsElb}" -validity "${validityDays}" -keypass "${serverPass}" -keystore "${serverFileKeystore}" -storepass "${serverPass}"
keytool -exportcert -alias "${serverAlias}" -file "${serverFileCertPublic}" -keystore "${serverFileKeystore}" -storepass "${serverPass}"
keytool -importcert -noprompt -alias "${serverAlias}" -file "${serverFileCertPublic}" -keystore "${clientEtlFileTruststore}" -storepass "${serverPass}"

keytool -genkeypair -alias "${clientEtlAlias}" -keyalg "${keyAlg}" -keysize "${keySize}" -dname "cn=${clientEtlDnsInternal}" -validity "${validityDays}" -keypass "${clientEtlPass}" -keystore "${clientEtlFileKeystore}" -storepass "${clientEtlPass}"
keytool -exportcert -alias "${clientEtlAlias}" -file "${clientEtlFileCertPublic}" -keystore "${clientEtlFileKeystore}" -storepass "${serverPass}"
keytool -importcert -noprompt -alias "${clientEtlAlias}" -file "${clientEtlFileCertPublic}" -keystore "${serverFileTruststore}" -storepass "${serverPass}"

# This block should be commented out most of the time, unless a demo cert is actually needed.
#keytool -genkeypair -alias "${clientDemoAlias}" -keyalg "${keyAlg}" -keysize "${keySize}" -dname "cn=${clientDemoDnsInternal}" -validity "${validityDays}" -keypass "${clientDemoPass}" -keystore "${clientDemoFileKeystore}" -storepass "${clientDemoPass}"
#keytool -exportcert -alias "${clientDemoAlias}" -file "${clientDemoFileCertPublic}" -keystore "${clientDemoFileKeystore}" -storepass "${serverPass}"
#keytool -importcert -noprompt -alias "${clientDemoAlias}" -file "${clientDemoFileCertPublic}" -keystore "${serverFileTruststore}" -storepass "${serverPass}"
#keytool -importkeystore -srckeystore "${clientDemoFileKeystore}" -destKeystore "${clientDemoFilePfx}" -deststoretype PKCS12 -srcstorepass "${clientDemoPass}" -deststorepass "${clientDemoPass}" -srcalias "${clientDemoAlias}"

