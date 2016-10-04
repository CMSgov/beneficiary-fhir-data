#!/bin/bash

##
# This script will create a Python virtualenv with Ansible installed in it.
#
# Usage:
# 
# $ ansible_init.sh
##

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

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

# Verify that Python's virtualenv and pip are available.
command -v virtualenv >/dev/null 2>&1 || { echo >&2 "Python virtualenv utility not found."; exit 1; }
command -v pip >/dev/null 2>&1 || { echo >&2 "Python pip utility not found."; exit 1; }

##
# Note: Running into problems here due to 
# https://github.com/pypa/pip/issues/1773 and/or 
# https://github.com/pypa/virtualenv/issues/596, which effects the `pip` and 
# `ansible-playbook` commands used here. As a workaround, we pass the command's 
# scripts to the Python interpreter directly, so as to avoid the shebang 
# problems.
##

# If the virtualenv hasn't already been created, do so.
virtualEnvDirectory="${scriptDirectory}/../../../target/python-venv-ansible"
if [[ ! -d "${virtualEnvDirectory}" ]]; then
	echo "Creating Python 2.7 virtualenv for Ansible in '${virtualEnvDirectory}'..."
	virtualenv -p /usr/bin/python2.7 "${virtualEnvDirectory}"
	source "${virtualEnvDirectory}/bin/activate"
	echo 'Python virtualenv created.'

	echo "Installing Ansible and its dependencies into the virtualenv..."
	python `which pip` install -r "${scriptDirectory}/requirements.txt"
	echo 'Ansible and dependencies have been installed.'
else
	echo "Python virtualenv already exists in '${virtualEnvDirectory}'."
	source "${virtualEnvDirectory}/bin/activate"
fi
