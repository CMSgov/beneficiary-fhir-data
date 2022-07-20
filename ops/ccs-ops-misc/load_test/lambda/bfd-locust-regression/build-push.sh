#!/usr/bin/env bash
set -eou pipefail

# Constants
readonly GIT_SHORT_HASH
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
readonly AWS_REGION
AWS_REGION="us-east-1"
readonly PRIVATE_REGISTRY_URI
PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region $AWS_REGION | jq -r '.registryId').dkr.ecr.$AWS_REGION.amazonaws.com"
readonly IMAGE_NAME
IMAGE_NAME="$PRIVATE_REGISTRY_URI/bfd-mgmt-locust-regression"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"${GIT_SHORT_HASH}"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

docker build . \
  --file "./lambda/bfd-locust-regression/Dockerfile" \
  --tag "$IMAGE_NAME:${DOCKER_TAG}" \
  --tag "$IMAGE_NAME:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64"

aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"
docker push "$IMAGE_NAME" --all-tags
