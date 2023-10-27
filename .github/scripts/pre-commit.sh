#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

REPO_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel)"
readonly REPO_ROOT

checkSecretFilesForPlainText() {
  echo 'Verifying secret files are not in plaintext...'
  cd "$REPO_ROOT"
  set +e
  # read list of files containing secrets
  IFS=$'\n' read -d '' -r -a secrets <"$REPO_ROOT/.github/resources/.sensitive-files"

  # make list of files to be committed by printing out a newline
  # separated list of files staged for commit to a temporary file
  # and reading it as an array
  tmpfile=$(mktemp)
  git diff --cached --name-only --diff-filter=ACM >"$tmpfile"
  IFS=$'\n' read -d '' -r -a commits <"$tmpfile"

  # for all files to be committed which are a secret file, grep for
  #   1. "<<SECURE>> or <</SECURE>>" 
  #   2. "CIPHER ENCRYPTED FILE HEADER; SHOULD BE ENCRYPTED" 
  # if either is found, this indicates that secrets are possibly unencrypted and the file should not
  # be committed. additionally, the file header preamble is grepped for to ensure that the header
  # was not removed unintentionally
  for commitFile in "${commits[@]}"; do
    for secretFile in "${secrets[@]}"; do
      if [ "$commitFile" == "$secretFile" ]; then
        unencrypted_header=$(git show :./"$secretFile" | grep "CIPHER FILE HEADER, DO NOT REMOVE")
        if [[ -z "$unencrypted_header" ]]; then
          echo "$unencrypted_header"
          echo "attempting to commit an unencrypted secret: $secretFile; aborting"
          exit 1
        fi

        encrypted_header=$(git show :./"$secretFile" | grep "CIPHER ENCRYPTED FILE HEADER; SHOULD BE ENCRYPTED")
        if [[ -n "$encrypted_header" ]]; then
          echo "$encrypted_header"
          echo "attempting to commit an unencrypted secret: $secretFile; aborting"
          exit 1
        fi

        secure=$(git show :./"$secretFile" | grep -E "<<SECURE>>|<</SECURE>>")
        if [[ -n "$secure" ]]; then
          echo "$secure"
          echo "attempting to commit an unencrypted secret: $secretFile; aborting"
          exit 1
        fi
      fi
    done
  done
  set -e
}

checkSecretFilesForPlainText
