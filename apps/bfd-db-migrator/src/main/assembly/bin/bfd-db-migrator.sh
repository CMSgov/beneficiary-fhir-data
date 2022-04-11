#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_JAR="${APP_DIR}/$(basename "$APP_DIR").jar"
CLASS_PATH="${APP_JAR}:${APP_DIR}/lib/*"
MAIN_CLASS="gov.cms.bfd.migrator.app.MigratorApp"
JAVA_BIN=$( command -v "${JAVA_HOME}/bin/java" || command -v java )

exec "$JAVA_BIN" -cp "$CLASS_PATH" "$@" "$MAIN_CLASS"
