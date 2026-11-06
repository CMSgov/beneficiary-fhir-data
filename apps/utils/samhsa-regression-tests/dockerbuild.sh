#!/usr/bin/env bash

set -Eeou pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT

DOCKER_BUILDKIT=1 podman buildx build "$REPO_ROOT" \
	--file "$REPO_ROOT"/apps/utils/samhsa-regression-tests/Dockerfile \
	--platform "linux/arm64" \
	--tag "bfd-platform-samhsa-regression" "$@"
