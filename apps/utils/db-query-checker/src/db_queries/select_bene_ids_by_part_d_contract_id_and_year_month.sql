SELECT "parentBeneficiary"
FROM "BeneficiaryMonthly"
WHERE "yearMonth" = $1::date
AND "partDContractNumberId" = $2
ORDER BY "parentBeneficiary" ASC
LIMIT $3