#!/bin/bash

##
# This script can be used to configure the JBoss EAP 7 instances used in the
# HealthAPT AWS environments to host the Blue Button API WAR.
##

# Constants.
serverTimeoutSeconds=120

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o h:s:k:t:w:u:n:p: \
	--long serverhome:,httpsport:,keystore:,truststore:,war:,dburl:,dbusername:,dbpassword: \
	-n 'bbonfhir-server-app-server-config-healthapt.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
serverHome=
httpsPort=
keyStore=
trustStore=
war=
dbUrl=
dbUsername=""
dbPassword=""
while true; do
	case "$1" in
		-h | --serverhome )
			serverHome="$2"; shift 2 ;;
		-s | --httpsport )
			httpsPort="$2"; shift 2 ;;
		-k | --keystore )
			keyStore="$2"; shift 2 ;;
		-t | --truststore )
			trustStore="$2"; shift 2 ;;
		-w | --war )
			war="$2"; shift 2 ;;
		-u | --dburl )
			dbUrl="$2"; shift 2 ;;
		-n | --dbusername )
			dbUsername="$2"; shift 2 ;;
		-p | --dbpassword )
			dbPassword="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${serverHome}" ]]; then >&2 echo 'The --serverhome option is required.'; exit 1; fi
if [[ -z "${httpsPort}" ]]; then >&2 echo 'The --httpsport option is required.'; exit 1; fi
if [[ -z "${keyStore}" ]]; then >&2 echo 'The --keystore option is required.'; exit 1; fi
if [[ -z "${trustStore}" ]]; then >&2 echo 'The --truststore option is required.'; exit 1; fi
if [[ -z "${war}" ]]; then >&2 echo 'The --war option is required.'; exit 1; fi
if [[ -z "${dbUrl}" ]]; then >&2 echo 'The --dburl option is required.'; exit 1; fi

# Exit immediately if something fails.
error() {
	local parent_lineno="$1"
	local message="$2"
	local code="${3:-1}"

	if [[ -n "$message" ]] ; then
		>&2 echo "Error on or near line ${parent_lineno}: ${message}; exiting with status ${code}"
	else
		>&2 echo "Error on or near line ${parent_lineno}; exiting with status ${code}"
	fi

	exit "${code}"
}
trap 'error ${LINENO}' ERR

# Check for required files.
for f in "${serverHome}/bin/jboss-cli.sh" "${keyStore}" "${trustStore}" "${war}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# Use the Wildfly CLI to configure the server.
# (Note: This interesting use of heredocs is documented here: http://unix.stackexchange.com/a/168434)
echo "Configuring server..."
cat <<EOF |
# Set the Java system properties that are required to configure the FHIR server.
if (outcome == success) of /system-property=bbfhir.db.url:read-resource
	/system-property=bbfhir.db.url:remove
end-if
if (outcome == success) of /system-property=bbfhir.db.username:read-resource
	/system-property=bbfhir.db.username:remove
end-if
if (outcome == success) of /system-property=bbfhir.db.password:read-resource
	/system-property=bbfhir.db.password:remove
end-if
/system-property=bbfhir.db.url:add(value="${dbUrl}")
/system-property=bbfhir.db.username:add(value="${dbUsername}")
/system-property=bbfhir.db.password:add(value="${dbPassword}")

# Enable and configure HTTPS.
if (outcome != success) of /subsystem=undertow/server=default-server/https-listener=https:read-resource
	/subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https,security-realm=ApplicationRealm)
end-if
/socket-binding-group=standard-sockets/socket-binding=https/:write-attribute(name=port,value="${httpsPort}")
if (outcome == success) of /core-service=management/security-realm=ApplicationRealm/server-identity=ssl:read-resource
	/core-service=management/security-realm=ApplicationRealm/server-identity=ssl:remove
end-if
/core-service=management/security-realm=ApplicationRealm/server-identity=ssl:add(keystore-path="${keyStore}",keystore-password="changeit",key-password="changeit")

# Configure and enable mandatory client-auth SSL.
if (outcome == success) of /core-service=management/security-realm=ApplicationRealm/authentication=truststore:read-resource
	/core-service=management/security-realm=ApplicationRealm/authentication=truststore:remove
end-if
/core-service=management/security-realm=ApplicationRealm/authentication=truststore:add(keystore-path="${trustStore}",keystore-password=changeit)
/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=verify-client,value=REQUIRED)

# Disable HTTP.
if (outcome == success) of /subsystem=undertow/server=default-server/http-listener=default:read-resource
	/subsystem=undertow/server=default-server/http-listener=default:remove()
end-if
/subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref,value=https)

# This data source was initially configured on the servers, but the app doesn't use it.
if (outcome == success) of /subsystem=datasources/data-source=fhirds:read-resource
	/subsystem=datasources/data-source=fhirds:remove
end-if

# Reload the server to apply those changes.
:reload
EOF
"${serverHome}/bin/jboss-cli.sh" \
	--connect \
	--file=/dev/stdin \
	&> "${serverHome}/server-config.log"

# Wait for the server to be ready again.
echo "Server configured. Waiting for it to finish reloading..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverTimeoutSeconds))
while true; do
	if "${serverHome}/bin/jboss-cli.sh" --connect --command=":read-attribute(name=server-state)" | grep --quiet "\"result\" => \"running\""; then
		echo "Server reloaded in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		error ${LINENO} "Error: Server failed to reload within ${serverTimeoutSeconds} seconds." 3
	fi
	sleep 1
done

# Deploy the application to the now-configured server.
echo "Deploying application: '${war}'..."
"${serverHome}/bin/jboss-cli.sh" \
	--connect \
	"deploy ${war} --name=ROOT.war --force" \
	&>> "${serverHome}/server-config.log"
# Note: No need to do extra waiting/watching here, as the command blocks until 
# deployment is completed, and returns a non-zero exit code if it fails.
echo 'Application deployed.'

