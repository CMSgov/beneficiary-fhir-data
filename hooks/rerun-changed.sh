#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
changed="$(git diff --name-only --cached)"
if [ -n "$changed" ]; then
	git add $changed
	./.git/hooks/pre-commit.prek
fi
