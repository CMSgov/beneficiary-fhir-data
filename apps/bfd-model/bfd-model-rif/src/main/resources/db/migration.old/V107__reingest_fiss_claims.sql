-- PACA-752 uses rdaClaimKey field for claimId instead of dcn

-- Truncate the fiss_claims table since all claimIds are going to change and
-- we need to set intermediary_nb to be not null.

${logic.psql-only} TRUNCATE rda.fiss_claims CASCADE;

-- Now that the table is truncated we can make intermediary_nb not nullable.

ALTER TABLE rda.fiss_claims ALTER COLUMN intermediary_nb SET NOT NULL;

-- Reset the progress table so we can re-ingest all FISS claims using the new claimIds.

DELETE FROM rda.rda_api_progress WHERE claim_type = 'FISS';
