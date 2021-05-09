#!/usr/bin/env bash
# eft efs mount helper script
set -euo pipefail

# partner info
PARTNER="${PARTNER:-}" # bcda, dpc, etc
EFT_ENV="${EFT_ENV:-test}" # prod, test, etc

# where you want the efs file system mounted under (defaults to /mnt/eft)
# note: the directory must be empty and we will create the directories if they do not exist
MOUNT_DIR="${MOUNT_DIR:-/mnt/eft}"

# you should not need to change these (they are variables for testing purposes)
BFD_EFS_ROLE_ARN=${BFD_EFS_ROLE_ARN:-}
ETC_HOSTS_FILE="${ETC_HOSTS_FILE:-/etc/hosts}"
FSTAB_FILE="${FSTAB_FILE:-/etc/fstab}"
MOUNT_SAME_AZ_ONLY="${MOUNT_SAME_AZ_ONLY:-true}" # only mount if fs is hosted on the same physical AZ as us
MOUNT_NOW="${MOUNT_NOW:-true}" # mount the file system when the script runs
ADD_FSTAB_ENTRY="${ADD_FSTAB_ENTRY:-true}" # add a persistant mount entry in $FSTAB_FILE
ADD_HOST_ENTRY="${ADD_HOST_ENTRY:-true}" # add a host entry for the file system (this must be true if the instance is not in BFD VPC)
FSTAB_OPTIONS="${FSTAB_OPTIONS:-'_netdev,noresvport,tls,iam 0 0'}"

# creds we will use to call assume-role
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-}"
AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-}"
AWS_SESSION_TOKEN="${AWS_SESSION_TOKEN:-}"

# interal script variables
file_systems=()


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
  results=$(aws sts assume-role --role-arn "$BFD_EFS_ROLE_ARN" --role-session-name "${PARTNER}-eft-mount-helper")
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
  # sed -i "/^.*\ ${1}/d" "$ETC_HOSTS_FILE" # remove the entry (the next line after the comment)
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
  # ensure mount directory exists
  if ! mkdir -p "$MOUNT_DIR/$1"; then
    echo "Error: could not create $MOUNT_DIR/$1"
    exit 1
  fi

  # ensure it's empty
  if [[ -n $(ls -A "$MOUNT_DIR/$1") ]]; then
    echo "Error: $MOUNT_DIR/$1 mount directory is not empty"
    exit 1
  fi
}

# mounts a file system to its mount directory
# $1 == file system id
mount_fs(){
  # mount it
  if mount -t efs -o tls,iam "$fs" "$MOUNT_DIR/$1" >/dev/null 2>&1; then
    echo "EFT EFS file system ($fs) mounted on $MOUNT_DIR/$1"
  else
    echo "Error: failed to mount $fs EFT file system on $MOUNT_DIR"
    exit 1
  fi
}

# creates an entry in /etc/fstab to automatically mount on reboot if not already present
# $1 == file system id
add_fstab_entry(){
  local fstab_entry # the fstab entry string
  fstab_entry="${1}:/ ${MOUNT_DIR}/${1} efs $FSTAB_OPTIONS"
  
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
