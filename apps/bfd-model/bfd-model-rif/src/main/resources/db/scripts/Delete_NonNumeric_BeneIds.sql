--
-- One-off script to remove the records in TEST that reference the
-- non-numeric bene_id's that all start with 'DEH'. This encompasses
-- all non-numeric data that was found by running queries in
-- Select_NonNumeric_BeneAndClaimIds.sql.
--

DELETE FROM carrier_claim_lines
WHERE clm_id IN (SELECT clm_id FROM carrier_claims WHERE bene_id LIKE 'DEH%');

DELETE FROM carrier_claims
WHERE bene_id LIKE 'DEH%';

DELETE FROM beneficiary_monthly
WHERE bene_id LIKE 'DEH%';

DELETE FROM beneficiaries
WHERE bene_id LIKE 'DEH%';
