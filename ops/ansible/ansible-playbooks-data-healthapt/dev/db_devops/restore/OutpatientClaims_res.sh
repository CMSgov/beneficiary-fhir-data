###OutpatientClaims_res.sh
##########################
echo " Starting OutpatientClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_OutpatientClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_OutpatientClaimLines.dmp
echo " End OutpatientClaims Table Restore "
date
