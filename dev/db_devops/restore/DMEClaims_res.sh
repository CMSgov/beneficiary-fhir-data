###DMEClaims_res.sh
####################
echo " Starting DMEClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_DMEClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_DMEClaimLines.dmp
echo " End DMEClaims Table Restore "
date