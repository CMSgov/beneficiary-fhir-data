-- This flyway script is intended to track changes to the beneficiary_monthly
-- table, primarily during the first couple of weeks of a new calendar year.
-- This enables BFD to track data changes to the beneficiary_monthly table;
-- visibility into BENEFICIARY ref_yr changes do not effect previous year's data.

${logic.psql-only} DROP TABLE IF EXISTS public.beneficiary_monthly_audit;
${logic.psql-only} DROP TRIGGER IF EXISTS audit_ccw_update ON beneficiary_monthly;

-- The sequence used for the `skipped_rif_records.record_id` column.
CREATE SEQUENCE ${logic.psql-only} IF NOT EXISTS
  bene_monthly_audit_seq
  AS bigint ${logic.sequence-start} 1 ${logic.sequence-increment} 1
  NO CYCLE;

-- Create beneficiary_monthly_audit table; we'll create the table in both HSQL & PSQL
-- but we don't bother with setting up the trigger in HSQL as this will only be useful
-- during that timeframe when we are actively vetting CCW end-of-year/new year data.
--
CREATE TABLE public.beneficiary_monthly_audit(
  bene_id                                bigint NOT NULL,
  year_month                             date NOT NULL,
  partd_contract_number_id               varchar(5),
  partc_contract_number_id               varchar(5),
  medicare_status_code                   varchar(2),
  fips_state_cnty_code                   varchar(5),
  entitlement_buy_in_ind                 char(1),
  hmo_indicator_ind                      char(1),
  medicaid_dual_eligibility_code         varchar(2),
  partd_pbp_number_id                    varchar(3),
  partd_retiree_drug_subsidy_ind         char(1),
  partd_segment_number_id                varchar(3),
  partd_low_income_cost_share_group_code varchar(2),
  partc_pbp_number_id                    varchar(3),
  partc_plan_type_code                   varchar(3),
  operation                              char(1) NOT NULL,
  update_ts                              timestamp NOT NULL,
  seq_id                                 bigint NOT NULL,
 CONSTRAINT beneficiary_monthly_audit_pkey PRIMARY KEY (seq_id));

CREATE INDEX beneficiary_monthly_audit_bene_id_year_month_idx
    on beneficiary_monthly_audit (bene_id, year_month);
--
-- Create function that tracks UPDATE operations to the beneficiary_monthly table.
-- This will create a row in beneficiary_monthly_audit to reflect the update operation
-- performed on beneficiary_monthly; psql variable TG_OP denotes the operation.}
--
${logic.psql-only} CREATE OR REPLACE FUNCTION public.track_bene_monthly_change() RETURNS TRIGGER
${logic.psql-only} AS $beneficiary_monthly_audit$
${logic.psql-only}     BEGIN
${logic.psql-only}        IF (TG_OP = 'UPDATE') THEN
${logic.psql-only}            INSERT INTO public.beneficiary_monthly_audit VALUES (OLD.*, 'U', now(), nextval('public.bene_monthly_audit_seq'));
${logic.psql-only}            RETURN NEW;
${logic.psql-only}        END IF;
${logic.psql-only}        RETURN NULL; -- result is ignored since this is an AFTER trigger
${logic.psql-only}     END;
${logic.psql-only} $beneficiary_monthly_audit$ LANGUAGE plpgsql;

-- Setup UPDATE triggers.
--
${logic.psql-only} DROP TRIGGER IF EXISTS audit_ccw_update ON beneficiary_monthly;

-- We'll track any changes to the beneficiary_monthly table; however,
-- since we create 12 'empty' buckets at the beginning of a new REF_YR,
-- we'd pretty much always see UPDATEs where the 'OLD' is zero'd out
-- and the 'NEW' has value(s); those sort of updates are expected and
-- frankly not interesting (nor problematic). So we'll check the original
-- Part C/D contract ID(s) to verify that any changes were meaningful.
--
${logic.psql-only} CREATE TRIGGER audit_ccw_update
${logic.psql-only} AFTER UPDATE ON public.beneficiary_monthly
${logic.psql-only} FOR EACH ROW
${logic.psql-only}     WHEN ((
${logic.psql-only}         OLD.partd_contract_number_id <> '0' OR
${logic.psql-only}         OLD.partc_contract_number_id <> '0')
${logic.psql-only}     AND ((
${logic.psql-only}         OLD.partd_contract_number_id,
${logic.psql-only}         OLD.partc_contract_number_id,
${logic.psql-only}         OLD.medicare_status_code,
${logic.psql-only}         OLD.fips_state_cnty_code,
${logic.psql-only}         OLD.entitlement_buy_in_ind,
${logic.psql-only}         OLD.hmo_indicator_ind,
${logic.psql-only}         OLD.medicaid_dual_eligibility_code,
${logic.psql-only}         OLD.partd_pbp_number_id,
${logic.psql-only}         OLD.partd_segment_number_id,
${logic.psql-only}         OLD.partd_low_income_cost_share_group_code,
${logic.psql-only}         OLD.partc_pbp_number_id,
${logic.psql-only}         OLD.partc_plan_type_code)
${logic.psql-only}     IS DISTINCT FROM (
${logic.psql-only}         NEW.partd_contract_number_id,
${logic.psql-only}         NEW.partc_contract_number_id,
${logic.psql-only}         NEW.medicare_status_code,
${logic.psql-only}         NEW.fips_state_cnty_code,
${logic.psql-only}         NEW.entitlement_buy_in_ind,
${logic.psql-only}         NEW.hmo_indicator_ind,
${logic.psql-only}         NEW.medicaid_dual_eligibility_code,
${logic.psql-only}         NEW.partd_pbp_number_id,
${logic.psql-only}         NEW.partd_segment_number_id,
${logic.psql-only}         NEW.partd_low_income_cost_share_group_code,
${logic.psql-only}         NEW.partc_pbp_number_id,
${logic.psql-only}         NEW.partc_plan_type_code)) )   
${logic.psql-only}     EXECUTE FUNCTION track_bene_monthly_change();