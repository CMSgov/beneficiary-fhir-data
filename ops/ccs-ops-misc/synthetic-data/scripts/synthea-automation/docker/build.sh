#!/usr/bin/env bash
set -eo pipefail

BUILD_ROOT_DIR=${PWD}
readonly BUILD_ROOT_DIR

SYNTHEA_PROPERTIES_RAW_URL="https://raw.githubusercontent.com/synthetichealth/synthea/master/src/main/resources/synthea.properties"
readonly SYNTHEA_PROPERTIES_RAW_URL

SYNTHEA_TEMP_BUILD_DIR="$BUILD_ROOT_DIR/build"
readonly SYNTHEA_TEMP_BUILD_DIR

SYNTHEA_PROPERTIES_FILE_LOCATION="$SYNTHEA_TEMP_BUILD_DIR/props"
readonly SYNTHEA_PROPERTIES_FILE_LOCATION

SYNTHEA_JAR_FILE_LOCATION="$SYNTHEA_TEMP_BUILD_DIR/jar"
readonly SYNTHEA_JAR_FILE_LOCATION

SYNTHEA_MAPPING_FILES_LOCATION="$SYNTHEA_TEMP_BUILD_DIR/mappings"
readonly SYNTHEA_MAPPING_FILES_LOCATION

SYNTHEA_SCRIPT_FILES_LOCATION="$SYNTHEA_TEMP_BUILD_DIR/scripts"
readonly SYNTHEA_SCRIPT_FILES_LOCATION

SYNTHEA_SCRIPTS_DIR="$(dirname "$BUILD_ROOT_DIR")"
readonly SYNTHEA_SCRIPTS_DIR

DOCKERFILE="$BUILD_ROOT_DIR/Dockerfile"
readonly DOCKERFILE

S3_UTILITIES_SCRIPT="$SYNTHEA_SCRIPTS_DIR/s3_utilities.py"
readonly S3_UTILITIES_SCRIPT

PREPARE_RUN_SYNTHEA_SCRIPT="$SYNTHEA_SCRIPTS_DIR/prepare-and-run-synthea.py"
readonly PREPARE_RUN_SYNTHEA_SCRIPT

SYNTHEA_JAR_FILE="synthea-with-dependencies.jar"
readonly SYNTHEA_JAR_FILE

SYNTHEA_LATEST_JAR="https://github.com/synthetichealth/synthea/releases/download/master-branch-latest/${SYNTHEA_JAR_FILE}"
readonly SYNTHEA_LATEST_JAR

ensure_paths() {
  if [ ! -f "$S3_UTILITIES_SCRIPT" ]; then
    echo "$S3_UTILITIES_SCRIPT not found; is this script running from the correct path?"
    exit 1
  fi

  if [ ! -f "$DOCKERFILE" ]; then
    echo "$DOCKERFILE not found; is this script running from the correct path?"
    exit 1
  fi
}

download_synthea_properties() {
  echo "Downloading latest synthea.properties file directly from GitHub to $SYNTHEA_PROPERTIES_FILE_LOCATION"
  mkdir -p "$SYNTHEA_PROPERTIES_FILE_LOCATION"
  curl -LkSs "$SYNTHEA_PROPERTIES_RAW_URL" -o "$SYNTHEA_PROPERTIES_FILE_LOCATION/synthea.properties"
}

download_synthea_latest() {
  echo "Downloading latest Synthea JAR to $SYNTHEA_JAR_FILE_LOCATION"
  mkdir -p "$SYNTHEA_JAR_FILE_LOCATION"
  curl -LkSs "${SYNTHEA_LATEST_JAR}" -o "$SYNTHEA_JAR_FILE_LOCATION/${SYNTHEA_JAR_FILE}"
}

download_mapping_files_from_s3() {
  echo "Downloading Synthea mapping files from S3 to $SYNTHEA_MAPPING_FILES_LOCATION"
  mkdir -p "$SYNTHEA_MAPPING_FILES_LOCATION"
  python3 "$S3_UTILITIES_SCRIPT" "$SYNTHEA_MAPPING_FILES_LOCATION" "download_file"
}

download_scripts_files_from_s3() {
  echo "Downloading Synthea script files from S3 to $SYNTHEA_MAPPING_FILES_LOCATION"
  mkdir -p "$SYNTHEA_SCRIPT_FILES_LOCATION"
  python3 "$S3_UTILITIES_SCRIPT" "$SYNTHEA_SCRIPT_FILES_LOCATION" "download_script"

  shopt -s nullglob
  for file in "$SYNTHEA_SCRIPT_FILES_LOCATION"/*.sh; do
    echo "Making $file executable..."
    chmod +x "$file"
  done
  shopt -u nullglob
}

copy_python_scripts_to_context() {
  cp "$PREPARE_RUN_SYNTHEA_SCRIPT" "$SYNTHEA_TEMP_BUILD_DIR"
}

build_docker_image() {
  docker build -t "bfd_2234_t5" \
    -f "$DOCKERFILE" \
    --build-arg "SYNTHEA_PROPS_DIR=build/props" \
    --build-arg "SYNTHEA_JAR_DIR=build/jar" \
    --build-arg "SYNTHEA_MAPPINGS_DIR=build/mappings" \
    --build-arg "SYNTHEA_SCRIPTS_DIR=build/scripts" \
    --build-arg "RUN_SCRIPT_PATH=build/prepare-and-run-synthea.py" \
    --platform "linux/amd64" \
    "$BUILD_ROOT_DIR"
}

clean_up() {
  rm -rf "$SYNTHEA_TEMP_BUILD_DIR"
}

clean_up

ensure_paths

download_synthea_properties
download_synthea_latest
download_mapping_files_from_s3
download_scripts_files_from_s3

copy_python_scripts_to_context

build_docker_image

clean_up
