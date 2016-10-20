#!/bin/bash

# Constants.
serverVersion='8.1.0.Final'
serverInstall="wildfly-${serverVersion}"
serverConnectTimeoutMilliseconds=$((30 * 1000))

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o d: \
	--long directory: \
	-n 'bluebutton-fhir-server-stop.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
directory=
while true; do
	case "$1" in
		-d | --directory )
			directory="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${directory}" ]]; then >&2 echo 'The --directory option is required.'; exit 1; fi

# If the server isn't actually running, just exit.
serverPids=$(pgrep --full ".*java.*-Dbluebutton-server.*jboss-modules\.jar.*")
if [[ -z "${serverPids}" ]]; then echo 'No 'bluebutton-server' processes found to stop.'; exit 0; fi

# Use the Wildfly CLI to stop the server.
serverLogRun="${directory}/${serverInstall}/server-console.log"
serverLogStop="${directory}/${serverInstall}/server-stop.log"
"${directory}/${serverInstall}/bin/jboss-cli.sh" \
	--connect \
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
		echo "Server stopped in $(($SECONDS - $startSeconds)) seconds."
		exit 0
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to stop within ${serverTimeoutSeconds} seconds."
		break
	fi
	sleep 1
done

# The server didn't die the nice way, so now let's be mean about it.
echo "Killing server processes with KILL signal..."
pkill -KILL --full ".*java.*-Dbluebutton-server.*jboss-modules\.jar.*"
echo "Sent KILL signal to all 'bluebutton-server' processes."
