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
  echo
  echo "Docker container for running Synthea generation:"
  echo
  echo "-n : Specifies the number of beneficiaries to generate. Defaults to 100"
  echo "-f : Specifies the number of months in the future to generate claim lines. Defaults to 0"
  echo "-v : Specifies whether the Synthea executable should log to STDOUT or if its logs should be discared. Defaults to unset (non-verbose)"
  echo "-h : Shows this help text"
  exit 0
}

while getopts ":n:f:vh" opt; do
  case $opt in
    n)
      if ! [[ "$OPTARG" =~ $NUM_REGEX ]]; then
        echo "ERROR, non-numeric specified ($OPTARG)...exiting" >&2
        exit 1
      fi
      num_generated_benes="$OPTARG"
      ;;
    f)
      if ! [[ "$OPTARG" =~ $NUM_REGEX ]]; then
        echo "ERROR, non-numeric future month value specified ($OPTARG)...exiting" >&2
        exit 1
      fi
      num_future_months="$OPTARG"
      if [[ "$OPTARG" -gt 0 ]]; then
        generate_future="true"
      fi
      ;;
    v)
      export VERBOSE_SYNTHEA_LOGS="true"
      ;;
    h)
      help
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
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
starting_datetime=$(date '+%F_%H-%M-%S')

echo "Running Synthea generation with $num_generated_benes benes and $num_future_months future months..."
{
  python3 prepare-and-run-synthea.py \
    "${BFD_END_STATE_PROPERTIES}" \
    "${TARGET_SYNTHEA_DIR}" \
    "${num_generated_benes}" \
    "${num_future_months}" 2>&1 &&
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
if [ "$generate_future" == 'true' ]; then
  mv "$TARGET_SYNTHEA_DIR/logs/split_future_claims.latest.log" "$TARGET_SYNTHEA_DIR/logs/split_future_claims-$starting_datetime.log"
fi
mv "$TARGET_SYNTHEA_DIR"/synthea-*.log "$TARGET_SYNTHEA_DIR/logs/synthea-$starting_datetime.log"

echo "Copying original end_state.properties into output/bfd..."
cp "$TARGET_SYNTHEA_DIR/end_state.properties" "$TARGET_SYNTHEA_DIR/output/bfd/end_state.properties.orig"

generated_output_dir="generated-$starting_datetime"
echo "Moving output to $generated_output_dir sub-directory..."
mkdir -p "$TARGET_SYNTHEA_DIR/output/$generated_output_dir"
(
  cd "$TARGET_SYNTHEA_DIR/output" && \
  mv ./* "$generated_output_dir" 2>/dev/null || true # mv will complain about moving the sub-dir, but will move all other files/dirs
)
