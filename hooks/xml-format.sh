#!/usr/bin/env bash
set -euo pipefail

xmllint "$1" --format -o "$1"
