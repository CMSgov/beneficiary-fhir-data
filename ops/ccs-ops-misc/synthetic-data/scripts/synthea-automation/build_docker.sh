#!/usr/bin/env bash
set -eo pipefail

# The build context's directory is the directory of this script, so always ensure that the context
# is set to the proper directory regardless of where this script is called
BUILD_CONTEXT_ROOT_DIR=$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")
readonly BUILD_CONTEXT_ROOT_DIR

SYNTHEA_PROPERTIES_RAW_URL="https://raw.githubusercontent.com/synthetichealth/synthea/master/src/main/resources/synthea.properties"
readonly SYNTHEA_PROPERTIES_RAW_URL

SYNTHEA_PROPERTIES_FILE_DIR="$BUILD_CONTEXT_ROOT_DIR/src/main/resources"
readonly SYNTHEA_PROPERTIES_FILE_DIR

SYNTHEA_MAPPING_FILES_DIR="$BUILD_CONTEXT_ROOT_DIR/src/main/resources/export"
readonly SYNTHEA_MAPPING_FILES_DIR

DOCKERFILE_PATH="$BUILD_CONTEXT_ROOT_DIR/Dockerfile"
readonly DOCKERFILE_PATH

BFD_S3_UTILITIES_SCRIPT="s3_utilities.py"
readonly BFD_S3_UTILITIES_SCRIPT

SYNTHEA_NATIONAL_V1_SCRIPT="national_bfd.sh"
readonly SYNTHEA_NATIONAL_V1_SCRIPT

SYNTHEA_NATIONAL_V2_SCRIPT="national_bfd_v2.sh"
readonly SYNTHEA_NATIONAL_V2_SCRIPT

SYNTHEA_JAR_FILE="synthea-with-dependencies.jar"
readonly SYNTHEA_JAR_FILE

SYNTHEA_LATEST_JAR_URL="https://github.com/synthetichealth/synthea/releases/download/master-branch-latest/${SYNTHEA_JAR_FILE}"
readonly SYNTHEA_LATEST_JAR_URL

GIT_SHORT_HASH="$(git rev-parse --short HEAD)"
readonly GIT_SHORT_HASH

AWS_REGION="us-east-1"
readonly AWS_REGION

PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.${AWS_REGION}.amazonaws.com"
readonly PRIVATE_REGISTRY_URI

IMAGE_NAME="${PRIVATE_REGISTRY_URI}/bfd-mgmt-synthea-generation"
readonly IMAGE_NAME

SSM_IMAGE_TAG="/bfd/mgmt/common/nonsensitive/synthea_generation_latest_image_tag"
readonly SSM_IMAGE_TAG

DOCKER_TAG="${DOCKER_TAG_OVERRIDE:-"$GIT_SHORT_HASH"}"
readonly DOCKER_TAG

DOCKER_TAG_LATEST="${DOCKER_TAG_LATEST_OVERRIDE:-"latest"}"
readonly DOCKER_TAG_LATEST

ensure_paths() {
  if [ ! -f "$DOCKERFILE_PATH" ]; then
    echo "$DOCKERFILE_PATH not found; is this script running from the correct path?"
    exit 1
  fi
}

download_synthea_properties() {
  echo "Downloading latest synthea.properties file directly from GitHub to $SYNTHEA_PROPERTIES_FILE_DIR"
  mkdir -p "$SYNTHEA_PROPERTIES_FILE_DIR"
  curl -LkSs "$SYNTHEA_PROPERTIES_RAW_URL" -o "$SYNTHEA_PROPERTIES_FILE_DIR/synthea.properties"
}

download_synthea_latest_jar() {
  echo "Downloading latest Synthea JAR to $BUILD_CONTEXT_ROOT_DIR"
  curl -LkSs "${SYNTHEA_LATEST_JAR_URL}" -o "$BUILD_CONTEXT_ROOT_DIR/${SYNTHEA_JAR_FILE}"
}

download_mapping_files_from_s3() {
  echo "Downloading Synthea mapping files from S3 to $SYNTHEA_MAPPING_FILES_DIR"
  mkdir -p "$SYNTHEA_MAPPING_FILES_DIR"
  python3 "$BUILD_CONTEXT_ROOT_DIR/$BFD_S3_UTILITIES_SCRIPT" "$SYNTHEA_MAPPING_FILES_DIR" "download_file"
}

download_scripts_files_from_s3() {
  echo "Downloading Synthea script files from S3 to $BUILD_CONTEXT_ROOT_DIR"
  python3 "$BUILD_CONTEXT_ROOT_DIR/$BFD_S3_UTILITIES_SCRIPT" "$BUILD_CONTEXT_ROOT_DIR" "download_script"
}

build_docker_image() {
  # Specified to enable Dockerfile local Dockerignore, see https://stackoverflow.com/a/57774684
  DOCKER_BUILDKIT=1 
  docker build -t "$IMAGE_NAME:$DOCKER_TAG" \
    -t "$IMAGE_NAME:$DOCKER_TAG_LATEST" \
    -f "$DOCKERFILE_PATH" \
    --target "dist" \
    --platform "linux/amd64" \
    "$BUILD_CONTEXT_ROOT_DIR"
}

push_image_to_ecr() {
  # Get registry password and tell docker to login
  aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$PRIVATE_REGISTRY_URI"

  # Push image to ECR
  docker push "$IMAGE_NAME" --all-tags
}

clean_up() {
  rm -rf "${BUILD_CONTEXT_ROOT_DIR:?}/src"
  rm -rf "${BUILD_CONTEXT_ROOT_DIR:?}/$SYNTHEA_JAR_FILE"
  rm -rf "${BUILD_CONTEXT_ROOT_DIR:?}/$SYNTHEA_NATIONAL_V1_SCRIPT"
  rm -rf "${BUILD_CONTEXT_ROOT_DIR:?}/$SYNTHEA_NATIONAL_V2_SCRIPT"
}

clean_up

ensure_paths

download_synthea_properties
download_synthea_latest_jar
download_mapping_files_from_s3
download_scripts_files_from_s3

build_docker_image
push_image_to_ecr

clean_up
