#!/bin/bash
set -e

# add a timestamp to this scripts log output and redirect to both console and logfile
exec > >(
	while read line; do
	    echo $(date +"%Y-%m-%d %H:%M:%S")" - $${line}" | tee -a /var/log/user_data.log 2>&1
	done
)

cd /beneficiary-fhir-data/ops/ansible/playbooks-ccs/

# TODO: Consider injecting ansible variables with more modern ansible versions. BFD-1890.
aws ssm get-parameters-by-path \
    --with-decryption \
    --path "/bfd/${env}/server/" \
    --recursive \
    --region us-east-1 \
    --query 'Parameters' | jq 'map({(.Name|split("/")[5]): .Value})|add' > server_vars.json

# build `data_server_ssl_client_certificates` from json values stored in SSM
jq '{data_server_ssl_client_certificates: .data_server_ssl_client_certificates_json|fromjson}' server_vars.json > client_certificates.json

aws ssm get-parameters-by-path \
    --path "/bfd/${env}/common/nonsensitive/" \
    --recursive \
    --region us-east-1 \
    --query 'Parameters' | jq 'map({(.Name|split("/")[5]): .Value})|add' > common_vars.json

cat <<EOF > extra_vars.json
{
  "data_server_appserver_jvmargs": "-Xms{{ ((ansible_memtotal_mb * 0.80) | int) - 2048 }}m -Xmx{{ ((ansible_memtotal_mb * 0.80) | int) - 2048 }}m -XX:MaxMetaspaceSize=2048m -XX:MaxMetaspaceSize=2048m -Xlog:gc*:{{ data_server_dir }}/gc.log:time,level,tags -XX:+PreserveFramePointer",
  "data_server_db_connections_max": "{{ ansible_processor_vcpus * 10 }}",
  "data_server_new_relic_app_name": "BFD Server ({{ env_name_std }})",
  "data_server_new_relic_environment": "{{ env_name_std }}",
  "data_server_tmp_dir": "{{ data_server_dir }}/tmp",
  "data_server_war": "bfd-server-war-1.0.0-SNAPSHOT.war",
  "env": "${env}"
}
EOF

mkdir -p logs

ansible-playbook --extra-vars '@server_vars.json' --extra-vars '@client_certificates.json' --extra-vars '@common_vars.json' --extra-vars '@extra_vars.json' --tags "post-ami" launch_bfd-server.yml

# Set login environment for all users:
# 1. make BFD_ENV_NAME available to all logins
# 2. change prompt color based on environment (red for prod and yellow for prod-sbx)
cat <<EOF > /etc/profile.d/set-bfd-login-env.sh
# make BFD_ENV_NAME available to all logins
export BFD_ENV_NAME="${env}"

# set prompt color based on environment (only if we are in an interactive shell)
if [[ \$- == *i* ]]; then
	case "\$BFD_ENV_NAME" in
		"prod") export PS1="[\[\033[1;31m\]\u@\h\[\033[00m\]:\[\033[1;31m\]\w\[\033[00m\]] " ;;
		"prod-sbx") export PS1="[\[\033[0;33m\]\u@\h\[\033[00m\]:\[\033[0;33m\]\w\[\033[00m\]] " ;;
	esac
fi
EOF
chmod 0644 /etc/profile.d/set-bfd-login-env.sh
