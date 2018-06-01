#!/bin/bash

# Check to see if we are running in Cygwin.
case "$( uname )" in
	CYGWIN*) cygwin=true ;;
	*) cygwin=false ;;
esac

# In Cygwin, some of the args will have unescaped backslashes. Fix that.
if [[ "${cygwin}" = true ]]; then
	set -- "${@//\\/\\\\}"
fi

# Makes debugging problems a lot easier if this is always logged.
echo "Server stop script is being run as follows:"
if [[ "${cygwin}" = true ]]; then
	echo -e "${0//\\/\\\\} $@\n"
else
	echo -e "$0 $@\n"
fi

# Constants.
serverVersion='8.1.0.Final'
serverInstall="wildfly-${serverVersion}"
serverConnectTimeoutMilliseconds=$((30 * 1000))

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	t: \
	$*`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
targetDirectory=
serverPortManagement=
while true; do
	case "$1" in
		-t )
			targetDirectory="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${targetDirectory}" ]]; then >&2 echo 'The -t option is required.'; exit 1; fi

# In Cygwin, some of those paths will come in as Windows-formatted. Fix that.
if [[ "${cygwin}" = true ]]; then targetDirectory=$(cygpath --unix "${targetDirectory}"); fi

# Ensure that the working directory is consistent.
cd "${targetDirectory}/.."

# Define all of the derived paths we'll need.
workDirectory="${targetDirectory}/bluebutton-server"
serverPortsFile="${workDirectory}/server-ports.properties"
bluebuttonServerIdFile="${workDirectory}/bluebutton-server-id.txt"
serverHome="${workDirectory}/${serverInstall}"
serverLogRun="${workDirectory}/server-console.log"
serverLogStop="${workDirectory}/server-stop.log"

# Check for required files.
for f in "${serverPortsFile}"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# Read bluebuttonServerId from a file.
bluebuttonServerId=
if [[ -f "${bluebuttonServerIdFile}" ]]; then
	bluebuttonServerId=$(cat "${bluebuttonServerIdFile}")
fi
if [[ -z "${bluebuttonServerId}" ]]; then >&2 echo "No server ID found in '${bluebuttonServerIdFile}'."; exit 1; fi

# Also try to read serverPortManagement from a file.
if [[ -f "${serverPortsFile}" ]]; then
	serverPortManagement=$(grep "^server.port.management=" "${serverPortsFile}" | cut -d'=' -f2 )
fi
if [[ -z "${serverPortManagement}" ]]; then >&2 echo "Server management port not specified in '${serverPortsFile}'."; exit 1; fi

# If the server isn't actually running, just exit.
serverPids=$(pgrep -f ".*java.*-Dbluebutton-server-${bluebuttonServerId}.*jboss-modules\.jar.*")
if [[ -z "${serverPids}" ]]; then echo "No '-Dbluebutton-server-${bluebuttonServerId}' processes found to stop."; exit 0; fi

# Use the Wildfly CLI to stop the server.
"${serverHome}/bin/jboss-cli.sh" \
	--connect \
	--controller=localhost:${serverPortManagement} \
	--timeout=${serverConnectTimeoutMilliseconds} \
	--command=shutdown \
	&> "${serverLogStop}"

# Watch the server log to wait for it to stop.
echo "Stop request issued. Waiting for server to stop..."
serverTimeoutSeconds=120
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverTimeoutSeconds))
while true; do
	if grep --quiet "CliInitializationException: Failed to connect to the controller" "${serverLogStop}"; then
		>&2 echo "Server was not running."
		break
	fi
	if grep --quiet "JBAS015950" "${serverLogRun}"; then
		echo "Server stopped (we think) in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to stop within ${serverTimeoutSeconds} seconds."
		break
	fi
	sleep 1
done

# The above block might not have been able to actually stop the server, either 
# because the server's management console wasn't up, or because the server 
# _said_ it stopped, but really didn't (I've observed this happening). So here,
# we just double check via the process list, and kill it the mean way if 
# needed.
serverPids=$(pgrep -f ".*java.*-Dbluebutton-server-${bluebuttonServerId}.*jboss-modules\.jar.*")
if [[ -z "${serverPids}" ]]; then
	echo "Server did actually stop."
	exit 0
else
	>&2 echo "Server processes still found. Sending KILL signal to all '-Dbluebutton-server-${bluebuttonServerId}' processes."
	pkill -KILL -f ".*java.*-Dbluebutton-server-${bluebuttonServerId}.*jboss-modules\.jar.*"
	>&2 echo "Server processes sent KILL signal."
fi
