#!/usr/bin/env bash
set -eo pipefail

TARGET_SYNTHEA_DIR=$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")
readonly TARGET_SYNTHEA_DIR

BFD_END_STATE_PROPERTIES="$TARGET_SYNTHEA_DIR/end_state.properties"
readonly BFD_END_STATE_PROPERTIES

NUM_REGEX="^[0-9]+$"
readonly NUM_REGEX

num_generated_benes=100
generate_future="false"
num_future_months=0

help() {
  echo "Help text"
  exit 1
}

args=$(getopt -l "num:num_future_months:help" -o "n:f:h" -- "$@")

while [ $# -ge 1 ]; do
  case "$1" in
    --)
      # No more options left.
      shift
      break
      ;;
    -n | --num)
      if ! [[ $2 =~ $NUM_REGEX ]]; then
        echo "ERROR, non-numeric specified ($2)...exiting" >&2
        exit 1
      fi
      num_generated_benes="$2"
      shift
      ;;
    -f | --num_future_months)
      if ! [[ $2 =~ $NUM_REGEX ]]; then
        echo "ERROR, non-numeric future month value specified ($2)...exiting" >&2
        exit 1
      fi
      num_future_months="$2"
      if [[ $2 -gt 0 ]]; then
        generate_future="true"
      fi
      shift
      ;;
    -h | --help)
      help
      ;;
  esac
  shift
done

echo "Checking if the output bind-mount directory is empty before generation..."
(
  cd "$TARGET_SYNTHEA_DIR/output"
  shopt -s nullglob 
  files=( * .* )
  if (( ${#files[@]} != 2 )); then
      # contents of files array is (. ..)
      echo "The output bind-mount directory is not empty; ensure the directory is empty before running"
      exit 1
  fi
)

echo "Preparing to run Synthea generation..."

echo "Running Synthea generation with $num_generated_benes benes and $num_future_months future months..."
{
  python3 prepare-and-run-synthea.py \
    "${BFD_END_STATE_PROPERTIES}" \
    "${TARGET_SYNTHEA_DIR}" \
    "${num_generated_benes}" \
    "${num_future_months}" &> "$TARGET_SYNTHEA_DIR/logs/prepare_and_run_synthea-$(date '+%F_%H:%M:%S').log" && \
  echo "Synthea generation finished, synthetic data should be available in the bind mounted output directory"
} || {
  echo "Synthea generation failed to complete. View the logs in the bind-mounted logs directory for more information"
}
mv "$TARGET_SYNTHEA_DIR"/synthea-*.log "$TARGET_SYNTHEA_DIR/logs/national_script-$(date '+%F_%H:%M:%S').log"
