#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -eou pipefail

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "$SCRIPT_DIR"

# Create and activate the Python virtualenv needed by Ansible.
if [ ! -d venv/ ]; then
  python3 -m venv venv
fi
source venv/bin/activate

# Install/upgrade wheel and pip to tamp down on noisy logs
pip install wheel pip --upgrade

# Install any requirements needed by the role or its tests.
if [ -f ../requirements.txt ]; then pip install --requirement ../requirements.txt; fi
if [ -f requirements.txt ]; then pip install --requirement requirements.txt; fi

ansible-galaxy collection install community.docker

# Prep the Ansible roles that the test will use.
if [ ! -d roles ]; then mkdir roles; fi
if [ ! -L "roles/${ROLE}" ]; then ln -s "$(cd .. && pwd)" "roles/${ROLE}"; fi

# Prep the Docker container that will be used (if it's not already running).
if [ ! "$(docker ps -f "name=${CONTAINER_NAME}" --format '{{.Names}}' | grep -E "^${CONTAINER_NAME}$")" ]; then
  docker run \
    --cap-add=SYS_ADMIN \
    --detach \
    --rm \
    --volume=/sys/fs/cgroup:/sys/fs/cgroup:ro \
    --tmpfs /run \
    --tmpfs /run/lock \
    --name "$CONTAINER_NAME" \
    "amazonlinux:2" \
    sleep infinity
fi
