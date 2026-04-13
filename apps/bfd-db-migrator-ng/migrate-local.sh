#!/usr/bin/env bash

SCRIPT_DIR=$(path=$(realpath "$0") && dirname "$path")
readonly SCRIPT_DIR

BFD_ENV=local "$SCRIPT_DIR"/migrate.sh
