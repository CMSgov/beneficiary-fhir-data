#!/bin/bash

##
# This script runs the equivalent of (one of) the Travis CI test cases locally.
##

# Stop immediately if any command returns a non-zero result.
set -e

# These same variables are defined in `.travis.yml`. They can be adjusted to
# change which test is run.
export ROLE=karlmdavis.bluebutton-data-pipeline
export CONTAINER_PREFIX=ansible_test_pipeline
export TEST_PLAY=test_basic.yml
export PLATFORM=centos_7
export ANSIBLE_SPEC="ansible"

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# The SSH public key to use when connecting to Docker containers.
sshPublicKey="$(eval echo ~)/.ssh/id_rsa.pub"
if [[ ! -f "${sshPublicKey}" ]]; then
  echo "No SSH public key available." 1>&2
  exit 1
fi

# Run the equivalent of Travis' `install` phase (prepares to run the tests).
"${SCRIPT_DIR}/pre-test.sh" "${sshPublicKey}"

# Run the equivalent of Travis' `script` phase (runs the actualt tests).
"${SCRIPT_DIR}/test.sh"

# Run the equivalent of Travis' `after_script` phase (cleans up after the
# tests).
"${SCRIPT_DIR}/post-test.sh"

echo ""
echo "Tests completed successfully."
