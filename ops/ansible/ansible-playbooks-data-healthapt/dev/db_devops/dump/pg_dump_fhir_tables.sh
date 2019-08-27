#! /bin/bash
#################################################
# FHIRDB TABLE PG_DUMP pg_dump_fhir_tables.sh  Scripts
# This script run pg_dump for a table
# It sends email to DBAs only
# Parameter is the table_name to pg_dump
# Created by Jo Kumedzro
# Create Date: 09/15/2017
# Modified Date: 07/12/2018
# Usage: pg_dump_fhir_tables.sh <table_name>
################################################
# *********************************************#
. /var/lib/pgsql/.bash_profile
export TABNAME=$1
export HNAME=`hostname`
export scripts=/u01/scripts
export PGPASSFILE=/var/lib/pgsql/scripts/.pgpass
export script_logs=/u01/scripts/logs
export JOBTIME=/var/lib/pgsql/scripts/pg_dumps/out/$TABNAME.out
export dba_email_list=/var/lib/pgsql/scripts/jo_email_list.txt

export today=`date +%F`
mkdir -p /u01/backups/fhirdb/$today
dumpfile=/u01/backups/fhirdb/$today/tbl_$TABNAME.dmp
echo "***************************************" > $JOBTIME
echo "TABLE: " $TABNAME >> $JOBTIME
echo "***************************************" >> $JOBTIME
echo 'DUMP FILE: ' $dumpfile >> $JOBTIME
echo "" >>$JOBTIME
echo 'START TIME: ' `date`  >> $JOBTIME
echo "" >>$JOBTIME
pg_dump -Fc fhirdb -h localhost -U gditdba --table public.\"$TABNAME\" -b > $dumpfile
pg_dump_status=$?
echo 'END TIME: ' `date`  >> $JOBTIME

bsize='0'
## email output
if [ -s $dumpfile ] || [ $pg_dump_status -eq 0 ] ; then
  bk_status='SUCCESSFUL'
  bsize=`ls -l $dumpfile | awk '{print $5}'`
else
  bk_status='FAILURE'
fi
echo ""  >> $JOBTIME
echo 'DUMP FILE SIZE IN BYTES: ' $bsize  >> $JOBTIME
echo ""  >> $JOBTIME
echo "JOB STATUS: " $bk_status >> $JOBTIME
echo ""  >> $JOBTIME
mailx -s "AWS-FHIR: PG_DUMP Backup Of $TABNAME Table $bk_status" `cat $dba_email_list` < $JOBTIME
## spool to backup report file
cat $JOBTIME >> $BACKUP_REPORT_OUT
## cleanup
find /var/lib/pgsql/scripts/pg_dumps/out/ -name "*.out" -mtime +30 -exec rm -rf {} \;
find /var/lib/pgsql/scripts/pg_dumps/out/ -name "*.RPT" -mtime +30 -exec rm -rf {} \;
find /u01/backups/fhirdb/ -mtime +90 -name "20*" -exec rm -rf {} \;
