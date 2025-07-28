#!/usr/bin/env bash

set -Eeou pipefail

# Get the latest tag on the current branch
bfd_version=$(git describe --tags --abbrev=0)

# If there's no tag on the current branch, find the closest tag
if [[ -z "$bfd_version" ]]; then
  bfd_version=$(git log -n 1 --pretty=format:'%d' | sed 's/, //g' | sed 's/ //g')
fi

echo "{\"bfd_version\": \"$bfd_version\"}"
