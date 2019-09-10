#!/bin/bash

LOCAL_PATH="/u01/backups/fhirdb/"
S3_PATH="(REPLACE)"

nohup aws s3 sync --no-progress $S3_PATH $LOCAL_PATH >>s3sync.log 2>&1 &
