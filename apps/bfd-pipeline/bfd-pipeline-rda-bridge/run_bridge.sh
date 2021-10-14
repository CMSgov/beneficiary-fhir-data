#!/bin/sh

PROJ_DIR=$(dirname "$0")

java -cp $PROJ_DIR/target/bfd-pipeline-rif-to-rda-1.0.0-SNAPSHOT.jar:$PROJ_DIR/target/dependency/* gov.cms.bfd.pipeline.bridge.RDABridge $@