#!/bin/bash

set -e

exec > >(tee -a /var/log/user_data.log 2>&1)

aws s3 cp s3://${bucket}/${env}/VAULT_PW .
aws s3 cp s3://${bucket}/${env}/REPO_URI .

ansible-playbook --vault-password-file=./VAULT_PW \
  -i "localhost" \
  -e "env=${env}" \
  -e "repo=$(cat REPO_URI)" \
  /var/pyapps/hhs_o_server/env_config.yml

rm VAULT_PW REPO_URI
