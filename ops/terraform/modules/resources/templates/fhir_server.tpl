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
    "data_server_version":"1.0.0-SNAPSHOT"
}
EOF

ansible-playbook --extra-vars '@extra_vars.json' --vault-password-file=vault.password --tags "post-ami" launch_bfd-server.yml

rm vault.password

# Set login environment for all users:
# 1. make BFD_ENV_NAME available to all logins
# 2. change prompt color based on environment (red for prod, yellow for prod-sbx, purple for test)
cat <<EOF >> /etc/profile/profile.d/set-bfd-login-env.sh
# make BFD_ENV_NAME available to all logins
export BFD_ENV_NAME="${env}"

# set prompt color based on environment (only if we are in an interactive shell)
if [[ $- == *i* ]]; then
	case "$$BFD_ENV_NAME" in
		"prod") export PS1="[\[\033[1;31m\]\u@\h\[\033[00m\]:\[\033[1;31m\]\w\[\033[00m\]] " ;;
		"prod-sbx") export PS1="[\[\033[0;33m\]\u@\h\[\033[00m\]:\[\033[0;33m\]\w\[\033[00m\]] " ;;
		"test") export PS1="[\[\033[0;35m\]\u@\h\[\033[00m\]:\[\033[0;35m\]\w\[\033[00m\]] " ;;
	esac
fi
EOF
chmod 0644 /etc/profile/profile.d/set-bfd-login-env.sh
