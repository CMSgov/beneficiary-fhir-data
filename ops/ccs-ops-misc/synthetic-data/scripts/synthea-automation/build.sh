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

BFD_SYNTHEA_SCRIPTS_DIR="$(dirname "$BUILD_CONTEXT_ROOT_DIR")"
readonly BFD_SYNTHEA_SCRIPTS_DIR

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
  python3 "$BFD_SYNTHEA_SCRIPTS_DIR/$BFD_S3_UTILITIES_SCRIPT" "$SYNTHEA_MAPPING_FILES_DIR" "download_file"
}

download_scripts_files_from_s3() {
  echo "Downloading Synthea script files from S3 to $BUILD_CONTEXT_ROOT_DIR"
  python3 "$BFD_SYNTHEA_SCRIPTS_DIR/$BFD_S3_UTILITIES_SCRIPT" "$BUILD_CONTEXT_ROOT_DIR" "download_script"
}

build_docker_image() {
  # Specified to enable Dockerfile local Dockerignore, see https://stackoverflow.com/a/57774684
  DOCKER_BUILDKIT=1 
  docker build -t "bfd_2234_t5" \
    -f "$DOCKERFILE_PATH" \
    --target "dist" \
    "$BUILD_CONTEXT_ROOT_DIR"
    # --platform "linux/amd64" \
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

clean_up
