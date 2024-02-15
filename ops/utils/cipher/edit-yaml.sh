#!/usr/bin/env bash
# This is a simple script that assists operators with editing values stored
# in encrypted yaml files.
# This is intended for use in local development
#
# Arguments:
#   $1 - the environment corresponding to the .yaml file to edit
#   $2 (Optional) - the ARN of the CMK of the corresponding key used to encrypt the target .yaml
#                   file, if unspecified the CMK is looked up based upon the environment

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

CMK_KEY_OVERRIDE="$2"
readonly CMK_KEY_OVERRIDE

CMK_KEY_ALIAS="${CMK_KEY_OVERRIDE:-"alias/bfd-$BFD_SEED_ENV-config-cmk"}"
CMK_ARN="$(
  aws kms describe-key \
    --key-id "${CMK_KEY_ALIAS}" \
    --query KeyMetadata.Arn \
    --output text
)"
readonly CMK_ARN

kotlin "$REPO_ROOT/apps/utils/cipher/cipher.main.kts" --key "$CMK_ARN" edit "${YAML_FILE}"
