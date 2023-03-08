-- PACA-752 uses rdaClaimKey field for claimId instead of dcn

-- Truncate the fiss_claims table since all claimIds are going to change.

${logic.psql-only} TRUNCATE rda.fiss_claims CASCADE;

-- Reset the progress table so we can reingest all FISS claims using the new claimIds.

DELETE FROM rda.rda_api_progress WHERE claim_type = 'FISS';
