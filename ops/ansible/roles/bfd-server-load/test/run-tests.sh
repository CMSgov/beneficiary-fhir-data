#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -eou pipefail

function help() {
    echo "This script runs our test cases locally, via Docker."
    echo "Usage: ${0} [-e extra-vars] [-hk] [image id]"
    echo "Options:"
    echo "  ${0} -e <extra-vars>: [e]xtra variables for ansible-playbook."
    echo "  ${0} -h:              [h]elp displays this message and exits."
    # TODO: complete the getopts implementation. See BFD-1628.
    # echo "  ${0} -i <ID>: image [i]d set in 'ghcr.io/cmsgov/bfd-apps:<ID>'. Defaults to current commit hash."
    echo "  ${0} -k:              [k]eeps the container under test instead of removing it. Defaults to removing the container."
}

REMOVE_CONTAINER=true # exported after getopts below...
export ROLE=bfd-server-load
export CONTAINER_NAME="$ROLE"
export TEST_PLAY=test_basic.yml

# iterate getopts
while getopts "e:hk" option; do
    case "$option" in
      e) # extra-vars
        EXTRA_VARS="$OPTARG"
        ;;
      h) # help
        help
        exit
        ;;
      # TODO: complete the getopts implementation. See BFD-1628.
      # i) # image id
      #    input_bfd_apps_image_id="$OPTARG";;
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

# TODO: complete the getopts implementation. See BFD-1628.
# use the input from option '-i' or default to current commit's short sha
# use input "$1" or default to current commit's short sha
export BFD_ANSIBLE_IMAGE_ID="${1:-master}"
export REMOVE_CONTAINER EXTRA_VARS

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
