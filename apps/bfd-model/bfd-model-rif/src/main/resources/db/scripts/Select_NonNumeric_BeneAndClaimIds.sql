--
-- Script to locate all records that have non-numeric bene_id's
-- or clm_id's in preparation for the conversion of bene_id from
-- varchar to bigint. All of these queries should return zero
-- on all environments as a precondition to that migration.
--

-- Find non-numeric bene_id's in claim and claim_lines tables
SELECT clm_id FROM carrier_claim_lines
WHERE clm_id IN (SELECT clm_id FROM carrier_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM carrier_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT clm_id FROM dme_claim_lines
WHERE clm_id IN (SELECT clm_id FROM dme_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM dme_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT clm_id FROM hha_claim_lines
WHERE clm_id IN (SELECT clm_id FROM hha_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM hha_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT clm_id FROM hospice_claim_lines
WHERE clm_id IN (SELECT clm_id FROM hospice_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM hospice_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT clm_id FROM inpatient_claim_lines
WHERE clm_id IN (SELECT clm_id FROM inpatient_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM inpatient_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT clm_id FROM outpatient_claim_lines
WHERE clm_id IN (SELECT clm_id FROM outpatient_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM outpatient_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT pde_id FROM partd_events
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT clm_id FROM snf_claim_lines
WHERE clm_id IN (SELECT clm_id FROM snf_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT bene_id, clm_id FROM snf_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$' OR clm_id !~ E'^([-+])?[0-9\.]+$';

-- Find non-numeric bene_id's in beneficiary tables
SELECT bene_id FROM beneficiaries_history
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT bene_id FROM beneficiary_monthly
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT bene_id FROM medicare_beneficiaryid_history
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT bene_id FROM beneficiaries
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';
