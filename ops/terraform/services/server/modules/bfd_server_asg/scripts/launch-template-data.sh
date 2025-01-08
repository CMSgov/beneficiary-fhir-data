#!/usr/bin/env sh
#######################################
# Fetch the remote launch template version
# Returned json object includes a "latest_version" as
# - a string when the launch template exists
# - "null" when the launch template does not yet exist
# Arguments:
#   $1 maps to desired environment, used in templated "bfd-${1}-fhir", e.g. "bfd-2558-test-fhir"
#######################################
set -eu
BFD_ENV="$1"

aws ec2 describe-launch-templates --launch-template-names "bfd-${BFD_ENV}-fhir" 2>/dev/null | yq --output-format=json '. | {"latest_version": .LaunchTemplates[0].LatestVersionNumber | tostring // null}'
