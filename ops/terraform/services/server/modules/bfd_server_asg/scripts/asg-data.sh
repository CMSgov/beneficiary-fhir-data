#!/usr/bin/env sh
#######################################
# Fetch an autoscaling group's instances.
# Returns a well-formatted json object instance "count" and "instance_launch_templates" as strings.
# This is intended for use in terraform as an external data resource.
# Requires yq and awscli
#
# Example usage:
# `./asg-data.sh 2558-test` might yield the following for an existing ASG:
#   {
#     "count": "3",
#     "instance_launch_templates": "[\"3\",\"3\",\"3\"]"
#   }
#
# `./asg-data.sh foo` yields the following for a non-existent ASG:
#   {
#     "count": "0",
#     "instance_launch_templates": "[]"
#   }
#
# Arguments:
#   $1 maps to desired environment, used in templated "bfd-${1}-fhir", e.g. "bfd-2558-test-fhir"
#######################################
set -eu
BFD_ENV="$1"

# shellcheck disable=SC2155
export asg="$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "bfd-${BFD_ENV}-fhir" \
    --output json --query 'AutoScalingGroups[0]')"

yq  --output-format=json --null-input '{
  "count": env(asg) | .Instances | length | tostring,
  "instance_launch_templates": env(asg) | [.Instances[].LaunchTemplate.Version] | @json
}'
