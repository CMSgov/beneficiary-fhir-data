#!/bin/bash
# shellcheck disable=SC2154

set -e

# add a timestamp to this scripts log output and redirect to both console and logfile
exec > >(
    # shellcheck disable=SC2034
	while read line; do
	    echo "$(date +"%Y-%m-%d %H:%M:%S")"" - $${line}" | tee -a /var/log/user_data.log 2>&1
	done
)

cd /beneficiary-fhir-data/ops/ansible/playbooks-ccs/

aws ssm get-parameters-by-path \
    --with-decryption \
    --path "/bfd/${env}/server/" \
    --recursive \
    --region us-east-1 \
    --query 'Parameters[? !contains(@.Name, `client_certificates`)]' \
    | jq 'map({(.Name|split("/")|last): .Value})|add' > server_vars.json

aws ssm get-parameters-by-path \
    --path "/bfd/${env}/server/nonsensitive/client_certificates/" \
    --recursive --region us-east-1 \
    --query 'Parameters' | jq '.[] | {"alias": (.Name|split("/")|last), "certificate": .Value}' \
    | jq -s '{ "client_certificates": . }' > client_certificates.json


aws ssm get-parameters-by-path \
    --path "/bfd/${env}/common/nonsensitive/" \
    --recursive \
    --region us-east-1 \
    --query 'Parameters' | jq 'map({(.Name|split("/")[5]): .Value})|add' > common_vars.json

cat <<EOF > extra_vars.json
{
  "data_server_new_relic_environment": "{{ env_name_std }}",
  "db_url": "${reader_endpoint}",
  "env": "${env}",
  "launch_lifecycle_hook": "${launch_lifecycle_hook}"
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

bash /usr/local/bin/permit-user-access "${seed_env}"

# Disable the JVM's indefinite DNS caching to aid in proper RDS node selection per-connection
# See https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/jvm-ttl-dns.html
mkdir -p "$JAVA_HOME/conf/security"
echo "networkaddress.cache.ttl=5" > "$JAVA_HOME/conf/security/java.security"
