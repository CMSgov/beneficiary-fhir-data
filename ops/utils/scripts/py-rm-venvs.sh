#!/usr/bin/env bash

# This script deletes the per-project virtual environments that the repository carried before
# it became a uv workspace. Python commands have run from the workspace root environment ever
# since, so a member's .venv is dead weight, and a bare `uv pip` run inside that member reads
# it instead of the root one. Directories named .venv are the only thing this script removes,
# and the workspace root's own .venv is never offered.

set -Eeuo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly REPO_ROOT

# @flag --yes Delete every stale environment without prompting
# @flag --dry-run List the environments and their sizes without deleting anything
eval "$(argc --argc-eval "$0" "$@")"

readonly yes="${argc_yes:-}"
readonly dry_run="${argc_dry_run:-}"

cd "$REPO_ROOT"

# -prune stops the walk at the first .venv, so nothing inside one is ever listed, and
# -mindepth 2 leaves the workspace root's own environment out of the list. A symbolic link
# named .venv does not match -type d, so it is left alone as well.
declare -a stale=()
while IFS= read -r -d '' path; do
	stale+=("$path")
done < <(find . -mindepth 2 -type d -name .venv -prune -print0 | sort -z)

if [[ ${#stale[@]} -eq 0 ]]; then
	echo "no stale virtual environments under ${REPO_ROOT}"
	exit 0
fi

deleted=0
kept=0

# The prompt reads the terminal directly, so that a caller redirecting stdin cannot answer for
# the user. Without a terminal there is no way to ask, so the script stops before any removal.
if [[ -z "$yes" && -z "$dry_run" ]] && ! { : </dev/tty; } 2>/dev/null; then
	echo "error: no terminal to prompt on, use --yes to delete without asking or --dry-run to list" >&2
	exit 1
fi

for path in "${stale[@]}"; do
	# The list already comes from find. This restates the rule so a path can never reach the
	# removal command unless it is a directory below the repository root named .venv.
	if [[ "$path" != ./*/.venv ]]; then
		echo "refusing to touch ${path}" >&2
		continue
	fi

	size="$(du -sh -- "$path" | cut -f1)"

	if [[ -n "$dry_run" ]]; then
		echo "${path} (${size})"
		kept=$((kept + 1))
		continue
	fi

	if [[ -n "$yes" ]]; then
		reply=y
	else
		printf '%s (%s), delete? [y/N] ' "$path" "$size"
		read -r reply </dev/tty
	fi

	case "$reply" in
	y | Y)
		rm -rf -- "$path"
		echo "deleted ${path}"
		deleted=$((deleted + 1))
		;;
	*)
		echo "kept ${path}"
		kept=$((kept + 1))
		;;
	esac
done

echo "${deleted} deleted, ${kept} kept, ${#stale[@]} found"
