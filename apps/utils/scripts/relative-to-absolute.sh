#!/usr/bin/env bash
set -Eeuo pipefail

for var in "$@"; do
	echo "$PWD/$var"
done
