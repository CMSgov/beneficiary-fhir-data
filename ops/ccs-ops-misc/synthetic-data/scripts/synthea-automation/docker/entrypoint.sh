#!/usr/bin/env bash
set -eo pipefail

TARGET_SYNTHEA_DIR="/usr/local/synthea"
readonly TARGET_SYNTHEA_DIR

BFD_STARTING_END_STATE_PROPS="/data/end_state.properties"
readonly BFD_STARTING_END_STATE_PROPS

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

# parse the args
# parse the args
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

cp "$BFD_STARTING_END_STATE_PROPS" "$TARGET_SYNTHEA_DIR/"
python3 prepare-and-run-synthea.py "${BFD_END_STATE_PROPERTIES}" "${TARGET_SYNTHEA_DIR}" "${num_generated_benes}" "${num_future_months}"
