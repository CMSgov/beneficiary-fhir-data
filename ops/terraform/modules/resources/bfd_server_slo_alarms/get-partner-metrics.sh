#!/bin/bash

set -eou pipefail

metric_name="${1}"
readonly metric_name

metric_namespace="${2}"
readonly metric_namespace

partners_regex_json="${3}"
readonly partners_regex_json

# Reads the partners regex json object's keys into a bash array
IFS=$'\n' read -r -d '' -a partners < <(
  set -o pipefail
  jq -r 'keys[]' <<<"${partners_regex_json}" && printf '\0'
)

partner_metrics=()
for partner in "${partners[@]}"; do
  partner_regex=$(jq -r ".[\"${partner}\"]" <<<"${partners_regex_json}")
  applicable_dimensions=$(
    aws cloudwatch list-metrics --namespace "${metric_namespace}" --metric-name "${metric_name}" \
      | jq -c "[
        .Metrics[] 
          | .Dimensions[] 
          | select(.Name == \"client_ssl\") 
          | select(.Value | test(\"${partner_regex}\")) 
          | .Value
      ]"
  )

  partner_dimensioned_metrics=$(
    jq -c "{
      ${partner}: [
        .[] | {
          name: \"${metric_name}\", 
          namespace: \"${metric_namespace}\", 
          dimension: .
        }
      ]
    }" <<<"${applicable_dimensions}"
  )
  partner_metrics+=("${partner_dimensioned_metrics}")
done

jq -s "." <<<"${partner_metrics[@]}"
