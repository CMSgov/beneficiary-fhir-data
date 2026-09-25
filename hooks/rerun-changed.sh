#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
# Only check for files that have staged and unstaged changes
# This means they were modified by a formatter
changed="$(git status -s | awk '/MM / { print $2 }')"
if [ -n "$changed" ]; then
	git add $changed
	./.git/hooks/pre-commit.prek
else
	# Force a failure here because we don't want the commit to succeed
	# if the original check failed and we don't have any changed files
	# to retry
	exit 1
fi
