#!/usr/bin/env bash
set -eou pipefail

# Constants
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
AWS_REGION="us-east-1"
PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.$AWS_REGION.amazonaws.com"
IMAGE_NAME="$PRIVATE_REGISTRY_URI/bfd-mgmt-locust-regression"
SSM_IMAGE_TAG_PARAMETER="/bfd/mgmt/server/nonsensitive/locust_regression_suite_latest_image_tag"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"${GIT_SHORT_HASH}"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

docker build . \
  --file "./lambda/bfd-locust-regression/Dockerfile" \
  --tag "$IMAGE_NAME:${DOCKER_TAG}" \
  --tag "$IMAGE_NAME:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64"

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"
docker push "$IMAGE_NAME" --all-tags

aws ssm put-parameter \
  --name "$SSM_IMAGE_TAG_PARAMETER" \
  --value "$DOCKER_TAG" \
  --overwrite \
  --region "$AWS_REGION" | jq
