#!/usr/bin/env bash

# Runs the pytest suites of the workspace members affected by a set of changed paths, given one
# per argument. The Prek checks CI workflow calls it with the changed files of a pull request; it
# is not a commit-time hook.
# Each path is attributed to the nearest ancestor directory holding a pyproject.toml. Paths under
# a pyproject.toml that is not a workspace member have no workspace environment to test, so they
# are skipped. Members without test files report and succeed.
# A change to the root pyproject.toml or uv.lock changes every member's environment, so every
# member with tests then runs.
# A new member with tests is covered without touching the workflow.

set -Eeuo pipefail

# shellcheck source=py-workspace-lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/py-workspace-lib.sh"

require_mac_tools grealpath fd

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT
cd "$REPO_ROOT"

# Darwin (MacOS) hosts need Homebrew's bash and coreutils (`grealpath`), both installed by
# `just bootstrap`, and the workspace library needs Homebrew's `fd`.
if [[ "$(uname)" == "Darwin" ]]; then
	realpath_cmd=grealpath
else
	realpath_cmd=realpath
fi
readonly realpath_cmd

load_uv_members

# Prints the nearest ancestor directory of the repo-root-relative path $1 that holds a
# pyproject.toml, or fails if there is none.
member_of() {
	local dir
	dir="$(dirname "$1")"
	while [[ "$dir" != "." ]]; do
		if [[ -f "${dir}/pyproject.toml" ]]; then
			echo "$dir"
			return 0
		fi
		dir="$(dirname "$dir")"
	done
	return 1
}

declare -A member_dirs=()
root_changed=false
for file in "$@"; do
	relative_path="$("$realpath_cmd" -m --relative-to="$REPO_ROOT" "$file")"
	# The root pyproject.toml or uv.lock changed, so every member's environment changed.
	if [[ "$relative_path" == "pyproject.toml" || "$relative_path" == "uv.lock" ]]; then
		root_changed=true
		continue
	fi
	if ! member_dir="$(member_of "$relative_path")"; then
		continue
	fi
	if [[ -n "${MEMBER_NAME_BY_DIR[$member_dir]:-}" ]]; then
		member_dirs["$member_dir"]=1
	fi
done

if [[ "$root_changed" == "true" ]]; then
	while IFS= read -r member_dir; do
		member_dirs["$member_dir"]=1
	done < <(member_dirs_with_tests)
fi

status=0
if [[ ${#member_dirs[@]} -gt 0 ]]; then
	while IFS= read -r member_dir; do
		if ! run_member_tests "$member_dir"; then
			status=1
		fi
	done < <(printf '%s\n' "${!member_dirs[@]}" | sort)
fi
exit "$status"
