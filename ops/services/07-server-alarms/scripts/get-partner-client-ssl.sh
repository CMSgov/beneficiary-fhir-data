#!/bin/bash

set -eou pipefail

metric_name="${1}"
readonly metric_name

metric_namespace="${2}"
readonly metric_namespace

partners_regex_json="${3}"
readonly partners_regex_json

# Reads the partners regex json object's keys into a bash array
readarray -t partners <<<"$(
  set -o pipefail
  jq -r 'keys[]' <<<"${partners_regex_json}" && printf '\0'
)"

client_ssls=()
for partner in "${partners[@]}"; do
  partner_regex=$(jq -r ".[\"${partner}\"]" <<<"${partners_regex_json}")
  applicable_dimension=$(
    aws cloudwatch list-metrics --namespace "${metric_namespace}" --metric-name "${metric_name}" \
      | jq -c "[
        .Metrics[]
          | .Dimensions[]
          | select(.Name == \"client_ssl\")
          | select(.Value | test(\"${partner_regex}\"))
          | .Value
      ][0]"
  )

  if [[ "${applicable_dimension}" == "null" ]]; then
    continue
  fi

  partner_client_ssl=$(
    jq -c "{
      ${partner}: .
    }" <<<"${applicable_dimension}"
  )
  client_ssls+=("${partner_client_ssl}")
done

if [[ "${client_ssls+defined}" = defined ]]; then
  jq -s "add" <<<"${client_ssls[@]}"
else
  echo '{}'
fi
