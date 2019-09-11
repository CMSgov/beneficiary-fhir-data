###########################################
## Script: db_restore.bash
## To Restore from S3 to Local FileSystem
## Usage : db_restore.bash <BAK_DATE>
## <BAK_DATE> Format: 'YYYY-MM-DD'
###########################################
#!/bin/bash

# This script will:
  # Use the BACKUP_DIR parameter to pull the backup compressed file from S3
  # Then it gunzips the file to restore the data for recovery use

# The script expects up to 2 parameters:
#   <BACKUP_DIR> # Required "<20170301>"
#   <ALT_RESTORE_DIR>  # Optional, Default "<BACKUP_DIR>"

unset https_proxy

if [ $# -eq 2 ]
  then
    BACKUP_DIR="$1"
    ALT_RESTORE_DIR="$2"
  elif [ $# -eq 1 ]
    then
      BACKUP_DIR="$1"
      ALT_RESTORE_DIR="$1"
  else
    echo "USAGE: $0 <BACKUP_DIR> <ALT_RESTORE_DIR>"
    echo "If you only provide BACKUP_DIR we will assume the restore"
    echo "goes to the same directory in /u01/backups/rman/dpii"
    exit 10
  fi

let JOBS=0
let JOB_LIMIT=20
let SLEEP_TIME=10

ENC_KEY="arn:aws:kms:us-east-1:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
PATH="/bin:/usr/bin:/usr/local/bin:/app/sftp/bin:/usr/local/aws/bin"

LOG_DIR="/u01/backups/logs"
LOG_FILE="$LOG_DIR/restore_`date +'%Y%m%d.%H%M'`"


# Make sure backup sure exists
BACKUP_BASE="/u01/backups/fhirdb"
TARGET_RESTORE_DIR="$BACKUP_BASE/$ALT_RESTORE_DIR"
cd $BACKUP_BASE
if [ -d $TARGET_RESTORE_DIR ]
  then
  echo "The Restore Directory $TARGET_RESTORE_DIR exists. To prevent any"
  echo "accidental overwrites this restore process will terminate now"
  echo "If you want to restore to $RESTOR_DIR, please move or delete"
  echo "the existing directory and restart this process"
  exit 7
  fi

S3_BASE="s3://tscws302.db/backups"
S3_RESTORE_DIR="$S3_BASE/$BACKUP_DIR"
let FILE_COUNT=`aws s3 ls ${S3_RESTORE_DIR}/ | wc -l`
if [ $FILE_COUNT -eq 0 ]
  then
  echo "No S3 folder $S3_RESTORE_DIR/"
  exit 8
  fi

echo "**** Copy for `date` starting ***" >> $LOG_FILE
echo "**** Starting Copy to $TARGET_RESTORE_DIR *****" >> $LOG_FILE
echo "**** Target S3 location: $S3_BACKUP_DIR *****" >> $LOG_FILE
echo "" >> $LOG_FILE

# Create TARGET_RESTORE_DIR
mkdir $TARGET_RESTORE_DIR
if [ -d $TARGET_RESTORE_DIR ]
  then
    for FILE in `aws s3 ls ${S3_RESTORE_DIR}/ |  awk '{print $4}'`
      do
        while [ $JOBS -gt $JOB_LIMIT ]
          do
            sleep $SLEEP_TIME
            let JOBS=`ps -ef | grep "s3 cp" | grep -v grep | wc -l`
          done
        echo $FILE
        aws s3 cp --sse aws:kms --sse-kms-key-id $ENC_KEY ${S3_RESTORE_DIR}/$FILE ${TARGET_RESTORE_DIR}/$FILE &>> $LOG_FILE &
        let JOBS=`ps -ef | grep "s3 cp" | grep -v grep | wc -l`
      done
  fi
wait
exit 0
