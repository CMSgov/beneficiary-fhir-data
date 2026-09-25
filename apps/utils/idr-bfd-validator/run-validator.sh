#!/usr/bin/env bash

set -euo pipefail

source ../scripts/load-idr-credentials.sh prod
source ../scripts/load-bfd-credentials.sh
uv run main.py
