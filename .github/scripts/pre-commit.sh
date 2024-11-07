#!/usr/bin/env bash
set -e

checkSecretFilesForPlainText() {
  echo 'Verifying secret files are not in plaintext...'
  set +e
  # read list of files containing secrets
  IFS=$'\n' read -d '' -r -a secrets <".github/resources/.sensitive-files"

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

# Runs shellcheck against any committed shell scripts
runShellCheckForCommitFiles() {
  echo 'Running shellcheck on committed files'
  if [ "$(which shellcheck)" ]; then
    tmpfile=$(mktemp)
    git diff --cached --name-only --diff-filter=ACM >"$tmpfile"
    commits=$(cat "$tmpfile")
    for file in $commits; do
      filename=$(basename -- "$file")
      extension="${filename##*.}"
      if [ "$extension" == "zip" ]; then
        continue
      fi
      firstTwo=$( sed 's/^\(..\).*/\1/;q' "$file" )
      # check for a hashbang or a .sh extension to determine if this is a shell script.
      if [ "$firstTwo" == "#!" ] && [ "$filename" != "Jenkinsfile" ] || [ "$extension" == "sh" ]; then
        # run shellcheck with severity level warning, and suppress warnings about invalid hashbangs (allows it to ignore other types of scripts, e.g. python)
        if ! shellcheck -e SC1071,SC2239 -S warning "$file"; then
          echo "Please fix errors before continuing."
          exit 1 
        fi
      fi
    done
  else
    echo 'Please install shellcheck before committing.'
    exit 1
  fi
}

checkGitleaks() {
  echo "Attempting to execute gitleaks. This may take a minute..."
  if ! command -v gitleaks >/dev/null; then
    echo "'gitleaks' not found. Install gitleaks before pushing your changes."
    return 1
  fi
  if gitleaks protect --staged --verbose; then
    return 0
  else
    return 1
  fi
}
checkSecretFilesForPlainText
runShellCheckForCommitFiles
checkGitleaks
