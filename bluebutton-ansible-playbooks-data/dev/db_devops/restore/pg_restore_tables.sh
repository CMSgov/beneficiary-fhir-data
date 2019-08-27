#################################################################
#### pg_restore_tables.sh  ######################################
#### The main restore Script built base on the backup dump files
#### This script includes individual Table restore scripts  #####
#### Created by : Jo Kumedzro
#### Created Date : 06/03/2019
#### Usage: /var/lib/pgsql/scripts/pg_restores/pg_restore_tables.sh  <BAK_DATE> /var/lib/pgsql/scripts/pg_restores/logs/pg_restore_tables.log 2>&1
#### BAK_DATE format : 'YYYY-MM-DD'
#################################################################
export BAK_DATE=$1
/var/lib/pgsql/scripts/pg_restores/Beneficiaries_res.sh       > /var/lib/pgsql/scripts/pg_restores/logs/Beneficiaries_res.log         2>&1 &
wait
/var/lib/pgsql/scripts/pg_restores/misc_tables_res.sh         > /var/lib/pgsql/scripts/pg_restores/logs/misc_tables_res.log           2>&1 &
/var/lib/pgsql/scripts/pg_restores/CarrierClaims_res.sh       > /var/lib/pgsql/scripts/pg_restores/logs/CarrierClaims_res.log         2>&1 &
/var/lib/pgsql/scripts/pg_restores/DMEClaims_res.sh           > /var/lib/pgsql/scripts/pg_restores/logs/DMEClaims_res.log             2>&1 &
/var/lib/pgsql/scripts/pg_restores/HHAClaims_res.sh           > /var/lib/pgsql/scripts/pg_restores/logs/HHAClaims_res.log             2>&1 &
/var/lib/pgsql/scripts/pg_restores/HospiceClaims_res.sh       > /var/lib/pgsql/scripts/pg_restores/logs/HospiceClaims_res.log         2>&1 &
/var/lib/pgsql/scripts/pg_restores/InpatientClaims_res.sh     > /var/lib/pgsql/scripts/pg_restores/logs/InpatientClaims_res.log       2>&1 &
/var/lib/pgsql/scripts/pg_restores/BeneficiariesHistory_res.sh > /var/lib/pgsql/scripts/pg_restores/logs/BeneficiariesHistory_res.log 2>&1 &
/var/lib/pgsql/scripts/pg_restores/OutpatientClaims_res.sh    > /var/lib/pgsql/scripts/pg_restores/logs/OutpatientClaims_res.log      2>&1 &
/var/lib/pgsql/scripts/pg_restores/PartDEvents_res.sh         > /var/lib/pgsql/scripts/pg_restores/logs/PartDEvents_res.log           2>&1 &
/var/lib/pgsql/scripts/pg_restores/SNFClaims_res.sh           > /var/lib/pgsql/scripts/pg_restores/logs/SNFClaims_res.log             2>&1 &