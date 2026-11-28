#!/usr/bin/env bash
set -Eeuo pipefail

# Note: #6.10.2 introduced some change that causes some missing values in the transform output
# v7 may have fixed these but has some other issue that causes display values to be missing
VALIDATOR_CLI_VERSION="6.10.1"
jar_path="$(git rev-parse --show-toplevel)/apps/bfd-model-idr/validator_cli.jar"
if [ -f "$jar_path" ]; then
	if java -jar "$jar_path" -help 2>&1 | grep -q "Version $VALIDATOR_CLI_VERSION"; then
		exit 0
	fi
fi

curl -L "https://github.com/hapifhir/org.hl7.fhir.core/releases/download/$VALIDATOR_CLI_VERSION/validator_cli.jar" >"$jar_path"
