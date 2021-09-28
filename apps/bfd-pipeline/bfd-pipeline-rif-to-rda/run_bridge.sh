#!/bin/sh

java -cp target/bfd-pipeline-rif-to-rda-1.0.0-SNAPSHOT.jar:target/dependency/* gov.cms.bfd.pipeline.bridge.Application $@