--
-- NOTES:
--   1. when you rename a table, indexes/constraints will trickle down to contraint & index directives,
--      BUT not contraint or index names themselves
--   2. don't try to rename a column that already has the name (i.e., "hicn" ${logic.rename-to} hicn)
--   3. optionally rename contraint and/or index names (i.e., remove camelCase)
--
-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file prior to performing table or column rename.
-- SCRIPT './bfd_schema_pre.txt';
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
--
-- BeneficiaryMonthly to beneficiary_monthly
--
alter table public."BeneficiaryMonthly" rename to beneficiary_monthly;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "yearMonth" ${logic.rename-to} year_month;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "parentBeneficiary" ${logic.rename-to} bene_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDContractNumberId" ${logic.rename-to} partd_contract_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCContractNumberId" ${logic.rename-to} partc_contract_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "fipsStateCntyCode" ${logic.rename-to} fips_state_cnty_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "medicareStatusCode" ${logic.rename-to} medicare_status_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "entitlementBuyInInd" ${logic.rename-to} entitlement_buy_in_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "hmoIndicatorInd" ${logic.rename-to} hmo_indicator_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCPbpNumberId" ${logic.rename-to} partc_pbp_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCPlanTypeCode" ${logic.rename-to} partc_plan_type_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDPbpNumberId" ${logic.rename-to} partd_pbp_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDSegmentNumberId" ${logic.rename-to} partd_segment_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDRetireeDrugSubsidyInd" ${logic.rename-to} partd_retiree_drug_subsidy_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDLowIncomeCostShareGroupCode" ${logic.rename-to} partd_low_income_cost_share_group_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "medicaidDualEligibilityCode" ${logic.rename-to} medicaid_dual_eligibility_code;
--
-- LoadedFiles to loaded_files
--
-- We have a bit of a funky condition between psql and hsql; for both loaded_files and loaded_batches
-- there is a column called "created". For psql there is no need to do a rename; in fact if we tried
-- to do something like:
--
--      psql: alter table public.loaded_files rename column "created" to created
--
-- we'd get an error. So in theory, maybe we don't even need to do a rename for that type of condition.
-- However, in hsql, if we don't do a rename, we end up with a column called "created" (ltierally,
-- meaning the double-quotes are an integral part of the column name). So for hsql we do need to
-- perform the rename so we can rid the column name of the double-quotes.
--
--      ${logic.hsql-only-alter}
--          psql: "--"
--          hsql: "alter"
--
alter table public."LoadedFiles" rename to loaded_files;
alter table public.loaded_files ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_fileid;
alter table public.loaded_files ${logic.alter-rename-column} "rifType" ${logic.rename-to} rif_type;
${logic.hsql-only-alter} table public.loaded_files ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- LoadedBatches to loaded_batches
--
alter table public."LoadedBatches" rename to loaded_batches;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedBatchId" ${logic.rename-to} loaded_batchid;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_fileid;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "beneficiaries" ${logic.rename-to} beneficiaries;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- MedicareBeneficiaryIdHistory to medicare_beneficiaryid_history
--
alter table public."MedicareBeneficiaryIdHistory" rename to medicare_beneficiaryid_history;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "medicareBeneficiaryIdKey" ${logic.rename-to} bene_mbi_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "claimAccountNumber" ${logic.rename-to} bene_clm_acnt_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "beneficiaryIdCode" ${logic.rename-to} bene_ident_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiCrntRecIndId" ${logic.rename-to} bene_crnt_rec_ind_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiSequenceNumber" ${logic.rename-to} mbi_sqnc_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} mbi_efctv_bgn_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEndDate" ${logic.rename-to} mbi_efctv_end_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEffectiveReasonCode" ${logic.rename-to} mbi_bgn_rsn_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEndReasonCode" ${logic.rename-to} mbi_end_rsn_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiCardRequestDate" ${logic.rename-to} mbi_card_rqst_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiAddUser" ${logic.rename-to} creat_user_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiAddDate" ${logic.rename-to} creat_ts;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiUpdateUser" ${logic.rename-to} updt_user_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiUpdateDate" ${logic.rename-to} updt_ts;
--
-- MedicareBeneficiaryIdHistoryInvalidBeneficiaries to medicare_beneficiaryid_history_invalid_beneficiaries
--
alter table public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries" rename to medicare_beneficiaryid_history_invalid_beneficiaries;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryIdKey" ${logic.rename-to} bene_mbi_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "claimAccountNumber" ${logic.rename-to} bene_clm_acnt_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryIdCode" ${logic.rename-to} bene_ident_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiCrntRecIndId" ${logic.rename-to} bene_crnt_rec_ind_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiSequenceNumber" ${logic.rename-to} mbi_sqnc_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} mbi_efctv_bgn_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEndDate" ${logic.rename-to} mbi_efctv_end_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEffectiveReasonCode" ${logic.rename-to} mbi_bgn_rsn_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEndReasonCode" ${logic.rename-to} mbi_end_rsn_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiCardRequestDate" ${logic.rename-to} mbi_card_rqst_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiAddUser" ${logic.rename-to} creat_user_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiAddDate" ${logic.rename-to} creat_ts;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiUpdateUser" ${logic.rename-to} updt_user_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiUpdateDate" ${logic.rename-to} updt_ts;
--
-- take care of primary keys
--
-- For hsql we need to (re-) create the primary key constraint since we deleted them at
-- beginning of script. For psql, we can just do a rename.
--
--      {alter-rename-index}
--          psql: "alter index if exists"
--          hsql: "--"
--
${logic.alter-rename-index} public."BeneficiariesHistoryInvalidBeneficiaries_pkey" rename to beneficiaries_history_invalid_beneficiaries_pkey;
${logic.alter-rename-index} public."BeneficiariesHistory_pkey" rename to beneficiaries_history_pkey;
${logic.alter-rename-index} public."Beneficiaries_pkey" rename to beneficiaries_pkey;
${logic.alter-rename-index} public."BeneficiaryMonthly_pkey" rename to beneficiary_monthly_pkey;
${logic.alter-rename-index} public."CarrierClaimLines_pkey"rename to carrier_claim_lines_pkey;
${logic.alter-rename-index} public."CarrierClaims_pkey" rename to carrier_claims_pkey;
${logic.alter-rename-index} public."DMEClaimLines_pkey" rename to dme_claim_lines_pkey;
${logic.alter-rename-index} public."DMEClaims_pkey" rename to dme_claims_pkey;
${logic.alter-rename-index} public."HHAClaimLines_pkey" rename to hha_claim_lines_pkey;
${logic.alter-rename-index} public."HHAClaims_pkey" rename to hha_claims_pkey;
${logic.alter-rename-index} public."HospiceClaimLines_pkey" rename to hospice_claim_lines_pkey;
${logic.alter-rename-index} public."HospiceClaims_pkey" rename to hospice_claims_pkey;
${logic.alter-rename-index} public."InpatientClaimLines_pkey" rename to inpatient_claim_lines_pkey;
${logic.alter-rename-index} public."InpatientClaims_pkey" rename to inpatient_claims_pkey;
${logic.alter-rename-index} public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey" rename to medicare_beneficiaryid_history_invalid_beneficiaries_pkey;
${logic.alter-rename-index} public."MedicareBeneficiaryIdHistory_pkey" rename to medicare_beneficiaryid_history_pkey;
${logic.alter-rename-index} public."OutpatientClaimLines_pkey" rename to outpatient_claim_lines_pkey;
${logic.alter-rename-index} public."OutpatientClaims_pkey" rename to outpatient_claims_pkey;
${logic.alter-rename-index} public."PartDEvents_pkey" rename to partd_events_pkey;
${logic.alter-rename-index} public."LoadedBatches_pkey" rename to loaded_batches_pkey;
${logic.alter-rename-index} public."LoadedFiles_pkey" rename to loaded_files_pkey;
--
--      ${logic.hsql-only-alter}
--          psql: "--"
--          hsql: "alter"
--
${logic.hsql-only-alter} table public.beneficiaries add constraint beneficiaries_pkey primary key (bene_id);
${logic.hsql-only-alter} table public.beneficiaries_history add constraint beneficiaries_history_pkey primary key (bene_history_id);  
${logic.hsql-only-alter} table public.beneficiaries_history_invalid_beneficiaries add constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (bene_history_id); 
${logic.hsql-only-alter} table public.beneficiary_monthly add constraint beneficiary_monthly_pkey primary key (bene_id, year_month);
${logic.hsql-only-alter} table public.carrier_claim_lines add constraint carrier_claim_lines_pkey primary key (clm_id, line_num);
${logic.hsql-only-alter} table public.carrier_claims add constraint carrier_claims_pkey primary key (clm_id);
${logic.hsql-only-alter} table public.dme_claim_lines add constraint dme_claim_lines_pkey primary key (clm_id, line_num);
${logic.hsql-only-alter} table public.dme_claims add constraint dme_claims_pkey primary key (clm_id);
${logic.hsql-only-alter} table public.hha_claim_lines add constraint hha_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.hha_claims add constraint hha_claims_pkey primary key (clm_id);  
${logic.hsql-only-alter} table public.hospice_claim_lines add constraint hospice_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.hospice_claims add constraint hospice_claims_pkey primary key (clm_id); 
${logic.hsql-only-alter} table public.inpatient_claim_lines add constraint inpatient_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.inpatient_claims add constraint npatient_claims_pkey primary key (clm_id); 
${logic.hsql-only-alter} table public.medicare_beneficiaryid_history_invalid_beneficiaries add constraint medicare_beneficiaryid_history_invalid_beneficiaries_pkey primary key (bene_mbi_id); 
${logic.hsql-only-alter} table public.medicare_beneficiaryid_history add constraint medicare_beneficiaryid_history_pkey primary key (bene_mbi_id);   
${logic.hsql-only-alter} table public.outpatient_claim_lines add constraint outpatient_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.outpatient_claims add constraint outpatient_claims_pkey primary key (clm_id); 
${logic.hsql-only-alter} table public.partd_events add constraint partd_events_pkey primary key (pde_id); 
--${logic.hsql-only-alter} table public.loaded_batches add constraint loaded_batches_pkey primary key (loaded_batchid);
--${logic.hsql-only-alter} table public.loaded_files add constraint loaded_files_pkey primary key (loaded_fileid);
--
-- rename indexes (index names are limited to 64 chars)
--
ALTER INDEX "BeneficiariesHistory_beneficiaryId_idx" RENAME TO beneficiaries_history_beneid_idx;
ALTER INDEX "BeneficiariesHistory_hicn_idx" RENAME TO beneficiaries_history_hicn_idx;
ALTER INDEX "Beneficiaries_history_mbi_hash_idx" RENAME TO beneficiaries_history_mbi_hash_idx;
ALTER INDEX "Beneficiaries_hicn_idx" RENAME TO beneficiaries_hicn_idx;
ALTER INDEX "Beneficiaries_mbi_hash_idx" RENAME TO beneficiaries_mbi_hash_idx; 
ALTER INDEX "BeneficiaryMonthly_partDContractNumId_yearMonth_parentBene_idx" RENAME TO beneficiary_monthly_year_month_partd_contract_beneid_idx;
ALTER INDEX "BeneficiaryMonthly_partDContractNumberId_yearmonth_idx" RENAME TO beneficiary_monthly_partd_contract_number_year_month_idx;
ALTER INDEX "CarrierClaims_beneficiaryId_idx" RENAME TO carrier_claims_beneid_idx;
ALTER INDEX "DMEClaims_beneficiaryId_idx" RENAME TO dme_claims_beneid_idx;
ALTER INDEX "HHAClaims_beneficiaryId_idx" RENAME TO hha_claims_beneid_idx;
ALTER INDEX "HospiceClaims_beneficiaryId_idx" RENAME TO hospice_claims_beneid_idx;
ALTER INDEX "InpatientClaims_beneficiaryId_idx" RENAME TO inpatient_claims_beneid_idx;
ALTER INDEX "LoadedBatches_created_index" RENAME TO loaded_batches_created_idx;
ALTER INDEX "MedicareBeneficiaryIdHistory_beneficiaryId_idx" RENAME TO medicare_beneficiaryid_history_beneid_idx;
ALTER INDEX "OutpatientClaims_beneficiaryId_idx" RENAME TO outpatient_claims_beneid_idx;
ALTER INDEX "PartDEvents_beneficiaryId_idx" RENAME TO partd_events_beneid_idx;

-- FIX THIS - why do we even have these???
ALTER INDEX "Beneficiaries_partd_contract_number_apr_id_idx" RENAME  TO beneficiaries_partd_contract_number_apr_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_aug_id_idx" RENAME  TO beneficiaries_partd_contract_number_aug_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_dec_id_idx" RENAME  TO beneficiaries_partd_contract_number_dec_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_feb_id_idx" RENAME  TO beneficiaries_partd_contract_number_feb_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jan_id_idx" RENAME  TO beneficiaries_partd_contract_number_jan_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jul_id_idx" RENAME  TO beneficiaries_partd_contract_number_jul_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jun_id_idx" RENAME  TO beneficiaries_partd_contract_number_jun_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_mar_id_idx" RENAME  TO beneficiaries_partd_contract_number_mar_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_may_id_idx" RENAME  TO beneficiaries_partd_contract_number_may_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_nov_id_idx" RENAME  TO beneficiaries_partd_contract_number_nov_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_oct_id_idx" RENAME  TO beneficiaries_partd_contract_number_oct_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_sept_id_idx" RENAME TO beneficiaries_partd_contract_number_sept_id_idx;
--
-- Add foreign key constraints
--
ALTER TABLE public.beneficiaries_history
    ADD CONSTRAINT beneficiaries_history_bene_id_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.beneficiary_monthly
    ADD CONSTRAINT beneficiary_monthly_beneid_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.carrier_claim_lines
    ADD CONSTRAINT carrier_claim_lines_clmid_to_carrier_claims FOREIGN KEY (clm_id) REFERENCES public.carrier_claims (clm_id);

ALTER TABLE public.carrier_claims
    ADD CONSTRAINT carrier_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.dme_claim_lines
    ADD CONSTRAINT dme_claim_lines_clmid_to_dme_claims FOREIGN KEY (clm_id) REFERENCES public.dme_claims (clm_id);

ALTER TABLE public.dme_claims
    ADD CONSTRAINT dme_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries(bene_id);

ALTER TABLE public.hha_claim_lines
    ADD CONSTRAINT hha_claim_lines_parent_claim_to_hha_claims FOREIGN KEY (clm_id) REFERENCES public.hha_claims (clm_id);

ALTER TABLE public.hha_claims
    ADD CONSTRAINT hha_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.hospice_claim_lines
    ADD CONSTRAINT hospice_claim_lines_parent_claim_to_hospice_claims FOREIGN KEY (clm_id) REFERENCES public.hospice_claims (clm_id);

ALTER TABLE public.hospice_claims
    ADD CONSTRAINT hospice_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.inpatient_claim_lines
    ADD CONSTRAINT inpatient_claim_lines_parent_claim_to_inpatient_claims FOREIGN KEY (clm_id) REFERENCES public.inpatient_claims (clm_id);

ALTER TABLE public.inpatient_claims
    ADD CONSTRAINT inpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.medicare_beneficiaryid_history
    ADD CONSTRAINT medicare_beneficiaryid_history_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.outpatient_claim_lines
    ADD CONSTRAINT outpatient_claim_lines_parent_claim_to_outpatient_claims FOREIGN KEY (clm_id) REFERENCES public.outpatient_claims (clm_id);

ALTER TABLE public.outpatient_claims
    ADD CONSTRAINT outpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.partd_events
    ADD CONSTRAINT partd_events_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.loaded_batches
    ADD CONSTRAINT loaded_batches_loaded_fileid FOREIGN KEY (loaded_fileid) REFERENCES public.loaded_files (loaded_fileid);

-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file; helpful in tracking down misnamed table or columns in an hsql db.
-- SCRIPT './bfd_schema_post.txt';