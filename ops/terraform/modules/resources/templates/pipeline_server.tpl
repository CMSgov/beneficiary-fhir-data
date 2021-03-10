#!/bin/bash
set -e

# add a timestamp to this scripts log output and redirect to both console and logfile
exec > >(
	while read line; do
	    echo $(date +"%Y-%m-%d %H:%M:%S")" - $${line}" | tee -a /var/log/user_data.log 2>&1
	done
)

# Extend gold image defined root partition with all available free space
sudo growpart /dev/nvme0n1 2
sudo pvresize /dev/nvme0n1p2
sudo lvextend -l +100%FREE /dev/VolGroup00/rootVol
sudo xfs_growfs /


# Checkout the latest ops files from master
git clone https://github.com/CMSgov/beneficiary-fhir-data.git --branch "$gitBranchName" --single-branch

# Load the secret key from the keyfile identified in vault.keyfile.id (keyfiles are stored in s3)
cd beneficiary-fhir-data/ops/ansible/playbooks-ccs/
keyfile=$(cat vault.keyfile.id) # the keyfile that was used to encrypt this branches files
tfkey="$(aws s3 cp s3://bfd-mgmt-admin-${accountId}/ansible/${keyfile} -)"

# The extra_vars.json file from the previous build step contains a few incorrect values
# and needs to get trimmed down to the following
cat <<EOF >> extra_vars.json
{
    "env":"${env}",
    "data_pipeline_jar":"/bluebutton-data-pipeline/bfd-pipeline-app-1.0.0-SNAPSHOT-capsule-fat.jar"
}
EOF

# Provision
ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=<(echo "$tfkey") --tags "post-ami" launch_bfd-pipeline.yml
