#!/bin/bash

##
# This script will wait for the Wildfly / JBoss EAP 7 instances used 
# to host the Blue Button Data Server WAR to be ready.
#
# Usage:
# 
# $ bluebutton-appserver-wait.sh --serverhome /path-to-jboss --managementport 9090 --managementusername someadmin --managementpasswork somepass
##

# Constants.
serverReadyTimeoutSeconds=120
serverConnectTimeoutMilliseconds=$((30 * 1000))

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o h:m:U:P:s:k:t:u:n:p:c: \
	--long serverhome:,managementport:,managementusername:,managementpassword: \
	-n 'bluebutton-appserver-wait.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
serverHome=
managementPort=9990
managementUsername=
managementPassword=
while true; do
	case "$1" in
		-h | --serverhome )
			serverHome="$2"; shift 2 ;;
		-m | --managementport )
			managementPort="$2"; shift 2 ;;
		-U | --managementusername )
			managementUsername="$2"; shift 2 ;;
		-P | --managementpassword )
			managementPassword="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${serverHome}" ]]; then >&2 echo 'The --serverhome option is required.'; exit 1; fi

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
for f in "${serverHome}/bin/jboss-cli.sh"; do
	if [[ ! -f "${f}" ]]; then
		>&2 echo "The following file is required but is missing: '${f}'."
		exit 1
	fi
done

# Determine the authentication arguments to use with the Wildfly CLI.
# Note: This isn't 100% secure. The CLI can't read the username and password 
# from a file, so they have to be specified as command line aguments. Such 
# arguments are visible in `/proc` and via `ps` while the command is running.
# For this script, that will only be a brief window, so the risk is acceptable.
cliArgsAuthentication=""
if [[ ! -z "${managementUsername}" ]]; then
	cliArgsAuthentication="--user=${managementUsername} --password=${managementPassword}"
fi

# Define a function that can wait for the server to be ready.
waitForServerReady() {
	echo "Waiting for server to be ready..." |& tee --append "${serverHome}/server-config.log"
	startSeconds=$SECONDS
	endSeconds=$(($startSeconds + $serverReadyTimeoutSeconds))
	while true; do
		if "${serverHome}/bin/jboss-cli.sh" --controller=localhost:${managementPort} --connect ${cliArgsAuthentication} --command=":read-attribute(name=server-state)" |& tee --append "${serverHome}/server-config.log" |& grep --quiet "\"result\" => \"running\""; then
			echo "Server ready after $(($SECONDS - $startSeconds)) seconds." |& tee --append "${serverHome}/server-config.log"
			break
		fi
		if [[ $SECONDS -gt $endSeconds ]]; then
			error ${LINENO} "Error: Server failed to be ready within ${serverReadyTimeoutSeconds} seconds." 3
		fi
		sleep 1
	done
}

# Wait for the server to be ready, in case this script is being called right
# after the server is started/restarted.
waitForServerReady

