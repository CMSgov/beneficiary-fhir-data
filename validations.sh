#!/bin/bash

failedValidations=()

ENDPOINT_DIR=apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2

RECENT_FILES=$(git log --name-only --pretty=oneline --full-index $GITHUB_REF..$GITHUB_BASE_REF | grep -vE '^[0-9a-f]{40} ' | sort | uniq | grep $ENDPOINT_DIR)

if [[ $1 == *-r* ]]; then
  for file in $RECENT_FILES; do
    if ! java -Xmx3G -Xms2G -jar validator_cli.jar $file -version 4.0; then
      failedValidations+=($file)
    fi
  done
else
  for file in $ENDPOINT_DIR/*.json; do
    if ! java -Xmx3G -Xms2G -jar validator_cli.jar $file -version 4.0; then
      failedValidations+=($file)
    fi
  done
fi

echo "${#failedValidations[@]} resources failed validation"

for file in ${failedValidations[@]}; do
  echo "    $file"
done
