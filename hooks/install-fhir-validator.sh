#!/usr/bin/env bash
set -euo pipefail

run_path="$(git rev-parse --show-toplevel)/apps/bfd-model-idr"
uv run --directory $run_path install-fhir-validator
