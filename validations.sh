#!/bin/bash

failedValidations=()

ENDPOINT_DIR=apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2

RECENT_FILES=$(git diff --name-only master... | grep $ENDPOINT_DIR | sort | uniq)

if [[ $1 == *-r* ]]; then
  for file in $RECENT_FILES; do
    # Some changes might be deletions, so check if the file still exists
    if [[ -f "$file" ]]; then
      if ! java -Xmx3G -Xms2G -jar validator_cli.jar $file -version 4.0; then
        failedValidations+=($file)
      fi
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