#!/usr/bin/env bash
# vault.sh - ansible-vault helper script for rekeying and testing vault encrypted files in BFD.
# Run this script with --help for more info.
SCRIPT_NAME=${0##*/}

## ENV VARS
CURRENT_KEYFILE="${CURRENT_KEYFILE:-}" # you can overide this via the --current-keyfile arg
NEW_KEYFILE="${NEW_KEYFILE:-}" # you can overide this via the --new-keyfile arg
CURRENT_KEY="${CURRENT_KEY:-}" # use an env file to overide this if needed
NEW_KEY="${NEW_KEY:-}" # use an env file to overide this if needed
REKEY_FILES="${REKEY_FILES:-false}" # this gets set to true if --rekey flag is present
TEST_FILES="${TEST_FILES:-false}" # this gets set to true if --test flag is present
SKIP_S3="${SKIP_S3:-false}" # do not upload or download from S3. if skipped, you must provide keys
OUTPUT_DIR="${OUTPUT_DIR:-}" # if SKIP_S3==true, set this to the directory to write the keyfile to
S3_BUCKET="${S3_BUCKET:-}" # this should be set via an env file which is stored somewhere like kb
KEY_LEN="${KEY_LEN:-100}" # the length of the new key (in characters) when automatically rekeying
DEFAULT_SEARCH_PATH="../../ansible/playbooks-ccs"
SEARCH_PATH="${SEARCH_PATH:-}" # last arg to script. Will overwrite DEFAULT_SEARCH_PATH if set

# The vault.keyfile.id stores the name of the keyfile that was used to encrypt our files. When new
# BFD instances are launched, the startup script will pull this keyfile down from s3 and decrypt 
# the files. The file identified in the vault.keyfile.id in master MUST be present in s3 or new
# instances will fail to launch.
VAULT_KEYFILE_ID="../../ansible/playbooks-ccs/vault.keyfile.id"

# other vars (arrays to track progress, interactive shell status, etc)
found_files=()
rekeyed_files=()
tmp_dir=

#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ HOUSEKEEPING STUFF ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
cleanup(){
  # undo any rekeyed files
  if [[ "${#rekeyed_files[@]}" -gt 0 ]]; then
    for f in "${rekeyed_files[@]}"; do
      if rekey "$f" "$NEW_KEY" "$CURRENT_KEY" >/dev/null 2>&1; then
        echo "restored $f"
      else
        echo "error restoring $f.. please manually review/restore this file."
      fi
    done
  fi
  
  # restore previous vault.keyfile.id file
  mv -f "${tmp_dir}/vault.keyfile.id.bak" "$VAULT_KEYFILE_ID" >/dev/null 2>&1

  # remove all tmp files
  rm -rf "$tmp_dir" >/dev/null 2>&1
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
      cleanup && printf "\nterminated" >&2 && exit 0 ;;
    *)
      error_exit "\nunknown error" ;;
  esac
}

# help and usage
usage(){
  echo -e "Usage: $SCRIPT_NAME [-h|--help] [-t|--test] [-r|--rekey] [--skip-s3]\
 [--current-keyfile /path/to/current/keyfile] [--new-keyfile /path/to/new/keyfile]\
 [--output-dir /path/to/export] [SEARCH_PATH]"
}

help_message(){
  cat <<- _EOF_
  $SCRIPT_NAME is an ansible-vault helper script used for working with ansible-vault encryped files
  within the BFD repo.
  
  This script is used to:
    1. Recursively search and rekey ansible-vault encrypted files found in SEARCH_PATH (defaults
       to $DEFAULT_SEARCH_PATH).
    2. Test to ensure all ansible-vault encrypted files can be successfully decrypted by BFD instances.
  
  $(usage)
  
  Examples:
  $> ./$SCRIPT_NAME --rekey
    Search and rekey files using default values. By default, this will rekey all ansible-vault encrypted
    files found in $DEFAULT_SEARCH_PATH. An auto-generated $KEY_LEN character long key will be created and
    a new keyfile with this key will be created and uploaded to \$S3_BUCKET. The vault.keyfile.id file
    will be updated to point to this new keyfile so new instances can fetch the key to decrypt files.
  $> ./$SCRIPT_NAME --test
    Test to ensure all ansible-vault encrypted files in the default search path \$DEFAULT_SEARCH_PATH
    can be decrypted with the default \$VAULT_KEYFILE_ID
  $> ./$SCRIPT_NAME --test --current-keyfile=/Volumes/secrets/foo ../foo.yml
    Test to see if ../foo.yml is an ansible-vault encrypted file that can be decrypted with foo.
  $> ./$SCRIPT_NAME --rekey --skip-s3 --new-keyfile=foo --output-dir=. 

  Options:
  -r, --rekey Rekeys (re-encrypts) ansible-vault encrypted files. By default, a new $KEY_LEN long
    character passkey will be generated unless CURRENT_KEY or CURRENT_KEYFILE is provided by the user.
  -t, --test Verifies that all files in the search path can be decrypted using the keyfile identified
    in the $VAULT_KEYFILE_ID.
  -h, --help  Display this help message and exit.
  [Optional] --current-keyfile Path to a keyfile that was previously used to encrypt files.
  [Optional] --new-keyfile Path to a keyfile that you want rekey files with.
  [Optional] --skip-s3 Do not upload or download keyfiles from AWS S3.
  [Optional] --output-dir Path to export the new keyfile to.

  Notes:
  This script makes heavy use of environment variables in addition to the above options. This is to
  allow easier automation and flexibility. If you choose to use environment variables, it's highly
  advised to add these to an environment file and source before calling the script. E.g.,
    > source .env && ./$SCRIPT_NAME
    
  Please be mindful and do not polute your shell history with secrets. E.g., do not do
    > export CURRENT_KEY=s3cr3tpassw9rd ./$SCRIPT_NAME' <--BAD

  *.env files should be added to BFD's .gitignore file, but please verify before commits.
_EOF_
  return
}

# trap signals
trap "signal_exit TERM" TERM HUP
trap "signal_exit INT"  INT


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ REKEY FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# Resolve and test paths.
resolve_path(){
  local fullpath=
  local basename=
  local dirname=
  if [[ -f $1 ]]; then
    # is a file
    basename=$(basename "$1")
    dirname=$(dirname "$1")
  elif [[ -d $1 ]]; then
    # is a directory
    dirname="$1"
  else
    return 1
  fi
  
  fullpath=$(cd "$dirname" && pwd -P)/"$basename"
  if [[ -f "$fullpath" || -d "$fullpath" ]]; then
    # return the full path
    printf '%s' "$fullpath"
  else
    return 1
  fi
}

# loads the CURRENT_KEY variable with the passkey that was used to decrypt existing files
load_current_key(){
  # if CURRENT_KEY was set manually, use it and stop here
  if [[ -n "$CURRENT_KEY" ]]; then
    echo "Using user supplied CURRENT_KEY."
    return
  fi

  # if the user provided a keyfile, load the key from it and stop here
  if [[ -n "$CURRENT_KEYFILE" ]]; then
    local keyfile
    keyfile=$(resolve_path "$CURRENT_KEYFILE")
    CURRENT_KEY="$(cat "$CURRENT_KEYFILE")"
    [[ -z "$CURRENT_KEY" ]] && error_exit "Could not load key from $CURRENT_KEYFILE"
    echo "Using user supplied key found in $CURRENT_KEYFILE"
    return
  fi

  # else, fetch the keyfile identified in VAULT_KEYFILE_ID from s3 and use it
  printf '%s' "Fetching $(cat "$VAULT_KEYFILE_ID") from s3.. "
  local keyfile_id
  local s3_file
  keyfile_id=$(cat "$VAULT_KEYFILE_ID")
  s3_file="${S3_BUCKET}/$keyfile_id"
  CURRENT_KEY=$(aws s3 cp "$s3_file" -)
  [[ -z "$CURRENT_KEY" ]] && echo "FAIL" && error_exit "Unable to load the key from s3."
  echo "OK"
}

# prints a random $KEY_LEN password with upper, lower, and special chars
gen_key(){
  printf '%s' "$(tr -dc 'A-Za-z0-9!"#()+,-./:;<=>?@[\]^_{|}~' </dev/urandom | head -c "$KEY_LEN")"
}

# loads or creates the "new" decryption key
load_new_key(){
  # stop here if NEW_KEY was provided by the user
  if [[ -n "$NEW_KEY" ]]; then
    echo "Rekeying with user supplied NEW_KEY."
    return
  fi

  # if a keyfile was provided by the user then load the key and stop here
  if [[ -n "$NEW_KEYFILE" ]]; then
    local keyfile
    keyfile=$(resolve_path "$NEW_KEYFILE")
    CURRENT_KEY=$(cat "$keyfile")
    [[ -z "$NEW_KEY" ]] && error_exit "Could not load key from $NEW_KEYFILE"
    echo "Rekeying with user supplied NEW_KEYFILE."
    return
  fi

  # else, generate a new key
  NEW_KEY="$(gen_key)"
  echo "Rekeying with auto-generated key."
}

# prints a unique keyfile name
gen_keyfile_name(){
  local keyfile_date
  local uuidstamp
  local keyfile_name
  keyfile_date="$(printf '%(%Y-%m-%d)T\n' -1)" # 2020-12-31
  uuidstamp="$(uuidgen | cut -d'-' -f 1)" # 186215EA
  keyfile_name="${keyfile_date}-${uuidstamp}.keyfile" # 2020-12-31-186215EA.keyfile
  printf '%s' "$keyfile_name"  
}

# creates a keyfile (a keyfile stores our secret) with the NEW_KEY
# returns the path to the keyfile
gen_keyfile(){
  local keyfilename
  keyfilename="$(gen_keyfile_name)"
  echo "$NEW_KEY" > "${tmp_dir}/${keyfilename}"
  printf '%s' "${tmp_dir}/${keyfilename}"
}

# makes a backup of $VAULT_KEYFILE_ID for use in cleanup/recovery
backup_vault_id(){
  [[ ! -f "$VAULT_KEYFILE_ID" ]] && error_exit "Could not find $VAULT_KEYFILE_ID"
  cp "$VAULT_KEYFILE_ID" "${tmp_dir}/vault.keyfile.id.bak"
}

# exports a file to either an $S3_BUCKET or a directory
# $1 == file to upload
# $2 == s3 path or file path
export_file(){
  [[ ! -f "$1" ]] && error_exit "$1 is not a valid keyfile."
  local fname
  fname=$(basename "$1")
  if [[ -d "$2" ]]; then
    mv "$1" "${2}${fname}"
    echo "Keyfile exported to ${2}${fname}"
  else
    printf '%s' "Uploading $fname to s3.. "
    if aws s3 cp "$1" "${2}${fname}"; then
      echo "OK"
    else
      error_exit "Unable to upload ${2}${fname} to S3. Aborting."
    fi
  fi
}

# rekeys files
# $1 == path to anisble vault encrypted file
# $2 == current key
# $3 == new key
rekey(){
  # abort if no key is set or the file is invalid
  [[ ! -f $1 || -z "$2" || -z "$3" ]] && error_exit "Cannot rekey $1"

  # else, rekey
  ansible-vault rekey --vault-password-file=<(echo "$2") --new-vault-password=<(echo "$3") "$1" >/dev/null 2>&1
}

# rekeys files found in $search_path
rekey_files(){
  # search for ansible vault encrypted files
  for f in $(find "$SEARCH_PATH" -type f); do
    # and add them to the found_files array
    if [[ $(sed -n "/^\$ANSIBLE_VAULT\;/p;q" "$f") ]]; then
      found_files+=("$f")
    fi
  done

  # exit if no files were found
  if [[ "${#found_files[@]}" -lt 1 ]]; then
    error_exit "No ansible vault encrypted files found."
  fi
  
  # parse and rekey the found files
  for f in "${found_files[@]}"; do
    printf '  rekeying %s.. ' "$f"
    if rekey "$f" "$CURRENT_KEY" "$NEW_KEY"; then
      rekeyed_files+=("$f")
      echo "OK"
    else
      error_exit "FAIL"
    fi
  done
}

# test decrypting an ansible vault encrypted file
# $1 == file to test
# $2 == key
test_decrypt(){
  [[ ! -f "$1" || -z "$2" ]] && error_exit "could not test file $1 with supplied secret"
  if [[ $(sed -n "/^\$ANSIBLE_VAULT\;/p;q" "$1") ]]; then
    printf '  %s' "$1.. "
    # test the file without writing to the terminal (e.g., don't print secrets)
    if ansible-vault view --vault-password=<(echo "$2") "$1" >/dev/null 2>&1; then
      echo "OK"
    else
      echo "FAIL"
      return 1
    fi
  fi
}

# test all the files (or a specific file if provided)
# $1 is a path or file to test (recursive if it's a path)
# $2 is the key to use to decrypt. 
test_all_files(){
  local flagged
  flagged=false
  if [[ -f $1 ]]; then
    # it's a single file
    test_decrypt "$1" "$2" || flagged=true
  else
    # recursive search and test
    for f in $(find "$SEARCH_PATH" -type f); do
      test_decrypt "$f" "$2" || flagged=true
    done
  fi
  if [[ "$flagged" == "true" ]]; then
    return 1
  fi
}

# updates $VAULT_KEYFILE_ID (vault.keyfile.id) file to point to $1
# $1 == keyfile
update_vault_id(){
  printf 'Updating %s.. ' "$VAULT_KEYFILE_ID"
  local fname
  fname=$(basename "$1")
  echo "$fname" > "$VAULT_KEYFILE_ID"
  echo "OK"
}


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ARG PARSING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# parse command-line options
if [[ $# -eq 0 ]] ; then
    help_message; exit 0
fi

while [[ -n $1 ]]; do
  case $1 in
    -h | --help)
      help_message; exit 0 ;;
    -r | --rekey)
      REKEY_FILES=true ;;
    -t | --test)
      TEST_FILES=true ;;
    --current-keyfile)
      shift; CURRENT_KEYFILE="$1" ;;
    --current-keyfile=*)
      CURRENT_KEYFILE="${1#*=}" ;;
    --new-keyfile)
      shift; NEW_KEYFILE="$1" ;;
    --new-keyfile=*)
      NEW_KEYFILE="${1#*=}" ;;
    --skip-s3)
      SKIP_S3=true ;;
    --output-dir)
      shift; OUTPUT_DIR="$1" ;;
    --output-dir=*)
      OUTPUT_DIR="${1#*=}" ;;
    -* )
      usage
      error_exit "Unknown option $1" ;;
    *)
      SEARCH_PATH="$1" ;;
  esac
  shift
done


#~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ BEGIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
# exit if no action was requested
if [[ "$TEST_FILES" != "true" && "$REKEY_FILES" != "true" ]]; then
  echo "Nothing to do."
  usage
  exit 1
fi

# access to s3
if [[ "$SKIP_S3" == "false" ]]; then
  [[ -z "$S3_BUCKET" ]] && error_exit "S3_BUCKET environment variable must be set. Please see --help"
  aws s3 ls "$S3_BUCKET" >/dev/null 2>&1 || \
  error_exit "Cannot access AWS S3 using cli tools. Please ensure aws utils are installed and MFA is enabled."
else
  # ensure output directory is set
  OUTPUT_DIR=$(resolve_path "$OUTPUT_DIR") || \
  error_exit "--output-dir must be set to a valid directory if we are not uploading to s3."
fi

# set the search path
[[ -z "$SEARCH_PATH" ]] && SEARCH_PATH="$DEFAULT_SEARCH_PATH"
SEARCH_PATH=$(resolve_path "$SEARCH_PATH") || error_exit "Could not resolve search path."

# find/load the current key into the global CURRENT_KEY variable. This is the key that was used
# previously to encrypt the files.
load_current_key

# test to make sure we can decrypt all the files with the current key
echo "Ensuring we can decrypt all the files.."
if ! test_all_files "$SEARCH_PATH" "$CURRENT_KEY"; then
  error_exit "Failed to decrypt one or more files. Aborting."
fi

# stop here if we are only testing and not rekeying
[[ "$TEST_FILES" == "true" && "$REKEY_FILES" == "false" ]] && exit 0

# create a temp directory for storing artifacts
tmp_dir="$(mktemp -d)"

# sets the new key using either an automatically generated key (the default), or by using
# a user provided key
load_new_key

# backup the old vault keyfile id in case we need to revert
backup_vault_id

# generate a new keyfile
keyfile="$(gen_keyfile)"

# search for and rekey ansible vault encrypted files
rekey_files

# test all the files with the new key
echo "Verifying.."
if ! test_all_files "$SEARCH_PATH" "$NEW_KEY"; then
  error_exit "Failed to rekey one or more files.. aborting." 
fi

# update the vault.keyfile.id file to point to the new keyfile
update_vault_id "$keyfile"

# export the keyfile
if [[ "$SKIP_S3" == "false" ]]; then
  # export to s3
  export_file "$keyfile" "$S3_BUCKET"
else
  # export to the output directory
  export_file "$keyfile" "$OUTPUT_DIR"
fi

echo "Finished."
