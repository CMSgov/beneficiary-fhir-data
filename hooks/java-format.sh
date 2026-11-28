#!/usr/bin/env bash
set -euo pipefail

base="$(git rev-parse --show-toplevel)"
"$base/hooks/install-java-format.sh"
jar_path="$base/hooks/google-java-format.jar"
java -jar "$jar_path" -r --skip-reflowing-long-strings "$@"
