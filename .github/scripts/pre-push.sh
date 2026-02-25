#!/usr/bin/env bash
set -e

./pre-push-check-java.sh
./pre-push-check-pyright.sh
./pre-push-check-ruff.sh
