#!/usr/bin/env bash
# This is a simple script that, with appropriate access to SSM, will print the Ansible vault
# password to STDOUT. This is useful because the ansible-vault CLI allows for passing executable
# scripts as the argument value for --vault-password-file. If the script prints the Ansible
# vault password to STDOUT, ansible-vault will consume the password as if it were simply a file
# containing the password in plaintext. This allows us to ensure that the vault password does not
# remain on the filesystem once an ansible-vault operation is complete, regardless of error

SSM_VAULT_PASSWORD_PATH="/bfd/mgmt/jenkins/sensitive/ansible_vault_password"

aws ssm get-parameter \
  --region us-east-1 \
  --output text \
  --with-decryption \
  --query 'Parameter.Value' \
  --name "$SSM_VAULT_PASSWORD_PATH"
