#!/usr/bin/env bash
# This is very a simple script that assists with reading and decrypting
# encrypted yaml files. As of this writing, this targets the ansible-vault
# encryption mechanism, and depends on access to the appropriate SSM
# Parameter Store hierarchy to resolve the password.
# This is intended for use in local development as well as by automated
# terraform work flows that incorporate an external data source:
# https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/data_source
#
# Globals:
#   SCRIPTS_DIR the parent directory of this script
#   MODULE_DIR the parent directory of SCRIPTS_DIR, assumed to be the "base_config" module
#   EYAML_FILE formatted "$1" positional argument for relative file resolution
#   SSM_VAULT_PASSWORD_PATH SSM Hierarchy containing the ansible vault password
#
# Arguments:
#   $1 is the desired eyaml file that can be entered with or without qualifying path or
#      suffix. `values/foo.eyaml`, `values/foo`, `foo.eyaml` and `foo` are equivalent.
SCRIPTS_DIR=$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")
MODULE_DIR="$(dirname "$SCRIPTS_DIR")"
EYAML_FILE="$(basename "$1" | sed 's/\.eyaml//').eyaml"
SSM_VAULT_PASSWORD_PATH="/bfd/mgmt/jenkins/sensitive/ansible_vault_password"

# decrypt desired eyaml file to stdout, reformat as JSON, and explicitly cast all values to strings
ansible-vault decrypt --vault-password-file=<(
  aws ssm get-parameter \
    --region us-east-1 \
    --output text \
    --with-decryption \
    --query 'Parameter.Value' \
    --name "$SSM_VAULT_PASSWORD_PATH"
) "$MODULE_DIR/values/${EYAML_FILE}" --output - |
  yq eval -o=j |
  jq 'with_entries(.value |= tostring)'
