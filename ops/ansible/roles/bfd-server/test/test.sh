#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -e

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "$SCRIPT_DIR"

# Re-activate existing virtualenv or exit 1
if [ ! -d venv ]; then
  echo 'Error: Missing directory venv.'
  exit 1
fi
source venv/bin/activate

# Basic role syntax check
ansible-playbook "$TEST_PLAY" --syntax-check --inventory=inventory.docker.yaml

# Run the Ansible test case.
ansible-playbook "$TEST_PLAY" --inventory=inventory.docker.yaml
