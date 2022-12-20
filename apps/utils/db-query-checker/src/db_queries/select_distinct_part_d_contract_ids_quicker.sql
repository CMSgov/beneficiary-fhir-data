SELECT DISTINCT partd_contract_number_id
FROM (
  SELECT *
  FROM beneficiary_monthly
  WHERE partd_contract_number_id IS NOT NULL
  LIMIT 1000000
) bene_monthly
LIMIT 10