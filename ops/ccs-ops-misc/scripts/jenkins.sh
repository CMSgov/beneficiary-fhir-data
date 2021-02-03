#!/usr/bin/env bash
# jenkins.sh - Script to help with patching/updating existing jenkins instances.
# Usage: see jenkins.sh [-h|--help]

# We make use of a lot of env vars, but most should not be changed unless you know what
# you are doing. The default .env file from kb is all you should need as the script
# will walk you through the process and prompt you for items that may need to be overidden.

ACTION="${ACTION:-}"                    # ./jenkins.sh [...] ACTION (build-ami, deploy, etc.)
GOLD_AMI="${GOLD_AMI:-}"                # defaults to latest gold itops image
SSH_USER=${SSH_USER:-''}                # you
SSH_KEY_PATH=${SSH_KEY_PATH:-''}        # your ssh key
ADMIN_S3_BUCKET=                        # dynamically set

JENKINS_ENV="${JENKINS_ENV:-}"          # script will prompt you for this
JENKINS_AZS="${JENKINS_AZS:-}"          # this is static and should not be changed (because of efs)
JENKINS_AMI="${JENKINS_AMI:-}"          # this is what we will deploy and is set after 'build-ami'
JENKINS_KEY_NAME="${JENKINS_KEY_NAME:-}"            # static and should not be changed
JENKINS_INSTANCE_SIZE="${JENKINS_INSTANCE_SIZE:-}"  # static, but could be changed if needed

JENKINS_PROD_URL="${JENKINS_PROD_URL}"  # full url you would use to visit prod from your local box
JENKINS_TEST_URL="${JENKINS_TEST_URL}"  # full url you would use to visit prod from your test box
JENKINS_URL=                            # dynamically set

JENKINS_PROD_SUBNET="${JENKINS_PROD_SUBNET:-}"  # static and should not be changed
JENKINS_TEST_SUBNET="${JENKINS_TEST_SUBNET:-}"  # static and should not be changed
JENKINS_SUBNET=                                 # dynamically set


# other vars used by the script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
required_vars=('SSH_USER' 'SSH_KEY_PATH')
required_vars+=('PROD_ADMIN_S3_BUCKET' 'TEST_ADMIN_S3_BUCKET' 'ADMIN_S3_BUCKET')
required_vars+=('JENKINS_AZS' 'JENKINS_KEY_NAME' 'JENKINS_INSTANCE_SIZE')
required_vars+=('JENKINS_PROD_SUBNET' 'JENKINS_TEST_SUBNET' 'JENKINS_SUBNET')
progname=${0##*/}
jenkins_envs=('prod' 'test')
jenkins_status=
selected_env=
target=

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ HOUSEKEEPING STUFF ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
cleanup(){
  [[ -n "$jenkins_status" ]] && jenkins_ctl 'restart'
}

error_exit(){
  cleanup
  echo -e "$1" >&2
  exit 1
}

# handle trapped signals
signal_exit(){
  case $1 in
    INT)
      error_exit "Aborting" ;;
    TERM)
      cleanup && printf "\nTerminated" >&2 && exit 0 ;;
    *)
      error_exit "\nUnknown error" ;;
  esac
}

usage() {
  echo -e "Usage: $progname [-h|--help] [upgrade|backup|build-ami|deploy]"
}

help_message() {
  cat <<- _EOF_
  $progname
  Tool to help with patching/updating our build (jenkins) servers.

  $(usage)

  Where [ACTION] is one of the following:
    upgrade
      1. Backs up the environment's /var/lib/jenkins efs to \$S3_BUCKET.
      2. Stops existing Jenkins server (will try to restart on failure).
      3. Builds a new Jenkins AMI.
      4. Deploys the new Jenkins AMI.
    backup
      1. Backs up the environment's /var/lib/jenkins efs to \$S3_BUCKET.
    build-ami
      1. Stops existing Jenkins server.
      2. Builds an updated Jenkins AMI and returns the AMI ID.
      3. Restarts existing Jenkins server.
    deploy
      1. Stops existing Jenkins server.
      2. Deploys the specified Jenkins AMI
      ** Note: the new instance will be running by default.
  
  Arguments:
  -h, --help  Display this help message and exit.

  Examples:
  # Do a full upgrade (backup, build, and deploy). Unless something has
  # gone wrong, this is what you should be doing.
  \$> source .env
  \$> ./$progname upgrade

  # Just build a new Jenkins ami using latest gold ami but do not deploy.
  # Returns the AMI ID if successful.
  \$> source .env
  \$> ./$progname build-ami
  ami-1234567890abcdef
  
  # Deploy the above Jenkins AMI ID using a specific Gold image
  \$> source .env
  \$> export GOLD_AMI=ami-amiid123456 ./$progname deploy ami-1234567890abcdef
  
_EOF_
  return
}

# Trap signals
trap "signal_exit TERM" TERM HUP
trap "signal_exit INT"  INT


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ APP METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#

# Prompt user to continue. (y/Y)
# $1 == prompt msg
c_prompt(){
  echo
  read -p "$1 " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    return
  else
    return 1
  fi
}

# Sets the working environment (prod/test), prompts user to select an ip (for backups), and sets the 
# ADMIN_S3_BUCKET accordingly
set_working_env(){
  # if JENKINS_ENV was set, use it, else prompt for it
  if [[ -n "$JENKINS_ENV" ]]; then
    case "$JENKINS_ENV" in
      'mgmt'|'prod')
        selected_env="bfd-mgmt-jenkins"
        JENKINS_ENV='prod'
      ;;
      'mgmt-test'|'test')
        selected_env="bfd-mgmt-test-jenkins"
        JENKINS_ENV='test'
      ;;
      *) error_exit "Unknown \$JENKINS_ENV '$JENKINS_ENV'" ;;
    esac
    echo "Using \$JENKINS_ENV ($JENKINS_ENV)"
  else
    PS3="Select the environment we are working with: "
    select e in "${jenkins_envs[@]}"
    do
      case "$e" in
        prod)
          selected_env="bfd-mgmt-jenkins"
          JENKINS_ENV='prod'
          break
        ;;
        test) 
          selected_env="bfd-mgmt-test-jenkins"
          JENKINS_ENV='test'
          break 
        ;;
      esac
    done
  fi

  # set vars based on env
  if [[ "$JENKINS_ENV" == "prod" ]]; then
    ADMIN_S3_BUCKET="$PROD_ADMIN_S3_BUCKET"
    JENKINS_SUBNET="$JENKINS_PROD_SUBNET"
    JENKINS_URL="$JENKINS_PROD_URL"
  elif [[ "$JENKINS_ENV" == "test" ]]; then
    ADMIN_S3_BUCKET="$TEST_ADMIN_S3_BUCKET"
    JENKINS_SUBNET="$JENKINS_TEST_SUBNET"
    JENKINS_URL="$JENKINS_TEST_URL"
  fi
  
  # TODO: default to instance IF only one exists, else error
  PS3="Select an instance from ($JENKINS_ENV) you wish to target: "
  select ip in $(aws ec2 describe-instances \
  --query 'Reservations[].Instances[].[PrivateIpAddress,Tags[?Key==`Name`]| [0].Value]' \
  --output table | grep "$selected_env" | awk '{print $2}' | grep -v "Abort" | grep -v "None" )
  do
    target="$ip"
    break
  done
  [[ -z "$target" ]] && error_exit "Error setting target. Aborting."
}

# runs an ssh command on the selected $target
# $1 == command to run on remote system
ssh_runner(){
  local cmd
  local result
  cmd="$@"
  if ! ssh -q -i "$SSH_KEY_PATH" -oStrictHostKeyChecking=no -o IdentitiesOnly=yes -T "$SSH_USER"@"$target" sudo "$cmd"; then
    error_exit "Could not execute ssh command. Aborting"
  fi
}

# tests ssh connectiont to $target
test_ssh(){
  printf '%s' "Testing connectivity to $target... "
  if ssh_runner "uptime" >/dev/null 2>&1; then
    echo "OK"
  else
    echo "FAIL"
    error_exit "Could not ssh to $target. Aborting."
  fi
}

# gets the systemctl status (running, stopped, etc) of Jenkins on $target
get_jenkins_status(){
  local result
  result="$(ssh_runner 'sudo systemctl show -p SubState jenkins' | cut -d'=' -f2)"
  [[ "$?" -ne 0 ]] && error_exit "Unable to get Jenkins' status. Aborting."
  case "$result" in
    'dead')
      echo "stopped"
    ;;
  esac
  echo "$result"
}

# polls until /login is available
wait_for_jenkins(){
  timeout=120 # 2 min
  printf "Waiting for Jenkins to become ready.."
  while [[ "$timeout" -gt 0 ]]; do
    if curl --insecure "${JENKINS_URL}/login" >/dev/null 2>&1; then
      echo " OK"
      return
    else
      sleep 1
      printf '.'
      timeout-=1
    fi
  done
  error_exit "Error reaching jenkins. Aborting."
}

# Runs 'sudo systemctl {stop|start|restart|status} jenkins' on $target and checks
# status until done
# $1 == 'stop' 'start' 'restart' or 'status'
jenkins_ctl(){
  local timeout
  case "$1" in
    'stop') printf "Stopping Jenkins.. " ;;
    'start') echo "Starting Jenkins.. " ;;
    'restart') echo "Restarting Jenkins.. " ;;
  esac
  
  # get jenkins status
  jenkins_status="$(get_jenkins_status)"
  case "$1" in
    stop)
      if [[ "$jenkins_status" == "stopped" ]]; then
        echo "OK"
      else
        if ssh_runner 'sudo systemctl stop jenkins' >/dev/null 2>&1; then
          echo "OK"
        else
          echo "FAIL"
          error_exit "Error stopping Jenkins. Aborting."
        fi
      fi
    ;;
    start)
      if [[ "$jenkins_status" == "running" ]]; then
        echo "OK"  
      else
        if ssh_runner 'sudo systemctl start jenkins' >/dev/null 2>&1; then
          echo "OK"
        else
          echo "FAIL"
          error_exit "Error starting Jenkins. Aborting."
        fi
      fi
      wait_for_jenkins
    ;;
    restart)
      if ssh_runner 'sudo systemctl restart jenkins' >/dev/null 2>&1; then
        echo "OK"
      else
        echo "FAIL"
        error_exit "Error restarting Jenkins. Aborting."
      fi
      wait_for_jenkins
    ;;
    *)
      error_exit "Unknown jenkins_ctl option. Aborting."
    ;;
  esac
}

# generates a mostly random filename for backups
gen_backup_name(){
  local backup_date
  local uuidstamp
  local backup_name
  backup_date="$(printf '%(%Y-%m-%d)T\n' -1)" # 2020-12-31
  uuidstamp="$(uuidgen | cut -d'-' -f 1)" # 186215EA
  backup_name="${backup_date}-${uuidstamp}.tar.gz" # 2020-12-31-186215EA.keyfile
  printf '%s' "$backup_name"  
}

# shell commands to back up /var/lib/jenkins
# $1 == backup file name
backup_cmd(){
  cat <<- _EOF_
touch "/var/lib/jenkins/$1" && \
sudo tar --exclude="/var/lib/jenkins/$1" -czvf "/var/lib/jenkins/$1" /var/lib/jenkins && \
sudo aws s3 cp "/var/lib/jenkins/$1" "${ADMIN_S3_BUCKET}/jenkins/backups/$1" && \
sudo rm "/var/lib/jenkins/$1"
_EOF_
}

# optionally backup /var/lib/jenkins to admin bucket (10+ minutes easily)
backup(){
  local backup_name
  local backup_cmd_str
  echo "This will make a full backup of the entire /var/lib/jenkins home directory and will upload it to the admin s3 bucket."
  echo "It will be large ~30GB compressed and will take 10+ minutes to complete."
  if ! c_prompt "Continue? (y/n)"; then
    error_exit "User aborted."
  fi

  # echo "Automatically backing up jenkins is a WIP. For now, ssh into $target and run the commands manually:"
  backup_name="$(gen_backup_name)"
  
  # build our backup command
  backup_cmd_str="$(backup_cmd "$backup_name")"
  
  # backup
  if ssh_runner "$backup_cmd_str"; then
    echo "Backup complete."
  else
    error_exit "Failed to backup jenkins. Aborting."
  fi
}

# Gets the latest gold ami from aws
get_gold(){
  local ami
  local filters
  filters='Name=name,Values="EAST-RH 7-? Gold Image V.1.?? (HVM) ??-??-??"'
  ami="$(aws ec2 describe-images \
    --filters "$filters" \
    Name=state,Values=available \
    --region us-east-1 \
    --output json | jq -r '.Images | sort_by(.CreationDate) | last(.[]).ImageId')"
    echo "$ami"
}

build_ami(){
  local packer_env
  local build_ami
  local timeout
  [[ "$JENKINS_ENV" == "prod" ]] && packer_env='mgmt'
  [[ "$JENKINS_ENV" == "test" ]] && packer_env='mgmt-test'
  if [[ -z "$GOLD_AMI" ]]; then
    GOLD_AMI="$(get_gold)"
  fi
  if [[ "$ACTION" != 'upgrade' ]]; then  
    if ! c_prompt "Build a new Jenkins AMI? Note: we will need to stop jenkins ($JENKINS_INSTANCE) during this somewhat lengthly process. Continue? (y/n)"; then
      error_exit "User aborted."
    fi
  fi
  
  # stop jenkins
  jenkins_ctl 'stop'
  
  echo "Building AMI"

  if cd ../../ansible/playbooks-ccs && packer build \
    --machine-readable \
    -var source_ami="$GOLD_AMI" \
    -var subnet_id="$JENKINS_SUBNET" \
    -var "env=$packer_env" \
    ../../packer/update_jenkins.json | tee "${SCRIPT_DIR}/packer-build.log"
  then
    build_ami="$(tail -2 "${SCRIPT_DIR}/packer-build.log" | head -2 | awk 'match($0, /ami-.*/) { print substr($0, RSTART, RLENGTH) }')"
  else
    error_exit "Failed to build AMI. Exiting."
  fi
  
  # quick and dirty regex test
  re="^ami-(.*)"
  [[ $build_ami =~ $re ]] || error_exit "Invalid ami id '$build_ami'. Aborting."
  
  if [[ "$ACTION" != "upgrade" ]]; then
    echo
    echo "AMI $build_ami was built successfully."
    jenkins_ctl 'start'
    echo "Done." && exit 0
  else
    JENKINS_AMI="$build_ami"
  fi
}

# apply terraform (deploy it)
# $1 == env to deploy (mgmt or mgmt-test)
terraform_deploy(){
  jenkins_ctl 'stop'
  
  # terraform init
  tfswitch -s 0.13 # backend was initialized with v0.13
  terraform apply \
    -var "env=$1" \
    -var "jenkins_key_name=$JENKINS_KEY_NAME" \
    -var "jenkins_ami=$JENKINS_AMI" \
    -var "jenkins_instance_size=$JENKINS_INSTANCE_SIZE" \
    -var "azs=$JENKINS_AZS"
}

# deploys
deploy(){
  if [[ "$ACTION" != 'upgrade' ]]; then
    if ! c_prompt "Deploy $JENKINS_AMI? Note: we will need to stop jenkins ($JENKINS_ENV) before deploying. Continue? (y/n)"; then
      error_exit "User aborted."
    fi
  fi
  [[ -z "$JENKINS_AMI" ]] && error_exit "JENKINS_AMI is not set. Please set with --jenkins-ami=foo"
  [[ "$JENKINS_ENV" == "prod" ]] && cd ../../terraform/env/mgmt/stateless && terraform_deploy 'mgmt'
  [[ "$JENKINS_ENV" == "test" ]] && cd ../../terraform/env/mgmt-test/stateless && terraform_deploy 'mgmt-test'
}

# builds an up-to-date ami and redeploys
upgrade(){
  if ! c_prompt "Build and deploy an upgraded Jenkins instance? Note: we will stop the existing $JENKINS_ENV instance during this somewhat lengthly process. Continue? (y/n)"; then
    error_exit "User aborted."
  fi
  build_ami && deploy
}

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ARG PARSING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
while [[ -n $1 ]]; do
  case $1 in
    -h | --help)
      help_message; exit 0
    ;;
    --jenkins-ami)
      shift; JENKINS_AMI=$1
    ;;
    -*)
      usage
      error_exit "Unknown option $1" ;;
    *)
      requested_action="$1"
      case "$requested_action" in
        deploy) ACTION='deploy' ;;
        upgrade) ACTION='upgrade' ;;
        backup) ACTION='backup' ;;
        status) ACTION='get_jenkins_status' ;;
        build*) 
          if [[ "$requested_action" != 'build-ami' ]]; then
            error_exit "Unknown action ${requested_action}.. did you mean build-ami?"
          else
            ACTION="$requested_action"
          fi
        ;;
        *)
          usage; error_exit "I do not know how to do $requested_action"
        ;;
      esac
      
      if [[ "$1" == "build-ami" ]]; then
        ACTION="build_ami"
      fi
  esac
  shift
done

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ BEGIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
set_working_env

# ensure all other required env vars are set
ok=true
for v in "${required_vars[@]}"; do
  # "${!v}" gets the value of the variable named in $v
  [[ -z "${!v}" ]] && echo "$v is not set" ok=false
done
if [[ "$ok" == "false" ]]; then
  echo "One or more env vars was not set. Did you source the required .env file?"
  exit 1
fi
test_ssh

# perform action
$ACTION
