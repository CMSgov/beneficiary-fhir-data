### Beneficiaries_res.sh
########################
echo " Starting Beneficiaries Table Restore " 
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_Beneficiaries.dmp
echo " End Beneficiaries Table Restore "
date
