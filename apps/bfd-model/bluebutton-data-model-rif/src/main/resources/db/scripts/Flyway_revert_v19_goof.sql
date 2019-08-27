/*
 * Reverts the original version of the V19 migratio, which failed in TEST and
 * PROD, but succeeded in DPR (and so needs to be manually rolled back there).
 * 
 * Note: In general, this is a bad idea: Flyway's documentation **really**
 * tries to talk you out of stuff like this. On the other hand, I'm not sure we
 * have any other options to recover from a migration that worked in some
 * environments but failed in others. Also, this feels like a handy thing to
 * have practiced. Worst case: we incur some downtime in DPR while restoring it
 * from a backup.
 * 
 * See: https://jira.cms.gov/browse/BLUEBUTTON-1115
 */

BEGIN;

ALTER TABLE "BeneficiariesHistory" 
  DROP CONSTRAINT "BeneficiariesHistory_beneficiaryId_to_Beneficiary";
INSERT INTO "BeneficiariesHistory"
  SELECT * FROM "BeneficiariesHistoryInvalidBeneficiaries";
DROP TABLE "BeneficiariesHistoryInvalidBeneficiaries";

ALTER TABLE "MedicareBeneficiaryIdHistory" 
   DROP CONSTRAINT "MedicareBeneficiaryIdHistory_beneficiaryId_to_Beneficiary";
INSERT INTO "MedicareBeneficiaryIdHistory"
  SELECT * FROM "MedicareBeneficiaryIdHistoryInvalidBeneficiaries";
DROP TABLE "MedicareBeneficiaryIdHistoryInvalidBeneficiaries";

DELETE FROM schema_version WHERE version = '19';

COMMIT;