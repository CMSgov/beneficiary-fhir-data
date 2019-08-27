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
serverVersion='8.1.0.Final'
serverInstall="wildfly-${serverVersion}"
serverTimeoutSeconds=120
serverConnectTimeoutMilliseconds=$((30 * 1000))
dbUsername=""
dbPassword=""

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	j:m:v:t:u: \
	$*`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
javaHome=""
maxHeapArg="-Xmx4g"
visualVm=""
targetDirectory=
dbUrl="jdbc:bluebutton-test:hsqldb:mem"
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
workDirectory="${targetDirectory}/bluebutton-server"
serverArtifact="${workDirectory}/wildfly-dist-${serverVersion}.tar.gz"
serverPortsFile="${workDirectory}/server-ports.properties"
warArtifact="${targetDirectory}/$(ls ${targetDirectory} | grep '^bluebutton-server-app-.*\.war$')"
keyStore="${scriptDirectory}/../../../../dev/ssl-stores/server-keystore.jks"
trustStore="${scriptDirectory}/../../../../dev/ssl-stores/server-truststore.jks"
rolesProps="${scriptDirectory}/../../../../dev/ssl-stores/server-roles.properties"
serverHome="${workDirectory}/${serverInstall}"
serverLog="${workDirectory}/server-console.log"

# Check for required files.
for f in "${serverArtifact}" "${serverPortsFile}" "${warArtifact}" "${keyStore}" "${trustStore}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# In Cygwin, some of those paths need to be Windows-formatted. Do that.
if [[ "${cygwin}" = true ]]; then warArtifact=$(cygpath --windows "${warArtifact}"); warArtifact="${warArtifact//\\/\\\\}"; fi
if [[ "${cygwin}" = true ]]; then keyStore=$(cygpath --mixed "${keyStore}"); fi
if [[ "${cygwin}" = true ]]; then trustStore=$(cygpath --mixed "${trustStore}"); fi

# If the server install already exists, clean it out to start fresh.
if [[ -d "${serverHome}" ]]; then
	echo "Previous server install found. Removing..."
	rm -rf "${serverHome}"
	echo "Previous server install removed."
fi

# Unzip the server.
if [[ ! -d "${serverHome}" ]]; then
	tar --extract \
		--file "${serverArtifact}" \
		--directory "${workDirectory}"
	echo "Unpacked server dist: '${serverHome}'"
fi

# Read the server ports to be used from the ports file.
serverPortManagement=$(grep "^server.port.management=" "${serverPortsFile}" | tr -d '\r' | cut -d'=' -f2 )
serverPortHttp=$(grep "^server.port.http=" "${serverPortsFile}" | tr -d '\r' | cut -d'=' -f2 )
serverPortHttps=$(grep "^server.port.https=" "${serverPortsFile}" | tr -d '\r' | cut -d'=' -f2 )
if [[ -z "${serverPortManagement}" ]]; then >&2 echo "Server management port not specified in '${serverPortsFile}'."; exit 1; fi
if [[ -z "${serverPortHttp}" ]]; then >&2 echo "Server HTTP port not specified in '${serverPortsFile}'."; exit 1; fi
if [[ -z "${serverPortHttps}" ]]; then >&2 echo "Server HTTPS port not specified in '${serverPortsFile}'."; exit 1; fi
echo "Configured server to run on HTTPS port '${serverPortHttps}', HTTP port '${serverPortHttp}', and management port '${serverPortManagement}'."

# Generate a random server ID and write it to a file.
bluebuttonServerId=$RANDOM
echo -n "${bluebuttonServerId}" > "${workDirectory}/bluebutton-server-id.txt"

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

# Rename the original server conf file.
if [[ ! -f "${serverHome}/bin/standalone.conf.original" ]]; then
	mv "${serverHome}/bin/standalone.conf" "${serverHome}/bin/standalone.conf.original"
fi

# Write a correct server conf file.
javaHomeLine=''
if [[ -z "${javaHome}" ]]; then
	javaHomeLine=''
else
	javaHomeLine="JAVA_HOME=${javaHome}"
fi
cat <<EOF > "${serverHome}/bin/standalone.conf"
## -*- shell-script -*- ######################################################
##                                                                          ##
##  JBoss Bootstrap Script Configuration                                    ##
##                                                                          ##
##############################################################################
${javaHomeLine}
JBOSS_MODULES_SYSTEM_PKGS="${jbossModulesSystemPackages}"
JAVA_OPTS="-Xms64m ${maxHeapArg} -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="\$JAVA_OPTS -Djboss.modules.system.pkgs=\$JBOSS_MODULES_SYSTEM_PKGS -Djava.awt.headless=true"
JAVA_OPTS="\$JAVA_OPTS ${visualVmArgs}"

# Uncomment this next line to enable debugging Wildfly at launch. It will wait 
# for a debugger to connect when first launching the server.
#JAVA_OPTS="\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"

# Uncomment this next line to enable SSL debug logging. Watch out: it's super noisy.
#JAVA_OPTS="\$JAVA_OPTS -Djavax.net.debug=all"

# These ports are only used until the server is configured, but need to be
# set anyways, as the defaults on first launch conflict with Jenkins and other 
# such services.
JAVA_OPTS="\$JAVA_OPTS -Djboss.management.http.port=${serverPortManagement} -Djboss.http.port=${serverPortHttp} -Djboss.https.port=${serverPortHttps}"

# These properties are all referenced within the standalone.xml we'll be using.
JAVA_OPTS="\$JAVA_OPTS -Dbbfhir.db.url=${dbUrl}"
JAVA_OPTS="\$JAVA_OPTS -Dbbfhir.ssl.keystore.path=${keyStore} -Dbbfhir.ssl.truststore.path=${trustStore} -Dbbfhir.roles=${rolesProps}"

# Used in src/main/resources/logback.xml as the directory to write the app log to. Must have a trailing slash.
JAVA_OPTS="\$JAVA_OPTS -Dbbfhir.logs.dir=${workDirectory}/"

# This just adds a searchable bit of text to the command line, so we can 
# determine which java processes were started by this script.
JAVA_OPTS="\$JAVA_OPTS -Dbluebutton-server-${bluebuttonServerId}"
EOF

# Swap out the original standalone.xml file for our customized one.
if [[ ! -f "${serverHome}/standalone/configuration/standalone.xml.original" ]]; then
	mv "${serverHome}/standalone/configuration/standalone.xml" "${serverHome}/standalone/configuration/standalone.xml.original"
fi
cp "${scriptDirectory}/standalone.xml" "${serverHome}/standalone/configuration/standalone.xml"

# Config is all done now.
echo "Configured server."

# Launch the server in the background.
#
# Note: there's no reliable way to get the PID of the actual java process for
# the server, as it's spawned as a child of standalone.sh. Unfortunately, even
# the JBOSS_PIDFILE option just seems to give us the PID of the script. Signals
# sent to the script are (unfortunately) not passed through to the child Java
# process. I think this is all just buggy in Wildfly 8.1. The only thing that
# mitigates the mess is that the script process does exit once the java process
# does.
# Debugging: Add `--debug 8787` to the command here to enable normal Java 
# remote debugging of the apps running in Wildfly on port 8787.
"${serverHome}/bin/standalone.sh" \
	&> "${serverLog}" \
	&

# Wait for the server to be ready.
echo "Server launched, logging to '${serverLog}'. Waiting for it to finish starting..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverTimeoutSeconds))
while true; do
	if grep --quiet "JBAS015874" "${serverLog}"; then
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

# Write the JBoss CLI script that will deploy the WAR.
scriptDeploy="${serverHome}/jboss-cli-script-deploy.txt"
if [[ "${cygwin}" = true ]]; then scriptDeployArg=$(cygpath --windows "${scriptDeploy}"); else scriptDeployArg="${scriptDeploy}"; fi
cat <<EOF > "${scriptDeploy}"
deploy "${warArtifact}" --name=ROOT.war
EOF

# Deploy the application to the now-configured server.
echo "Deploying application: '${warArtifact}'..."
"${serverHome}/bin/jboss-cli.sh" \
	--connect \
	--controller=localhost:${serverPortManagement} \
	--timeout=${serverConnectTimeoutMilliseconds} \
	--file=${scriptDeployArg} \
	>> "${workDirectory}/server-config.log" 2>&1
# Note: No need to watch log here, as the command blocks until deployment is
# completed, and returns a non-zero exit code if it fails.
echo 'Application deployed.'
