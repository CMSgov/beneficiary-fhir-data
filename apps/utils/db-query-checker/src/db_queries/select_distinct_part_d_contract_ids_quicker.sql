SELECT DISTINCT "partDContractNumberId"
FROM (
  SELECT *
  FROM "BeneficiaryMonthly"
  WHERE "partDContractNumberId" IS NOT NULL
  LIMIT 1000000
) bene_monthly
LIMIT 10