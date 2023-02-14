#!/usr/bin/env bash
set -eo pipefail

# The build context's directory is the directory of this script, so always ensure that the context
# is set to the proper directory regardless of where this script is called
BUILD_CONTEXT_ROOT_DIR=$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")
ALPINE_VERSION="3.17.1"
AWS_REGION="us-east-1"
REPOSITORY_URI="$(
  aws ecr-public describe-repositories \
    --query "repositories[?repositoryName==\`bfd-availability-checker\`].repositoryUri" \
    --output text
)"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"base-$ALPINE_VERSION"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

# Build tagged image
DOCKER_BUILDKIT=1 # Specified to enable Dockerfile local Dockerignore, see https://stackoverflow.com/a/57774684
docker build "$BUILD_CONTEXT_ROOT_DIR" \
  --file "$BUILD_CONTEXT_ROOT_DIR/Dockerfile" \
  --tag "${REPOSITORY_URI}:${DOCKER_TAG}" \
  --tag "${REPOSITORY_URI}:${DOCKER_TAG_LATEST}" \
  --build-arg "ALPINE_VERSION=$ALPINE_VERSION" \
  --platform "linux/amd64"

# Get registry password and tell docker to login
aws ecr-public get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "public.ecr.aws"

# Push image to ECR
docker push "$REPOSITORY_URI" --all-tags
