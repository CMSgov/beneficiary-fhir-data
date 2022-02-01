/*
 * This script will drop all primary keys, foreign keys, and indices used in 
 * the database. This is needed to speed up initial loads.
 */
ALTER TABLE beneficiaries_history
	DROP CONSTRAINT IF EXISTS beneficiaries_history_bene_id_to_beneficiary;

ALTER TABLE beneficiary_monthly
	DROP CONSTRAINT IF EXISTS beneficiary_monthly_bene_id_to_beneficiary;

ALTER TABLE carrier_claim_lines
	DROP CONSTRAINT IF EXISTS carrier_claim_lines_clm_id_to_carrier_claims;

ALTER TABLE carrier_claims
	DROP CONSTRAINT IF EXISTS carrier_claims_bene_id_to_beneficiaries;

ALTER TABLE dme_claim_lines
	DROP CONSTRAINT IF EXISTS dme_claim_lines_clm_id_to_dme_claims;

ALTER TABLE dme_claims
	DROP CONSTRAINT IF EXISTS dme_claims_bene_id_to_beneficiaries;

ALTER TABLE hha_claim_lines
	DROP CONSTRAINT IF EXISTS hha_claim_lines_clm_id_to_hha_claims;

ALTER TABLE hha_claims
	DROP CONSTRAINT IF EXISTS hha_claims_bene_id_to_beneficiaries;

ALTER TABLE hospice_claim_lines
	DROP CONSTRAINT IF EXISTS hospice_claim_lines_clm_id_to_hospice_claims;

ALTER TABLE hospice_claims
	DROP CONSTRAINT IF EXISTS hospice_claims_bene_id_to_beneficiaries;

ALTER TABLE inpatient_claim_lines
	DROP CONSTRAINT IF EXISTS inpatient_claim_lines_clm_id_to_inpatient_claims;

ALTER TABLE inpatient_claims
	DROP CONSTRAINT IF EXISTS inpatient_claims_bene_id_to_beneficiaries;

ALTER TABLE loaded_batches
	DROP CONSTRAINT IF EXISTS loaded_batches_loaded_file_id;

ALTER TABLE medicare_beneficiaryid_history
	DROP CONSTRAINT IF EXISTS medicare_beneficiaryid_history_bene_id_to_beneficiaries;

ALTER TABLE outpatient_claim_lines
	DROP CONSTRAINT IF EXISTS outpatient_claim_lines_clm_id_to_outpatient_claims;

ALTER TABLE outpatient_claims
	DROP CONSTRAINT IF EXISTS outpatient_claims_bene_id_to_beneficiaries;

ALTER TABLE partd_events
	DROP CONSTRAINT IF EXISTS partd_events_bene_id_to_beneficiaries;

ALTER TABLE snf_claim_lines
	DROP CONSTRAINT IF EXISTS snf_claim_lines_clm_id_to_snf_claims;

ALTER TABLE snf_claims
	DROP CONSTRAINT IF EXISTS snf_claims_bene_id_to_beneficiaries;

-- drop primary keys
ALTER TABLE beneficiaries_history_invalid_beneficiaries
	DROP CONSTRAINT IF EXISTS beneficiaries_history_invalid_beneficiaries_pkey;

ALTER TABLE beneficiaries_history
	DROP CONSTRAINT IF EXISTS beneficiaries_history_pkey;

ALTER TABLE beneficiaries
	DROP CONSTRAINT IF EXISTS beneficiaries_pkey;

ALTER TABLE beneficiary_monthly
	DROP CONSTRAINT IF EXISTS beneficiary_monthly_pkey;

ALTER TABLE carrier_claim_lines
	DROP CONSTRAINT IF EXISTS carrier_claim_lines_pkey;

ALTER TABLE carrier_claims
	DROP CONSTRAINT IF EXISTS carrier_claims_pkey;

ALTER TABLE dme_claim_lines
	DROP CONSTRAINT IF EXISTS dme_claim_lines_pkey;

ALTER TABLE dme_claims
	DROP CONSTRAINT IF EXISTS dme_claims_pkey;

ALTER TABLE hha_claim_lines
	DROP CONSTRAINT IF EXISTS hha_claim_lines_pkey;

ALTER TABLE hha_claims
	DROP CONSTRAINT IF EXISTS hha_claims_pkey;

ALTER TABLE hospice_claim_lines
	DROP CONSTRAINT IF EXISTS hospice_claim_lines_pkey;

ALTER TABLE hospice_claims
	DROP CONSTRAINT IF EXISTS hospice_claims_pkey;

ALTER TABLE inpatient_claim_lines
	DROP CONSTRAINT IF EXISTS inpatient_claim_lines_pkey;

ALTER TABLE inpatient_claims
	DROP CONSTRAINT IF EXISTS inpatient_claims_pkey;

ALTER TABLE loaded_batches
	DROP CONSTRAINT IF EXISTS loaded_batches_pkey;

ALTER TABLE loaded_files
	DROP CONSTRAINT IF EXISTS loaded_files_pkey;

ALTER TABLE medicare_beneficiaryid_history_invalid_beneficiaries
	DROP CONSTRAINT IF EXISTS medicare_beneficiaryid_history_invalid_beneficiaries_pkey;

ALTER TABLE medicare_beneficiaryid_history
	DROP CONSTRAINT IF EXISTS medicare_beneficiaryid_history_pkey;

ALTER TABLE outpatient_claim_lines
	DROP CONSTRAINT IF EXISTS outpatient_claim_lines_pkey;

ALTER TABLE outpatient_claims
	DROP CONSTRAINT IF EXISTS outpatient_claims_pkey;

ALTER TABLE partd_events
	DROP CONSTRAINT IF EXISTS partd_events_pkey;

ALTER TABLE snf_claim_lines
	DROP CONSTRAINT IF EXISTS snf_claim_lines_pkey;

ALTER TABLE snf_claims
	DROP CONSTRAINT IF EXISTS snf_claims_pkey;

-- drop indexes in beneficiaries table
DROP INDEX IF EXISTS beneficiaries_hicn_idx;
DROP INDEX IF EXISTS beneficiaries_mbi_hash_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_apr_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_aug_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_dec_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_feb_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_jan_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_jul_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_jun_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_mar_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_may_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_nov_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_oct_id_idx;
DROP INDEX IF EXISTS beneficiaries_partd_contract_number_sept_id_idx;