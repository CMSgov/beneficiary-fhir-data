#!/usr/bin/env bash
set -e
pwd
./.github/scripts/pre-push-check-java.sh
./.github/scripts/pre-push-check-pyright.sh
./.github/scripts/pre-push-check-ruff.sh
