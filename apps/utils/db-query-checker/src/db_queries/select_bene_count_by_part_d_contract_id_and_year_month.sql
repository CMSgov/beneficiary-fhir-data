SELECT count(*)
FROM ccw.beneficiary_monthly
WHERE year_month = $1::date
AND partd_contract_number_id = $2