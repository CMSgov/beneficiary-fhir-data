#!/usr/bin/env bash

if [[ $# != 1 ]] ; then
  printf "Generates data dictionary csv and json files.\nUsage: %s project_version\n" "$(basename "$0")"
  exit 1
fi

PROJECT_VERSION=$1

# Identify the root of the repository as `REPO_ROOT`
REPO_ROOT="$(git rev-parse --show-toplevel)"

# Create the typical, temporary `dist` directory
mkdir -p "${REPO_ROOT}/dist"

# Generate Versioned CSV and JSON Data Dictionaries
for version in V1 V2; do
    python3 "${REPO_ROOT}/apps/bfd-server/dev/bfd-data-dictionary/app/dd-transformer/dd_to_csv.py" \
        --template "${REPO_ROOT}/apps/bfd-server/dev/bfd-data-dictionary/app/dd-transformer/template/${version,,}-to-csv.json" \
        --source "${REPO_ROOT}/apps/bfd-server/dev/bfd-data-dictionary/data/${version}/" \
        --target "${REPO_ROOT}/dist/${version}-data-dictionary-${PROJECT_VERSION}.csv"
    python3 "${REPO_ROOT}/apps/bfd-server/dev/bfd-data-dictionary/app/dd-transformer/dd_to_json.py" \
        --source "${REPO_ROOT}/apps/bfd-server/dev/bfd-data-dictionary/data/${version}/" \
        --target "${REPO_ROOT}/dist/${version}-data-dictionary-${PROJECT_VERSION}.json"
done

