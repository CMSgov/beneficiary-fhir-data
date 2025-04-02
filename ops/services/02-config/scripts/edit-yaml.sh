#!/usr/bin/env bash

set -Eeou pipefail

if [[ "$#" -ne 1 ]]; then
  echo "No argument provided. 'test', 'prod-sbx', or 'prod' is required"
  exit 1
fi

BFD_ENVIRONMENT="$1"
readonly BFD_ENVIRONMENT

if [[ $BFD_ENVIRONMENT != "test" && $BFD_ENVIRONMENT != "prod-sbx" && $BFD_ENVIRONMENT != "prod" ]]; then
  echo "1st argument must be one of 'test', 'prod-sbx', or 'prod'"
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

SERVICE_DIR="$(dirname "$SCRIPT_DIR")"
readonly SERVICE_DIR

YAML_FILE="$SERVICE_DIR/values/$BFD_ENVIRONMENT.sops.yaml"
readonly YAML_FILE

if [[ ! -f "$YAML_FILE" ]]; then
  echo "Invalid environment $BFD_ENVIRONMENT, no corresponding $BFD_ENVIRONMENT.sops.yaml found."
  echo 1
fi

AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query 'Account' --output text)"
readonly AWS_ACCOUNT_ID

cleanup() {
  sed "s/\(arn:.*\)${AWS_ACCOUNT_ID}/\1\$\{ACCOUNT_ID}/" <"$YAML_FILE" | sponge "$YAML_FILE"
  exit
}
# After editing, the account ID will be in the key arn. This trap ensures all literal account IDs
# will be replaced
trap cleanup ERR EXIT

# Replace occurrences of ACCOUNT_ID using envsubst and save back to file
ACCOUNT_ID="$AWS_ACCOUNT_ID" envsubst '$ACCOUNT_ID' <"$YAML_FILE" | sponge "$YAML_FILE"

# Then, open with sops for editing now that the full key ARN is there
(cd "$SERVICE_DIR/values" && sops edit  "$YAML_FILE") || exit # Exit on failure so that the trap will replace the literal account ID with a placeholder
