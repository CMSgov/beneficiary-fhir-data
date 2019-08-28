###BeneficiariesHistory_res.sh
##############################
echo " Starting BeneficiariesHistory Table Restore " 
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_BeneficiariesHistory.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_BeneficiariesHistoryTemp.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_MedicareBeneficiaryIdHistory.dmp
echo " End Beneficiaries Table Restore "
date