DELETE 
FROM carrier_claim_lines
WHERE clm_id IN (SELECT clm_id
FROM carrier_claims
WHERE clm_id NOT LIKE '-%' AND bene_id LIKE '-%');

DELETE 
FROM carrier_claims
WHERE clm_id NOT LIKE '-%' AND bene_id LIKE '-%';

DELETE 
FROM inpatient_claim_lines
WHERE clm_id IN (SELECT clm_id
FROM inpatient_claims
WHERE clm_id NOT LIKE '-%' AND bene_id LIKE '-%');

DELETE 
FROM inpatient_claims
WHERE clm_id NOT LIKE '-%' AND bene_id LIKE '-%';

DELETE 
FROM outpatient_claim_lines
WHERE clm_id IN (SELECT clm_id
FROM outpatient_claims
WHERE clm_id NOT LIKE '-%' AND bene_id LIKE '-%');

DELETE 
FROM outpatient_claims
WHERE clm_id NOT LIKE '-%' AND bene_id LIKE '-%';

DELETE 
FROM partd_events
WHERE pde_id NOT LIKE '-%' AND bene_id LIKE '-%';