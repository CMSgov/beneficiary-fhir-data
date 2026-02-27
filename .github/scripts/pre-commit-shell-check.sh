#!/usr/bin/env bash
set -e

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

runShellCheckForCommitFiles
