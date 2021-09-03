#!/bin/bash

# Stop immediately if any command returns a non-zero result.
set -e

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "${SCRIPT_DIR}"

# Activate the Python virtual env.
source venv/bin/activate

# Basic role syntax check
ansible-playbook "${TEST_PLAY}" --inventory=inventory --syntax-check

# Run the Ansible test case.
ansible-playbook "${TEST_PLAY}" --inventory=inventory

# Run the role/playbook again, checking to make sure it's idempotent.
ansible-playbook "${TEST_PLAY}" --inventory=inventory \
  | tee /dev/stderr \
  | grep -q 'docker_container.*changed=0.*failed=0' \
  && (echo 'Idempotence test: pass' && exit 0) \
  || (echo 'Idempotence test: fail' && exit 1)
