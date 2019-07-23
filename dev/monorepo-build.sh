#!/bin/bash

##
# Creates a new Git repository by copying the history from a bunch of other Git
# repository's `master` branches into it.
#
# Intended as a one-off script for converting the Beneficiary FHIR Server to a
# monorepo. It will import all of the specified repositories into the
# repository in subdirs of the current directory.
#
# Usage:
#
#     $ monorepo-build.sh
#
# References/credit:
# * <https://medium.com/lgtm/migrating-to-the-monorepo-582106142654'
##

set -e

githubOrg='CMSgov'
declare -a sourceNames=(
  'bluebutton-parent-pom'
  'bluebutton-data-model'
  'bluebutton-data-pipeline'
  'bluebutton-data-server'
  'bluebutton-data-server-perf-tests'
  'bluebutton-ansible-playbooks-data'
  'ansible-role-bluebutton-data-pipeline'
  'ansible-role-bluebutton-data-server'
  'bluebutton-ansible-playbooks-data-sandbox'
  'bluebutton-functional-tests'
  'bluebutton-data-ccw-db-extract'
  'bluebutton-text-to-fhir'
  'bluebutton-csv-codesets'
)

# Verify that we're in an empty Git repository.
if [[ ! -d .git ]]; then
  >&2 echo 'Current directory is not the root of a Git repo.'
  exit 1
fi
if [[ "$(git log --oneline)" ]]; then
  # Note: this isn't a perfect check, just an easy anti-footgun.
  >&2 echo 'Git repo not empty.'
  exit 2
fi

for sourceName in "${sourceNames[@]}"
do
  echo "Migrating '${sourceName}'..."
  
  # Add the source repo as a remote we can fetch from.
  git remote add "${sourceName}" "git@github.com:${githubOrg}/${sourceName}.git"
  git fetch "${sourceName}"

  # Migrate things by:
  # 1. Checking out the source repo's master branch here.
  # 2. Moving everything from the source repo into a subdir, to avoid path conflicts in the monorepo.
  # 3. Committing that change and merging it into the monorepo's master branch.
  # 4. Cleaning up the source repo branch and remote.
  git checkout -b "${sourceName}" "${sourceName}/master"
  mkdir "${sourceName}"
  find . -maxdepth 1 -mindepth 1 -not -name .git -exec git mv {} "${sourceName}/" \;
  git commit -m "Moved '${sourceName}' to monorepo subdir."
  git checkout master
  git merge "${sourceName}" --allow-unrelated-histories -m "Migrated '${sourceName}' to monorepo."
  git branch -D "${sourceName}"

  git remote remove "${sourceName}"
  echo "Migrated '${sourceName}'."
done

echo 'Migration complete.'
