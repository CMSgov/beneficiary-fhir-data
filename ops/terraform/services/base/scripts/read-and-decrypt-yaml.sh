#!/usr/bin/env bash
# This is intended for use in local development as well as by automated
# terraform work flows that incorporate an external data source:
# https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/data_source
#
# Arguments:
#   $1 - the environment corresponding to the .yaml file to decrypt
#   $2 (Optional) - the ARN of the CMK of the corresponding key used to encrypt the target .yaml
#                   file, if unspecified the CMK is looked up based upon the environment

set -Eeo pipefail

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

MODULE_DIR="$(dirname "$SCRIPT_DIR")"
readonly MODULE_DIR

REPO_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel)"
readonly REPO_ROOT

BFD_SEED_ENV="$1"
readonly BFD_SEED_ENV

YAML_FILE="${MODULE_DIR}/values/${BFD_SEED_ENV}.yaml"
readonly YAML_FILE

CMK_ARN_OVERRIDE="$2"
readonly CMK_ARN_OVERRIDE

CMK_LOOKUP="$(
  # Only attempt to lookup the CMK if the override is undefined
  if [[ -z "$CMK_ARN_OVERRIDE" ]]; then
    aws kms describe-key \
      --key-id "alias/bfd-${BFD_SEED_ENV}-config-cmk" \
      --query KeyMetadata.Arn \
      --output text
  fi
)"
readonly CMK_LOOKUP

CMK_ARN="${CMK_ARN_OVERRIDE:-"$CMK_LOOKUP"}"
readonly CMK_ARN

if test -f "$YAML_FILE"; then
  untemplated_json="$(kotlin "${REPO_ROOT}/apps/utils/cipher/cipher.main.kts" \
    --key "$CMK_ARN" cat "$YAML_FILE" | yq 'map_values(tostring)' --output-format=json)"

  # Using eval we can exploit heredoc to template any values (like ${env}) within the final JSON,
  # provided that the templated variable is defined within the environment or within this script.
  # This enables ephemeral configuration support.
  eval "cat <<EOF
$untemplated_json
EOF" 2>/dev/null

else
  echo '{}'
fi
