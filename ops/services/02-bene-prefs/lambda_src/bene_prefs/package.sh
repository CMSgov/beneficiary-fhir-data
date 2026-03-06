#!/usr/bin/env bash
# shellcheck disable=SC2164
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
SHA256_SUM="$(git ls-files "$SCRIPT_DIR" \
    | sort \
    | xargs sha256sum \
    | sha256sum | cut -f1 -d ' ')"

uv export \
    --directory "$SCRIPT_DIR" \
    --frozen \
    --no-dev \
    --no-editable \
    -qo requirements.txt

uv pip install \
   --directory "$SCRIPT_DIR" \
   --no-installer-metadata \
   --no-compile-bytecode \
   --python-platform aarch64-manylinux2014 \
   --target packages \
   -qr requirements.txt

cd "${SCRIPT_DIR}/packages"
zip -qr "${SCRIPT_DIR}/package.zip" .
cd "$SCRIPT_DIR"
zip -qr package.zip app

jq --null-input --arg hash "$SHA256_SUM" '{"hash": $hash}'
