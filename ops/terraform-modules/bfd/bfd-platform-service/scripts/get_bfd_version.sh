#!/usr/bin/env bash

set -Eeou pipefail

# Get the latest tag on the current branch that roughly matches the current release format
bfd_version="$(git describe --abbrev=0 --tags --match='2.*.*' "$(git rev-list --tags --max-count=1)")"

echo "{\"bfd_version\": \"$bfd_version\"}"
