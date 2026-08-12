#!/usr/bin/env bash

set -Eeou pipefail

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

DOCKER_BUILDKIT=1 podman buildx build "$SCRIPT_DIR" \
  --file "$SCRIPT_DIR"/Dockerfile \
  --platform "linux/arm64" \
  --build-context idr-pipeline="$SCRIPT_DIR/packages/$(readlink "$SCRIPT_DIR"/packages/idr-pipeline)" \
  --tag "bfd-platform-idr-bfd-validator" "$@"
