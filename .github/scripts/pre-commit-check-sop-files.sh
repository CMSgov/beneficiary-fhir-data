#!/usr/bin/env bash
set -e

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

checkSopsFiles
