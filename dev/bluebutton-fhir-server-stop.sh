#!/bin/bash

# Constants.
jettyVersion='9.3.11.v20160721'
jettyInstall="jetty-distribution-${jettyVersion}"
jettyPortStop=9095
jettyTimeoutSeconds=120

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o j:d: \
	--long javahome:,directory: \
	-n 'bluebutton-fhir-server-stop.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
javaBinDir=""
directory=
while true; do
	case "$1" in
		-j | --javahome )
			javaBinDir="${2}/bin/"; shift 2 ;;
		-d | --directory )
			directory="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${directory}" ]]; then >&2 echo 'The --directory option is required.'; exit 1; fi

# Verify that java was found.
command -v "${javaBinDir}java" >/dev/null 2>&1 || { echo >&2 "Java not found. Specify --javahome option."; exit 1; }

# Run the Jetty stop command.
cd "${directory}/${jettyInstall}"
jettyLogRun='logs/jetty-console.log'
jettyLogStop='logs/jetty-stop.log'
"${javaBinDir}java" \
	-DSTOP.PORT=${jettyPortStop} \
	-DSTOP.KEY='supersecure' \
	-jar start.jar \
	--stop \
	&> "${jettyLogStop}"

# Wait for the "...INFO:oejsh.ContextHandler:ShutdownMonitor: Stopped o.e.j.w.WebAppContext@..." in the log.
echo "Stop request issued. Waiting for server to stop..."
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $jettyTimeoutSeconds))
while true; do
	if grep --quiet "java.net.ConnectException: Connection refused" "${jettyLogStop}"; then
		>&2 echo "Server was not running."
		break
	fi
	if grep --quiet "INFO:oejsh.ContextHandler:ShutdownMonitor: Stopped o.e.j.w.WebAppContext@" "${jettyLogRun}"; then
		echo "Server stopped in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to stop within ${jettyTimeoutSeconds} seconds."
		exit 3
	fi
	sleep 1
done

