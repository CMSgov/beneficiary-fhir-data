/*
 * APPLY ONLY TO THE TEST DATABASE
 *
 * Reverts the original version of the V21 migration, which was done in a developer
 * experimental branch, but didn't get into the master branch. 
 *
 * Note: In general, this is a bad idea, but another V21 migration made it into
 * production, so removing this migration was thought as the best fix. See the v19 goof
 * for a similar situation.
 *
 * See: https://jira.cms.gov/browse/BLUEBUTTON-1727
 */

BEGIN;

ALTER TABLE "Beneficiaries" DROP COLUMN "lastupdated";

ALTER TABLE "BeneficiariesHistory" DROP COLUMN "lastupdated";

ALTER TABLE "CarrierClaims" DROP COLUMN "lastupdated";

ALTER TABLE "DMEClaims" DROP COLUMN "lastupdated";

ALTER TABLE "HHAClaims" DROP COLUMN "lastupdated";

ALTER TABLE "HospiceClaims" DROP COLUMN "lastupdated";

ALTER TABLE "InpatientClaims" DROP COLUMN "lastupdated";

ALTER TABLE "MedicareBeneficiaryIdHistory" DROP COLUMN "lastupdated";

ALTER TABLE "OutpatientClaims" DROP COLUMN "lastupdated";

ALTER TABLE "PartDEvents" DROP COLUMN "lastupdated";

ALTER TABLE "SNFClaims" DROP COLUMN "lastupdated";

DELETE FROM schema_version WHERE script = 'V21__Add_lastUpdated_columns.sql';

COMMIT;
