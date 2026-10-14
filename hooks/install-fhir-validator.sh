#!/usr/bin/env bash
set -euo pipefail

VALIDATOR_CLI_VERSION="6.7.10"
jar_path="$(git rev-parse --show-toplevel)/apps/bfd-model-idr/validator_cli.jar"
curl -L "https://github.com/hapifhir/org.hl7.fhir.core/releases/download/$VALIDATOR_CLI_VERSION/validator_cli.jar" >"$jar_path"
