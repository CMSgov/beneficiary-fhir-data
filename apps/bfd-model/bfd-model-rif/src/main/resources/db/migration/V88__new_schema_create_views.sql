-- NEW_SCHEMA_CREATE_VIEWS.SQL
--
-- This flyway script creates db views over the schema _new tables. This
-- allows the BFD services code, in particular the generated entity beans,
-- to point to updatable views instead of actual tables. This slight of hand
-- allows us to introduce a subsequent flyway script which will then get the
-- entire schema back to original table names, FK constraint names, index names,
-- etc. without any db downtime.
--
-- NOTE: for psql (Postgres) we create the view 'with check option' which
--       enables the view to be updatable.

-- carrier_claims and carrier_claim_lines
create view carrier_claim_lines as
  select * from carrier_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view carrier_claims as
  select * from carrier_claims_new
  ${logic.psql-only} with check option
  ;

  -- dme_claims and dme_claim_lines
create view dme_claim_lines as
  select * from dme_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view dme_claims as
  select * from dme_claims_new
  ${logic.psql-only} with check option
  ;

-- hha_claims and hha_claim_lines
create view hha_claim_lines as
  select * from hha_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view hha_claims as
  select * from hha_claims_new
  ${logic.psql-only} with check option
  ;

-- hospice_claims and hospice_claim_lines
create view hospice_claim_lines as
  select * from hospice_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view hospice_claims as
  select * from hospice_claims_new
  ${logic.psql-only} with check option
  ;

-- inpatient_claims and inpatient_claim_lines
create view inpatient_claim_lines as
  select * from inpatient_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view inpatient_claims as
  select * from inpatient_claims_new
  ${logic.psql-only} with check option
  ;

-- outpatient_claims and outpatient_claim_lines
create view outpatient_claim_lines as
  select * from outpatient_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view outpatient_claims as
  select * from outpatient_claims_new
  ${logic.psql-only} with check option
  ;

-- partd_events
create view partd_events as
  select * from partd_events_new
  ${logic.psql-only} with check option
  ;

-- snf_claims and snf_claim_lines
create view snf_claim_lines as
  select * from snf_claim_lines_new
  ${logic.psql-only} with check option
  ;
create view snf_claims as
  select * from snf_claims_new
  ${logic.psql-only} with check option
  ;

-- beneficiaries_history
create view beneficiaries_history as
  select * from beneficiaries_history_new
  ${logic.psql-only} with check option
  ;

-- medicare_beneficiaryid_history
create view medicare_beneficiaryid_history as
  select * from medicare_beneficiaryid_history_new
  ${logic.psql-only} with check option
  ;

-- beneficiary_monthly
create view beneficiary_monthly as
  select * from beneficiary_monthly_new
  ${logic.psql-only} with check option
  ;

-- beneficiaries
create view beneficiaries as
  select * from beneficiaries_new
  ${logic.psql-only} with check option
  ;

-- skipped_rif_records
create view skipped_rif_records as
  select * from skipped_rif_records_new
  ${logic.psql-only} with check option
  ;