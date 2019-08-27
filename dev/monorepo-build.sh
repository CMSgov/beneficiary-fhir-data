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
#     $ mkdir beneficiary-fhir-server.git && cd beneficiary-fhir-server.git && git init
#     $ ./monorepo-build.sh
#
# References/credit:
# * <https://medium.com/lgtm/migrating-to-the-monorepo-582106142654'
##

set -e
set -o pipefail

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
  find . -maxdepth 1 -mindepth 1 -not -name .git -not -name "${sourceName}" -exec git mv {} "${sourceName}/" \;
  git commit -m "Moved '${sourceName}' to monorepo subdir."
  git checkout master
  git merge "${sourceName}" --allow-unrelated-histories -m "Migrated '${sourceName}' to monorepo."
  git branch -D "${sourceName}"

  git remote remove "${sourceName}"
  echo "Migrated '${sourceName}'."
done

##
# Do some refactoring, while the patient is already in surgery.
##

# Remove the old tags, as they're pretty useless.
git tag | xargs git tag -d

# Reorganize the ops stuff.
opsDir="ops"
mkdir -p "${opsDir}/ansible"
git mv bluebutton-ansible-playbooks-data "${opsDir}/ansible/ansible-playbooks-data"
git mv bluebutton-ansible-playbooks-data-sandbox "${opsDir}/ansible/ansible-playbooks-data-sandbox"
git mv ansible-role-bluebutton-data-pipeline "${opsDir}/ansible/ansible-role-data-pipeline"
git mv ansible-role-bluebutton-data-server "${opsDir}/ansible/ansible-role-data-server"
git mv "${opsDir}/ansible/ansible-playbooks-data/terraform" "${opsDir}/terraform"
git commit -m 'Reorganized ops projects.'

# Reorganize the Java stuff.
javaDir="apps"
mkdir "${javaDir}"
git mv bluebutton-parent-pom/pom.xml "${javaDir}/"
git mv bluebutton-data-server/.gitignore "${javaDir}/"  # Seems to be a superset of the others.
git mv bluebutton-parent-pom/{README,LICENSE,CONTRIBUTING}.md ./
git rm bluebutton-parent-pom/{.gitignore,Jenkinsfile}
git mv bluebutton-parent-pom/dev ./
rmdir bluebutton-parent-pom
git rm bluebutton-data-model/.gitignore
git mv bluebutton-data-model "${javaDir}/bfd-model"  # May want to pull all the sub-modules here up to top-level.
git mv "${javaDir}/bfd-model/rfcs" rfcs
git rm --force "${javaDir}/bfd-model/dev/monorepo-build.sh"
git rm bluebutton-data-pipeline/.gitignore
git mv bluebutton-data-pipeline "${javaDir}/bfd-pipeline"
git mv bluebutton-data-server "${javaDir}/bfd-server"
git rm bluebutton-data-server-perf-tests/.gitignore
git mv bluebutton-data-server-perf-tests "${javaDir}/bfd-server-test-perf"
git rm bluebutton-functional-tests/.gitignore
git mv bluebutton-functional-tests "${javaDir}/bfd-server-test-functions"
git commit -m 'Reorganized Java app projects.'

# Reorganize the CCW stuff.
mkdir ccw
git mv bluebutton-data-ccw-db-extract ccw/rif-extract
git commit -m 'Renamed CCW extract project.'

# Remove deprecated projects.
git rm -r bluebutton-text-to-fhir
git commit -m "Removed deprecated 'bluebutton-text-to-fhir' project."
git rm -r bluebutton-csv-codesets
git commit -m "Removed deprecated 'bluebutton-csv-codesets' project."

# Remove duplicate files.
find . -mindepth 2 -name CONTRIBUTING.md -exec git rm {} \;
find . -mindepth 2 -name LICENSE.md -exec git rm {} \;
git commit -m 'Removeed duplicate license/contribution files.'

# Things to do (probably by hand, afterwards):
#
# * Update Maven groupIds to one single, thing.
# * Update Maven artficactIds to match new folder structure.
# * Update Maven versions to one consistent thing, e.g. 1.0.0-SNAPSHOT.
# * Combine the Data Pipeline modules?
# * Rename all of the Java packages.
# * Get everyone to update to the latest Eclipse.
# * Update our Eclipse auto-formatting preferences:
#     * However many characters I got folks to agree to last time.
#     * Ensure comments get reformatted.
#     * Look for a command line formatter, again.
# * Format all of the Java source.
# * Edit the top-level README to be more useful.
# * Edit the subdir READMEs to be correct.
# * Go through dev/ folders and see what needs to be pulled up or reorganized.
# * Create a top-level Jenkinsfile -- can it call subdir Jenkinsfiles?
# * Move Zulim's Terraform stuff.
# * Recreate any PRs that we want.
# * Archive all of the old projects on GitHub.
# * Publish a Google Group post about the change.

echo 'Migration complete.'
