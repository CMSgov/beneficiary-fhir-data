#!/bin/bash

# Check to see if we are running in Cygwin.
uname="$(uname 2>/dev/null)"
if [[ -z "${uname}" ]]; then uname="$(/usr/bin/uname 2>/dev/null)"; fi
if [[ -z "${uname}" ]]; then echo "Unable to find uname." >&2; exit 1; fi
case "${uname}" in
	CYGWIN*) cygwin=true ;;
	*) cygwin=false ;;
esac

# In Cygwin, some of the args will have unescaped backslashes. Fix that.
if [[ "${cygwin}" = true ]]; then
	set -- "${@//\\/\\\\}"
fi

# Makes debugging problems a lot easier if this is always logged.
echo "Server start script is being run as follows:"
if [[ "${cygwin}" = true ]]; then
	echo -e "${0//\\/\\\\} $@\n"
else
	echo -e "$0 $@\n"
fi

# In Cygwin, non-login shells have no path. Fix that.
# (Note: And we can't run this in a login shell, as it won't exit until ALL
# child processes do, even with setsid/disown/etc.)
if [[ "${cygwin}" = true ]]; then
	source /etc/profile
fi

# Constants.
serverTimeoutSeconds=120
dbUsername=""
dbPassword=""

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	j:m:v:t:u:e: \
	$*`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
javaHome=""
maxHeapArg="-Xmx4g"
visualVm=""
targetDirectory=
dbUrl="jdbc:bfd-test:hsqldb:mem"
v2Enabled="true"
preAdjEnabled="true"
while true; do
	case "$1" in
		-j )
			javaHome="$2"; shift 2 ;;
		-m )
			maxHeapArg="$2"; shift 2 ;;
		-v )
			visualVm="$2"; shift 2 ;;
		-t )
			targetDirectory="$2"; shift 2 ;;
		-u )
			dbUrl="$2"; shift 2 ;;
		-e )
			v2Enabled="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# On my system (windows with cygwin) the javaHome variable was being parsed incorrectly. In order to get it to work I just hardcoded it here.
#javaHome="C:\Program Files\Java\jdk1.8.0_181\jre"

# Verify that all required options were specified.
if [[ -z "${targetDirectory}" ]]; then >&2 echo 'The -t option is required.'; exit 1; fi

# In Cygwin, some of those paths will come in as Windows-formatted. Fix that.
if [[ "${cygwin}" = true ]]; then
	if [[ ! -z "${javaHome}" ]]; then javaHome="$(cygpath --unix "${javaHome}")"; fi
	if [[ ! -z "${visualVm}" ]]; then visualVm="$(cygpath --unix ${visualVm})"; fi
	if [[ ! -z "${targetDirectory}" ]]; then targetDirectory="$(cygpath --unix ${targetDirectory})"; fi
fi

# Verify that java was found.
if [[ -z "${javaHome}" ]]; then
	command -v java >/dev/null 2>&1 || { echo >&2 "Java not found. Specify -j option."; exit 1; }
else
	command -v "${javaHome}/bin/java" >/dev/null 2>&1 || { echo >&2 "Java not found in -j: '${javaHome}'"; exit 1; }
fi

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
	"${scriptDirectory}/server-stop.sh" -t "${targetDirectory}"

	>&2 echo "Exiting with status ${code}."
	exit "${code}"
}
trap 'error ${LINENO}' ERR

# Ensure that the working directory is consistent.
cd "${targetDirectory}/.."

# Define all of the derived paths we'll need.
workDirectory="${targetDirectory}/server-work"
serverLauncher="${workDirectory}/$(ls ${workDirectory} | grep '^bfd-server-launcher-.*\.jar$')"
serverPortsFile="${workDirectory}/server-ports.properties"
serverLog="${workDirectory}/server-console.log"
warArtifact="${targetDirectory}/$(ls ${targetDirectory} | grep '^bfd-server-war-.*\.war$')"
keyStore="${scriptDirectory}/../../../../dev/ssl-stores/server-keystore.jks"
trustStore="${scriptDirectory}/../../../../dev/ssl-stores/server-truststore.jks"

# Check for required files.
for f in "${serverLauncher}" "${serverPortsFile}" "${warArtifact}" "${keyStore}" "${trustStore}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# In Cygwin, some of those paths need to be Windows-formatted. Do that.
if [[ "${cygwin}" = true ]]; then warArtifact=$(cygpath --windows "${warArtifact}"); warArtifact="${warArtifact//\\/\\\\}"; fi
if [[ "${cygwin}" = true ]]; then keyStore=$(cygpath --mixed "${keyStore}"); fi
if [[ "${cygwin}" = true ]]; then trustStore=$(cygpath --mixed "${trustStore}"); fi

# Read the server port to be used from the ports file.
serverPortHttps=${BFD_PORT:-$(grep "^server.port.https=" "${serverPortsFile}" | tr -d '\r' | cut -d'=' -f2)}
if [[ -z "${serverPortHttps}" ]]; then >&2 echo "Server HTTPS port not specified in '${serverPortsFile}'."; exit 1; fi
echo "Configured server to run on HTTPS port '${serverPortHttps}'."

# Generate a random server ID and write it to a file.
bfdServerId=$RANDOM
echo -n "${bfdServerId}" > "${workDirectory}/bfd-server-id.txt"

# Build the args to pass to the server for VisualVM (if any).
if [[ -f "${visualVm}/profiler/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so" ]]; then
	echo "Found VisualVM directory: '${visualVm}'"
	visualVmArgs="-agentpath:${visualVm}/profiler/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so=${visualVm}/profiler/lib,5140"
	visualVmArgs="${visualVmArgs} -Dorg.osgi.framework.bootdelegation=org.netbeans.lib.profiler.server,org.netbeans.lib.profiler.server.*"
	visualVmArgs="${visualVmArgs} -Djava.util.logging.manager=org.jboss.logmanager.LogManager" 
	visualVmArgs="${visualVmArgs} -Xbootclasspath/p:${serverHome}/modules/system/layers/base/org/jboss/logmanager/main/jboss-logmanager-1.5.2.Final.jar" 
	jbossModulesSystemPackages="org.netbeans.lib.profiler.server,org.jboss.logmanager"
else
	echo "Warning: VisualVM directory not found: '${visualVm}'"
	visualVmArgs=""
fi

# To enable JVM debugging, uncomment and add this line to the server start command below.
#	-Dcapsule.jvm.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8083" \

# Launch the server in the background.
BFD_PORT="${serverPortHttps}" \
	BFD_KEYSTORE="${keyStore}" \
	BFD_TRUSTSTORE="${trustStore}" \
	BFD_WAR="${warArtifact}" \
	"${javaHome}/bin/java" \
	"${maxHeapArg}" \
	"-Dbfd-server-${bfdServerId}" \
	"-DbfdServer.db.url=${dbUrl}" \
	"-DbfdServer.v2.enabled=${v2Enabled}" \
	"-DbfdServer.preadj.enabled=${preAdjEnabled}" \
	"-DbfdServer.db.username=" \
	"-DbfdServer.db.password=" \
	"-DbfdServer.db.schema.apply=true" \
	-jar "${serverLauncher}" \
	>"${serverLog}" 2>&1 \
	&

# Wait for the server to be ready.
echo "Server launched, logging to '${serverLog}'. Waiting for it to finish starting..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverTimeoutSeconds))
while true; do
	if grep --quiet "Started Jetty." "${serverLog}"; then
		echo "Server started in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to start within ${serverTimeoutSeconds} seconds. Trying to stop it..."
		"${scriptDirectory}/server-stop.sh" -t "${targetDirectory}"
		exit 3
	fi
	sleep 1
done
