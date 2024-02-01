
-- Allow extra space for the new claim_id to accommodate the Base64 encoding.

ALTER TABLE rda.fiss_claims ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_audit_trails ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_diagnosis_codes ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_payers ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_proc_codes ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_revenue_lines ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.claim_message_meta_data ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.message_errors ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);

-- Truncate FISS claims and associated meta data, the key they use is invalid now

${logic.psql-only} TRUNCATE rda.fiss_claims CASCADE;

DELETE FROM rda.claim_message_meta_data WHERE claim_type = 'F';

-- Reset the progress table so we can re-ingest all FISS claims with the new key.

DELETE FROM rda.rda_api_progress WHERE claim_type = 'FISS';