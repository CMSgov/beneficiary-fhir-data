#!/bin/bash

##
# This script will run the `benchmark_etl.yml` Ansible playbook, which will
# configure the benchmark systems and start the ETL process.
#
# Usage:
# 
# $ benchmark_etl.sh --iteration 42 --ec2keyfile somedir/foo
##

# Constants.
serverTimeoutSeconds=120

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
	-o i:f: \
	--long iteration:,ec2keyfile: \
	-n 'benchmark_etl.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"

# Parse the getopt results.
iteration=
ec2KeyFile=
while true; do
	case "$1" in
		-i | --iteration )
			iteration="$2"; shift 2 ;;
		-f | --ec2keyfile )
			ec2KeyFile="$2"; shift 2 ;;
		-- ) shift; break ;;
		* ) break ;;
	esac
done

# Verify that all required options were specified.
if [[ -z "${iteration}" ]]; then >&2 echo 'The --iteration option is required.'; exit 1; fi

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

# Ensure that the EC2 SSH key (if specified) is available for Ansible to use.
if [[ ! -z "${ec2KeyFile}" ]]; then
	ssh-add "${ec2KeyFile}"
fi

##
# Note: See ansible_init.py for an explanation of why we're calling 
# `ansible-playbook` this way.
##

# Run the Ansible playbook, if there's an inventory to run against. (There 
# may not be, if the `provision.yml` playbook failed very early.)
inventory="${scriptDirectory}/../../../target/benchmark-iterations/${iteration}/ansible_hosts"
if [[ -f "${inventory}" ]]; then
	echo 'Running Ansible playbook...'
	cd "${scriptDirectory}"
	python `which ansible-playbook` \
		benchmark_etl.yml \
		--inventory-file="${inventory}" \
		--extra-vars "iteration_index=${iteration}"
	echo 'Ansible playbook completed successfully.'
else
	echo 'No Ansible inventory found, so skipping teardown.'
fi
