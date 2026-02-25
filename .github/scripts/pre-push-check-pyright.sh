#!/usr/bin/env bash
set -e

checkPythonPyright() {
  if [ "$(which pyright)" ]; then
    echo 'Verifying Python source code format with Pyright...'
    branch_name="$(git rev-parse --abbrev-ref HEAD 2>&1)"

    git fetch origin 2>&1
    doesRemoteExist="$(git ls-remote origin ${branch_name})"

    tmpfile=$(mktemp)
    if [ "$doesRemoteExist" = '' ]; then
      echo 'No Remote exists, checking apps directory on local for changes to Python files.'
      git diff --name-only origin/master..HEAD apps > "$tmpfile"
    else
      echo "Remote exists, checking apps directory on branch: ${branch_name} for changes to Python files"
      git diff --name-only "origin/${branch_name}:apps" "${branch_name}:apps" > "$tmpfile"
    fi

    if [ -s "$tmpfile" ]; then
      echo "Filter out non-Python files"
      cat $tmpfile | grep .py$ > $tmpfile
    fi

    commits=$(cat "$tmpfile")
    rm "$tmpfile"
    for file in $commits
    do
      if ! pyright "$file"; then
        echo "Please fix errors before continuing."
        exit 1
      fi
    done
  else
    echo 'Please install pyright before committing.'
    exit 1
  fi
}

checkPythonPyright
