###InpatientClaims_res.sh
#########################
echo " Starting InpatientClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_InpatientClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_InpatientClaimLines.dmp
echo " End InpatientClaims Table Restore "
date