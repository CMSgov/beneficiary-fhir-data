#!/usr/bin/env bash

# This script runs the pytest suites of workspace members. With no arguments it runs the suite of
# every member that has tests. --member runs one member, given by project name or by a directory
# relative to the working directory. --list prints the names of the members that have tests.

set -Eeuo pipefail

# shellcheck source=py-workspace-lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/py-workspace-lib.sh"

require_mac_tools fd

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT
START_PWD="$PWD"
readonly START_PWD

# @flag --list List the names of workspace members that have tests
# @option --member <member> Run one member's tests, by project name or directory
eval "$(argc --argc-eval "$0" "$@")"

readonly arg_list="${argc_list:-}"
readonly arg_member="${argc_member:-}"

cd "$REPO_ROOT"

if [[ -n "$arg_list" && -n "$arg_member" ]]; then
	echo "error: --list and --member cannot be combined" >&2
	exit 1
fi

load_uv_members

# Resolves the --member argument to a member directory relative to the repo root, matching a
# directory (relative to the caller's working directory) first and a project name second.
resolve_member() {
	local member="$1" resolved_directory
	if [[ -d "${START_PWD}/${member}" ]]; then
		resolved_directory="$(cd "${START_PWD}/${member}" && pwd)"
		if [[ "$resolved_directory" == "$REPO_ROOT"/* && -f "${resolved_directory}/pyproject.toml" ]]; then
			echo "${resolved_directory#"$REPO_ROOT"/}"
			return 0
		fi
	fi
	if [[ -n "${MEMBER_DIR_BY_NAME[$member]:-}" ]]; then
		echo "${MEMBER_DIR_BY_NAME[$member]}"
		return 0
	fi
	return 1
}

if [[ -n "$arg_list" ]]; then
	members_with_tests
	exit 0
elif [[ -n "$arg_member" ]]; then
	if ! member_dir="$(resolve_member "$arg_member")"; then
		echo "error: '${arg_member}' is not a member name or a path to a member. Members with tests:" >&2
		members_with_tests >&2
		exit 1
	fi
	run_member_tests "$member_dir"
	exit $?
else
	mapfile -t testable_dirs < <(member_dirs_with_tests)
	if [[ ${#testable_dirs[@]} -eq 0 ]]; then
		echo "no workspace member has tests" >&2
		exit 1
	fi
	status=0
	for dir in "${testable_dirs[@]}"; do
		if ! run_member_tests "$dir"; then
			status=1
		fi
	done
	exit "$status"
fi
