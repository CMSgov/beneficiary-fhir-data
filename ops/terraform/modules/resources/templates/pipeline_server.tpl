#!/bin/bash
set -e

# add a timestamp to this scripts log output and redirect to both console and logfile
exec > >(
	while read line; do
	    echo $(date +"%Y-%m-%d %H:%M:%S")" - $${line}" | tee -a /var/log/user_data.log 2>&1
	done
)

git clone https://github.com/CMSgov/beneficiary-fhir-data.git --branch ${gitBranchName} --single-branch

cd beneficiary-fhir-data/ops/ansible/playbooks-ccs/

# At this time gitCommitId is a unique merge commit in Jenkins that cannot be properly
# checked out via GitHub, uncommenting this will break instance launch!
# git checkout ${gitCommitId}

aws s3 cp s3://bfd-mgmt-admin-${accountId}/ansible/vault.password .

# The extra_vars.json file from the previous build step contains a few incorrect values
# and needs to get trimmed down to the following
cat <<EOF >> extra_vars.json
{
    "env":"${env}",
    "data_pipeline_zip":"/bluebutton-data-pipeline/bfd-pipeline-app-1.0.0-SNAPSHOT.zip"
}
EOF

ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=vault.password --tags "post-ami" launch_bfd-pipeline.yml

rm vault.password
