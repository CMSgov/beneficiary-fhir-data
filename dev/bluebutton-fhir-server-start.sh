#!/bin/bash

# Constants.
jettyVersion='9.3.11.v20160721'
jettyArtifact="jetty-distribution-${jettyVersion}.tar.gz"
jettyInstall="jetty-distribution-${jettyVersion}"
jettyPortHttps=9094
jettyPortStop=9095
jettyTimeoutSeconds=120
warArtifact='bbonfhir-server-app.war'
configArtifact='bbonfhir-server-app-jetty-config.xml'

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o j:m:d:u:n:p: \
	--long javahome:,maxheaparg:,directory:,dburl:,dbusername:,dbpassword: \
	-n 'bluebutton-fhir-server-start.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
javaBinDir=""
maxHeapArg="-Xmx4g"
directory=
dbUrl="jdbc:hsqldb:mem:test"
dbUsername=""
dbPassword=""
while true; do
	case "$1" in
		-j | --javahome )
			javaBinDir="${2}/bin/"; shift 2 ;;
		-m | --maxheaparg )
			maxHeapArg="$2"; shift 2 ;;
		-d | --directory )
			directory="$2"; shift 2 ;;
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
if [[ -z "${directory}" ]]; then >&2 echo 'The --directory option is required.'; exit 1; fi

# Verify that java was found.
command -v "${javaBinDir}java" >/dev/null 2>&1 || { echo >&2 "Java not found. Specify --javahome option."; exit 1; }

# Exit immediately if something fails.
set -e

# Check for required files.
for f in "${directory}/${jettyArtifact}" "${directory}/${warArtifact}" "${directory}/${configArtifact}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# Unzip Jetty.
if [[ ! -d "${directory}/${jettyInstall}" ]]; then
	tar --extract \
		--file "${directory}/${jettyArtifact}" \
		--directory "${directory}"
	echo "Unpacked Jetty dist: '${directory}/${jettyInstall}'"

	# By default, Jetty will enable all sorts of things that we don't need. 
	# Some of them, e.g. HTTP, are giant security holes. So: disable all 
	# the things!
	mv "${directory}/${jettyInstall}/start.ini" "${directory}/${jettyInstall}/start.ini.original"
fi

# Copy the Jetty resources into its directory (required by Jetty).
cp "${directory}/${configArtifact}" "${directory}/${jettyInstall}/etc/bbfhir.xml"
cp "${directory}/bbonfhir-server-app.war" "${directory}/${jettyInstall}"

# Create the Blue Button module for Jetty to load.
cat <<EOF > "${directory}/${jettyInstall}/modules/bbfhir.mod"
#
# A custom Jetty configuration module for the Blue Button API FHIR Server web application.
#

# As much as possible, use existing modules to pull in the libraries we need.
# DO NOT, however, pull in any modules that include XML configuration (watch 
# out for transitive dependencies with this!).
[depend]
ext

[lib]

# From modules/server.mod:
lib/servlet-api-3.1.jar
lib/jetty-schemas-3.1.jar
lib/jetty-http-${jettyVersion}.jar
lib/jetty-server-${jettyVersion}.jar
lib/jetty-xml-${jettyVersion}.jar
lib/jetty-util-${jettyVersion}.jar
lib/jetty-io-${jettyVersion}.jar

# From modules/deploy.mod:
lib/jetty-deploy-${jettyVersion}.jar

# From modules/webapp.mod:
lib/jetty-webapp-${jettyVersion}.jar

# From modules/servlet.mod:
lib/jetty-servlet-${jettyVersion}.jar

# From modules/security.mod:
lib/jetty-security-${jettyVersion}.jar

# From modules/annotations.mod:
lib/jetty-annotations-${jettyVersion}.jar
lib/annotations/*.jar

# From modules/plus.mod:
lib/jetty-plus-${jettyVersion}.jar

# From modules/jndi.mod:
lib/jetty-jndi-${jettyVersion}.jar
lib/jndi/*.jar

[xml]
etc/bbfhir.xml
EOF

# Create a new start.ini for Jetty.
cat <<EOF > "${directory}/${jettyInstall}/start.ini"
#
# A trimmed-down start.ini, which only enables this app's actual 
# dependencies.
#
--module=bbfhir

# Set the Jetty configuration properties.
jetty.ssl.port=${jettyPortHttps}
jetty.sslContext.keyStorePath=${scriptDirectory}/ssl-stores/server.keystore
jetty.sslContext.trustStorePath=${scriptDirectory}/ssl-stores/server.truststore
bbfhir.war.path=bbonfhir-server-app.war
EOF

# Launch Jetty in the background.
cd "${directory}/${jettyInstall}"
jettyLog='logs/jetty-console.log'
jobs &> /dev/null
"${javaBinDir}java" \
	"${maxHeapArg}" \
	-DSTOP.PORT=${jettyPortStop} \
	-DSTOP.KEY='supersecure' \
	-Dbbfhir.db.url="${dbUrl}" \
	-Dbbfhir.db.username="${dbUsername}" \
	-Dbbfhir.db.password="${dbPassword}" \
	-jar start.jar \
	&> "${jettyLog}" \
	&

# Calculate Jetty's PID. (Reference: http://unix.stackexchange.com/a/90250)
newJobStarted="$(jobs -n)"
if [ -n "${newJobStarted}" ]; then jettyPid=$!; else jettyPid=; fi
if [[ -z "${jettyPid}" ]]; then
	>&2 echo 'Server launch failed. Exiting.'
	exit 2
fi

# Wait for the "...INFO:oejs.Server:main: Started @..." in the log.
echo "Server launched with PID '${jettyPid}', logging to '${jettyLog}'. Waiting for it to finish starting..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $jettyTimeoutSeconds))
while true; do
	if grep --quiet "INFO:oejs.Server:main: Started @" "${jettyLog}"; then
		echo "Server started in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to start within ${jettyTimeoutSeconds} seconds."
		kill "${jettyPid}"
		>&2 echo "Server with PID '${jettyPid}' killed. Exiting."
		exit 3
	fi
	sleep 1
done

