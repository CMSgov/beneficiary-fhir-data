#!/usr/bin/env bash
set -Eeuo pipefail

# One pytest hook for every workspace member.
# Each file prek passes is attributed to the nearest ancestor directory with a pyproject.toml. That
# directory is the member the file belongs to.
# A new member with tests is covered without adding a hook to prek.toml.
# A change to the root pyproject.toml or uv.lock changes every member's environment. Every member
# with tests then runs.

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT
cd "$REPO_ROOT"

# Darwin (MacOS) hosts get the GNU tools this script uses from Homebrew (contributors via
# `just bootstrap`):
# 1. `grealpath` (GNU `realpath`), used for path resolution
# 2. bash new enough for the `member_dirs` associative array
# Note that a GUI git client can start a commit with a PATH that holds neither, so the version
# and the tool are checked here to report that instead of failing later with a builtin error.
if [[ "$(uname)" == "Darwin" ]]; then
	if ((BASH_VERSINFO[0] < 4)); then
		echo "hooks/run-member-tests.sh needs bash 4 or newer on macOS, running ${BASH_VERSION}; run 'just bootstrap' and make sure Homebrew's bin directory is in your PATH" >&2
		exit 1
	fi
	if ! command -v grealpath >/dev/null 2>&1; then
		echo "hooks/run-member-tests.sh needs GNU coreutils for grealpath on macOS; run 'just bootstrap'" >&2
		exit 1
	fi
	realpath_cmd=grealpath
else
	realpath_cmd=realpath
fi
readonly realpath_cmd

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

has_tests() {
	grep -qE '(^|/)(test_[^/]*|[^/]*_test)\.py$' <<<"$(git ls-files "$1")" || return 1
}

declare -A member_dirs=()
root_changed=false
for file in "$@"; do
	relative_path="$("$realpath_cmd" -m --relative-to="$REPO_ROOT" "$file")"
	# The root pyproject.toml or uv.lock changed, so we need to run every member with tests.
	if [[ "$relative_path" == "pyproject.toml" || "$relative_path" == "uv.lock" ]]; then
		root_changed=true
		continue
	fi
	if ! member_dir="$(member_of "$relative_path")"; then
		continue
	fi
	member_dirs["$member_dir"]=1
done

if [[ "$root_changed" == "true" ]]; then
	while IFS= read -r pyproject; do
		member_dir="${pyproject%/pyproject.toml}"
		if [[ "$member_dir" != "pyproject.toml" ]]; then
			member_dirs["$member_dir"]=1
		fi
	done < <(git ls-files '*pyproject.toml')
fi

status=0
for member_dir in "${!member_dirs[@]}"; do
	if ! has_tests "$member_dir"; then
		continue
	fi
	echo "pytest ${member_dir}"
	if ! uv run pytest "$member_dir"; then
		status=1
	fi
done
exit "$status"
