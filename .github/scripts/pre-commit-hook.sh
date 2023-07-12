#!/usr/bin/env bash
set -e

checkSecretFilesForPlainText() {
  echo 'Verifying secret files are not in plaintext...'
  set +e
  # read list of files containing secrets
  IFS=$'\n' read -d '' -r -a secrets < .secrets

  # make list of files to be committed by printing out a newline
  # separated list of files staged for commit to a temporary file
  # and reading it as an array
  tmpfile=$(mktemp)
  git diff --cached --name-only --diff-filter=ACM > $tmpfile
  IFS=$'\n' read -d '' -r -a commits < $tmpfile

  # for all files to be committed which are a secret file,
  # grep for the header "$ANSIBLE_VAULT;1.1;AES256"
  # if it's not there, abort.
  for commitFile in ${commits[@]}; do
    for secretFile in ${secrets[@]}; do
      if [ $commitFile == $secretFile ]; then
        header=$(echo "$(git show :./$secretFile)" | grep "\$ANSIBLE_VAULT;1.1;AES256")
        if [ -z $header ]; then
          echo "attempting to commit an unencrypted secret: $secretFile; aborting"
          exit 1
        fi
      fi
    done
  done
  set -e
}

checkSecretFilesForPlainText
