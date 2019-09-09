#!/bin/bash
set -e

git clone https://github.com/CMSgov/beneficiary-fhir-data.git
cd beneficiary-fhir-data/ops/ansible/playbooks-ccs/

# Either we need to deploy the vault password file to this instance or inject variables another way
mv ~/vault.password .

echo <<EOF >> extra_vars.json
{
    "env":"${env}"
}
EOF

ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=vault.password --skip-tags "pre-ami" launch_bfd-server.yml
