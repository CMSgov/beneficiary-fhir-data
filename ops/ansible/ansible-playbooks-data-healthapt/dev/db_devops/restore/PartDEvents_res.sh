###PartDEvents_res.sh
#####################
echo " Starting PartDEvents Table Restore "
date
pg_restore -h localhost -U gditdba -C -d fhirdb /u01/backups/fhirdb/$BAK_DATE/tbl_PartDEvents.dmp
echo " End PartDEvents Table Restore "
date