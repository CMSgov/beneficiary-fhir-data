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

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Verify that all required options were specified.
if [[ -z "${1}" ]]; then >&2 echo 'Specify the DB file to import to as the first argument.'; exit 1; fi
if [[ ! -f "${1}" ]]; then >&2 echo 'Unable to find DB to import to.'; exit 1; fi
targetDb="${1}"
if [[ -z "${2}" ]]; then >&2 echo 'Specify the access log file to import as the second argument.'; exit 1; fi
if [[ ! -f "${2}" ]]; then >&2 echo 'Unable to find access log file to import.'; exit 1; fi
accessLog="${2}"

# In Cygwin, some of those paths will come in as Windows-formatted. Fix that.
if [[ "${cygwin}" = true ]]; then
	targetDb="$(cygpath --unix ${targetDb})"
	accessLog="$(cygpath --unix ${accessLog})"
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

	>&2 echo "Exiting with status ${code}."
	exit "${code}"
}
trap 'error ${LINENO}' ERR

# Import the CSV log file to the DB.
sqlite3 "${targetDb}" <<EOF
.mode csv
.headers off
.import ${accessLog} access_log
EOF

echo 'Access log imported.'
