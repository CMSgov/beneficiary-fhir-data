--
-- Script to locate all records that have non-numeric bene_id's
-- or clm_id's in preparation for the conversion of bene_id from
-- varchar to bigint.
--

-- Find non-numeric bene_id's in claim and claim_lines tables
SELECT COUNT(*) FROM carrier_claim_lines
WHERE clm_id IN (SELECT clm_id FROM carrier_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM carrier_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM dme_claim_lines
WHERE clm_id IN (SELECT clm_id FROM dme_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM dme_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM hha_claim_lines
WHERE clm_id IN (SELECT clm_id FROM hha_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM hha_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM hospice_claim_lines
WHERE clm_id IN (SELECT clm_id FROM hospice_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM hospice_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM inpatient_claim_lines
WHERE clm_id IN (SELECT clm_id FROM inpatient_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM inpatient_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM outpatient_claim_lines
WHERE clm_id IN (SELECT clm_id FROM outpatient_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM outpatient_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM partd_events
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM snf_claim_lines
WHERE clm_id IN (SELECT clm_id FROM snf_claims WHERE bene_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM snf_claims
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

-- Find non-numeric bene_id's in beneficiary tables
SELECT COUNT(*) FROM beneficiaries_history
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM beneficiary_monthly
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM medicare_beneficiaryid_history
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM beneficiaries
WHERE bene_id !~ E'^([-+])?[0-9\.]+$';

-- Find non-numeric clm_id's in claim tables
SELECT COUNT(*) FROM carrier_claim_lines
WHERE clm_id IN (SELECT clm_id FROM carrier_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM carrier_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM dme_claim_lines
WHERE clm_id IN (SELECT clm_id FROM dme_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM dme_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM hha_claim_lines
WHERE clm_id IN (SELECT clm_id FROM hha_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM hha_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM hospice_claim_lines
WHERE clm_id IN (SELECT clm_id FROM hospice_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM hospice_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM inpatient_claim_lines
WHERE clm_id IN (SELECT clm_id FROM inpatient_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM inpatient_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM outpatient_claim_lines
WHERE clm_id IN (SELECT clm_id FROM outpatient_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM outpatient_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM partd_events
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';

SELECT COUNT(*) FROM snf_claim_lines
WHERE clm_id IN (SELECT clm_id FROM snf_claims WHERE clm_id !~ E'^([-+])?[0-9\.]+$');

SELECT COUNT(*) FROM snf_claims
WHERE clm_id !~ E'^([-+])?[0-9\.]+$';
