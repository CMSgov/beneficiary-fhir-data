#!/usr/bin/env bash

set -Eeou pipefail

if [[ "$#" -ne 1 ]]; then
  exit 1
fi

if [[ ! "$(command -v sops)" ]]; then
  echo "'sops' not found. Try 'brew install sops' and run again"
  exit 1
fi

if [[ ! "$(command -v sponge)" ]]; then
  echo "'sponge' not found. Try 'brew install moreutils' and run again"
  exit 1
fi

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

BFD_ENVIRONMENT="$1"
readonly BFD_ENVIRONMENT

YAML_FILE="$SCRIPT_DIR/$BFD_ENVIRONMENT.sops.yaml"
readonly YAML_FILE

if [[ ! -f "$YAML_FILE" ]]; then
  echo "Invalid environment $BFD_ENVIRONMENT, no corresponding $BFD_ENVIRONMENT.sops.yaml found."
  echo 1
fi

AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query 'Account' --output text)"
readonly AWS_ACCOUNT_ID

# Replace occurrences of ACCOUNT_ID using envsubst and save back to file
ACCOUNT_ID="$AWS_ACCOUNT_ID" envsubst '$ACCOUNT_ID' < "$YAML_FILE" | sponge "$YAML_FILE"

# Then, open with sops for editing now that the full key ARN is there
sops edit "$YAML_FILE"

# Afterwards, the account ID will be in the key arn. Replace all instances with "${ACCOUNT_ID}",
# explicitly
sed -i -e "s/\(arn:.*\)${AWS_ACCOUNT_ID}/\1\$\{ACCOUNT_ID}/" "$YAML_FILE"
