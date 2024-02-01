-- Create index for BeneficiaryMonthly for column indexes yearMonth and partDContractNumberId.


DROP INDEX "BeneficiaryMonthly_yearmonth_partDContractNumberId_idx";

CREATE INDEX "BeneficiaryMonthly_partDContractNumberId_yearmonth_idx" 
ON "BeneficiaryMonthly" ("partDContractNumberId", "yearMonth");
