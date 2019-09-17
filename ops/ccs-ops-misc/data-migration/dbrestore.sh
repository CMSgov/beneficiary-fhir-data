#!/bin/bash

export LOCAL_PATH="/u01/backups/fhirdb"
export DB_URI="(REPLACE)"
export DB_JOBS="(REPLACE)"
export PGPASSWORD="(REPLACE)"

function restore_set() {
  for DUMP_FILE in $@; do
    echo "$(date +%F-%T): starting $DUMP_FILE"
    pg_restore -v --dbname=$DB_URI --jobs=$DB_JOBS $LOCAL_PATH/$DUMP_FILE
    echo "$(date +%F-%T): ending $DUMP_FILE"
  done
}
export -f restore_set

nohup bash -c "restore_set tbl_Beneficiaries.dmp" >>dbrestore.log 2>&1 &
wait
nohup bash -c "restore_set tbl_schema_version.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_BeneficiariesHistory.dmp tbl_BeneficiariesHistoryInvalidBeneficiaries.dmp tbl_BeneficiariesHistory_old.dmp tbl_MedicareBeneficiaryIdHistory.dmp tbl_MedicareBeneficiaryIdHistoryInvalidBeneficiaries.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_CarrierClaims.dmp tbl_CarrierClaimLines.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_DMEClaims.dmp tbl_DMEClaimLines.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_HHAClaims.dmp tbl_HHAClaimLines.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_HospiceClaims.dmp tbl_HospiceClaimLines.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_InpatientClaims.dmp tbl_InpatientClaimLines.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_OutpatientClaims.dmp tbl_OutpatientClaimLines.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_PartDEvents.dmp" >>dbrestore.log 2>&1 &
nohup bash -c "restore_set tbl_SNFClaims.dmp tbl_SNFClaimLines.dmp" >>dbrestore.log 2>&1 &
