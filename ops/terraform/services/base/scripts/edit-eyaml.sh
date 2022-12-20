#!/usr/bin/env bash
# This is a simple script that assists operators with editing values stored
# in encrypted yaml files. As of this writing, this targets the ansible-vault
# encryption mechanism, and depends on access to the appropriate SSM Parameter
# Store hierarchy to resolve the password.
# This is intended for use in local development.
#
# Globals:
#   EYAML_FILE formatted "$1" positional argument for relative file resolution
#   SSM_VAULT_PASSWORD_PATH SSM Hierarchy containing the ansible vault password
#
# Arguments:
#   $1 is the desired eyaml file that can be entered with or without qualifying path or
#      suffix. `values/foo.eyaml`, `values/foo`, `foo.eyaml` and `foo` are equivalent.
EYAML_FILE="$(basename "$1" | sed 's/\.eyaml//').eyaml"
SSM_VAULT_PASSWORD_PATH="/bfd/mgmt/jenkins/sensitive/ansible_vault_password"

# temporarily store vault password
aws ssm get-parameter \
    --region us-east-1 \
    --output text \
    --with-decryption \
    --query 'Parameter.Value' \
    --name "$SSM_VAULT_PASSWORD_PATH" > vault.password

# open a desired eyaml file from a temporary, decrypted, user-owned location on disk
# in the user's desired `$EDITOR`
ansible-vault edit --vault-password-file vault.password "values/${EYAML_FILE}"

#######################################
# Ensure temporary ansible-vault password file is destroyed.
# This is to be called as the action in
# `trap [action] [signal]` as below.
#######################################
cleanup() {
    rm -rf vault.password
}
trap cleanup EXIT
