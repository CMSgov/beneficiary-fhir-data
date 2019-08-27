####CarrierClaims_res.sh
########################
echo " Starting CarrierClaims Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_CarrierClaims.dmp
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_CarrierClaimLines.dmp
echo " End CarrierClaims Table Restore "
date