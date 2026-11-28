#!/usr/bin/env bash
set -Eeuo pipefail

GOOGLE_JAVA_FORMAT_VERSION="1.36.1"
jar_path="$(git rev-parse --show-toplevel)/hooks/google-java-format.jar"

if [ -f "$jar_path" ]; then
	if java -jar "$jar_path" -version 2>&1 | grep -q "Version $GOOGLE_JAVA_FORMAT_VERSION"; then
		exit 0
	fi
fi

curl -L "https://github.com/google/google-java-format/releases/download/v${GOOGLE_JAVA_FORMAT_VERSION}/google-java-format-${GOOGLE_JAVA_FORMAT_VERSION}-all-deps.jar" >"$jar_path"
