#!/usr/bin/env bash
# This is a simple script that assists operators with editing values stored
# in encrypted yaml files. 
# This is intended for use in local development
#
# Arguments:
#   $1 is the environment corresponding to the .yaml file to edit
#   $2 is the CMK of the corresponding key used to encrypt the target .yaml file

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

MODULE_DIR="$(dirname "$SCRIPT_DIR")"
readonly MODULE_DIR

REPO_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel)"
readonly REPO_ROOT

YAML_FILE="$MODULE_DIR/values/$1.yaml"
readonly YAML_FILE

CMK_ARN="$2"
readonly CMK_ARN

kotlin "$REPO_ROOT/apps/utils/cipher/cipher.main.kts" --key "$CMK_ARN" edit "${YAML_FILE}"
