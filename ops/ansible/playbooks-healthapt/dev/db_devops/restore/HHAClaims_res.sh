###HHAClaims_res.sh
###################
echo " Starting HHAClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_HHAClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_HHAClaimLines.dmp
echo " End HHAClaims Table Restore "
date