#!/usr/bin/env bash
set -eou pipefail

# Overridable Defaults
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
DOCKER_TAG="${CBC_DOCKER_TAG:-"jdk17-mvn3-an29-tfenv-${GIT_SHORT_HASH}"}"
DOCKER_TAG_LATEST="${CBC_DOCKER_TAG_LATEST:-"jdk17-mvn3-an29-tfenv-latest"}"

docker build . \
  --build-arg JAVA_VERSION="${CBC_JAVA_VERSION:-17}" \
  --build-arg MAVEN_VERSION="${CBC_MAVEN_VERSION:-3}" \
  --build-arg ANSIBLE_VERSION="${CBC_ANSIBLE_VERSION:-2.9.27}" \
  --build-arg PACKER_VERSION="${CBC_PACKER_VERSION:-1.6.6}" \
  --build-arg TFENV_REPO_HASH="${CBC_TFENV_REPO_HASH:-7e89520}" \
  --build-arg TFENV_VERSIONS="${CBC_TFENV_VERSIONS:-0.12.31 1.1.9}" \
  --tag "public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG}"

docker tag "public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG}" "public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG_LATEST}"

aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws
docker push "public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG}"
docker push "public.ecr.aws/c2o1d8s9/bfd-cbc-build:${DOCKER_TAG_LATEST}"
