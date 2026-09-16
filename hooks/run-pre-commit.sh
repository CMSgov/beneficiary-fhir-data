#!/usr/bin/env bash
set -euo pipefail

./.git/hooks/pre-commit.prek || ./.git/hooks/pre-commit.rerun
