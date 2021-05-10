#!/usr/bin/env bash
# eft efs mount helper script
set -eo pipefail

# you must provide these values for the script to work
PARTNER="${PARTNER:-}" # bcda, dpc, etc
EFT_ENV="${EFT_ENV:-}" # prod, test, etc
BFD_EFS_ROLE_ARN="${BFD_EFS_ROLE_ARN:-}" # contact BFD ops team for this value

# where you want the efs file system mounted under (defaults to /mnt/eft)
# note: the directory must be empty and we will create the directories if they do not exist
MOUNT_DIR="${MOUNT_DIR:-/mnt/eft}"
MOUNT_USER_NAME="${MOUNT_USER_NAME:-}" # who will own the mount directories (defaults to current user)
MOUNT_GROUP_NAME="${MOUNT_GROUP_NAME:-}" # what group will own the mount directories (defaults to current group)
MOUNT_PERMS="${MOUNT_PERMS:-0750}" # defaults to owner=rw group=r others=none

# other options (defaults are likely ok here)
MOUNT_SAME_AZ_ONLY="${MOUNT_SAME_AZ_ONLY:-true}" # only mount if we find a mount target on the same az as us
MOUNT_NOW="${MOUNT_NOW:-true}" # mount the file system when the script runs
ADD_HOST_ENTRY="${ADD_HOST_ENTRY:-true}" # add a host entry for the file system (this must be true if the instance is not in BFD VPC)
ADD_FSTAB_ENTRY="${ADD_FSTAB_ENTRY:-true}" # add a persistant mount entry in $FSTAB_FILE
FSTAB_OPTIONS="${FSTAB_OPTIONS:-'_netdev,noresvport,tls,iam 0 0'}"

# you should not need to edit these (they are variables for testing purposes)
ETC_HOSTS_FILE="${ETC_HOSTS_FILE:-/etc/hosts}"
FSTAB_FILE="${FSTAB_FILE:-/etc/fstab}"

# we will use these creds to assume the role
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-}"
AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-}"
AWS_SESSION_TOKEN="${AWS_SESSION_TOKEN:-}"

# interal script variables
PROGNAME=${0##*/}
file_systems=()


usage() {
  echo -e "Usage: $PROGNAME [-h|--help] [-p|--partner-name PARTNER]"
}

help_message() {
  cat <<- _EOF_
  $PROGNAME
  BFD EFT EFS mount helper script

  $(usage)

  Options (either pass these with command line flags, or export using ENV vars. Corresponding env vars are in [CAPS]:
  -h, --help  Display this help message and exit.
  -p, --partner-name [PARTNER] bcda, dpc, etc
  -e | --eft-environment [EFT_ENV] test, prod, etc (defaults to test)
  -r | --bfd-role-arn [BFD_EFS_ROLE_ARN] get this value from BFD ops
  -m | --mount-dir [MOUNT_DIR] base directory where we will mount file systems (defaults to /mnt/eft)
  --mount-dir-user-name [MOUNT_USER_NAME] user name who will own mount dir (defaults to current user)
  --mount-dir-group-name [MOUNT_GROUP_NAME] group name who will own mount dirs (defaults to current group)
  --mount-dir-posix-perms [MOUNT_PERMS] posix perms in #### format (defaults to 0750)
  --mount-same-az-only [MOUNT_SAME_AZ_ONLY] only mount file system hosted on the same azid to avoid cross az data charges (true or false - defaults to true)
  --mount-now [MOUNT_NOW] mount file system now (true or false - defaults to true)
  --add-fstab-entry [ADD_FSTAB_ENTRY] add an entry to /etc/fstab to persist reboots (true or false - defaults to true)
  --add-hosts-entry [ADD_HOSTS_ENTRY] add an entry to /etc/hosts (true or false - defaults to true and is required if you are not on BFD VPC)
  --aws-region [AWS_REGION] region used to assume BFD EFS RW role (defaults to us-east-1 if not set via env var)
  --aws-access-key-id [AWS_ACCESS_KEY_ID] access key id used to assume BFD EFS RW role (defaults to environment)
  --aws-secret-access-key [AWS_SECRET_ACCESS_KEY] key used to assume BFD EFS RW role (defaults to environment)
  --aws-session-token [AWS_SESSION_TOKEN] token used to assume BFD EFS RW role (defaults to environment)

  Examples:
  # using env vars (either exported, via .env file, etc)
  export PARTNER=bcda
  export EFT_ENV=test
  export BFD_EFS_ROLE_ARN='arn:aws:iam::1234567:role/bcda-eft-efs-test-role'
  ./mount-eft-efs.sh

  # using command line args
  ./mount-eft-efs.sh -p bcda -e test -r 'arn:aws:iam::1234567:role/bcda-eft-efs-test-role' --mount-dir=/path/to/mount/directory --mount-same-az-only=false
  
  # editing the default values in this script. setting default partner name for example:
  vim mount-eft-efs.sh
  # change this line
    PARTNER="${PARTNER:-}" # bcda, dpc, etc
  # to this
    PARTNER="${PARTNER:-bcda}" # bcda, dpc, etc
  :wq
_EOF_
  return
}


# make sure we have all the stuff we need to run this script
system_check(){
  local tools failed
  tools=(aws jq)
  export failed=false
  for tool in "${tools[@]}"; do
    if ! command -v >/dev/null 2>&1; then
      echo "Error: this script requires '$tool'. Please install '$tool' and try again."
      failed=true
    fi
  done
  if $failed; then
    exit 1
  fi
}

# assumes the bcda-efs-rw-access-role
assume_role(){
  results=$(aws sts assume-role --role-arn "$BFD_EFS_ROLE_ARN" --role-session-name "${PARTNER}_eft_mount_helper")
  AWS_ACCESS_KEY_ID=$(jq -r '.Credentials.AccessKeyId' <(echo "$results"))
  AWS_SECRET_ACCESS_KEY=$(jq -r '.Credentials.SecretAccessKey' <(echo "$results"))
  AWS_SESSION_TOKEN=$(jq -r '.Credentials.SessionToken' <(echo "$results"))
  export AWS_ACCESS_KEY_ID
  export AWS_SECRET_ACCESS_KEY
  export AWS_SESSION_TOKEN
}

# sets the $filesystems array to {{partner}}'s eft efs file systems
set_filesystems(){
  local fs_ids
  if fs_ids=$(aws efs describe-file-systems --query 'FileSystems[].[FileSystemId,Name]' --output text | grep "${PARTNER}-eft-efs-${EFT_ENV}" | cut -f1); then  
    for fs in $fs_ids; do
      file_systems+=("$fs")
    done
  else
    echo "No EFT file systems found for '${PARTNER^^}'"
    exit 1
  fi
}

# checks if $1 is a valid ipv4 address
is_valid_ip(){
  if ! [[ "$1" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
    return 1
  fi
}

# Adds an entry to $ETC_HOSTS_FILE
# $1 == file system id
add_host_entry() {
  local ip # mount target ip
  
  # the default here is to only mount file systems available on the same physical AZ (to avoid cross-az data fees)
  if [[ "$MOUNT_SAME_AZ_ONLY" == "true" ]]; then
    AZID=$(curl -s http://169.254.169.254/latest/meta-data/placement/availability-zone-id)
    if ! [[ "$AZID" =~ ^(.*)-az[0-9]{1,2}$ ]]; then
      echo "Could not use AZID '$AZID'."
      exit 1
    fi
    if ! ip=$(aws efs describe-mount-targets --region "$AWS_REGION" --file-system-id="$1" --query 'MountTargets[*].{ID:AvailabilityZoneId,IP:IpAddress}' --output text | grep "$AZID" | cut -f2); then
      echo "Error: could not find a mount target hosted on the same Availability Zone as this instance and MOUNT_SAME_AZ_ONLY is set to true"
      exit 1
    fi
  else
    # else, randomly pick an ip address (you cannot round robin in /etc/hosts, so no reason to add more than one entry)
    local ips
    ips=()
    for mt_ip in $(aws efs describe-mount-targets --region "$AWS_REGION" --file-system-id="$1" --query 'MountTargets[*].{ID:AvailabilityZoneId,IP:IpAddress}' --output text | cut -f2); do
      ips+=("$mt_ip")
    done

    # pick a random ip
    local array_length rand_num
    array_length="${#ips[@]}"
    case "$array_length" in
      0) echo "Error: could not find any EFT mount targets" && exit 1 ;;
      1) ip="${ips[0]}" ;;
      
      # else, set $ip to a random ip from the array
      *)
        rand_num=$(("$RANDOM" % "${#ips[@]}"))
        ip="${ips[${rand_num}]}" ;;
    esac
  fi
  
  # simple ipv4 test
  if ! is_valid_ip "$ip" >/dev/null 2>&1; then
    echo "Unable to retrieve a valid mount target IP address for file system $1 on AZ $AZID."
    exit 1
  fi
  
  # remove any existing hosts entries
  sed -i.bak "/^#\ ${1}\ EFT\ EFS\ HOST\ ENTRY\ -\ DO\ NOT\ EDIT$/{N;d;}" "$ETC_HOSTS_FILE" # remove the entry (the next line found after the comment)
  sed -i "/^#\ ${1}\ EFT\ EFS\ HOST\ ENTRY\ -\ DO\ NOT\ EDIT$/d" "$ETC_HOSTS_FILE" # remove the comment

  # add the entry to the ETC_HOSTS_FILE
  printf "\n\n# ${1} EFT EFS HOST ENTRY - DO NOT EDIT\n%s %s %s.efs.%s.amazonaws.com\n" "$ip" "$1" "$1" "$AWS_REGION" >> "$ETC_HOSTS_FILE"
  
  # cleanup multiple empty lines (a byproduct if this script is run multiple times)
  # shellcheck disable=SC2016
  sed -i '$!N;/^\n$/{$q;D;};P;D;' "$ETC_HOSTS_FILE"
}

# prepares a mount directory for the given file system
# $1 file system id
prepare_mount_directory(){
  # default to current user and group
  [[ -z "$MOUNT_USER_NAME" ]] && MOUNT_USER_NAME="$(id -u -n)"
  [[ -z "$MOUNT_GROUP_NAME" ]] && MOUNT_GROUP_NAME="$(id -g -n)"
  
  # ensure the base directory for mount points exists (create it if not)
  if ! [[ -d "$MOUNT_DIR" ]]; then
    # shellcheck disable=SC2174
    if ! mkdir -p -m 0755 "$MOUNT_DIR"; then
      # try with sudo
      sudo mkdir -p -m 0755 "$MOUNT_DIR"
    fi
    
    # chown it
    if ! chown "${MOUNT_USER_NAME}:${MOUNT_GROUP_NAME}" "$MOUNT_DIR" >/dev/null 2>&1; then
      sudo chown "${MOUNT_USER_NAME}:${MOUNT_GROUP_NAME}" "$MOUNT_DIR" >/dev/null 2>&1
    fi
  fi

  # ensure file system mount directory exists
  # shellcheck disable=SC2174
  if ! mkdir -p -m "$MOUNT_PERMS" "$MOUNT_DIR/$1"; then
    if ! sudo mkdir -p -m "$MOUNT_PERMS" "$MOUNT_DIR/$1"; then
      echo "Error: could not create mount directory at $MOUNT_DIR/$1"
      exit 1
    fi
  fi

  # ensure it's empty
  if [[ -n $(ls -A "$MOUNT_DIR/$1") ]]; then
    echo "Error: $MOUNT_DIR/$1 mount directory is not empty"
    exit 1
  fi

  # chown it
  if ! chown "${MOUNT_USER_NAME}:${MOUNT_GROUP_NAME}" "$MOUNT_DIR/$1" >/dev/null 2>&1; then
    # try with sudo
    sudo chown "${MOUNT_USER_NAME}:${MOUNT_GROUP_NAME}" "$MOUNT_DIR/$1"
  fi
}

# mounts a file system to its mount directory
# $1 == file system id
mount_fs(){
  # mount it
  if mount -t efs -o tls,iam "${1}:/" "$MOUNT_DIR/$1" >/dev/null 2>&1; then
    echo "EFT EFS file system $1 mounted on ${MOUNT_DIR}/$1"
  else
    echo "Error: failed to mount EFT EFS file system $1 on $MOUNT_DIR"
    exit 1
  fi
}

# creates an entry in /etc/fstab to remount on reboots
# $1 == file system id
add_fstab_entry(){
  local fstab_entry # the fstab entry string
  fstab_entry="${1}:/ $MOUNT_DIR/$1 efs $FSTAB_OPTIONS"
  
  local epdir # escaped partner dir for use in sed
  epdir=$(sed 's#/#\\/#g' <(echo "$MOUNT_DIR"))

  # remove any previous file system fstab entries
  cp -f "$FSTAB_FILE" "${FSTAB_FILE}.bak" # back it up
  sed -i "/^#\ EFT\ EFS\ mount\ point\ for\ ${PARTNER}$/d" "$FSTAB_FILE" # remove entry comment
  sed -i "/^${1}:\/[[:space:]]${epdir}.*$/d" "$FSTAB_FILE" # remove the entry

  # add our new fstab entry (with comment)
  printf "\n# EFT EFS mount point for %s\n%s\n" "$PARTNER" "$fstab_entry" >> "$FSTAB_FILE"

  # cleanup multiple empty lines (a byproduct if this script is run multiple times)
  # shellcheck disable=SC2016
  sed -i '$!N;/^\n$/{$q;D;};P;D;' "$FSTAB_FILE"
}


#### PARSE COMMAND LINE OPTIONS ####
while [[ -n "$1" ]]; do
  case $1 in
    -h | --help)
      help_message; exit ;;
    -p | --partner-name)
      shift; PARTNER="$1" ;;
    --partner-name=*)
      PARTNER="${1#*=}" ;;
    -e | --eft-environment)
      shift; EFT_ENV="$1" ;;
    --eft-environment=*)
      EFT_ENV="${1#*=}" ;;
    -r | --bfd-efs-role-arn)
      shift; BFD_EFS_ROLE_ARN="$1" ;;
    --bfd-efs-role-arn=*)
      BFD_EFS_ROLE_ARN="${1#*=}" ;;
    -m | --mount-dir)
      shift; MOUNT_DIR="$1" ;;
    --mount-dir=*)
      MOUNT_DIR="${1#*=}" ;;
    --mount-dir-user-name)
      shift; MOUNT_USER_NAME="$1" ;;
    --mount-dir-user-name=*)
      MOUNT_USER_NAME="${1#*=}" ;;
    --mount-dir-group-name)
      shift; MOUNT_GROUP_NAME="$1" ;;
    --mount-dir-group-name=*)
      MOUNT_GROUP_NAME="${1#*=}" ;;
    --mount-dir-posix-perms)
      shift; MOUNT_PERMS="$1" ;;
    --mount-dir-posix-perms=*)
      MOUNT_PERMS="${1#*=}" ;;
    --mount-same-az-only)
      shift; MOUNT_SAME_AZ_ONLY="$1" ;;
    --mount-same-az-only=*)
      MOUNT_SAME_AZ_ONLY="${1#*=}" ;;
    --mount-now)
      shift; MOUNT_NOW="$1" ;;
    --mount-now=*)
      MOUNT_NOW="${1#*=}" ;;
    --add-fstab-entry)
      shift; ADD_FSTAB_ENTRY="$1" ;;
    --add-fstab-entry=*)
      ADD_FSTAB_ENTRY="${1#*=}" ;;
    --add-hosts-entry)
      shift; ADD_HOST_ENTRY="$1" ;;
    --add-hosts-entry=*)
      ADD_HOST_ENTRY="${1#*=}" ;;
    --aws-region)
      shift; AWS_REGION="$1" ;;
    --aws-region=*)
      AWS_REGION="${1#*=}" ;;
    --aws-access-key-id)
      shift; AWS_ACCESS_KEY_ID="$1" ;;
    --aws-access-key-id=*)
      AWS_ACCESS_KEY_ID="${1#*=}" ;;
    --aws-secret-access-key)
      shift; AWS_SECRET_ACCESS_KEY="$1" ;;
    --aws-secret-access-key=*)
      AWS_SECRET_ACCESS_KEY="${1#*=}" ;;
    --aws-session-token)
      shift; AWS_SESSION_TOKEN="$1" ;;
    --aws-session-token=*)
      AWS_SESSION_TOKEN="${1#*=}" ;;
    -*)
      usage
      echo "Unknown option $1"; exit 1;;
    *)
      echo "Do not know $1"; exit 1 ;;
  esac
  shift
done

#### BEGIN ####
# fail early if we do not have all the tools
system_check

# so we can query file systems hosted in BFD's account
assume_role

# sets the $file_system array to a list of your EFT file systems (likely only one per env)
set_filesystems

# for each file system found
for fs in "${file_systems[@]}"; do
  # prepare its mount directory
  prepare_mount_directory "$fs"

  # add an entry to /etc/hosts
  [[ "$ADD_HOST_ENTRY" == true ]] && add_host_entry "$fs" 

  # mount the file system now if desired
  [[ "$MOUNT_NOW" == true ]] && mount_fs "$fs"
  
  # add a persistant mount entry to fstab if desired
  [[ "$ADD_FSTAB_ENTRY" == true ]] && add_fstab_entry "$fs"
done 

# create a friendly symlink if there is only one file system 
if [[ "${#file_systems[@]}" -eq 1 ]]; then
  echo "Creating symlink to ${file_systems[0]} at ${MOUNT_DIR}/eft"
  if ! unlink "${MOUNT_DIR}/eft" >/dev/null 2>&1; then
    if [[ -f "${MOUNT_DIR}/eft" ]]; then
      echo "Error: cannot eft symlink to ${file_systems[0]}. eft file exists."
      exit
    fi
  fi
  ln -s "${MOUNT_DIR}/${file_systems[0]}" "${MOUNT_DIR}/eft"
fi
