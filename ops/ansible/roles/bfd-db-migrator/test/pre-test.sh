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

docker network create "$CONTAINER_NAME" || true

if [ ! "$(docker ps -f "name=${CONTAINER_NAME}-db" --format '{{.Names}}' | grep -E "^${CONTAINER_NAME}-db$")" ]; then
docker run \
    --detach \
    "--net=${CONTAINER_NAME}" \
    --net-alias=db \
    -e 'POSTGRES_DB=fhirdb' \
    -e 'POSTGRES_USER=bfd' \
    -e 'POSTGRES_PASSWORD=bfd' \
    --name "${CONTAINER_NAME}-db" \
    --rm \
    postgres:16 -c max_connections=200
fi

# Prep the Docker container that will be used (if it's not already running).
if [ ! "$(docker ps -f "name=${CONTAINER_NAME}" --format '{{.Names}}' | grep -E "^${CONTAINER_NAME}$")" ]; then
  docker run \
    "--net=${CONTAINER_NAME}" \
    --cap-add=SYS_ADMIN \
    --detach \
    --rm \
    --volume=/sys/fs/cgroup:/sys/fs/cgroup:ro \
    --tmpfs /run \
    --tmpfs /run/lock \
    --name "$CONTAINER_NAME" \
    "ghcr.io/cmsgov/bfd-apps:${BFD_APPS_IMAGE_ID}"
fi

# Ensure the ansible host's artifact directory exists
mkdir -p "${HOME}/${ARTIFACT_DIRECTORY}"
# Copy the artifact from the container onto the ansible host
docker cp "${CONTAINER_NAME}:/${ARTIFACT_DIRECTORY}/${ARTIFACT}" "${HOME}/${ARTIFACT_DIRECTORY}/${ARTIFACT}"
