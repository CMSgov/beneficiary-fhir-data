#!/usr/bin/env bash

set -Eeou pipefail

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

DOCKER_BUILDKIT=1 podman buildx build "$SCRIPT_DIR" \
	--file "$SCRIPT_DIR"/Dockerfile \
	--platform "linux/arm64" \
	--build-context server-ng-resources="$SCRIPT_DIR/../../bfd-server-ng/src/main/resources" \
	--tag "bfd-platform-samhsa-regression" "$@"
