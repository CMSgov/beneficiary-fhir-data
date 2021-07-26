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
echo "Server stop script is being run as follows:"
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
workDirectory="${targetDirectory}/server-work"
bfdServerIdFile="${workDirectory}/bfd-server-id.txt"
serverLogRun="${workDirectory}/server-console.log"
serverLogStop="${workDirectory}/server-stop.log"

# Read bfdServerId from a file.
bfdServerId=
if [[ -f "${bfdServerIdFile}" ]]; then
	bfdServerId=$(cat "${bfdServerIdFile}")
fi
if [[ -z "${bfdServerId}" ]]; then >&2 echo "No server ID found in '${bfdServerIdFile}'."; exit 1; fi

# If the server isn't actually running, just exit.
serverPids=$(pgrep -f ".*java.*-Dbfd-server-${bfdServerId}.*")
if [[ -z "${serverPids}" ]]; then echo "No '-Dbfd-server-${bfdServerId}' processes found to stop."; exit 0; fi

# Stop the server.
>&2 echo "Sending KILL signal to all '-Dbfd-server-${bfdServerId}' processes."
pkill -TERM -f ".*java.*-Dbfd-server-${bfdServerId}.*"
>&2 echo "Server processes sent TERM signal."

# Watch the server log to wait for it to stop.
echo "Waiting for server to stop..."
serverTimeoutSeconds=120
startSeconds=$SECONDS
endSeconds=$(($startSeconds + $serverTimeoutSeconds))
while true; do
	if grep --quiet "Application shutdown housekeeping complete." "${serverLogRun}"; then
		echo "Server stopped (we think) in $(($SECONDS - $startSeconds)) seconds."
		break
	fi
	if [[ $SECONDS -gt $endSeconds ]]; then
		>&2 echo "Error: Server failed to stop within ${serverTimeoutSeconds} seconds."
		break
	fi
	sleep 1
done
