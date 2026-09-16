#!/usr/bin/env bash
set -euo pipefail

echo "file $1"
xmllint "$1" --format -o "$1"
