#!/bin/bash

##
# This script will run the `provision.yml` Ansible playbook.
#
# Usage:
# 
# $ provision.sh --iteration 42 --ec2keyname foo
##

# Constants.
serverTimeoutSeconds=120

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o i:n: \
	--long iteration:,ec2keyname: \
	-n 'provision.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
iteration=
ec2KeyName=
while true; do
	case "$1" in
		-i | --iteration )
			iteration="$2"; shift 2 ;;
		-n | --ec2keyname )
			ec2KeyName="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${iteration}" ]]; then >&2 echo 'The --iteration option is required.'; exit 1; fi
if [[ -z "${ec2KeyName}" ]]; then >&2 echo 'The --ec2keyname option is required.'; exit 1; fi

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

# If the virtualenv hasn't already been created, error out.
virtualEnvDirectory="${scriptDirectory}/../../../target/python-venv-ansible"
if [[ ! -d "${virtualEnvDirectory}" ]]; then
	>&2 echo "Virtual directory missing: '${virtualEnvDirectory}'."
	exit 2
fi
source "${virtualEnvDirectory}/bin/activate"

##
# Note: See ansible_init.py for an explanation of why we're calling 
# `ansible-playbook` this way.
##

# Run the Ansible playbook.
cd "${scriptDirectory}"
echo 'Running Ansible playbook...'
python `which ansible-playbook` \
	provision.yml \
	--extra-vars "ec2_key_name=${ec2KeyName} iteration_index=${iteration}"
echo 'Ansible playbook completed successfully.'
