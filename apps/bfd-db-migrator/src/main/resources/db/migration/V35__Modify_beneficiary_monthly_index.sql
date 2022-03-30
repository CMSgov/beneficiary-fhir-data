-- create index for BeneficiaryMonthly for column indexes yearMonth, partDContractNumberId, parentBeneficiary

CREATE INDEX IF NOT EXISTS "BeneficiaryMonthly_partDContractNumId_yearMonth_parentBene_idx"
    ON public."BeneficiaryMonthly" ("partDContractNumberId" ASC, "yearMonth" ASC, "parentBeneficiary" ASC);