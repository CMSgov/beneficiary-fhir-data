SELECT count(*)
FROM "BeneficiaryMonthly"
WHERE "yearMonth" = $1::date
AND "partDContractNumberId" = $2