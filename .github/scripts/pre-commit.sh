#!/usr/bin/env bash
set -e

./.github/scripts/pre-commit-check-sop-files.sh
./.github/scripts/pre-commit-shell-check.sh
./.github/scripts/pre-commit-check-git-leaks.sh
