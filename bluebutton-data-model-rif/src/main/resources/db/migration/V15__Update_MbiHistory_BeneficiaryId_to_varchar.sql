/*
 * Alters the MedicareBeneficiaryIdHistory table's `beneficiaryId` to be a varchar(15), making it consistent with our representation in other tables. 
 * 
 * See https://jira.cms.gov/browse/BLUEBUTTON-1010.
 */

alter table "MedicareBeneficiaryIdHistory"
  alter column "beneficiaryId" ${logic.alter-column-type} varchar(15);