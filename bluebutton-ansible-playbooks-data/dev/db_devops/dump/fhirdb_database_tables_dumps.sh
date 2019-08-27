#! /bin/bash
#################################################
# fhirdb_database_tables_dumps.sh
# This is to run pg_dump for all tables in the public schema
# This script sends email with a summary report of the table dumps and the upload to S3.
# Created by Jo Kumedzro
# Create Date: 15/09/2017
# modified Date: 12/07/2018
################################################

# *********************************************#
typeset var ME=/tmp/fhirdb_database_tables_dumps.lock
# Check if this script is currently running
# *********************************************#
if [ -f $ME ] ; then
   # the lock file already exists, so what to do?
   if [ "$(ps -p `cat $ME` | wc -l)" -gt 1 ]; then
      # Process is still running
      echo "$0: quit at start: Scripts already running `cat $ME`"
      exit 0
   else
     # process not running, but lock file not deleted?
     echo " $0: orphan lock file warning. lock file deleted."
     rm $ME
   fi
fi
## create lock file and traps
echo $$ > $ME
chmod 777 $ME
trap 'rm -f "$ME" >/dev/null 2>&1' 0
trap "exit 2" 1 2 3 15
##**********************************************#
export PGPASSFILE=/var/lib/pgsql/scripts/.pgpass
export today_dir=`date +%F`
export file_date=$(date +%Y%m%d |sed 's/://g')
export script_home=/var/lib/pgsql/scripts/pg_dumps/fhirdb
export BACKUP_REPORT_OUT=/var/lib/pgsql/scripts/pg_dumps/out/BACKUP_$file_date.RPT
export S3_BACKUP_LIST=/var/lib/pgsql/scripts/pg_dumps/out/S3_BACKUP_LIST_$file_date.RPT
export BK_STARTING=/var/lib/pgsql/scripts/bk_starting.txt

## Change to script_home
cd $script_home
## Generate the report
psql -s -d fhirdb <<SQL
\o fhirdb_database_tables_dumps_t1.dat
select '/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh '|| tablename ||' > /var/lib/pgsql/scripts/logs/'||tablename||'.log 2>&1 &'
from pg_tables where schemaname='public' and tablename not like 'hfj_%' AND tablename not like 'trm_%';
\q
SQL
## remove headings
cat fhirdb_database_tables_dumps_t1.dat |grep "pg_dump_fhir_tables.sh" > fhirdb_database_tables_dumps_t2.dat

## prepare final script
echo "export today_dir=`date +%F`" > /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "export file_date=$(date +%Y%m%d |sed 's/://g')" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "export BACKUP_REPORT_OUT=/var/lib/pgsql/scripts/pg_dumps/out/BACKUP_$file_date.RPT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "export S3_BACKUP_LIST=/var/lib/pgsql/scripts/pg_dumps/out/S3_BACKUP_LIST_$file_date.RPT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "export BK_STARTING=/var/lib/pgsql/scripts/bk_starting.txt" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "export email_list=/var/lib/pgsql/scripts/dba_email_list" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "mailx -s \""AWS-FHIR-PROD: STARTING PG_DUMP BACKUP OF TABLES\"" \`cat \$email_list\`  < \$BK_STARTING" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
cat fhirdb_database_tables_dumps_t2.dat >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "wait" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "/opt/backups/db_backup.bash $today_dir" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "/opt/backups/db_list.bash $today_dir > \$S3_BACKUP_LIST" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "echo \"\"  >> \$BACKUP_REPORT_OUT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "echo \"***************************\"  >> \$BACKUP_REPORT_OUT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "echo \"BACKUP FILES UPLOADED TO S3\"  >> \$BACKUP_REPORT_OUT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "echo \"***************************\"  >> \$BACKUP_REPORT_OUT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "cat \$S3_BACKUP_LIST >> \$BACKUP_REPORT_OUT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "mailx -s \""AWS-FHIR-PROD: PG_DUMP BACKUP OF TABLES REPORT - COMPLETE\"" \`cat \$email_list\`  < \$BACKUP_REPORT_OUT" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
echo "find /u01/backups/fhirdb/ -mtime +5 -name "20*" -exec rm -rf {} \;" >> /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh
sed 's/^ *//' /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_new.sh > /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_tabs_$file_date.sh
## run the script /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_tabs_$file_date.sh
chmod 700 /var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_tabs_$file_date.sh
/var/lib/pgsql/scripts/pg_dumps/fhirdb/fhirdb_pg_dump_all_tabs_$file_date.sh  > /var/lib/pgsql/scripts/logs/fhirdb_pg_dump_all_tabs_$file_date.log 2>&1
