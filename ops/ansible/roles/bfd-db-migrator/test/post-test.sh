#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -e

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "$SCRIPT_DIR"

# Remove the Docker instance used in the tests.
if [ "$REMOVE_CONTAINER" ]; then
    echo "Removing ${CONTAINER_NAME} container..."
    docker rm --force "$CONTAINER_NAME"
fi
