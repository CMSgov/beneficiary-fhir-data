#!/bin/bash
# This script reads log events from stdin, buffers them temporarily, and uploads them to AWS
# CloudWatch Logs in batches every second, on exit, and on SIGTERM/SIGINT to avoid being throttled.
# The script will send the buffered log messages to the specified CloudWatch log group ($log_group)
# and log stream ($log_stream). They must exist prior to this script being invoked.
# Usage:
#   echo "Log message 1" | ./stdout_to_cwlogs.sh "log_group" "log_stream"
#   tofu plan -no-color | ./stdout_to_cwlogs.sh "log_group" "log_stream"
#   CLOUDWATCH_LOG_GROUP="..." CLOUDWATCH_LOG_STREAM="..." tofu plan -no-color | ./stdout_to_cwlogs.sh

set -Eeou pipefail

cloudwatch_log_group="${1:-$CLOUDWATCH_LOG_GROUP}"
readonly cloudwatch_log_group

cloudwatch_log_stream="${2:-$CLOUDWATCH_LOG_STREAM}"
readonly cloudwatch_log_stream

event_buffer=""
current_time=0
last_echo_time=0

log_events() {
  aws_log_events="$(
    echo "$event_buffer" | jq -R 'split("\u001D") |
    map(select(. != "")) |
    map(
      split("\u001E") |
        {
          "timestamp": (.[0] | tonumber),
          "message": (.[1] | tostring)
        }
    ) |
    map(select(.message != "" and .message != null))'
  )"
  if [[ $aws_log_events != "[]" ]]; then
    aws logs put-log-events --log-group-name "$cloudwatch_log_group" \
      --log-stream-name "$cloudwatch_log_stream" \
      --log-events "$aws_log_events" 1>/dev/null
  fi
  event_buffer=""
  last_echo_time=$current_time
}

cleanup() {
  log_events
  exit
}

trap 'cleanup' SIGTERM
trap 'cleanup' SIGINT

while IFS= read -r line; do
  current_time=$(date +%s%3N)
  # Append current time and line to event_buffer with \x1E (record separator) for timestamp-message
  # split, and \x1D (group separator) to separate individual events for later JSON parsing
  log_event=$(printf "%s\x1E%s\x1D" "$current_time" "$line")
  event_buffer+="$log_event"

  if (((current_time - last_echo_time) >= 1000)); then
    log_events
  fi
done

cleanup
