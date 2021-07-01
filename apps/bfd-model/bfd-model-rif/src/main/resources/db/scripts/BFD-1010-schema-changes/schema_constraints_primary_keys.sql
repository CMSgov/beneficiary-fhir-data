--
alter table only public.beneficiaries
add constraint beneficiaries_pkey primary key (bene_id);
--
alter table only public.beneficiaries_history
add constraint beneficiaries_history_pkey primary key (beneficiary_history_id);
--
alter table only public.beneficiaries_history_invalid_beneficiaries
add constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (beneficiary_history_id);
--
alter table only public.beneficiary_monthly
add constraint beneficiary_monthly_pkey primary key (parent_beneficiary, year_month);
--
alter table only public.carrier_claim_lines
add constraint carrier_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.carrier_claims
add constraint carrier_claims_pkey primary key (clm_id);
--
alter table only public.dme_claim_lines
add constraint dme_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.dme_claims
add constraint dme_claims_pkey primary key (clm_id);
--
alter table only public.hha_claim_lines
add constraint hha_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.hha_claims
add constraint hha_claims_pkey primary key (clm_id);
--
alter table only public.hospice_claim_lines
add constraint hospice_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.hospice_claims
add constraint hospice_claims_pkey primary key (clm_id);
--
alter table only public.inpatient_claim_lines
add constraint inpatient_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.inpatient_claims
add constraint inpatient_claims_pkey primary key (clm_id);
--
alter table only public.loaded_batches
add constraint loaded_batches_pkey primary key (loaded_batch_id);
--
alter table only public.loaded_files
add constraint loaded_files_pkey primary key (loaded_file_id);
--
alter table only public.medicare_beneficiaryid_history
add constraint medicare_beneficiaryid_history_pkey primary key (medicare_beneficiaryid_key);
--
alter table only public.medicare_beneficiaryid_history_invalid_beneficiaries
add constraint medicare_beneficiaryid_history_invalid_beneficiaries_pkey primary key (medicare_beneficiaryid_key);
--
alter table only public.outpatient_claim_lines
add constraint outpatient_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.outpatient_claims
add constraint outpatient_claims_pkey primary key (clm_id);
--
alter table only public.partd_events
add constraint partd_events_pkey primary key (clm_id);
--
alter table only public.snf_claim_lines
add constraint snf_claim_lines_pkey primary key (parent_claim, clm_line_num);
--
alter table only public.snf_claims
add constraint snf_claims_pkey primary key (clm_id);