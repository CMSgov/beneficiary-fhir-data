#!/usr/bin/env bash
set -eou pipefail

# Constants
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
AWS_REGION="us-east-1"
PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_NAME_BROKER="${PRIVATE_REGISTRY_URI}/bfd-mgmt-server-load-broker"
IMAGE_NAME_CONTROLLER="${PRIVATE_REGISTRY_URI}/bfd-mgmt-server-load-controller"
IMAGE_NAME_NODE="${PRIVATE_REGISTRY_URI}/bfd-mgmt-server-load-node"
SSM_IMAGE_TAG_BROKER="/bfd/mgmt/server/nonsensitive/server_load_broker_latest_image_tag"
SSM_IMAGE_TAG_CONTROLLER="/bfd/mgmt/server/nonsensitive/server_load_controller_latest_image_tag"
SSM_IMAGE_TAG_NODE="/bfd/mgmt/server/nonsensitive/server_load_node_latest_image_tag"

# Overridable defaults
DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"$GIT_SHORT_HASH"}"
DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"

docker build . \
  --file ./lambda/server-load/Dockerfile \
  --target broker \
  --tag "${IMAGE_NAME_BROKER}:${DOCKER_TAG}" \
  --tag "${IMAGE_NAME_BROKER}:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64"

docker build . \
  --file ./lambda/server-load/Dockerfile \
  --target controller \
  --tag "${IMAGE_NAME_CONTROLLER}:${DOCKER_TAG}" \
  --tag "${IMAGE_NAME_CONTROLLER}:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64"

docker build . \
  --file ./lambda/server-load/Dockerfile \
  --target node \
  --tag "${IMAGE_NAME_NODE}:${DOCKER_TAG}" \
  --tag "${IMAGE_NAME_NODE}:${DOCKER_TAG_LATEST}" \
  --platform "linux/amd64"

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"

docker push "$IMAGE_NAME_BROKER" --all-tags
docker push "$IMAGE_NAME_CONTROLLER" --all-tags
docker push "$IMAGE_NAME_NODE" --all-tags

aws ssm put-parameter \
  --name "$SSM_IMAGE_TAG_BROKER" \
  --value "${IMAGE_NAME_BROKER}:${DOCKER_TAG}" \
  --overwrite \
  --type String \
  --region "$AWS_REGION"

aws ssm put-parameter \
  --name "$SSM_IMAGE_TAG_CONTROLLER" \
  --value "${IMAGE_NAME_CONTROLLER}:${DOCKER_TAG}" \
  --overwrite \
  --type String \
  --region "$AWS_REGION"

aws ssm put-parameter \
  --name "$SSM_IMAGE_TAG_NODE" \
  --value "${IMAGE_NAME_NODE}:${DOCKER_TAG}" \
  --overwrite \
  --type String \
  --region "$AWS_REGION"
