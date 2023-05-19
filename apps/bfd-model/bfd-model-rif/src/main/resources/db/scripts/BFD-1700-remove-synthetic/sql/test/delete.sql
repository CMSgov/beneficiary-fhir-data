-- carrier_claim_lines

DELETE FROM carrier_claim_lines AS lines
USING carrier_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- carrier_claims

DELETE
FROM carrier_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- dme_claim_lines

DELETE FROM dme_claim_lines AS lines
USING dme_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- dme_claims

DELETE
FROM dme_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- hha_claim_lines

DELETE FROM hha_claim_lines AS lines
USING hha_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- hha_claims

DELETE
FROM hha_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- hospice_claim_lines

DELETE FROM hospice_claim_lines AS lines
USING hospice_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- hospice_claims

DELETE
FROM hospice_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- inpatient_claim_lines

DELETE FROM inpatient_claim_lines AS lines
USING inpatient_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- inpatient_claims

DELETE
FROM inpatient_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- outpatient_claim_lines

DELETE FROM outpatient_claim_lines AS lines
USING outpatient_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- outpatient_claims

DELETE
FROM outpatient_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- snf_claim_lines

DELETE FROM snf_claim_lines AS lines
USING snf_claims AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR bene_id NOT LIKE '-%'
  );


-- snf_claims

DELETE
FROM snf_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- snf_claim_lines_new

DELETE FROM snf_claim_lines_new AS lines
USING snf_claims_new AS claims
WHERE
  claims.clm_id = lines.clm_id
  AND (
    bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
    OR CAST(bene_id AS CHARACTER VARYING(15)) NOT LIKE '-%'
  );


-- snf_claims_new

DELETE
FROM snf_claims_new
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR CAST(bene_id AS CHARACTER VARYING(15)) NOT LIKE '-%';


-- beneficiaries_history

DELETE
FROM beneficiaries_history
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- beneficiary_monthly

DELETE
FROM beneficiary_monthly
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- beneficiaries_history_invalid_beneficiaries

DELETE
FROM beneficiaries_history_invalid_beneficiaries
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';

-- partd_events

DELETE
FROM partd_events
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- beneficiaries

DELETE
FROM beneficiaries
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


