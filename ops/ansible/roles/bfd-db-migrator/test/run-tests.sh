#!/usr/bin/env bash

##
# This script runs our test cases locally, via Docker.
##

# Stop immediately if any command returns a non-zero result.
set -eou pipefail

# These variables can be adjusted to change which test is run.
export ROLE=bfd-db-migrator
export CONTAINER_NAME="$ROLE"
export TEST_PLAY=test_basic.yml
export ANSIBLE_SPEC="ansible"
# use input "$1" or default to current commit's short sha
export BFD_APPS_IMAGE_ID="${1:-$(git rev-parse --short HEAD)}"
export ARTIFACT_DIRECTORY=".m2/repository/gov/cms/bfd/bfd-db-migrator/1.0.0-SNAPSHOT"
export ARTIFACT="bfd-db-migrator-1.0.0-SNAPSHOT.zip"

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Launch the Docker container that will be used in the tests.
"${SCRIPT_DIR}/pre-test.sh"

# Run the actual tests.
"${SCRIPT_DIR}/test.sh"

# Clean up the Docker container that was used in the tests.
"${SCRIPT_DIR}/post-test.sh"

echo ""
echo "Tests completed successfully."
