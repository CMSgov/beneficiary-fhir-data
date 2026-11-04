#!/usr/bin/env bash
set -euo pipefail

base_path="$(git rev-parse --show-toplevel)/apps/bfd-model-idr/src/gen_structure_def/R4-def/"
zip_path="$base_path/def.zip"
curl -L "https://hl7.org/fhir/R4/definitions.json.zip" >"$zip_path"

unzip $zip_path profiles-resources.json -d $base_path
unzip $zip_path profiles-types.json -d $base_path
rm $zip_path
