-- Create "beneficiaryId" column indexes for claim tables.

CREATE INDEX beneficiarymonthly__idx ON "BeneficiaryMonthly_yearmonth_partDContractNumberId_idx" ("yearMonth", "partDContractNumberId");
