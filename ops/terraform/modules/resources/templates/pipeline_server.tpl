#!/bin/bash
set -e

exec > >(tee -a /var/log/user_data.log 2>&1)

git clone https://github.com/CMSgov/beneficiary-fhir-data.git
cd beneficiary-fhir-data/ops/ansible/playbooks-ccs/

aws s3 cp s3://bfd-mgmt-admin-${accountId}/ansible/vault.password .

# The extra_vars.json file from the previous build step contains a few incorrect values
# and needs to get trimmed down to the following
cat <<EOF >> extra_vars.json
{
    "env":"${env}",
    "data_pipeline_jar":"/bluebutton-data-pipeline/bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar"
}
EOF

ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=vault.password --tags "post-ami" launch_bfd-pipeline.yml

rm vault.password