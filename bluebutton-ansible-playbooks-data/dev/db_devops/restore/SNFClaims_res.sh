###SNFClaims_res.sh
###################
echo " Starting SNFClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_SNFClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_SNFClaimLines.dmp
echo " End SNFClaims Table Restore "
date