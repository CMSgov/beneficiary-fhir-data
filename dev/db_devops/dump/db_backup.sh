#!/bin/bash

# This script will:
  # Use the BACKUP_DATE parameter to select the backup that will be synced
  # up to S3

# The script expects 1 parameters:
#   <BACKUP_DATE>
# Note this is the directory the backup files are in. It does not really
# matter if you give it a date or another name.

if [ $# -eq 1 ]
  then
    BACKUP_DATE="$1"
  elif [ $# -eq 0 ]
    then
      BACKUP_DATE="`date +%Y%m%d`"
  else
    echo "USAGE: $0 <BACKUP_DIR>"
    exit 10
  fi

# This is needed to guarantee we are not using the NAT server to get to S3
unset https_proxy

BACKUP_BASE="/u01/backups/fhirdb"
cd $BACKUP_BASE
BACKUP_DIR=`ls -d $BACKUP_DATE*`
if [ $? -ne 0 ]
  then
  echo "No backup directory"
  exit 7
  fi

S3_BASE="s3://tscws###.db/backups"
LOG_DIR="/u01/backups/logs"
PATH="/bin:/usr/bin:/usr/local/bin:/app/sftp/bin:/usr/local/aws/bin"
LOG_FILE="$LOG_DIR/`date +'%Y%m%d.%H%M'`"
ENC_KEY="arn:aws:kms:us-east-1:##########################################"

echo "**** Sync for `date` starting ***" >> $LOG_FILE
echo "**** Starting Sync for $BACKUP_DIR *****" >> $LOG_FILE
echo "**** Target S3 location: $S3_BASE *****" >> $LOG_FILE
echo "" >> $LOG_FILE

# Verify BACKUP_DIR exists
if [ -d $BACKUP_DIR ]
  then
    # We are going to cd to the $BACKUP_BASE and sync all files to S3
    # directly since postgreSQL is compressing them and we see very little
    # benefit from doing a gzip.
    echo "Starting aws sync of ${BACKUP_DIR}: `date`" >>  $LOG_FILE
    aws s3 sync --storage-class STANDARD_IA --sse aws:kms --sse-kms-key-id $ENC_KEY ${BACKUP_BASE}/${BACKUP_DIR} ${S3_BASE}/${BACKUP_DIR} &>> $LOG_FILE
    if [ $? -ne 0 ]
      then
        echo "$BACKUP_DIR failed to transfer up" >> $LOG_FILE
        echo "Transfers for $BACKUP_DIR are ending" >> $LOG_FILE
        exit 12
      else
        echo "$BACKUP_DIR transferred successfully to S3" >> $LOG_FILE
      fi # if $? -ne 0
    echo "Completed aws sync of ${BACKUP_DIR} at: `date`" >>  $LOG_FILE
  else
    echo "$BACKUP_DIR does not exist" >> $LOG_FILE
  fi
exit 0


  fi
wait
exit 0

