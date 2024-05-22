
-- Allow extra space for the new claim_id to be larger than dcn was.

ALTER TABLE rda.claim_message_meta_data ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);
ALTER TABLE rda.message_errors ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);