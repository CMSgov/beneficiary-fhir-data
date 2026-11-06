#!/usr/bin/env bash

# This script runs every check a Python change must pass, excluding tests. The same
# checks, plus the workspace member test suites, run as prek hooks on each commit. Pass
# --no-baseline to see the findings that the committed baseline normally suppresses.

set -euo pipefail

# @flag --no-baseline Run basedpyright with an empty baseline, so the suppressed findings are listed
eval "$(argc --argc-eval "$0" "$@")"

readonly no_baseline="${argc_no_baseline:-}"

cd "$(git rev-parse --show-toplevel)"

status=0

echo "== uv lock --check"
uv lock --check || status=1

echo "== ruff check"
uv run ruff check || status=1

echo "== ruff format --check"
uv run ruff format --check || status=1

# basedpyright has no flag that ignores the baseline: it finds .basedpyright/baseline.json by
# name next to the config. --no-baseline points it at an empty baseline in a scratch directory
# instead, which leaves the committed file untouched. Writing that file is its own act,
# `basedpyright --writebaseline`.
basedpyright_args=()
if [[ -n "${no_baseline}" ]]; then
	scratch="$(mktemp -d)"
	trap 'rm -rf "${scratch}"' EXIT
	printf '{"files":{}}\n' >"${scratch}/empty-baseline.json"
	basedpyright_args=(--baselinefile "${scratch}/empty-baseline.json")
fi

echo "== basedpyright${no_baseline:+, baseline disabled}"
uv run basedpyright "${basedpyright_args[@]}" || status=1

if [[ "${status}" -ne 0 ]]; then
	echo "py-check failed, see the output above"
fi

exit "${status}"
