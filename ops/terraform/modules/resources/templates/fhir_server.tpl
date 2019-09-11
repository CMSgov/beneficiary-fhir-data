#!/bin/bash
set -e

git clone https://github.com/CMSgov/beneficiary-fhir-data.git
cd beneficiary-fhir-data/ops/ansible/playbooks-ccs/

aws s3 cp s3://bfd-mgmt-admin-${accountId}/ansible/vault.password .

# The extra_vars.json file from the previous build step contains a few incorrect values
# and needs to get trimmed down to the following
echo <<EOF >> extra_vars.json
{
    "env":"${env}",
    "data_server_container_name":"wildfly-8.1.0.Final"
}
EOF

ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=vault.password --tags "post-ami" launch_bfd-server.yml

rm vault.password