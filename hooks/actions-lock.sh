#!/usr/bin/env bash
set -euo pipefail

# The lockfile is written by the `gh actions-lock` CLI extension, which is not
# installed on a fresh CI runner or a new checkout, so install it when it is
# missing.
extension="github/gh-actions-lock"

if ! command -v gh >/dev/null; then
	echo "The GitHub CLI is needed to check the Actions lockfile."
	exit 1
fi

if ! gh extension list | grep -q "gh-actions-lock"; then
	gh extension install "$extension"
fi

gh actions-lock --verify-local --no-fix
