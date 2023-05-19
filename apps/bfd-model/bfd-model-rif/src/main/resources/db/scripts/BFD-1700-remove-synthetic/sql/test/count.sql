-- carrier_claim_lines

SELECT COUNT(lines.*) AS carrier_claim_lines
FROM carrier_claim_lines AS lines
LEFT JOIN carrier_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- carrier_claims

SELECT COUNT(*) AS carrier_claims
FROM carrier_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- dme_claim_lines

SELECT COUNT(lines.*) AS dme_claim_lines
FROM dme_claim_lines AS lines
LEFT JOIN dme_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- dme_claims

SELECT COUNT(*) AS dme_claims
FROM dme_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- hha_claim_lines

SELECT COUNT(lines.*) AS hha_claim_lines
FROM hha_claim_lines AS lines
LEFT JOIN hha_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- hha_claims

SELECT COUNT(*) AS hha_claims
FROM hha_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- hospice_claim_lines

SELECT COUNT(lines.*) AS hospice_claim_lines
FROM hospice_claim_lines AS lines
LEFT JOIN hospice_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- hospice_claims

SELECT COUNT(*) AS hospice_claims
FROM hospice_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- inpatient_claim_lines

SELECT COUNT(lines.*) AS inpatient_claim_lines
FROM inpatient_claim_lines AS lines
LEFT JOIN inpatient_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- inpatient_claims

SELECT COUNT(*) AS inpatient_claims
FROM inpatient_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- outpatient_claim_lines

SELECT COUNT(lines.*) AS outpatient_claim_lines
FROM outpatient_claim_lines AS lines
LEFT JOIN outpatient_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- outpatient_claims

SELECT COUNT(*) AS outpatient_claims
FROM outpatient_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- snf_claim_lines

SELECT COUNT(lines.*) AS snf_claim_lines
FROM snf_claim_lines AS lines
LEFT JOIN snf_claims AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- snf_claims

SELECT COUNT(*) AS snf_claims
FROM snf_claims
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- snf_claim_lines_new

SELECT COUNT(lines.*) AS snf_claim_lines_new
FROM snf_claim_lines_new AS lines
LEFT JOIN snf_claims_new AS claims ON (claims.clm_id = lines.clm_id)
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR CAST(bene_id AS CHARACTER VARYING(15)) NOT LIKE '-%';


-- snf_claims_new

SELECT COUNT(*) AS snf_claims_new
FROM snf_claims_new
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR CAST(bene_id AS CHARACTER VARYING(15)) NOT LIKE '-%';


-- beneficiaries_history

SELECT COUNT(*) AS beneficiaries_history
FROM beneficiaries_history
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- beneficiary_monthly

SELECT COUNT(*) AS beneficiary_monthly
FROM beneficiary_monthly
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- beneficiaries_history_invalid_beneficiaries

SELECT COUNT(*) AS beneficiaries_history_invalid_beneficiaries
FROM beneficiaries_history_invalid_beneficiaries
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';

-- partd_events

SELECT COUNT(*) AS partd_events
FROM partd_events
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


-- beneficiaries

SELECT COUNT(*) AS beneficiaries
FROM beneficiaries
WHERE
  bene_id IN ('-201', '-202', '-203', '-204', '-207', '-208', '-209', '-210', '-211', '-212', '-213', '-214', '-215', '-216', '-217', '-218', '-219', '-220', '-221', '-222', '-223', '-400', '-401', '-403', '-410', '-88880000000001')
  OR bene_id NOT LIKE '-%';


