#!/usr/bin/env bash
set -e

./pre-commit-check-sop-files.sh
./pre-commit-shell-check.sh
./pre-commit-check-git-leaks.sh
