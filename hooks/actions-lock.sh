#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

# The lockfile is written by the `gh actions-lock` CLI extension, which is not
# installed on a fresh CI runner or a new checkout, so install it when it is
# missing.
extension="github/gh-actions-lock"

if ! command -v gh >/dev/null; then
	echo "The GitHub CLI is needed to keep the Actions lockfile up to date."
	exit 1
fi

if ! gh extension list | grep -q "gh-actions-lock"; then
	gh extension install "$extension"
fi

# CI has nowhere to put a regenerated lockfile, so there the hook only checks it.
if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
	gh actions-lock --verify --no-fix
	exit 0
fi

# Locally the lockfile is rewritten so that editing a workflow is enough on its own.
# prek reports a hook that changed files as a failure, and the rerun in the pre-commit
# shim stages the regenerated lockfile so it lands in the same commit.
gh actions-lock
