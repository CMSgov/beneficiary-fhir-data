#!/usr/bin/env bash
set -eou pipefail

# Overridable Defaults
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
IMAGE_NAME="public.ecr.aws/c2o1d8s9/bfd-cbc-build"
IMAGE_TAG="${CBC_IMAGE_TAG:-"jdk17-mvn3-tfenv3-${GIT_SHORT_HASH}"}"
IMAGE_TAG_LATEST="${CBC_IMAGE_TAG_LATEST:-"jdk17-mvn3-tfenv3-latest"}"

docker build . \
  --build-arg JAVA_VERSION="${CBC_JAVA_VERSION:-17}" \
  --build-arg MAVEN_VERSION="${CBC_MAVEN_VERSION:-3}" \
  --build-arg PACKER_VERSION="${CBC_PACKER_VERSION:-1.6.6}" \
  --build-arg TFENV_REPO_HASH="${CBC_TFENV_REPO_HASH:-c05c364}" \
  --build-arg TFENV_VERSIONS="${CBC_TFENV_VERSIONS:-0.12.31 1.1.9}" \
  --build-arg PYTHON3_TAR_SOURCE="${CBC_PYTHON3_TAR_SOURCE:-https://www.python.org/ftp/python/3.9.16/Python-3.9.16.tgz}" \
  --build-arg YQ_VERSION="${CBC_YQ_VERSION:-4}" \
  --tag "${IMAGE_NAME}:${IMAGE_TAG}" \
  --tag "${IMAGE_NAME}:${IMAGE_TAG_LATEST}"

aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws
docker push "$IMAGE_NAME" --all-tags
