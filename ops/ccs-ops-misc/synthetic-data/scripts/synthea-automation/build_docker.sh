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

SYNTHEA_NATIONAL_SCRIPT="national_bfd.sh"
readonly SYNTHEA_NATIONAL_SCRIPT

SYNTHEA_JAR_FILE="synthea-with-dependencies.jar"
readonly SYNTHEA_JAR_FILE

SYNTHEA_LATEST_JAR_URL="https://github.com/synthetichealth/synthea/releases/download/master-branch-latest/${SYNTHEA_JAR_FILE}"
readonly SYNTHEA_LATEST_JAR_URL

AWS_REGION="us-east-1"
readonly AWS_REGION

PRIVATE_REGISTRY_URI="$(aws ecr describe-registry --region "$AWS_REGION" | jq -r '.registryId').dkr.ecr.${AWS_REGION}.amazonaws.com"
readonly PRIVATE_REGISTRY_URI

IMAGE_NAME="${PRIVATE_REGISTRY_URI}/bfd-mgmt-synthea-generation"
readonly IMAGE_NAME

DOCKER_LOCAL_VARIANT_TAG="${DOCKER_LOCAL_VARIANT_TAG_OVERRIDE:-"latest"}"
readonly DOCKER_LOCAL_VARIANT_TAG

ensure_paths() {
  if [ ! -f "$DOCKERFILE_PATH" ]; then
    echo "$DOCKERFILE_PATH not found; is this script running from the correct path?"
    exit 1
  fi
}

setup_python_venv() {
  echo "Setting up local Python3 virtualenv for s3_utilities.py..."
  python3 -m venv .venv
  source .venv/bin/activate
  pip3 install boto3
  deactivate
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
  source .venv/bin/activate
  python3 "$BUILD_CONTEXT_ROOT_DIR/$BFD_S3_UTILITIES_SCRIPT" "$SYNTHEA_MAPPING_FILES_DIR" "download_file"
  deactivate
}

download_scripts_files_from_s3() {
  echo "Downloading Synthea script files from S3 to $BUILD_CONTEXT_ROOT_DIR"
  source .venv/bin/activate
  python3 "$BUILD_CONTEXT_ROOT_DIR/$BFD_S3_UTILITIES_SCRIPT" "$BUILD_CONTEXT_ROOT_DIR" "download_script"
  deactivate
}

build_docker_image() {
  # Specified to enable Dockerfile local Dockerignore, see https://stackoverflow.com/a/57774684
  DOCKER_BUILDKIT=1
  docker build -t "$IMAGE_NAME:$DOCKER_LOCAL_VARIANT_TAG" \
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
  rm -rf "${BUILD_CONTEXT_ROOT_DIR:?}/$SYNTHEA_NATIONAL_SCRIPT"
}

clean_up

ensure_paths
setup_python_venv

download_synthea_properties
download_synthea_latest_jar
download_mapping_files_from_s3
download_scripts_files_from_s3

build_docker_image

clean_up

push_image_to_ecr
