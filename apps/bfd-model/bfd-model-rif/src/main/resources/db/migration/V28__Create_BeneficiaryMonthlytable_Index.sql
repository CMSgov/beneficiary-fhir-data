-- Create "beneficiaryId" column indexes for claim tables.

CREATE INDEX "BeneficiaryMonthly_yearmonth_partDContractNumberId_idx" 
ON "BeneficiaryMonthly" ("yearMonth", "partDContractNumberId");
