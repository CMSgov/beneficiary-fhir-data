#!/usr/bin/env bash

# Sourced by py-test.sh and run-member-tests.sh so only one place defines how workspace member test
# suites are discovered and run. Needs uv and fd on the PATH.

# Exits with a message when a Darwin host lacks the bash version or the named Homebrew tools that
# the workspace scripts need. The script name in the message is derived from the caller. Note
# that a GUI git client can start a commit with a PATH that lacks Homebrew's bin directory, so
# everything is checked here to report that instead of failing later with a builtin error.
require_mac_tools() {
	local script caller_dir repo_root tool
	if [[ "$(uname)" != "Darwin" ]]; then
		return 0
	fi
	caller_dir="$(cd "$(dirname "${BASH_SOURCE[1]}")" && pwd)"
	script="${caller_dir}/$(basename "${BASH_SOURCE[1]}")"
	# Report the script repo-root-relative when it is in a repository; the absolute path otherwise.
	if repo_root="$(git -C "${caller_dir}" rev-parse --show-toplevel 2>/dev/null)"; then
		script="${script#"$repo_root"/}"
	fi
	if ((BASH_VERSINFO[0] < 4)); then
		echo "${script} needs bash 4 or newer on macOS, running ${BASH_VERSION}; run 'just bootstrap' and make sure Homebrew's bin directory is in your PATH" >&2
		exit 1
	fi
	for tool in "$@"; do
		if ! command -v "$tool" >/dev/null 2>&1; then
			echo "${script} needs ${tool} on macOS; run 'just bootstrap' and make sure Homebrew's bin directory is in your PATH" >&2
			exit 1
		fi
	done
}

# Fills MEMBER_DIRS with every workspace member directory relative to the repo root, plus the
# lookup maps MEMBER_DIR_BY_NAME and MEMBER_NAME_BY_DIR. Reads uv once because each `uv workspace`
# call costs about half a second.
load_uv_members() {
	local repo_root member_name member_path member_dir
	repo_root="$(git rev-parse --show-toplevel)"
	declare -ga MEMBER_DIRS=()
	declare -gA MEMBER_DIR_BY_NAME=() MEMBER_NAME_BY_DIR=()
	# shellcheck disable=SC2034  # MEMBER_DIR_BY_NAME is read by callers of load_uv_members
	while IFS=$'\t' read -r member_name member_path; do
		member_dir="${member_path#"$repo_root"/}"
		MEMBER_DIRS+=("$member_dir")
		MEMBER_DIR_BY_NAME["$member_name"]="$member_dir"
		MEMBER_NAME_BY_DIR["$member_dir"]="$member_name"
	done < <(paste <(uv workspace list) <(uv workspace list --paths))

	if [[ ${#MEMBER_DIRS[@]} -eq 0 ]]; then
		echo "error: uv listed no workspace members; install uv and check the workspace root pyproject.toml" >&2
		exit 1
	fi
}

# Returns success when the tree below $1 holds a pytest test file. fd skips git-ignored paths.
dir_has_tests() {
	fd --quiet --type file --regex '^(test_.*|.*_test)\.py$' "$1"
}

# Prints every member directory with pytest test files, in uv's order.
member_dirs_with_tests() {
	local dir
	for dir in "${MEMBER_DIRS[@]}"; do
		if dir_has_tests "$dir"; then
			echo "$dir"
		fi
	done
}

# Prints the names of the members with pytest test files, sorted.
members_with_tests() {
	local dir
	while IFS= read -r dir; do
		echo "${MEMBER_NAME_BY_DIR[$dir]}"
	done < <(member_dirs_with_tests) | sort
}

# Runs the pytest suite of the member whose repo-root-relative directory is $1. Prints a label
# first, reports and succeeds when the member has no test files, and otherwise returns the
# suite's exit status.
run_member_tests() {
	local member_dir="$1"
	if ! dir_has_tests "$member_dir"; then
		echo "no test files found in member ${member_dir}"
		return 0
	fi
	echo "tests for ${MEMBER_NAME_BY_DIR[$member_dir]:-$member_dir}"
	uv run pytest "$member_dir" </dev/null
}
