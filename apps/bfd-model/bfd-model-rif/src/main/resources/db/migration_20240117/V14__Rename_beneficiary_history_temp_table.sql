/*
 * Rename BeneficiariesHistoryTemp table/index/constraint to BeneficariesHistory table/index/constraint
 */

-- Rename Old table,Index and Constraint

alter table public."BeneficiariesHistory" rename to "BeneficiariesHistory_old";
alter index public."BeneficiariesHistory_hicn_idx" rename to "BeneficiariesHistory_hicn_idx_old";
--alter table public."BeneficiariesHistory_old" rename constraint "BeneficiariesHistory_pkey" TO "BeneficiariesHistory_pkey_old";
alter table public."BeneficiariesHistory_old" drop constraint "BeneficiariesHistory_pkey";
alter table public."BeneficiariesHistory_old"
   add constraint "BeneficiariesHistory_pkey_old" primary key ("beneficiaryHistoryId");

-- Rename New table,Index and Constraint
alter table public."BeneficiariesHistoryTemp" rename to "BeneficiariesHistory";
alter index public."BeneficiariesHistoryTemp_hicn_idx" rename to "BeneficiariesHistory_hicn_idx";
--alter table public."BeneficiariesHistory" rename constraint "BeneficiariesHistoryTemp_pkey" TO "BeneficiariesHistory_pkey";   
alter table public."BeneficiariesHistory" drop constraint "BeneficiariesHistoryTemp_pkey";
alter table public."BeneficiariesHistory"
   add constraint "BeneficiariesHistory_pkey" primary key ("beneficiaryHistoryId");

