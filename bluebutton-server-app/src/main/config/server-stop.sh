#!/bin/bash

# Constants.
serverVersion='8.1.0.Final'
serverInstall="wildfly-${serverVersion}"
serverConnectTimeoutMilliseconds=$((30 * 1000))

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	d: \
	$*`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
directory=
while true; do
	case "$1" in
		-d )
			directory="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${directory}" ]]; then >&2 echo 'The -d option is required.'; exit 1; fi

# If the server isn't actually running, just exit.
serverPids=$(pgrep -f ".*java.*-Dbluebutton-server.*jboss-modules\.jar.*")
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
serverPids=$(pgrep -f ".*java.*-Dbluebutton-server.*jboss-modules\.jar.*")
if [[ -z "${serverPids}" ]]; then
	echo "Server did actually stop."
	exit 0
else
	>&2 echo "Server processes still found. Sending KILL signal to all 'bluebutton-server' processes."
	pkill -KILL -f ".*java.*-Dbluebutton-server.*jboss-modules\.jar.*"
	>&2 echo "Server processes sent KILL signal."
fi
