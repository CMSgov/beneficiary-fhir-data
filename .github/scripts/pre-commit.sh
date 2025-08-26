#!/usr/bin/env bash
set -e

checkSecretFilesForPlainText() {
  echo 'Verifying secret files are not in plaintext...'
  set +e
  # read list of files containing secrets
  IFS=$'\n' read -d '' -r -a secrets < <(git show :".github/resources/.sensitive-files")

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

checkSopsFiles() {
  echo "Verifying sopsw.yaml files..."

  set +e

  IFS=$'\n' read -d '' -r -a sops_files < <(git diff --cached --name-only --diff-filter=ACM | grep "sopsw\.yaml" && printf '\0')

  for sops_file in "${sops_files[@]}"; do
    if [[ ! "$(command -v yq)" ]]; then
      echo "'yq' not found. Try 'brew install yq' and commit again"
      exit 1
    fi

    sops_file_name="$(basename -- "$sops_file")"
    echo "Checking sops file $sops_file_name..."

    sops_file_path="$(git rev-parse --show-toplevel)/$sops_file"
    if grep -E 'arn:aws:.*:\d{12}' "$sops_file_path" &>/dev/null; then
      echo "A bare AWS account ID is exposed in $sops_file_name. Try using 'sopsw -C $sops_file_name' to convert the file, or make the value sensitive."
      exit 1
    fi

    sops_unencrypted_regex="$(yq '.sops.unencrypted_regex' < "$sops_file_path")"
    # Now, if the following naive check for invalid encrypted values fails that means that some
    # encrypted values are not encrypted properly and we should not commit the changes
    if [[ "$(
        UNENCRYPTED_REGEX="$sops_unencrypted_regex" yq 'to_entries
          | map(
              select(.key != "sops"
                and (.key | test(env(UNENCRYPTED_REGEX)) | not)
                and (.value
                  | tostring
                  | test("^ENC\[AES256_GCM,data:(.*),iv:(.*),tag:(.*),type:(.*)\]$")
                  | not
                )
              )
            )
          | length' < "$sops_file_path"
      )" != "0" ]]; then
      echo "There are invalid sensitive values in $sops_file_name. This file cannot be committed as-is. Fix the invalid values and try again."
      exit 1
    fi

    echo "$sops_file_name is OK"
  done
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
      file_path="$(git rev-parse --show-toplevel)/$file"

      # Skip binary formats and groovy files
      case "$extension" in
        "zip" | "p12" | "pfx" | "cer" | "pem" | "png" | "jpg" | "groovy")
          continue
          ;;
        *) ;;
      esac

      firstTwo=$(sed 's/^\(..\).*/\1/;q' "$file_path")
      # check for a hashbang or a .sh extension to determine if this is a shell script.
      if [ "$firstTwo" == "#!" ] && [ "$filename" != "Jenkinsfile" ] || [ "$extension" == "sh" ]; then
        # run shellcheck with severity level warning, and suppress warnings about invalid hashbangs (allows it to ignore other types of scripts, e.g. python)
        if ! shellcheck -e SC1071,SC2239 -S warning "$file_path"; then
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
checkSopsFiles
runShellCheckForCommitFiles
checkGitleaks
