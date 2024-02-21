#!/usr/bin/env bash
set -eou pipefail

# SCRIPT_DIR will return the directory where this script is located, and CONTEXT_DIR will be the
# directory of the Docker build context (locust_tests). This way, this script can be called from
# _any_ directory and there will be no issues
SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
REPO_ROOT="$(git rev-parse --show-toplevel)"
# Divine the latest release version of BFD from the latest release on GitHub, and if that fails,
# fallback to parsing the pom.xml
BFD_PARENT_VERSION="$(curl -s https://api.github.com/repos/CMSgov/beneficiary-fhir-data/releases/latest | yq -r '.tag_name')"
if [[ $BFD_PARENT_VERSION == "null" ]]; then
  BFD_PARENT_VERSION="$(yq '.project.version' "$REPO_ROOT/apps/pom.xml")"
fi
CONTEXT_DIR="${REPO_ROOT}/ops/terraform/services/eft/lambda_src/sftp_outbound_transfer"

# Constants
AWS_REGION="us-east-1"
PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | yq -r '.registryId').dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_NAME_NODE="${PRIVATE_REGISTRY_URI}/bfd-mgmt-eft-sftp-outbound-transfer-lambda"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"$BFD_PARENT_VERSION"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"

# Build tagged node image. Note that DOCKER_BUILDKIT is specified to enable Dockerfile local
# Dockerignore, see https://stackoverflow.com/a/57774684
DOCKER_BUILDKIT=1 docker buildx build "$CONTEXT_DIR" \
  --file "$SCRIPT_DIR/Dockerfile" \
  --provenance false \
  --target lambda \
  --tag "${IMAGE_NAME_NODE}:${DOCKER_TAG}" \
  --tag "${IMAGE_NAME_NODE}:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64" \
  --push
