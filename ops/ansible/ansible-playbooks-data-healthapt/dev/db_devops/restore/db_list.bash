#####################################
### db_list.bash
### To list files on S3
## uaage : db_list.bash <BAK_DATE>
## <BAK_DATE> format: 'YYYY-MM-DD'
#####################################
#!/bin/bash

# This script will:
# List all the backups up in S3
# If a parameter is given it will try to list the files in the folder
# the user provided

unset https_proxy

S3_BASE="s3://tscwsXXX.db/backups"
PATH="/bin:/usr/bin:/usr/local/bin:/app/sftp/bin:/usr/local/aws/bin"

if [ $# -eq 1 ]
  then
    TARGET="$S3_BASE/$1"
  else
    TARGET=$S3_BASE
  fi

aws s3 ls $TARGET/

exit 0
