#!/usr/bin/env bash
set -euo pipefail

# Re-run after adding any changes if the first one fails
./.git/hooks/pre-commit.prek || ./.git/hooks/pre-commit.rerun
