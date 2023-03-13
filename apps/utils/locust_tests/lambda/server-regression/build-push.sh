#!/usr/bin/env bash
set -eou pipefail

# Constants
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
AWS_REGION="us-east-1"
PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.$AWS_REGION.amazonaws.com"
IMAGE_NAME="$PRIVATE_REGISTRY_URI/bfd-mgmt-server-regression"
SSM_IMAGE_TAG_PARAMETER="/bfd/mgmt/server/nonsensitive/server_regression_latest_image_tag"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"${GIT_SHORT_HASH}"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

IMAGE_TAGGED_HASH="$IMAGE_NAME:$DOCKER_TAG"
IMAGE_TAGGED_LATEST="$IMAGE_NAME:$DOCKER_TAG_LATEST"

aws ecr-public get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "public.ecr.aws"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"

DOCKER_BUILDKIT=1 # Specified to enable Dockerfile local Dockerignore, see https://stackoverflow.com/a/57774684
docker build . \
  --file "./lambda/server-regression/Dockerfile" \
  --tag "$IMAGE_TAGGED_HASH" \
  --tag "$IMAGE_TAGGED_LATEST" \
  --platform "linux/amd64"

docker image push "$IMAGE_TAGGED_HASH"
docker image push "$IMAGE_TAGGED_LATEST"

aws ssm put-parameter \
  --name "$SSM_IMAGE_TAG_PARAMETER" \
  --value "$DOCKER_TAG" \
  --overwrite \
  --region "$AWS_REGION" | jq
