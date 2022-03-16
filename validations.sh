#!/bin/bash

failedValidations=()

for file in ./apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2/*.json; do
  if ! java -Xmx3G -Xms2G -jar validator_cli.jar $file -version 4.0; then
    failedValidations+=($file)
  fi
done

echo "${#failedValidations[@]} resources failed validation"

for file in ${failedValidations[@]}; do
  echo "    $file"
done
