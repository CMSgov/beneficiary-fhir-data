#!/usr/bin/env bash

# This script runs every check a Python change must pass, excluding tests. The same
# checks, plus the workspace member test suites, run as prek hooks on each commit.

set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

status=0

echo "== uv lock --check"
uv lock --check || status=1

echo "== ruff check"
uv run ruff check || status=1

echo "== ruff format --check"
uv run ruff format --check || status=1

echo "== basedpyright"
uv run basedpyright || status=1

if [[ "${status}" -ne 0 ]]; then
	echo "py-check failed, see the output above"
fi

exit "${status}"
