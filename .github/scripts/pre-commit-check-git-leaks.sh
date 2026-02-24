#!/usr/bin/env bash
set -e

checkGitleaks() {
  echo "Attempting to execute gitleaks. This may take a minute..."
  if ! command -v gitleaks >/dev/null; then
    echo "'gitleaks' not found. Install gitleaks before pushing your changes."
    return 1
  fi
  if gitleaks protect --staged --verbose; then
    return 0
  else
    return 1
  fi
}

checkGitleaks
