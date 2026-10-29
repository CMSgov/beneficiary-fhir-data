#!/usr/bin/env bash
set -euo pipefail

# Git hook shims are copies made by `just bootstrap`, so they drift from the tracked scripts in
# hooks/ whenever those change. Refresh them here so a change takes effect without everyone
# re-running bootstrap: the wrapper invokes the rerun script by path, so the refreshed copy is
# used by the retry within the same commit.
cd "$(git rev-parse --show-toplevel)"
hooks_dir="$(git rev-parse --git-path hooks)"

# A repo that only ran `prek install` has no wrapper to refresh, so build the pair first.
if [[ ! -f "${hooks_dir}/pre-commit.prek" ]]; then
	prek install --force
	mv "${hooks_dir}/pre-commit" "${hooks_dir}/pre-commit.prek"
fi

for pair in "pre-commit run-pre-commit.sh" "pre-commit.rerun rerun-changed.sh"; do
	dest="${hooks_dir}/${pair%% *}"
	src="hooks/${pair#* }"
	if cmp -s "$src" "$dest"; then
		continue
	fi
	# Rename rather than copy so a shim that is mid-execution is not truncated
	tmp="$(mktemp)"
	cp -p "$src" "$tmp"
	mv "$tmp" "$dest"
done
