export today_dir=2019-05-14
export file_date=20190514
export BACKUP_REPORT_OUT=/var/lib/pgsql/scripts/pg_dumps/out/BACKUP_20190514.RPT
export S3_BACKUP_LIST=/var/lib/pgsql/scripts/pg_dumps/out/S3_BACKUP_LIST_20190514.RPT
export BK_STARTING=/var/lib/pgsql/scripts/bk_starting.txt
export email_list=/var/lib/pgsql/scripts/dba_email_list
mailx -s "AWS-FHIR-PROD: STARTING PG_DUMP BACKUP OF TABLES" `cat $email_list`  < $BK_STARTING
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh Beneficiaries > /var/lib/pgsql/scripts/logs/Beneficiaries.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh BeneficiariesHistory > /var/lib/pgsql/scripts/logs/BeneficiariesHistory.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh MedicareBeneficiaryIdHistory > /var/lib/pgsql/scripts/logs/MedicareBeneficiaryIdHistory.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh BeneficiariesHistoryTemp > /var/lib/pgsql/scripts/logs/BeneficiariesHistoryTemp.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh DMEClaimLines > /var/lib/pgsql/scripts/logs/DMEClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh OutpatientClaimLines > /var/lib/pgsql/scripts/logs/OutpatientClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh SNFClaimLines > /var/lib/pgsql/scripts/logs/SNFClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh DMEClaims > /var/lib/pgsql/scripts/logs/DMEClaims.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh OutpatientClaims > /var/lib/pgsql/scripts/logs/OutpatientClaims.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh SNFClaims > /var/lib/pgsql/scripts/logs/SNFClaims.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh schema_version > /var/lib/pgsql/scripts/logs/schema_version.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh HospiceClaimLines > /var/lib/pgsql/scripts/logs/HospiceClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh HHAClaims > /var/lib/pgsql/scripts/logs/HHAClaims.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh PartDEvents > /var/lib/pgsql/scripts/logs/PartDEvents.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh HHAClaimLines > /var/lib/pgsql/scripts/logs/HHAClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh CarrierClaimLines > /var/lib/pgsql/scripts/logs/CarrierClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh InpatientClaimLines > /var/lib/pgsql/scripts/logs/InpatientClaimLines.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh CarrierClaims > /var/lib/pgsql/scripts/logs/CarrierClaims.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh HospiceClaims > /var/lib/pgsql/scripts/logs/HospiceClaims.log 2>&1 &
/var/lib/pgsql/scripts/pg_dumps/fhirdb/pg_dump_fhir_tables.sh InpatientClaims > /var/lib/pgsql/scripts/logs/InpatientClaims.log 2>&1 &
wait
/opt/backups/db_backup.bash 2019-05-14
/opt/backups/db_list.bash 2019-05-14 > $S3_BACKUP_LIST
echo ""  >> $BACKUP_REPORT_OUT
echo "***************************"  >> $BACKUP_REPORT_OUT
echo "BACKUP FILES UPLOADED TO S3"  >> $BACKUP_REPORT_OUT
echo "***************************"  >> $BACKUP_REPORT_OUT
cat $S3_BACKUP_LIST >> $BACKUP_REPORT_OUT
mailx -s "AWS-FHIR-PROD: PG_DUMP BACKUP OF TABLES REPORT - COMPLETE" `cat $email_list`  < $BACKUP_REPORT_OUT
find /u01/backups/fhirdb/ -mtime +5 -name 20* -exec rm -rf {} \;
