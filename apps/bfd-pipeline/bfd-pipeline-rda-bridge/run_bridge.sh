#!/bin/sh

PROJ_DIR=$(dirname "$0")
VERSION_NUM=$2

exec java -cp "$PROJ_DIR/target/bfd-pipeline-rda-bridge-$VERSION_NUM-SNAPSHOT.jar:$PROJ_DIR/target/dependency/*" gov.cms.bfd.pipeline.bridge.RDABridge "$@"
