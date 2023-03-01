#!/usr/bin/env bash
# This is a simple script that assists operators with editing values stored
# in encrypted yaml files. As of this writing, this targets the ansible-vault
# encryption mechanism, and depends on access to the appropriate SSM Parameter
# Store hierarchy to resolve the password.
# This is intended for use in local development.
#
# Globals:
#   SCRIPTS_DIR the parent directory of this script
#   MODULE_DIR the parent directory of SCRIPTS_DIR, assumed to be the "base_config" module
#   EYAML_FILE formatted "$1" positional argument for relative file resolution
#
# Arguments:
#   $1 is the desired eyaml file that can be entered with or without qualifying path or
#      suffix. `values/foo.eyaml`, `values/foo`, `foo.eyaml` and `foo` are equivalent.
SCRIPTS_DIR=$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")
MODULE_DIR="$(dirname "$SCRIPTS_DIR")"
EYAML_FILE="$(basename "$1" | sed 's/\.eyaml//').eyaml"

# open a desired eyaml file from a temporary, decrypted, user-owned location on disk
# in the user's desired `$EDITOR`
ansible-vault edit "--vault-password-file=$SCRIPTS_DIR/get-vault-password.sh" \
  "$MODULE_DIR/values/${EYAML_FILE}"
