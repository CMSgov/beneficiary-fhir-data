#!/bin/bash

# Constants.
serverReadyTimeoutSeconds=120
serverConnectTimeoutMilliseconds=$((30 * 1000))

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o h:s:k:t:w:u:n:p: \
	--long serverhome:,httpsport:,keystore:,truststore:,war:,dburl:,dbusername:,dbpassword: \
	-n 'bbonfir-server-app-server-config.sh' -- "$@"`
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

# Exit immediately if something fails.
set -e

# Check for required files.
for f in "${serverHome}/bin/jboss-cli.sh" "${keyStore}" "${trustStore}" "${war}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# Use the Wildfly CLI to configure the server.
# (Note: This interesting use of heredocs is documented here: http://unix.stackexchange.com/a/168434)
cat <<EOF |
# Apply all of the configuration in a single transaction.
batch

# Set the Java system properties that are required to configure the FHIR server.
/system-property=bbfhir.db.url:add(value="${dbUrl}")
/system-property=bbfhir.db.username:add(value="${dbUsername}")
/system-property=bbfhir.db.password:add(value="${dbPassword}")

# Enable and configure HTTPS.
/subsystem=undertow/server=default-server/https-listener=https/:add(socket-binding=https,security-realm=ApplicationRealm)
/socket-binding-group=standard-sockets/socket-binding=https/:write-attribute(name=port,value="${httpsPort}")
/core-service=management/security-realm=ApplicationRealm/server-identity=ssl/:add(keystore-path="${keyStore}",keystore-password="changeit",key-password="changeit")

# Configure and enable mandatory client-auth SSL.
/core-service=management/security-realm=ApplicationRealm/authentication=truststore:add(keystore-path="${trustStore}",keystore-password=changeit)
/subsystem=undertow/server=default-server/https-listener=https/:write-attribute(name=verify-client,value=REQUIRED)

# Disable HTTP.
/subsystem=undertow/server=default-server/http-listener=default/:remove()
/subsystem=remoting/http-connector=http-remoting-connector/:write-attribute(name=connector-ref,value=https)

# Commit the configuration transaction.
run-batch

# Reload the server to apply those changes.
:reload
EOF
"${serverHome}/bin/jboss-cli.sh" \
	--connect \
	--timeout=${serverConnectTimeoutMilliseconds} \
	--file=/dev/stdin \
	&> "${serverHome}/server-config.log"

# Wait for the server to be ready again.
echo "Server configured. Waiting for it to finish reloading..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverReadyTimeoutSeconds))
while true; do
	if "${serverHome}/bin/jboss-cli.sh" --connect --command="ls" &> /dev/null; then
		echo "Server reloaded in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to reload within ${serverReadyTimeoutSeconds} seconds. Trying to stop it..."
		"${scriptDirectory}/bluebutton-fhir-server-stop.sh" --directory "${directory}"
		exit 3
	fi
	sleep 1
done

# Deploy the application to the now-configured server.
echo "Deploying application: '${war}'..."
"${serverHome}/bin/jboss-cli.sh" \
	--connect \
	--timeout=${serverConnectTimeoutMilliseconds} \
	"deploy ${war} --name=ROOT.war" \
	&>> "${serverHome}/server-config.log"
# Note: No need to watch log here, as the command blocks until deployment is 
# completed, and returns a non-zero exit code if it fails.
echo 'Application deployed.'

