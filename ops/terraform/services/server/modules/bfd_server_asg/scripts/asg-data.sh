#!/usr/bin/env sh
#######################################
# Fetch the even/odd autoscaling group launch template versions, instance and warm_pool counts.
# Returned json object includes...
# "odd_launch_template_version", "odd_desired_capacity", "odd_min_size", "odd_warmpool_min_size",
# "even_launch_template_version", "even_desired_capacity", "even_min_size", and
# "even_warmpool_min_size" as strings.
# This is intended for use in terraform as an external data resource.
# Requires yq and awscli
#
# Example usage:
# `./asg-data.sh 2558-test` might yield the following for an existing ASG:
#
#  {
#    "odd_launch_template_version": "9",
#    "odd_desired_capacity": "3",
#    "odd_min_size": "3",
#    "odd_warmpool_min_size": "3",
#    "even_launch_template_version": "10",
#    "even_desired_capacity": "0",
#    "even_min_size": "0",
#    "even_warmpool_min_size": "0"
#  }
#
# `./asg-data.sh foo` yields the following for a non-existent ASG:
#
#  {
# .  "odd_launch_template_version": "null",
#    "odd_desired_capacity": "0",
#    "odd_min_size": "0",
#    "odd_warmpool_min_size": "0",
# .  "even_launch_template_version": "null",
#    "even_desired_capacity": "0",
#    "even_min_size": "0",
#    "even_warmpool_min_size": "0"
#  }
#
# Arguments:
#   $1 maps to desired environment, used in templated "bfd-${1}-fhir", e.g. "bfd-2558-test-fhir"
#######################################
set -eu
BFD_ENV="$1"

# shellcheck disable=SC2155
export odd="$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "bfd-${BFD_ENV}-fhir-odd" \
    --output json --query 'AutoScalingGroups[0]')"

# shellcheck disable=SC2155
export even="$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "bfd-${BFD_ENV}-fhir-even" \
    --output json --query 'AutoScalingGroups[0]')"

yq  --output-format=json --null-input '{
  "odd_launch_template_version": env(odd) | .LaunchTemplate.Version | tostring,
  "odd_desired_capacity": env(odd) | .DesiredCapacity // 0 | tostring,
  "odd_min_size": env(odd) | .MinSize // 0 | tostring,
  "odd_warmpool_min_size": env(odd) | .WarmPoolConfiguration.MinSize // 0 | tostring,
  "even_launch_template_version": env(even) | .LaunchTemplate.Version | tostring,
  "even_desired_capacity": env(even) | .DesiredCapacity // 0 | tostring,
  "even_min_size": env(even) | .MinSize // 0 | tostring,
  "even_warmpool_min_size": env(even) | .WarmPoolConfiguration.MinSize // 0 | tostring
}'
