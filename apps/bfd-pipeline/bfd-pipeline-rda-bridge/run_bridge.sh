#!/bin/sh

PROJ_DIR=$(dirname "$0")
PROJ_VERSION=${1:-"$(yq .project.parent.version "${PROJ_DIR}/pom.xml")"}

exec java -cp "${PROJ_DIR}/target/bfd-pipeline-rda-bridge-${PROJ_VERSION}.jar:${PROJ_DIR}/target/dependency/*" gov.cms.bfd.pipeline.bridge.RDABridge "$@"
