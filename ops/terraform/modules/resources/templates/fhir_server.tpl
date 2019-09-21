#!/bin/bash
set -e

exec > >(tee -a /var/log/user_data.log 2>&1)

git clone https://github.com/CMSgov/beneficiary-fhir-data.git --branch ${gitBranchName} --single-branch

cd beneficiary-fhir-data/ops/ansible/playbooks-ccs/

git checkout ${gitCommitId}

aws s3 cp s3://bfd-mgmt-admin-${accountId}/ansible/vault.password .

# The extra_vars.json file from the previous build step contains a few incorrect values
# and needs to get trimmed down to the following
cat <<EOF >> extra_vars.json
{
    "env":"${env}",
    "data_server_container_name":"wildfly-8.1.0.Final",
    "data_server_version":"1.0.0-SNAPSHOT"
}
EOF

ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=vault.password --tags "post-ami" launch_bfd-server.yml

rm vault.password