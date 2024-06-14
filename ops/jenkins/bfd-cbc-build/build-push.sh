#!/usr/bin/env bash
set -eou pipefail

BUILD_ROOT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
REPO_ROOT="$(cd "$BUILD_ROOT_DIR" && git rev-parse --show-toplevel)"
# Overridable Defaults
GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
IMAGE_NAME="public.ecr.aws/c2o1d8s9/bfd-cbc-build"
IMAGE_TAG="${CBC_IMAGE_TAG:-"jdk21-mvn3-tfenv3-${GIT_SHORT_HASH}"}"
IMAGE_TAG_LATEST="${CBC_IMAGE_TAG_LATEST:-"jdk21-mvn3-tfenv3-kt1.9-latest"}"
CIPHER_SCRIPT="$(cat "$REPO_ROOT/apps/utils/cipher/cipher.main.kts")"

docker build "$BUILD_ROOT_DIR" \
  --build-arg JAVA_VERSION="${CBC_JAVA_VERSION:-21}" \
  --build-arg MAVEN_VERSION="${CBC_MAVEN_VERSION:-3}" \
  --build-arg PACKER_VERSION="${CBC_PACKER_VERSION:-1.6.6}" \
  --build-arg TFENV_REPO_HASH="${CBC_TFENV_REPO_HASH:-c05c364}" \
  --build-arg TFENV_VERSIONS="${CBC_TFENV_VERSIONS:-1.5.0}" \
  --build-arg PYTHON3_TAR_SOURCE="${CBC_PYTHON3_TAR_SOURCE:-https://www.python.org/ftp/python/3.11.9/Python-3.11.9.tgz}" \
  --build-arg KOTLINC_ZIP_SOURCE="${CBC_KOTLINC_ZIP_SOURCE:-https://github.com/JetBrains/kotlin/releases/download/v1.9.10/kotlin-compiler-1.9.10.zip}" \
  --build-arg CIPHER_SCRIPT="${CIPHER_SCRIPT}" \
  --build-arg YQ_VERSION="${CBC_YQ_VERSION:-4}" \
  --tag "${IMAGE_NAME}:${IMAGE_TAG}" \
  --tag "${IMAGE_NAME}:${IMAGE_TAG_LATEST}"

aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws
docker push "$IMAGE_NAME" --all-tags
