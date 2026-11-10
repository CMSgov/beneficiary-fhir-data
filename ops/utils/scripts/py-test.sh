#!/usr/bin/env bash

# This script runs the pytest suites of workspace members. With no arguments it runs the suite of
# every member that has tests. --member runs one member, given by project name or by a directory
# relative to the working directory. --list prints the names of the members that have tests.

set -Eeuo pipefail

# Darwin (MacOS) hosts need a bash new enough for the `mapfile` and the associative arrays below,
# which Homebrew provides. `fd` comes from Homebrew too, installed by the same `just bootstrap`.
# Note that a GUI git client can start a commit with a PATH that has neither, so both are
# checked here to report that instead of failing later with a builtin error.
if [[ "$(uname)" == "Darwin" ]]; then
	if ((BASH_VERSINFO[0] < 4)); then
		echo "ops/utils/scripts/py-test.sh needs bash 4 or newer on macOS, running ${BASH_VERSION}; run 'just bootstrap' and make sure Homebrew's bin directory is in your PATH" >&2
		exit 1
	fi
	if ! command -v fd >/dev/null 2>&1; then
		echo "ops/utils/scripts/py-test.sh needs fd on macOS; run 'just bootstrap'" >&2
		exit 1
	fi
fi

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

# Workspace members, read once because each `uv workspace` call costs about half a second.
declare -a MEMBER_DIRS=()
declare -A MEMBER_DIR_BY_NAME=()
declare -A MEMBER_NAME_BY_DIR=()
while IFS=$'\t' read -r member_name member_path; do
	member_dir="${member_path#"$REPO_ROOT"/}"
	MEMBER_DIRS+=("$member_dir")
	MEMBER_DIR_BY_NAME["$member_name"]="$member_dir"
	MEMBER_NAME_BY_DIR["$member_dir"]="$member_name"
done < <(paste <(uv workspace list) <(uv workspace list --paths))

if [[ ${#MEMBER_DIRS[@]} -eq 0 ]]; then
	echo "error: uv listed no workspace members; install uv and check the workspace root pyproject.toml" >&2
	exit 1
fi
readonly -a MEMBER_DIRS
readonly -A MEMBER_DIR_BY_NAME MEMBER_NAME_BY_DIR

# Returns success when the tree below $1 holds a pytest test file. fd skips git-ignored paths
has_tests() {
	fd --quiet --type file --regex '^(test_.*|.*_test)\.py$' "$1"
}

# Prints every member directory with pytest test files, in uv's order.
test_dirs() {
	local dir
	for dir in "${MEMBER_DIRS[@]}"; do
		if has_tests "$dir"; then
			echo "$dir"
		fi
	done
}

# Prints the names of the members with pytest test files, sorted.
test_names() {
	local dir
	while IFS= read -r dir; do
		echo "${MEMBER_NAME_BY_DIR[$dir]}"
	done < <(test_dirs) | sort
}

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
	test_names
	exit 0
elif [[ -n "$arg_member" ]]; then
	if ! member_dir="$(resolve_member "$arg_member")"; then
		echo "error: '${arg_member}' is not a member name or a path to a member. Members with tests:" >&2
		test_names >&2
		exit 1
	fi
	if ! has_tests "$member_dir"; then
		echo "no test files found in member ${member_dir}"
		exit 0
	fi
	echo "tests for ${MEMBER_NAME_BY_DIR[$member_dir]:-$member_dir}"
	exec uv run pytest "$member_dir"
else
	mapfile -t testable_dirs < <(test_dirs)
	if [[ ${#testable_dirs[@]} -eq 0 ]]; then
		echo "no workspace member has tests" >&2
		exit 1
	fi
	status=0
	for dir in "${testable_dirs[@]}"; do
		echo "tests for ${MEMBER_NAME_BY_DIR[$dir]}"
		if ! uv run pytest "$dir" </dev/null; then
			status=1
		fi
	done
	exit "$status"
fi
