#!/usr/bin/env bash
set -e

checkPythonRuff() {
  if [ "$(which ruff)" ]; then
    echo 'Verifying Python source code format with Ruff...'
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
      if ! ruff check "$file"; then
        echo "Please fix errors before continuing."
        exit 1
      fi
    done
  else
    echo 'Please install ruff before committing.'
    exit 1
  fi
}

checkPythonRuff
