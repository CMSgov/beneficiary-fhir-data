-- Add new column introduced in RDA API v0.13

ALTER TABLE rda.fiss_claims ADD intermediary_nb varchar(5);
