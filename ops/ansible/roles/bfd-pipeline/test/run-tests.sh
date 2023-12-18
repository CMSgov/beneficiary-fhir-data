#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -eou pipefail

function help() {
    echo "This script runs our test cases locally, via Docker."
    echo "Usage: ${0} [-e extra-vars] [-hk] [-i image] [bfd_version]"
    echo "Options:"
    echo "  ${0} -e <extra-vars>: [e]xtra variables for ansible-playbook."
    echo "  ${0} -h:              [h]elp displays this message and exits."
    echo "  ${0} -i <ID>:         [i]mage id set in 'ghcr.io/cmsgov/bfd-apps:<ID>'. Defaults to current commit hash."
    echo "  ${0} -k:              [k]eep the container under test instead of removing it. Defaults to removing the container."
}

# exported after getopts below...
REMOVE_CONTAINER=true
BFD_APPS_IMAGE_ID="$(git rev-parse --short HEAD)"

export ROLE=bfd-pipeline
export CONTAINER_NAME="$ROLE"
export TEST_PLAY=test_basic.yml
export ANSIBLE_SPEC="ansible"

# iterate getopts
while getopts "e:i:hk" option; do
    case "$option" in
      e) # extra-vars
        EXTRA_VARS="$OPTARG"
        ;;
      h) # help
        help
        exit
        ;;
      i) # image id
        BFD_APPS_IMAGE_ID="$OPTARG";;
      k) # keep container
        REMOVE_CONTAINER=false
        ;;
     \?) # Invalid
        help
        exit 1
        ;;
    esac
done
shift "$((OPTIND-1))"

export BFD_VERSION="$1"
export ARTIFACT_DIRECTORY=".m2/repository/gov/cms/bfd/bfd-pipeline-app/${BFD_VERSION}"
export ARTIFACT="bfd-pipeline-app-${BFD_VERSION}.zip"

export REMOVE_CONTAINER EXTRA_VARS BFD_APPS_IMAGE_ID

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
