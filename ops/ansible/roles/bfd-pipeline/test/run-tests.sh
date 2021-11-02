#!/bin/bash

##
# This script runs our test cases locally, via Docker.
##

# Stop immediately if any command returns a non-zero result.
set -e

# These variables can be adjusted to change which test is run.
export ROLE=bfd-pipeline
export CONTAINER_PREFIX=ansible_test_pipeline
export TEST_PLAY=test_basic.yml
export PLATFORM=centos_7
export ANSIBLE_SPEC="ansible"

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# The SSH public key to use when connecting to Docker containers.
sshPublicKey="$(eval echo ~)/.ssh/id_ed25519.pub"
if [[ ! -f "${sshPublicKey}" ]]; then
  echo "No SSH public key available." 1>&2
  exit 1
fi

# Launch the Docker container that will be used in the tests.
"${SCRIPT_DIR}/pre-test.sh" "${sshPublicKey}"

# Run the actual tests.
"${SCRIPT_DIR}/test.sh"

# Clean up the Docker container that was used in the tests.
"${SCRIPT_DIR}/post-test.sh"

echo ""
echo "Tests completed successfully."
