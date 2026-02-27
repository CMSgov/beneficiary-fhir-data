#!/usr/bin/env bash
set -e

checkJavaFormat() {
  echo 'Verifying Java source code format...'
  branch_name="$(git rev-parse --abbrev-ref HEAD 2>&1)"

  git fetch origin 2>&1
  localChanges=''
  doesRemoteExist="$(git ls-remote origin "$branch_name")"

  if [[ "$doesRemoteExist" = '' ]]; then
    echo 'No Remote exists, checking apps directory on local for changes.'
    localChanges="$(git log origin/master..HEAD apps)"
  else
    localChanges="$(git diff --name-only "origin/${branch_name}:apps" "${branch_name}:apps" 2>&1)"
  fi

  echo "Verifying Java source code format for branch: ${branch_name} ..."
  if [[ "$localChanges" = '' ]]; then
    echo 'No changes detected. Exiting.'
  else
    set +e

    echo 'Relevant app-directory changes detected. Running fmt plugin check.'
    if mvn -f "$(git rev-parse --show-toplevel)/apps/" com.spotify.fmt:fmt-maven-plugin:check --threads 1C >/dev/null 2>&1; then
      echo 'Verified Java source code format: a-okay.'
    else
      echo "Inconsistencies discovered in formatting check. Run 'mvn com.spotify.fmt:fmt-maven-plugin:check --threads 1C' for details or 'mvn com.spotify.fmt:fmt-maven-plugin:format' to automatically apply the required formatting."
      return 1
    fi
  fi
  return 0
}

checkJavaFormat
