-- Create index for BeneficiaryMonthly for column indexes yearMonth and partDContractNumberId.

CREATE INDEX "BeneficiaryMonthly_yearmonth_partDContractNumberId_idx" 
ON "BeneficiaryMonthly" ("yearMonth", "partDContractNumberId");
