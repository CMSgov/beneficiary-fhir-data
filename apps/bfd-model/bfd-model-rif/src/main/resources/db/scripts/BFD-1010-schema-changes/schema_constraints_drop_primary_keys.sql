--
alter table only cmac.beneficiaries
drop constraint if exists beneficiaries_pkey;
--
alter table only cmac.beneficiaries_history
drop constraint if exists beneficiaries_history_pkey;
--
alter table only cmac.beneficiaries_history_invalid_beneficiaries
drop constraint if exists beneficiaries_history_invalid_beneficiaries_pkey;
--
alter table only cmac.beneficiary_monthly
drop constraint if exists beneficiary_monthly_pkey;
--
alter table only cmac.carrier_claim_lines
drop constraint if exists carrier_claim_lines_pkey;
--
alter table only cmac.carrier_claims
drop constraint if exists carrier_claims_pkey;
--
alter table only cmac.dme_claim_lines
drop constraint if exists dme_claim_lines_pkey;
--
alter table only cmac.dme_claims
drop constraint if exists dme_claims_pkey;
--
alter table only cmac.hha_claim_lines
drop constraint if exists hha_claim_lines_pkey;
--
alter table only cmac.hha_claims
drop constraint if exists hha_claims_pkey;
--
alter table only cmac.hospice_claim_lines
drop constraint if exists hospice_claim_lines_pkey;
--
alter table only cmac.hospice_claims
drop constraint if exists hospice_claims_pkey;
--
alter table only cmac.inpatient_claim_lines
drop constraint if exists inpatient_claim_lines_pkey;
--
alter table only cmac.inpatient_claims
drop constraint if exists inpatient_claims_pkey;
--
alter table only cmac.loaded_batches
drop constraint if exists loaded_batches_pkey;
--
alter table only cmac.loaded_files
drop constraint if exists loaded_files_pkey;
--
alter table only cmac.medicare_beneficiaryid_history
drop constraint if exists medicare_beneficiaryid_history_pkey;
--
alter table only cmac.medicare_beneficiaryid_history_invalid_beneficiaries
drop constraint if exists medicare_beneficiaryid_history_invalid_beneficiaries_pkey;
--
alter table only cmac.outpatient_claim_lines
drop constraint if exists outpatient_claim_lines_pkey;
--
alter table only cmac.outpatient_claims
drop constraint if exists outpatient_claims_pkey;
--
alter table only cmac.partd_events
drop constraint if exists partd_events_pkey;
--
alter table only cmac.snf_claim_lines
drop constraint if exists snf_claim_lines_pkey;
--
alter table only cmac.snf_claims
drop constraint if exists snf_claims_pkey;