-- PACA-751 switches to using a new RDA API primary key field for FISS claims.

-- These changes require that all of the fiss tables are empty.

${logic.psql-only} TRUNCATE rda.fiss_claims CASCADE;

-- Now that fiss_claims has been truncated we can fix nullable status on clm_typ_ind

ALTER TABLE rda.fiss_claims ALTER COLUMN clm_typ_ind SET NOT NULL;

-- Using RENAME leaves all of the foreign key references intact.

ALTER TABLE rda.fiss_audit_trails ${logic.alter-rename-column} dcn ${logic.rename-to} claim_id;
ALTER TABLE rda.fiss_diagnosis_codes ${logic.alter-rename-column} dcn ${logic.rename-to} claim_id;
ALTER TABLE rda.fiss_payers ${logic.alter-rename-column} dcn ${logic.rename-to} claim_id;
ALTER TABLE rda.fiss_proc_codes ${logic.alter-rename-column} dcn ${logic.rename-to} claim_id;
ALTER TABLE rda.fiss_revenue_lines ${logic.alter-rename-column} dcn ${logic.rename-to} claim_id;

-- We still want a dcn column in fiss_claims.

ALTER TABLE rda.fiss_claims ${logic.alter-rename-column} dcn ${logic.rename-to} claim_id;
ALTER TABLE rda.fiss_claims ADD dcn varchar(23) NOT NULL;

-- Allow extra space for the new claim_id to be larger than dcn was.

ALTER TABLE rda.fiss_claims ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);
ALTER TABLE rda.fiss_audit_trails ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);
ALTER TABLE rda.fiss_diagnosis_codes ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);
ALTER TABLE rda.fiss_payers ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);
ALTER TABLE rda.fiss_proc_codes ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);
ALTER TABLE rda.fiss_revenue_lines ALTER COLUMN claim_id ${logic.alter-column-type} varchar(32);

-- Reset the progress table so we can reingest all FISS claims under the new schema.

DELETE FROM rda.rda_api_progress WHERE claim_type = 'FISS';
