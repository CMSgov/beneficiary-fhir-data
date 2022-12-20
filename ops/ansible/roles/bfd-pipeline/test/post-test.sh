#!/usr/bin/env bash

# Stop immediately if any command returns a non-zero result.
set -e

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "$SCRIPT_DIR"

# echo "Removing ${CONTAINER_NAME}, ${CONTAINER_NAME}-db containers..."
# docker rm --force "$CONTAINER_NAME" "${CONTAINER_NAME}-db"
