#!/usr/bin/env bash
# This is very a simple script that assists with reading and decrypting
# encrypted yaml files. As of this writing, this targets the ansible-vault
# encryption mechanism, and depends on access to the appropriate SSM
# Parameter Store hierarchy to resolve the password.
#
# This is intended for use in local development as well as by automated
# terraform work flows that incorporate an external data source:
# https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/data_source
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


if test -f "values/${EYAML_FILE}"; then
    # temporarily store vault password
    aws ssm get-parameter \
        --region us-east-1 \
        --output text \
        --with-decryption \
        --query 'Parameter.Value' \
        --name "$SSM_VAULT_PASSWORD_PATH" > vault.password

    # decrypt desired eyaml file to stdout, reformat as JSON, and explicitly cast all values to strings
    ansible-vault decrypt --vault-password-file vault.password "values/${EYAML_FILE}" --output - | yq eval -o=j | jq 'with_entries(.value |= tostring)'
else
    echo '{}'
fi

#######################################
# Ensure temporary ansible-vault password file is destroyed.
# To be called as the action in `trap [action] [signal]`.
#######################################
cleanup() {
    rm -rf vault.password
}
trap cleanup EXIT
