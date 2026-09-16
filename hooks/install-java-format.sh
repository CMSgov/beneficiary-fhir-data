#!/usr/bin/env bash
set -euo pipefail

GOOGLE_JAVA_FORMAT_VERSION="1.36.1"
jar_path="$(git rev-parse --show-toplevel)/hooks/google-java-format.jar"
curl -L "https://github.com/google/google-java-format/releases/download/v${GOOGLE_JAVA_FORMAT_VERSION}/google-java-format-${GOOGLE_JAVA_FORMAT_VERSION}-all-deps.jar" >"$jar_path"
