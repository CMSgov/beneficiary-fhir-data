#!/usr/bin/env bash

set -Eeou pipefail

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT

BFD_ENV="${1:-prod}"
readonly BFD_ENV

STORED_SHA256_SUM="$(aws lambda get-function --function-name "bfd-${BFD_ENV}-bene-prefs-function" --query Tags.sha256 --output text 2>/dev/null || echo 0)"
readonly STORED_SHA256_SUM

# Hash the app code and the workspace lockfile the layer is built from.
SHA256_SUM="$(git ls-files "$SCRIPT_DIR" "$REPO_ROOT/pyproject.toml" "$REPO_ROOT/uv.lock" |
	grep -v README.md |
	sort |
	xargs sha256sum |
	sha256sum | cut -f1 -d ' ')"
readonly SHA256_SUM

if [[ "$STORED_SHA256_SUM" != "$SHA256_SUM" ]]; then
	uv export \
		--frozen \
		--no-dev \
		--no-editable \
		--project "$SCRIPT_DIR" \
		-qo "${SCRIPT_DIR}/requirements.txt"

	uv pip install \
		--no-installer-metadata \
		--no-compile-bytecode \
		--python-platform aarch64-manylinux2014 \
		--target "${SCRIPT_DIR}/packages" \
		-qr "${SCRIPT_DIR}/requirements.txt"

	(cd "${SCRIPT_DIR}/packages" && zip -qr "${SCRIPT_DIR}/package.zip" .)
	(cd "$SCRIPT_DIR" && zip -qr "${SCRIPT_DIR}/package.zip" app)
fi

jq --null-input --arg hash "$SHA256_SUM" '{"hash": $hash}'
