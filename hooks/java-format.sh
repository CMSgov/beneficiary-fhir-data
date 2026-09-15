#!/usr/bin/env bash
set -e

java -jar "$(git rev-parse --show-toplevel)/hooks/google-java-format-1.36.1-all-deps.jar" -r --skip-reflowing-long-strings "$@"
