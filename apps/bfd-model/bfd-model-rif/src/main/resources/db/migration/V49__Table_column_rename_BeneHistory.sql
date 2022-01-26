-- Rename tables and table columns; syntax:
--
--      psql: alter table public.beneficiaries rename column "beneficiaryId" to bene_id;
--      hsql: alter table public.beneficiaries alter column  "beneficiaryId" rename to bene_id;
--
--      ${logic.alter-rename-column}
--          psql: "rename column"
--          hsql: "alter column"
--
--      ${logic.rename-to}
--          psql: "to"
--          hsql: "rename to"
--
-- BeneficiariesHistory to beneficiaries_history
--
alter table public."BeneficiariesHistory" rename to beneficiaries_history;
alter table public.beneficiaries_history ${logic.alter-rename-column} "beneficiaryHistoryId" ${logic.rename-to} bene_history_id;
alter table public.beneficiaries_history ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries_history ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries_history ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries_history ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries_history ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiHash" ${logic.rename-to} mbi_hash;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} efctv_bgn_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiObsoleteDate" ${logic.rename-to} efctv_end_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
--
-- BeneficiariesHistoryInvalidBeneficiaries to beneficiaries_history_invalid_beneficiaries
--
alter table public."BeneficiariesHistoryInvalidBeneficiaries" rename to beneficiaries_history_invalid_beneficiaries;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryHistoryId" ${logic.rename-to} bene_history_id;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;

-- psql only
${logic.psql-only-alter} index if exists public."BeneficiariesHistoryInvalidBeneficiaries_pkey" rename to beneficiaries_history_invalid_beneficiaries_pkey;
${logic.psql-only-alter} index if exists public."BeneficiariesHistory_pkey" rename to beneficiaries_history_pkey;

${logic.psql-only-alter} table public.beneficiaries_history rename constraint "BeneficiariesHistory_beneficiaryId_to_Beneficiary" to beneficiaries_history_bene_id_to_beneficiary;

-- hsql only
${logic.hsql-only-alter} table public.beneficiaries_history add constraint beneficiaries_history_pkey primary key (bene_history_id);  
${logic.hsql-only-alter} table public.beneficiaries_history_invalid_beneficiaries add constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (bene_history_id);

${logic.hsql-only-alter} table public.beneficiaries_history ADD CONSTRAINT beneficiaries_history_bene_id_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

-- both psql and hsql support non-primary key index renaming
ALTER INDEX "BeneficiariesHistory_beneficiaryId_idx" RENAME TO beneficiaries_history_bene_id_idx;
ALTER INDEX "BeneficiariesHistory_hicn_idx" RENAME TO beneficiaries_history_hicn_idx;
ALTER INDEX "Beneficiaries_history_mbi_hash_idx" RENAME TO beneficiaries_history_mbi_hash_idx;
