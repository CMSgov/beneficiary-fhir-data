#!/bin/bash

# Constants.
serverVersion='8.1.0.Final'
serverArtifact="wildfly-dist-${serverVersion}.tar.gz"
serverInstall="wildfly-${serverVersion}"
serverPortHttps=9094
serverTimeoutSeconds=120
warArtifact='bluebutton-server-app.war'
configArtifact='bluebutton-server-app-server-config.sh'

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Check to see if we are running in Cygwin.
case "$( uname )" in
	CYGWIN*) cygwin=true ;;
	*) cygwin=false ;;
esac

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o j:m:v:d:k:t:u:n:p: \
	--long javahome:,maxheaparg:,visualvm:,directory:,keystore:,truststore:,dburl:,dbusername:,dbpassword: \
	-n 'bluebutton-fhir-server-start.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
javaHome=""
maxHeapArg="-Xmx4g"
visualVm=""
directory=
keyStore=
trustStore=
dbUrl="jdbc:hsqldb:mem:test"
dbUsername=""
dbPassword=""
while true; do
	case "$1" in
		-j | --javahome )
			javaHome="$2"; shift 2 ;;
		-m | --maxheaparg )
			maxHeapArg="$2"; shift 2 ;;
		-v | --visualvm )
			visualVm="$2"; shift 2 ;;
		-d | --directory )
			directory="$2"; shift 2 ;;
		-k | --keystore )
			keyStore="$2"; shift 2 ;;
		-t | --truststore )
			trustStore="$2"; shift 2 ;;
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

#echo "javaHome: '${javaHome}', maxHeapArg: '${maxHeapArg}', visualVm: '${visualVm}', directory: '${directory}', keyStore: '${keyStore}', trustStore: '${trustStore}', dbUrl: '${dbUrl}', dbUsername: '${dbUsername}', dbPassword: '${dbPassword}'"

# Verify that all required options were specified.
if [[ -z "${directory}" ]]; then >&2 echo 'The --directory option is required.'; exit 1; fi
if [[ -z "${keyStore}" ]]; then >&2 echo 'The --keystore option is required.'; exit 1; fi
if [[ -z "${trustStore}" ]]; then >&2 echo 'The --truststore option is required.'; exit 1; fi

# Verify that java was found.
if [[ -z "${javaHome}" ]]; then
	command -v java >/dev/null 2>&1 || { echo >&2 "Java not found. Specify --javahome option."; exit 1; }
else
	if [[ "${cygwin}" = true ]]; then javaHome=$(cygpath --unix "${javaHome}"); fi
	command -v "${javaHome}/bin/java" >/dev/null 2>&1 || { echo >&2 "Java not found in --javahome: '${javaHome}'"; exit 1; }
fi

# Munge paths for Cygwin.
if [[ "${cygwin}" = true ]]; then directory=$(cygpath --unix "${directory}"); fi

# Exit immediately if something fails.
error() {
	local parent_lineno="$1"
	local message="$2"
	local code="${3:-1}"

	if [[ -n "$message" ]] ; then
		>&2 echo "Error on or near line ${parent_lineno}: ${message}."
	else
		>&2 echo "Error on or near line ${parent_lineno}."
	fi
	
	# Before bailing, always try to stop any running servers.
	>&2 echo "Trying to stop any running servers before exiting..."
	"${scriptDirectory}/bluebutton-server-app-server-stop.sh" --directory "${directory}"

	>&2 echo "Exiting with status ${code}."
	exit "${code}"
}
trap 'error ${LINENO}' ERR

# Check for required files.
for f in "${directory}/${serverArtifact}" "${directory}/${warArtifact}" "${directory}/${configArtifact}" "${keyStore}" "${trustStore}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# If the server install already exists, clean it out to start fresh.
if [[ -d "${directory}/${serverInstall}" ]]; then
	echo "Previous server install found. Removing..."
	rm -rf "${directory}/${serverInstall}"
	echo "Previous server install removed."
fi

# Unzip the server.
if [[ ! -d "${directory}/${serverInstall}" ]]; then
	tar --extract \
		--file "${directory}/${serverArtifact}" \
		--directory "${directory}"
	echo "Unpacked server dist: '${directory}/${serverInstall}'"
fi

# Rename the original server conf file.
if [[ ! -f "${directory}/${serverInstall}/bin/standalone.conf.original" ]]; then
	mv "${directory}/${serverInstall}/bin/standalone.conf" "${directory}/${serverInstall}/bin/standalone.conf.original"
fi

# Build the args to pass to the server for VisualVM (if any).
if [[ -f "${visualVm}/profiler/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so" ]]; then
	echo "Found VisualVM directory: '${visualVm}'"
	visualVmArgs="-agentpath:${visualVm}/profiler/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so=${visualVm}/profiler/lib,5140"
	visualVmArgs="${visualVmArgs} -Dorg.osgi.framework.bootdelegation=org.netbeans.lib.profiler.server,org.netbeans.lib.profiler.server.*"
	visualVmArgs="${visualVmArgs} -Djava.util.logging.manager=org.jboss.logmanager.LogManager" 
	visualVmArgs="${visualVmArgs} -Xbootclasspath/p:${directory}/${serverInstall}/modules/system/layers/base/org/jboss/logmanager/main/jboss-logmanager-1.5.2.Final.jar" 
	jbossModulesSystemPackages="org.netbeans.lib.profiler.server,org.jboss.logmanager"
else
	echo "VisualVM directory not found: '${visualVm}'"
	visualVmArgs=""
fi

# Write a correct server conf file.
javaHomeLine=''
if [[ -z "${javaHome}" ]]; then
	javaHomeLine=''
else
	javaHomeLine="JAVA_HOME=${javaHome}"
fi
cat <<EOF > "${directory}/${serverInstall}/bin/standalone.conf"
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
# JAVA_OPTS="\$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"

# These ports are only used until the server is configured, but need to be
# set anyways, as the defaults on first launch conflict with Jenkins and other 
# such services.
JAVA_OPTS="\$JAVA_OPTS -Djboss.http.port=7780 -Djboss.https.port=${serverPortHttps}"

# This just adds a searchable bit of text to the command line, so we can 
# determine which java processes were started by this script.
JAVA_OPTS="\$JAVA_OPTS -Dbluebutton-server"
EOF

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
serverLog="${directory}/${serverInstall}/server-console.log"
"${directory}/${serverInstall}/bin/standalone.sh" \
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
		"${scriptDirectory}/bluebutton-server-app-server-stop.sh" --directory "${directory}"
		exit 3
	fi
	sleep 1
done

# Configure the server.
echo "Configuring server..."
chmod a+x "${directory}/${configArtifact}"
"${directory}/${configArtifact}" \
	--serverhome "${directory}/${serverInstall}" \
	--httpsport "${serverPortHttps}" \
	--keystore "${keyStore}" \
	--truststore "${trustStore}" \
	--war "${directory}/${warArtifact}" \
	--dburl "${dbUrl}" \
	--dbusername "${dbUsername}" \
	--dbpassword "${dbPassword}"

