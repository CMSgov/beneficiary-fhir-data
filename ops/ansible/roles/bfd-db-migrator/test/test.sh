#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -e

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "$SCRIPT_DIR"

if [ ! -d venv ]; then
  echo 'Error: Missing directory venv.'
  exit 1
fi
source venv/bin/activate

# Basic role syntax check
ansible-playbook "$TEST_PLAY" --inventory=inventory.docker.yaml --syntax-check

# Run the Ansible test case.
ansible-playbook "$TEST_PLAY" --inventory=inventory.docker.yaml

# Run the role/playbook again, checking to make sure it's idempotent.
if ansible-playbook "$TEST_PLAY" --inventory=inventory.docker.yaml | tee /dev/stderr | grep -q "${CONTAINER_NAME}.*changed=0.*failed=0"; then
  echo 'Idempotence test: pass' && exit 0
else
  echo 'Idempotence test: fail' && exit 1
fi
