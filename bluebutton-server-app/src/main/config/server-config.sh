#!/bin/bash

# Constants
# Note: The username and password here are wildly insecure, but this is a dev-
# only config, so it's fine.
serverReadyTimeoutSeconds=120
serverConnectTimeoutMilliseconds=$((30 * 1000))
wildflyManagementUsername='developer'
wildflyManagementPassword='notterriblysecure'

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check to see if we are running in Cygwin.
case "$( uname )" in
	CYGWIN*) cygwin=true ;;
	*) cygwin=false ;;
esac

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	h:s:k:t:w:u:n:p: \
	$*`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
serverHome=
httpsPort=
keyStore=
trustStore=
war=
dbUrl="jdbc:hsqldb:mem:test"
dbUsername=""
dbPassword=""
while true; do
	case "$1" in
		-h )
			serverHome="$2"; shift 2 ;;
		-s )
			httpsPort="$2"; shift 2 ;;
		-k )
			keyStore="$2"; shift 2 ;;
		-t )
			trustStore="$2"; shift 2 ;;
		-w )
			war="$2"; shift 2 ;;
		-u )
			dbUrl="$2"; shift 2 ;;
		-n )
			dbUsername="$2"; shift 2 ;;
		-p )
			dbPassword="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

#echo "serverHome: '${serverHome}', httpsPort: '${httpsPort}', keyStore: '${keyStore}', trustStore: '${trustStore}', war: '${war}', dbUrl: '${dbUrl}', dbUsername: '${dbUsername}', dbPassword: '${dbPassword}'"

# Verify that all required options were specified.
if [[ -z "${serverHome}" ]]; then >&2 echo 'The -h option is required.'; exit 1; fi
if [[ -z "${httpsPort}" ]]; then >&2 echo 'The -s option is required.'; exit 1; fi
if [[ -z "${keyStore}" ]]; then >&2 echo 'The -k option is required.'; exit 1; fi
if [[ -z "${trustStore}" ]]; then >&2 echo 'The -t option is required.'; exit 1; fi
if [[ -z "${war}" ]]; then >&2 echo 'The -w option is required.'; exit 1; fi

# Exit immediately if something fails.
error() {
	local parent_lineno="$1"
	local message="$2"
	local code="${3:-1}"

	if [[ -n "$message" ]] ; then
		>&2 echo "Error on or near line ${parent_lineno} of file `basename $0`: ${message}."
	else
		>&2 echo "Error on or near line ${parent_lineno} of file `basename $0`."
	fi
	
	# Before bailing, always try to stop any running servers.
	>&2 echo "Trying to stop any running servers before exiting..."
	"${scriptDirectory}/bluebutton-server-app-server-stop.sh" -d "${directory}"

	>&2 echo "Exiting with status ${code}."
	exit "${code}"
}
trap 'error ${LINENO}' ERR

# Munge paths for Cygwin.
if [[ "${cygwin}" = true ]]; then keyStore=$(cygpath --windows "${keyStore}"); fi
if [[ "${cygwin}" = true ]]; then trustStore=$(cygpath --windows "${trustStore}"); fi
if [[ "${cygwin}" = true ]]; then war=$(cygpath --windows "${war}"); fi

# Check for required files.
for f in "${serverHome}/bin/jboss-cli.sh" "${keyStore}" "${trustStore}" "${war}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# Use Wildfly's `add-user.sh` utility to add a management user. This enables 
# use of JMX with the Wildfly instance. Note that Wildfly/JBoss require the use 
# of a custom authentication plugin with jConsole. See 
# https://dzone.com/articles/remote-jmx-access-wildfly-or for details, but the 
# upshot is that the Wildfly-provided `bin/jconsole.sh` script must be used and 
# the connection URL (even for local processes) must be of the form 
# `service:jmx:http-remoting-jmx://localhost:9990`.
"${serverHome}/bin/add-user.sh" \
	--silent \
	"${wildflyManagementUsername}" \
	"${wildflyManagementPassword}"

# Write the Wildfly CLI config script that will be used to configure the server.
scriptConfig="${serverHome}/jboss-cli-script-config.txt"
if [[ "${cygwin}" = true ]]; then scriptConfigArg=$(cygpath --windows "${scriptConfig}"); else scriptConfigArg="${scriptConfig}"; fi
cat <<EOF > "${scriptConfig}"
# Apply all of the configuration in a single transaction.
batch

# Set applications to use SLF4J for the logging, rather than JBoss' builtin 
# logger. See jboss-deployment-structure.xml for details.
/system-property=org.jboss.logging.provider:add(value="slf4j")

# Set the Java system properties that are required to configure the FHIR server.
/system-property=bbfhir.db.url:add(value="${dbUrl}")
/system-property=bbfhir.db.username:add(value="${dbUsername}")
/system-property=bbfhir.db.password:add(value="${dbPassword}")

# Enable and configure HTTPS.
/subsystem=undertow/server=default-server/https-listener=https/:add(socket-binding=https,security-realm=ApplicationRealm)
/socket-binding-group=standard-sockets/socket-binding=https/:write-attribute(name=port,value="${httpsPort}")
/core-service=management/security-realm=ApplicationRealm/server-identity=ssl/:add(keystore-path="${keyStore//\\//}",keystore-password="changeit",key-password="changeit")

# Per the recommendations on https://wiki.mozilla.org/Security/Server_Side_TLS,
# we only support TLS v1.2 and the cipher suites listed below. The cipher
# suites have two names: their names from their specification and their names
# in OpenSSL. JBoss/Wildfly require the spec names, though most docs use the
# OpenSSL names. The OpenSSL docs contain a mapping of the names:
# https://www.openssl.org/docs/man1.1.0/apps/ciphers.html.
#
# +===============================================+===============================+
# | Specification Name                            | OpenSSL Name                  |
# +===============================================|===============================+
# | TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384       | ECDHE-ECDSA-AES256-GCM-SHA384 |
# | TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384         | ECDHE-RSA-AES256-GCM-SHA384   |
# | TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256 | ECDHE-ECDSA-CHACHA20-POLY1305 |
# | TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256   | ECDHE-RSA-CHACHA20-POLY1305   |
# | TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256       | ECDHE-ECDSA-AES128-GCM-SHA256 |
# | TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256         | ECDHE-RSA-AES128-GCM-SHA256   |
# | TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384       | ECDHE-ECDSA-AES256-SHA384     |
# | TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384         | ECDHE-RSA-AES256-SHA384       |
# | TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256       | ECDHE-ECDSA-AES128-SHA256     |
# | TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256         | ECDHE-RSA-AES128-SHA256       |
# +-----------------------------------------------+-------------------------------+
#
# Note: Turns out that, for some unknown reason, the ChaCha20 algorithms aren't
# supported in our HealthAPT AWS environments, which use JBoss EAP 7.0 (JBoss
# fails to start if they're enabled, with an error). So we exclude them from
# `enabled-cipher-suites`.
/subsystem=undertow/server=default-server/https-listener=https/:write-attribute(name=enabled-protocols,value="TLSv1.2")
/subsystem=undertow/server=default-server/https-listener=https/:write-attribute(name=enabled-cipher-suites,value="TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256")

# Configure and enable mandatory client-auth SSL.
/core-service=management/security-realm=ApplicationRealm/authentication=truststore:add(keystore-path="${trustStore//\\//}",keystore-password=changeit)
/subsystem=undertow/server=default-server/https-listener=https/:write-attribute(name=verify-client,value=REQUIRED)

# Disable HTTP.
/subsystem=undertow/server=default-server/http-listener=default/:remove()
/subsystem=remoting/http-connector=http-remoting-connector/:write-attribute(name=connector-ref,value=https)

# Enable and configure HTTP access logging.
# 
# References:
# * https://kb.novaordis.com/index.php/Undertow_WildFly_Subsystem_Configuration_-_access-log
# * https://stackoverflow.com/questions/34614874/wildfly-9-access-logging
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=record-request-start-time,value=true)
/subsystem=undertow/server=default-server/host=default-host/setting=access-log:add(pattern="%h %l %u %t \\"%r\\" \\"?%q\\" %s %B %D %{i,BlueButton-OriginalQueryId} %{i,BlueButton-OriginalQueryCounter} [%{i,BlueButton-OriginalQueryTimestamp}] %{i,BlueButton-DeveloperId} \\"%{i,BlueButton-Developer}\\" %{i,BlueButton-ApplicationId} \\"%{i,BlueButton-Application}\\" %{i,BlueButton-UserId} \\"%{i,BlueButton-User}\\" %{i,BlueButton-BeneficiaryId}", directory="\${jboss.server.log.dir}", prefix="access", suffix=".log")

# Commit the configuration transaction.
run-batch

# Reload the server to apply those changes.
:reload
EOF

# Calls the JBoss/Wildfly CLI with the specified arguments.
jBossCli ()
{
	if [[ "${cygwin}" = true ]]; then
		cliApp="${serverHome}/bin/jboss-cli.bat"
		chmod a+x "${cliApp}"
		
		cmd /C "set NOPAUSE=true && $(cygpath --windows ${cliApp}) $@"
	else
		cliApp="${serverHome}/bin/jboss-cli.sh"
		
		"${cliApp}" $@
	fi
}

# Use the Wildfly CLI to configure the server.
jBossCli \
	--connect \
	--timeout=${serverConnectTimeoutMilliseconds} \
	--file=${scriptConfigArg} \
	&> "${serverHome}/server-config.log"

# Wait for the server to be ready again.
echo "Server configured. Waiting for it to finish reloading..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverReadyTimeoutSeconds))
while true; do
	if jBossCli --connect --command="ls" &> /dev/null; then
		echo "Server reloaded in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to reload within ${serverReadyTimeoutSeconds} seconds. Trying to stop it..."
		"${scriptDirectory}/bluebutton-fhir-server-stop.sh" -d "${directory}"
		exit 3
	fi
	sleep 1
done

# Write the JBoss CLI script that will deploy the WAR.
scriptDeploy="${serverHome}/jboss-cli-script-deploy.txt"
if [[ "${cygwin}" = true ]]; then scriptDeployArg=$(cygpath --windows "${scriptDeploy}"); else scriptDeployArg="${scriptDeploy}"; fi
cat <<EOF > "${scriptDeploy}"
deploy "${war}" --name=ROOT.war
EOF

# Deploy the application to the now-configured server.
echo "Deploying application: '${war}'..."
jBossCli \
	--connect \
	--timeout=${serverConnectTimeoutMilliseconds} \
	--file=${scriptDeployArg} \
	>> "${serverHome}/server-config.log" 2>&1
# Note: No need to watch log here, as the command blocks until deployment is 
# completed, and returns a non-zero exit code if it fails.
echo 'Application deployed.'
