--
--
alter table only cmac.beneficiaries
add constraint beneficiaries_pkey primary key (bene_id);
--
--
alter table only cmac.beneficiaries_history
add constraint beneficiaries_history_pkey primary key (beneficiary_history_id);
--
--
alter table only cmac.beneficiaries_history_invalid_beneficiaries
add constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (beneficiary_history_id);
--
--
alter table only cmac.beneficiary_monthly
add constraint beneficiary_monthly_pkey primary key (parent_beneficiary, year_month);
--
--
alter table only cmac.carrier_claim_lines
add constraint carrier_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
--
alter table only cmac.carrier_claims
add constraint carrier_claims_pkey primary key (clm_id);
--
--
alter table only cmac.dme_claim_lines
add constraint dme_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
--
alter table only cmac.dme_claims
add constraint dme_claims_pkey primary key (clm_id);
--
--
alter table only cmac.hha_claim_lines
add constraint hha_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
--
alter table only cmac.hha_claims
add constraint hha_claims_pkey primary key (clm_id);
--
--
alter table only cmac.hospice_claim_lines
add constraint hospice_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
-
alter table only cmac.hospice_claims
add constraint hospice_claims_pkey primary key (clm_id);
--
--
alter table only cmac.inpatient_claim_lines
add constraint inpatient_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
--
alter table only cmac.inpatient_claims
add constraint inpatient_claims_pkey primary key (clm_id);
--
--
alter table only cmac.loaded_batches
add constraint loaded_batches_pkey primary key (loaded_batch_id);
--
--
alter table only cmac.loaded_files
add constraint loaded_files_pkey primary key (loaded_file_id);
--
--
alter table only cmac.medicare_beneficiaryid_history
add constraint cmac.medicare_beneficiaryid_history_pkey primary key (medicare_beneficiaryid_key);
--
--
alter table only cmac.medicare_beneficiaryid_history_invalid_beneficiaries
add constraint medicare_beneficiaryid_history_invalid_beneficiaries_pkey primary key (medicare_beneficiaryid_key);
--
--
alter table only cmac.outpatient_claim_lines
add constraint outpatient_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
--
alter table only cmac.outpatient_claims
add constraint outpatient_claims_pkey primary key (clm_id);
--
--
alter table only cmac.partd_events
add constraint partd_events_pkey primary key (clm_id);
--
--
alter table only cmac.snf_claim_lines
add constraint snf_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
--
alter table only cmac.snf_claims
add constraint snf_claims_pkey primary key (clm_id);