/*
 * Reverts the original version of the V22 migration, which was done in a developer
 * experimental branch, but didn't get into the master branch.
 *
 * Note: In general, this is a bad idea, but another V22 migration made it into
 * production, so removing this migration was thought as the best fix. See the v19 goof
 * for a similar situation.
 *
 * See: https://jira.cms.gov/browse/BLUEBUTTON-1727
 */

BEGIN;

DROP VIEW claims_partd;

DROP VIEW benes_monthly;

DROP VIEW benes_mbis;

DROP VIEW benes_hicns;

DROP VIEW benes;

DELETE FROM schema_version WHERE script = 'V22__Add_Beneficiary_and_PartD_views.sql';

COMMIT;
