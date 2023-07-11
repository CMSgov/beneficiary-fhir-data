#!/usr/bin/env bash
set -eou pipefail

# SCRIPT_DIR will return the directory where this script is located, and CONTEXT_DIR will be the
# directory of the Docker build context (locust_tests). This way, this script can be called from
# _any_ directory and there will be no issues
SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
REPO_ROOT="$(git rev-parse --show-toplevel)"
CONTEXT_DIR="${REPO_ROOT}/apps/utils/locust_tests/"

# Constants
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
AWS_REGION="us-east-1"
PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_NAME_NODE="${PRIVATE_REGISTRY_URI}/bfd-mgmt-server-load-node"
SSM_IMAGE_TAG_NODE="/bfd/mgmt/server/nonsensitive/server_load_node_latest_image_tag"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"$GIT_SHORT_HASH"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

# Build tagged node image
docker build "$CONTEXT_DIR" \
  --file "$SCRIPT_DIR/Dockerfile" \
  --target node \
  --tag "${IMAGE_NAME_NODE}:${DOCKER_TAG}" \
  --tag "${IMAGE_NAME_NODE}:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64"

# Get registry password and tell docker to login
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"

# Push image to ECR
docker push "$IMAGE_NAME_NODE" --all-tags

# Put the image tag in SSM
aws ssm put-parameter \
  --name "$SSM_IMAGE_TAG_NODE" \
  --value "${IMAGE_NAME_NODE}:${DOCKER_TAG}" \
  --overwrite \
  --type String \
  --region "$AWS_REGION"
