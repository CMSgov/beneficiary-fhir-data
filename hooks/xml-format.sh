#!/usr/bin/env bash
set -e

echo "file $1"
xmllint "$1" --format -o "$1"
