/*
 * This script will re-create all primary keys, foreign keys, and indices used 
 * in the database. It's intended to undo (when/as needed) its sister 
 * `Drop_all_constraints.sql` script.
 */

-- Create all of the primary keys.
ALTER TABLE beneficiaries_history 
    ADD CONSTRAINT beneficiaries_history_pkey PRIMARY KEY (bene_history_id);
  
ALTER TABLE beneficiaries 
    ADD CONSTRAINT beneficiaries_pkey PRIMARY KEY (bene_id);
  
ALTER TABLE beneficiary_monthly 
    ADD CONSTRAINT beneficiary_monthly_pkey PRIMARY KEY (bene_id, year_month);
  
ALTER TABLE carrier_claim_lines 
    ADD CONSTRAINT carrier_claim_lines_pkey PRIMARY KEY (clm_id, line_num);
  
ALTER TABLE carrier_claims 
    ADD CONSTRAINT carrier_claims_pkey PRIMARY KEY (clm_id);
  
ALTER TABLE dme_claim_lines 
    ADD CONSTRAINT dme_claim_lines_pkey PRIMARY KEY (clm_id, line_num);
  
ALTER TABLE dme_claims 
    ADD CONSTRAINT dme_claims_pkey PRIMARY KEY (clm_id);
  
ALTER TABLE hha_claim_lines 
    ADD CONSTRAINT hha_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
  
ALTER TABLE hha_claims 
    ADD CONSTRAINT hha_claims_pkey PRIMARY KEY (clm_id);
  
ALTER TABLE hospice_claim_lines 
    ADD CONSTRAINT hospice_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num); 
  
ALTER TABLE hospice_claims 
    ADD CONSTRAINT hospice_claims_pkey PRIMARY KEY (clm_id);
  
ALTER TABLE inpatient_claim_lines 
    ADD CONSTRAINT inpatient_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
  
ALTER TABLE inpatient_claims 
    ADD CONSTRAINT inpatient_claims_pkey PRIMARY KEY (clm_id);
  
ALTER TABLE loaded_batches 
    ADD CONSTRAINT loaded_batches_pkey PRIMARY KEY (loaded_batch_id);
  
ALTER TABLE loaded_files 
    ADD CONSTRAINT loaded_files_pkey PRIMARY KEY (loaded_file_id);

ALTER TABLE beneficiaries_history_invalid_beneficiaries 
    ADD CONSTRAINT beneficiaryies_history_invalid_beneficiaries_pkey PRIMARY KEY (bene_history_id);

ALTER TABLE medicare_beneficiaryid_history_invalid_beneficiaries 
    ADD CONSTRAINT medicare_beneficiaryid_history_invalid_beneficiaries_pkey PRIMARY KEY (bene_mbi_id);
  
ALTER TABLE medicare_beneficiaryid_history 
    ADD CONSTRAINT medicare_beneficiaryid_history_pkey PRIMARY KEY (bene_mbi_id);
  
ALTER TABLE outpatient_claim_lines 
    ADD CONSTRAINT outpatient_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
  
ALTER TABLE outpatient_claims 
    ADD CONSTRAINT outpatient_claims_pkey PRIMARY KEY (clm_id);
  
ALTER TABLE partd_events 
    ADD CONSTRAINT partd_events_pkey PRIMARY KEY (pde_id);
  
ALTER TABLE snf_claim_lines 
    ADD CONSTRAINT snf_claim_lines_pkey PRIMARY KEY (clm_id, clm_line_num);
  
ALTER TABLE snf_claims 
    ADD CONSTRAINT snf_claims_pkey PRIMARY KEY (clm_id);

-- Create all of the header-to-line table foreign keys.
ALTER TABLE carrier_claim_lines 
	ADD CONSTRAINT carrier_claim_lines_clm_id_to_carrier_claims
  		FOREIGN KEY (clm_id) REFERENCES carrier_claims(clm_id);
  
ALTER TABLE dme_claim_lines 
	ADD CONSTRAINT dme_claim_lines_clm_id_to_dme_claims
  		FOREIGN KEY (clm_id) REFERENCES dme_claims(clm_id);
  
ALTER TABLE hha_claim_lines 
	ADD CONSTRAINT hha_claim_lines_clm_id_to_hha_claims
  		FOREIGN KEY (clm_id) REFERENCES hha_claims(clm_id);
  
ALTER TABLE hospice_claim_lines 
	ADD CONSTRAINT hospice_claim_lines_clm_id_to_hospice_claims
  		FOREIGN KEY (clm_id) REFERENCES hospice_claims(clm_id);
  
ALTER TABLE inpatient_claim_lines 
	ADD CONSTRAINT inpatient_claim_lines_clm_id_to_inpatient_claims
  		FOREIGN KEY (clm_id) REFERENCES inpatient_claims(clm_id);
  
ALTER TABLE outpatient_claim_lines 
	ADD CONSTRAINT outpatient_claim_lines_clm_id_to_outpatient_claims
  		FOREIGN KEY (clm_id) REFERENCES outpatient_claims(clm_id);
  
ALTER TABLE snf_claim_lines 
	ADD CONSTRAINT snf_claim_lines_clm_id_to_snf_claims
  		FOREIGN KEY (clm_id) REFERENCES snf_claims(clm_id);

-- Create all of the claim-to-bene table foreign keys.
ALTER TABLE carrier_claims
	ADD CONSTRAINT carrier_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE dme_claims
	ADD CONSTRAINT dme_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE hha_claims
	ADD CONSTRAINT hha_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE hospice_claims
	ADD CONSTRAINT hospice_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE inpatient_claims
	ADD CONSTRAINT inpatient_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE outpatient_claims
	ADD CONSTRAINT outpatient_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE partd_events
	ADD CONSTRAINT partd_events_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

ALTER TABLE snf_claims
	ADD CONSTRAINT snf_claims_bene_id_to_beneficiaries
		FOREIGN KEY (bene_id) REFERENCES beneficiaries(bene_id);

-- Create all of the "bene_id" column indexes for claim tables.
CREATE INDEX IF NOT EXISTS carrier_claims_bene_id_idx
    ON carrier_claims (bene_id);

CREATE INDEX IF NOT EXISTS dme_claims_bene_id_idx
    ON dme_claims (bene_id);

CREATE INDEX IF NOT EXISTS hha_claims_bene_id_idx
    ON hha_claims (bene_id);

CREATE INDEX IF NOT EXISTS hospice_claims_bene_id_idx
    ON hospice_claims (bene_id);

CREATE INDEX IF NOT EXISTS inpatient_claims_bene_id_idx
    ON inpatient_claims (bene_id);

CREATE INDEX IF NOT EXISTS loaded_batches_created_idx
    ON loaded_batches (created);

CREATE INDEX IF NOT EXISTS medicare_beneficiaryid_history_bene_id_idx
    ON medicare_beneficiaryid_history (bene_id);

CREATE INDEX IF NOT EXISTS outpatient_claims_bene_id_idx
    ON outpatient_claims (bene_id);

CREATE INDEX IF NOT EXISTS partd_events_bene_id_idx
    ON partd_events (bene_id);

CREATE INDEX IF NOT EXISTS snf_claims_bene_id_idx
    ON snf_claims (bene_id);

-- Create all of the indexes on beneficiary tables.
CREATE INDEX IF NOT EXISTS beneficiaries_hicn_idx
    ON beneficiaries (bene_crnt_hic_num);

CREATE INDEX IF NOT EXISTS beneficiaries_mbi_hash_idx
    ON beneficiaries (mbi_hash);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_apr_id_idx
    ON beneficiaries (ptd_cntrct_apr_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_aug_id_idx
    ON beneficiaries (ptd_cntrct_aug_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_dec_id_idx
    ON beneficiaries (ptd_cntrct_dec_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_feb_id_idx
    ON beneficiaries (ptd_cntrct_feb_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_jan_id_idx
    ON beneficiaries (ptd_cntrct_jan_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_jul_id_idx
    ON beneficiaries (ptd_cntrct_jul_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_jun_id_idx
    ON beneficiaries (ptd_cntrct_jun_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_mar_id_idx
    ON beneficiaries (ptd_cntrct_mar_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_may_id_idx
    ON beneficiaries (ptd_cntrct_may_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_nov_id_idx
    ON beneficiaries (ptd_cntrct_nov_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_oct_id_idx
    ON beneficiaries (ptd_cntrct_oct_id);

CREATE INDEX IF NOT EXISTS beneficiaries_partd_contract_number_sept_id_idx
    ON beneficiaries (ptd_cntrct_sept_id);

  -- Create all of the "bene_id" column indexes for ancillary beneficiary tables.
CREATE INDEX IF NOT EXISTS beneficiaries_history_bene_id_idx
    ON beneficiaries_history (bene_id);

CREATE INDEX IF NOT EXISTS beneficiaries_history_hicn_idx
    ON beneficiaries_history (bene_crnt_hic_num);

CREATE INDEX IF NOT EXISTS beneficiaries_history_mbi_hash_idx
    ON beneficiaries_history (mbi_hash);

CREATE INDEX IF NOT EXISTS beneficiary_monthly_partd_contract_number_year_month_idx
    ON beneficiary_monthly (partd_contract_number_id, year_month);

CREATE INDEX IF NOT EXISTS beneficiary_monthly_year_month_partd_contract_bene_id_idx
    ON beneficiary_monthly (partd_contract_number_id, year_month, bene_id);