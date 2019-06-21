/*
 * Alters the MedicareBeneficiaryIdHistory table's `beneficiaryId` to be a varchar(15), making it consistent with our representation in other tables. 
 * 
 * See http://issues.hhsdevcloud.us/browse/CBBD-306.
 */

alter table "MedicareBeneficiaryIdHistory"
  alter column "beneficiaryId" ${logic.alter-column-type} varchar(15);