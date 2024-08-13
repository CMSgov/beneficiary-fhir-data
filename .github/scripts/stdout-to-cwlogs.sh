#!/bin/bash
# Helper script intended to be used in GHA steps which may log _dynamic_ sensitive data and are thus
# unable to be masked using GHA's built-in _static_ masking. By piping the stdout from the command
# that emits sensitive log output to this script the log output can instead be logged to CloudWatch.
# Requires the Log Group and Log Stream to exist prior to use, as well as a valid AWS session with
# permission to PutLogEvents to the Log Group and Stream.
# Environment Variables:
#   CLOUDWATCH_LOG_GROUP: The Log Group to log to
#   CLOUDWATCH_LOG_STREAM: The Log Stream to log to
# Usage Notes:
#   If this script is used in GHA, the companion Composite Action "await-cw-logging" should be
#   appended to the end of the Workflow/Action as an "always" step to ensure that orphaned
#   background AWS PutLogEvents processes are able to complete without being cleaned up

set -eou pipefail

# Ensure that the CLOUDWATCH_LOG_GROUP environment variable is set
trimmed_log_group="$(echo "$CLOUDWATCH_LOG_GROUP" | tr -d '[:space:]')"
if [[ -z $trimmed_log_group ]]; then
  echo "CLOUDWATCH_LOG_GROUP must not be an empty string or whitespace"
  exit 1
fi

# Ensure that the CLOUDWATCH_LOG_STREAM environment variable is set
trimmed_log_stream="$(echo "$CLOUDWATCH_LOG_STREAM" | tr -d '[:space:]')"
if [[ -z $trimmed_log_stream ]]; then
  echo "CLOUDWATCH_LOG_STREAM must not be an empty string or whitespace"
  exit 1
fi

while read -r line; do
  log_event="$(
    jq -n \
      --arg unix_ts "$(date +%s%3N)" \
      --arg line "$line" \
      '[{ "timestamp": ($unix_ts | tonumber), "message": ($line | tostring) }]'
  )"
  # Runs in a background subshell to avoid slowing down the process that is piping to this script
  # while trying to put the log output to CloudWatch. Errors are ignored and all output is
  # redirected to /dev/null
  (aws logs put-log-events --log-group-name "$trimmed_log_group" \
    --log-stream-name "$trimmed_log_stream" \
    --log-events "$log_event" &>/dev/null || true) &
done
