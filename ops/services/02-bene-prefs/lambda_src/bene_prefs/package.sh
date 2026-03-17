#!/usr/bin/env bash
# shellcheck disable=SC2164
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
BFD_ENV="${1:-prod}"
STORED_SHA256_SUM="$(aws lambda get-function --function-name "bfd-${BFD_ENV}-bene-prefs-function" --query Tags.sha256 --output text 2>/dev/null || echo 0)"
SHA256_SUM="$(git ls-files "$SCRIPT_DIR" | grep -v README.md \
    | sort \
    | xargs sha256sum \
    | sha256sum | cut -f1 -d ' ')"

if ! [ "$STORED_SHA256_SUM" = "$SHA256_SUM" ]; then
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
    zip -qr "${SCRIPT_DIR}/package.zip" app
fi

jq --null-input --arg hash "$SHA256_SUM" '{"hash": $hash}'
