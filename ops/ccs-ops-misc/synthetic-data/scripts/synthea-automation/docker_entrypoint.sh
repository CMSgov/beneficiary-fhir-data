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

echo "Checking if the output directory is empty before generation..."
(
  cd "$TARGET_SYNTHEA_DIR/output"
  shopt -s nullglob
  files=(* .*)
  if ((${#files[@]} != 2)); then
    # contents of files array is (. ..)
    echo "The output directory is not empty; ensure the directory is empty before running"
    exit 1
  fi
)

echo "Preparing to run Synthea generation..."
starting_datetime=$(date '+%F_%H:%M:%S')

echo "Running Synthea generation with $num_generated_benes benes and $num_future_months future months..."
{
  python3 prepare-and-run-synthea.py \
    "${BFD_END_STATE_PROPERTIES}" \
    "${TARGET_SYNTHEA_DIR}" \
    "${num_generated_benes}" \
    "${num_future_months}" 2>&1 | tee -a "$TARGET_SYNTHEA_DIR/logs/prepare_and_run_synthea.latest.log" &&
  echo "Synthea generation finished, generated synthetic data can be found in the output directory"
} || {
  echo "Synthea generation failed to complete. View the logs in the logs directory for more information"
}

if [ "$generate_future" == 'true' ]; then
  echo "Generating future months was specified, splitting future claims to allow for proper loading..."
  {
    python3 split-future-claims.py "$TARGET_SYNTHEA_DIR" \
      2>&1 | tee -a "$TARGET_SYNTHEA_DIR/logs/split_future_claims.latest.log" &&
    echo "Future claims splitting succeeded, view the split_future_claims-$starting_datetime.log for more information"
  } || {
    echo "Future claims splitting failed, view the split_future_claims-$starting_datetime.log for more information"
  }
fi

echo "Renaming *.latest.log logs to *$starting_datetime.log..."
mv "$TARGET_SYNTHEA_DIR/logs/prepare_and_run_synthea.latest.log" "$TARGET_SYNTHEA_DIR/logs/prepare_and_run_synthea-$starting_datetime.log"
if [ "$generate_future" == 'true' ]; then
  mv "$TARGET_SYNTHEA_DIR/logs/split_future_claims.latest.log" "$TARGET_SYNTHEA_DIR/logs/split_future_claims-$starting_datetime.log"
fi
mv "$TARGET_SYNTHEA_DIR"/synthea-*.log "$TARGET_SYNTHEA_DIR/logs/synthea-$starting_datetime.log"
