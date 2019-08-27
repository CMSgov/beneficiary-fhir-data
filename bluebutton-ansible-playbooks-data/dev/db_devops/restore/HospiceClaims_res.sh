###HospiceClaims_res.sh
#######################
echo " Starting HospiceClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_HospiceClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_HospiceClaimLines.dmp
echo " End HospiceClaims Table Restore "
date