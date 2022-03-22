#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -eou pipefail

function help() {
    echo "This script runs our test cases locally, via Docker."
    echo "Usage: ${0} [-hk] [image id]"
    echo "Options:"
    echo "  ${0} -h:      [h]elp displays this message and exits."
    # TODO: complete the getopts implementation
    # echo "  ${0} -i <ID>: image [i]d set in 'ghcr.io/cmsgov/bfd-apps:<ID>'. Defaults to current commit hash."
    echo "  ${0} -k:      [k]eep the container under test instead of removing it. Defaults to removing the container."
}

REMOVE_CONTAINER=true # exported after getopts below...
export ROLE=bfd-db-migrator
export CONTAINER_NAME="$ROLE"
export TEST_PLAY=test_basic.yml
export ANSIBLE_SPEC="ansible"
export ARTIFACT_DIRECTORY=".m2/repository/gov/cms/bfd/bfd-db-migrator/1.0.0-SNAPSHOT"
export ARTIFACT="bfd-db-migrator-1.0.0-SNAPSHOT.zip"

# iterate getopts
while getopts "hk" option; do
   case "$option" in
      h) # help
        help
        exit;;
      # TODO: complete the getopts implementation
      # i) # image id
      #    input_bfd_apps_image_id="$OPTARG";;
      k) # keep container
         REMOVE_CONTAINER=false;;
     \?) # Invalid
         help
         exit 1;;
   esac
done
shift "$((OPTIND-1))"

# TODO: complete the getopts implementation
# use the input from option '-i' or default to current commit's short sha
# export BFD_APPS_IMAGE_ID="${input_bfd_apps_image_id:-$(git rev-parse --short HEAD)}"
# use input "$1" or default to current commit's short sha
export BFD_APPS_IMAGE_ID="${1:-$(git rev-parse --short HEAD)}"
export REMOVE_CONTAINER

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Launch the Docker container that will be used in the tests.
"${SCRIPT_DIR}/pre-test.sh"

# Run the actual tests.
"${SCRIPT_DIR}/test.sh"

# Clean up after testing
"${SCRIPT_DIR}/post-test.sh"

echo ""
echo "Tests completed successfully."
