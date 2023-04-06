
-- Allow extra space for the new claim_id to be larger than dcn was.

ALTER TABLE rda.fiss_claims ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_audit_trails ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_diagnosis_codes ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_payers ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_proc_codes ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.fiss_revenue_lines ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.claim_message_meta_data ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);
ALTER TABLE rda.message_errors ALTER COLUMN claim_id ${logic.alter-column-type} varchar(43);