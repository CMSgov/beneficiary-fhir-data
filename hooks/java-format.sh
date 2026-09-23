#!/usr/bin/env bash
set -euo pipefail

jar_path="$(git rev-parse --show-toplevel)/hooks/google-java-format.jar"
java -jar "$jar_path" -r --skip-reflowing-long-strings "$@"
