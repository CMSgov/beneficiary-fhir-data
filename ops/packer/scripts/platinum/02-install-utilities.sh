#!/usr/bin/env sh

# Copy the RBAC utility script
cat <<"EOS" | sudo tee /usr/local/bin/permit-user-access
set -e

###########################################################
# This script serves to grant SSH and sudo access to users
# whose properties and permissions are stored in SSM.
###########################################################

ENV=$1

# Return users from SSM, filtering results to include only the relevant user data
# based on their respective EUA codes. AWS Cli doesn't support this type of filtering
# natively, so we do so via jq.
get_ssm_users() {
  user_ssm="/bfd/mgmt/common/sensitive/user/"
  # SSM path ends with a valid, alphanumeric EUA code
  eua_identifier_pattern="[A-Z0-9]{4}$"
  users=$(aws ssm get-parameters-by-path \
    --with-decryption \
    --path $user_ssm \
    --recursive \
    --region us-east-1 \
    --query 'Parameters' \
    | jq -r "map(select(.Name | test(\"^${user_ssm}${eua_identifier_pattern}\")))")
  echo "$users"
}

# Grants SSH access to the specified user, if applicable, based
# on their environment permissions stored in SSM.
permit_user_ssh() {
  user=$1

  # Skip the user if they do not have ssh access to the current environment
  has_env_access=$(echo "$user" | jq -r ".env_access | any(. == \"${ENV}\")")
  if ! $has_env_access ; then
    return
  fi

  user_name=$(echo "$user" | jq -r '.user_name')
  ssh_public_key=$(echo "$user" | jq -r '.ssh_public_key')
  user_ssh_dir="/home/$user_name/.ssh"

  id -u "$user_name" &>/dev/null || adduser --comment '' "$user_name"
  mkdir -p "$user_ssh_dir"
  echo "$ssh_public_key" > "$user_ssh_dir"/authorized_keys
  chown -R "$user_name":"$user_name" "$user_ssh_dir"
  chmod 700 "$user_ssh_dir"
  chmod 600 "$user_ssh_dir"/authorized_keys
}

# Grants sudo access to the specified user, if applicable, based
# on their environment permissions stored in SSM.
permit_user_sudo() {
  user=$1
  user_name=$(echo "$user" | jq -r '.user_name')

  # Skip the user if they do not have sudo access to the current environment
  has_sudo_env_access=$(echo "$user" | jq -r ".env_privileged_access | any(. == \"${ENV}\")")
  if ! $has_sudo_env_access ; then
    return
  fi

  cat <<EOF >> /etc/sudoers
${user_name} ALL=(ALL) NOPASSWD: ALL
EOF
}

# Fetch the privileged users from SSM and grant them SSH or sudo access, if
# applicable for the current environment.
add_ssh_users_and_sudoers() {
  users=$(get_ssm_users)
  for encoded_user in $(echo "$users" | jq -r '.[].Value | @base64'); do
    _jq() {
      user=$(echo "$encoded_user" | base64 --decode | jq -r "${1}")
      permit_user_ssh "$user"
      permit_user_sudo "$user"
    }
    _jq '.'
  done
}

add_ssh_users_and_sudoers
EOS

# Add execute permissions to RBAC script
sudo chmod 0644 /usr/local/bin/permit-user-access
