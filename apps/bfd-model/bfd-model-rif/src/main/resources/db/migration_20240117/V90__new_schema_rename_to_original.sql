-- NEW_SCHEMA_RENAME_TO ORIGINAL
--
-- This flyway script is intended to a perform final fixup of BFD database
-- table names, index names, and constraint names. It also removed (dropped)
-- current db views so that BFD services will now point to actual db tables.
--
-- BFD services app previously deployed code with entity beans pointing to
-- db view(s) that followed a naming convention of:
--
--   : current db "[table_name]_new" and drop the "_new" suffix.
--   : change BFD services entity beans to point to the db view name
--
-- In theory, we should not need to make any code changes since the current
-- BFD app services are pointing to db views that match the same name as
-- this script's table renames. However, there is a minor bit of code in the
-- PipelineTestUtils.java (marked by a TODO) that needed to be removed; prior
-- code would construct a table name by deriving it from the entity bean class.
-- However, since the entity beans were pointing to views, the code had to
-- construct an actual table name by appending "_new" to the name in order for
-- the TRUNCATE TABLE invocation to work. So that code was removed (basically
-- reverting back to its original state/name).

-- =====================
-- beneficiaries
-- =====================

-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.beneficiaries_new_pkey rename to beneficiaries_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key;
-- note the use of cascade, which will drop constraints tied to beneficiaries_new.bene_id.
${logic.hsql-only} alter table public.beneficiaries_new
${logic.hsql-only}     drop constraint public.beneficiaries_new_pkey cascade;
${logic.hsql-only} alter table public.beneficiaries_new
${logic.hsql-only}     add constraint beneficiaries_pkey primary key (bene_id);

-- both psql and hsql support non-primary key index renaming
alter index ${logic.psql-only} if exists
    public.beneficiaries_new_hicn_idx rename to beneficiaries_hicn_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_mbi_hash_idx rename to beneficiaries_mbi_hash_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_jan_id_idx rename to beneficiaries_ptd_cntrct_number_jan_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_feb_id_idx rename to beneficiaries_ptd_cntrct_number_feb_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_mar_id_idx rename to beneficiaries_ptd_cntrct_number_mar_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_apr_id_idx rename to beneficiaries_ptd_cntrct_number_apr_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_may_id_idx rename to beneficiaries_ptd_cntrct_number_may_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_jun_id_idx rename to beneficiaries_ptd_cntrct_number_jun_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_jul_id_idx rename to beneficiaries_ptd_cntrct_number_jul_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_aug_id_idx rename to beneficiaries_ptd_cntrct_number_aug_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_sept_id_idx rename to beneficiaries_ptd_cntrct_number_sept_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_oct_id_idx rename to beneficiaries_ptd_cntrct_number_oct_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_nov_id_idx rename to beneficiaries_ptd_cntrct_number_nov_id_idx;
alter index ${logic.psql-only} if exists 
    public.beneficiaries_new_ptd_cntrct_number_dec_id_idx rename to beneficiaries_ptd_cntrct_number_dec_id_idx;

-- =====================
-- beneficiaries_history
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.beneficiaries_history_new_pkey
${logic.psql-only}    rename to beneficiaries_history_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.beneficiaries_history_new
${logic.hsql-only}     drop constraint public.beneficiaries_history_new_pkey;
${logic.hsql-only} alter table public.beneficiaries_history_new
${logic.hsql-only}     add constraint beneficiaries_history_pkey primary key (bene_history_id);

-- change the name of the index for bene_id
alter index ${logic.psql-only} if exists 
    public.beneficiaries_history_new_bene_id_idx rename to beneficiaries_history_bene_id_idx;
  
-- change the name of the index for bene_crnt_hic_num
alter index ${logic.psql-only} if exists 
    public.beneficiaries_history_new_hicn_idx rename to beneficiaries_history_hicn_idx;
    
-- change the name of the index for mbi_hash
alter index ${logic.psql-only} if exists 
    public.beneficiaries_history_new_mbi_hash_idx rename to beneficiaries_history_mbi_hash_idx;

-- for hsql, we previously dropped the primary key with cascade, so we need
-- to actually create the fk constraint
alter table if exists public.beneficiaries_history_new
${logic.hsql-only} add constraint beneficiaries_history_to_beneficiaries foreign key (bene_id)
${logic.hsql-only}     references public.beneficiaries_new (bene_id);
${logic.psql-only} rename constraint beneficiaries_history_new_to_benficiaries
${logic.psql-only}     to beneficiaries_history_to_beneficiaries;

-- ==================================
-- medicare_beneficiaryid_history_new
-- ==================================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.medicare_beneficiaryid_history_new_pkey
${logic.psql-only}    rename to medicare_beneficiaryid_history_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.medicare_beneficiaryid_history_new
${logic.hsql-only}     drop constraint public.medicare_beneficiaryid_history_new_pkey;
${logic.hsql-only} alter table public.medicare_beneficiaryid_history_new
${logic.hsql-only}     add constraint medicare_beneficiaryid_history_pkey primary key (bene_mbi_id);

-- change the name of the index for bene_id
alter index ${logic.psql-only} if exists 
    public.medicare_beneficiaryid_history_new_bene_id_idx rename to medicare_beneficiaryid_history_bene_id_idx;

alter table if exists public.medicare_beneficiaryid_history_new
${logic.hsql-only} add constraint medicare_beneficiaryid_history_to_benficiaries foreign key (bene_id)
${logic.hsql-only}     references public.beneficiaries_new (bene_id);
${logic.psql-only} rename constraint medicare_beneficiaryid_history_new_to_benficiaries
${logic.psql-only}     to medicare_beneficiaryid_history_to_benficiaries;

-- =====================
-- beneficiary_monthly
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.beneficiary_monthly_new_pkey
${logic.psql-only}    rename to beneficiary_monthly_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.beneficiary_monthly_new
${logic.hsql-only}     drop constraint public.beneficiary_monthly_new_pkey;
${logic.hsql-only} alter table public.beneficiary_monthly_new
${logic.hsql-only}     add constraint beneficiary_monthly_pkey primary key (bene_id, year_month);

alter index ${logic.psql-only} if exists 
    public.beneficiary_monthly_new_partd_contract_year_month_bene_id_idx
        rename to beneficiary_monthly_partd_contract_year_month_bene_id_idx;

alter table if exists public.beneficiary_monthly_new
${logic.hsql-only} add constraint beneficiary_monthly_to_benficiaries foreign key (bene_id)
${logic.hsql-only}     references public.beneficiaries_new (bene_id);
${logic.psql-only} rename constraint beneficiary_monthly_new_to_benficiaries
${logic.psql-only}     to beneficiary_monthly_to_benficiaries;

-- =====================
-- skipped_rif_records
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.skipped_rif_records_new_pkey
${logic.psql-only}    rename to skipped_rif_records_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.skipped_rif_records_new
${logic.hsql-only}     drop constraint public.skipped_rif_records_new_pkey;
${logic.hsql-only} alter table public.skipped_rif_records_new
${logic.hsql-only}     add constraint skipped_rif_records_pkey primary key (record_id);

alter index ${logic.psql-only} if exists 
    public.skipped_rif_records_new_bene_id_idx
        rename to skipped_rif_records_bene_id_idx;

-- =====================
-- carrier_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.carrier_claims_new_pkey
${logic.psql-only}    rename to carrier_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.carrier_claims_new
${logic.hsql-only}     drop constraint public.carrier_claims_new_pkey cascade;
${logic.hsql-only} alter table public.carrier_claims_new
${logic.hsql-only}     add constraint carrier_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.carrier_claims_new_bene_id_idx
        rename to carrier_claims_bene_id_idx;

-- =====================
-- carrier_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.carrier_claim_lines_new_pkey
${logic.psql-only}    rename to carrier_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.carrier_claim_lines_new
${logic.hsql-only}     drop constraint public.carrier_claim_lines_new_pkey cascade;
${logic.hsql-only} alter table public.carrier_claim_lines_new
${logic.hsql-only}     add constraint carrier_claim_lines_pkey primary key (clm_id, line_num);

-- =====================
-- dme_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.dme_claims_new_pkey
${logic.psql-only}    rename to dme_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.carrier_claims_new
${logic.hsql-only}     drop constraint public.dme_claims_new_pkey cascade;;
${logic.hsql-only} alter table public.dme_claims_new
${logic.hsql-only}     add constraint dme_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.dme_claims_new_bene_id_idx
        rename to dme_claims_bene_id_idx;

-- =====================
-- dme_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.dme_claim_lines_new_pkey
${logic.psql-only}    rename to dme_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.dme_claim_lines_new
${logic.hsql-only}     drop constraint public.dme_claim_lines_new_pkey;
${logic.hsql-only} alter table public.dme_claim_lines_new
${logic.hsql-only}     add constraint dme_claim_lines_pkey primary key (clm_id, line_num);

-- =====================
-- hha_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.hha_claims_new_pkey
${logic.psql-only}    rename to hha_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.hha_claims_new
${logic.hsql-only}     drop constraint public.hha_claims_new_pkey cascade;;
${logic.hsql-only} alter table public.hha_claims_new
${logic.hsql-only}     add constraint hha_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.hha_claims_new_bene_id_idx
        rename to hha_claims_bene_id_idx;

-- =====================
-- hha_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.hha_claim_lines_new_pkey
${logic.psql-only}    rename to hha_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.hha_claim_lines_new
${logic.hsql-only}     drop constraint public.hha_claim_lines_new_pkey;
${logic.hsql-only} alter table public.hha_claim_lines_new
${logic.hsql-only}     add constraint hha_claim_lines_pkey primary key (clm_id, clm_line_num);

-- =====================
-- hospice_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.hospice_claims_new_pkey
${logic.psql-only}    rename to hospice_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.hospice_claims_new
${logic.hsql-only}     drop constraint public.hospice_claims_new_pkey cascade;;
${logic.hsql-only} alter table public.hospice_claims_new
${logic.hsql-only}     add constraint hospice_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.hospice_claims_new_bene_id_idx
        rename to hospice_claims_bene_id_idx;

-- =====================
-- hospice_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.hospice_claim_lines_new_pkey
${logic.psql-only}    rename to hospice_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.hospice_claim_lines_new
${logic.hsql-only}     drop constraint public.hospice_claim_lines_new_pkey;
${logic.hsql-only} alter table public.hospice_claim_lines_new
${logic.hsql-only}     add constraint hospice_claim_lines_pkey primary key (clm_id, clm_line_num);

-- =====================
-- inpatient_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.inpatient_claims_new_pkey
${logic.psql-only}    rename to inpatient_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.inpatient_claims_new
${logic.hsql-only}     drop constraint public.inpatient_claims_new_pkey cascade;;
${logic.hsql-only} alter table public.inpatient_claims_new
${logic.hsql-only}     add constraint inpatient_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.inpatient_claims_new_bene_id_idx
        rename to inpatient_claims_bene_id_idx;

-- =====================
-- inpatient_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.inpatient_claim_lines_new_pkey
${logic.psql-only}    rename to inpatient_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.inpatient_claim_lines_new
${logic.hsql-only}     drop constraint public.inpatient_claim_lines_new_pkey;
${logic.hsql-only} alter table public.inpatient_claim_lines_new
${logic.hsql-only}     add constraint inpatient_claim_lines_pkey primary key (clm_id, clm_line_num);

-- =====================
-- outpatient_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.outpatient_claims_new_pkey
${logic.psql-only}    rename to outpatient_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.outpatient_claims_new
${logic.hsql-only}     drop constraint public.outpatient_claims_new_pkey cascade;;
${logic.hsql-only} alter table public.outpatient_claims_new
${logic.hsql-only}     add constraint outpatient_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.outpatient_claims_new_bene_id_idx
        rename to outpatient_claims_bene_id_idx;

-- =====================
-- outpatient_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.outpatient_claim_lines_new_pkey
${logic.psql-only}    rename to outpatient_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.outpatient_claim_lines_new
${logic.hsql-only}     drop constraint public.outpatient_claim_lines_new_pkey;
${logic.hsql-only} alter table public.outpatient_claim_lines_new
${logic.hsql-only}     add constraint outpatient_claim_lines_pkey primary key (clm_id, clm_line_num);

-- =====================
-- partd_events
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.partd_events_new_pkey
${logic.psql-only}    rename to partd_events_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.partd_events_new
${logic.hsql-only}     drop constraint public.partd_events_new_pkey cascade;;
${logic.hsql-only} alter table public.partd_events_new
${logic.hsql-only}     add constraint partd_events_pkey primary key (pde_id);

alter index ${logic.psql-only} if exists 
    public.partd_events_new_bene_id_idx
        rename to partd_events_bene_id_idx;

-- =====================
-- snf_claims
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.snf_claims_new_pkey
${logic.psql-only}    rename to snf_claims_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.snf_claims_new
${logic.hsql-only}     drop constraint public.snf_claims_new_pkey cascade;;
${logic.hsql-only} alter table public.snf_claims_new
${logic.hsql-only}     add constraint snf_claims_pkey primary key (clm_id);

alter index ${logic.psql-only} if exists 
    public.snf_claims_new_bene_id_idx
        rename to snf_claims_bene_id_idx;

-- =====================
-- snf_claim_lines
-- =====================
-- psql allows you to rename primary key
${logic.psql-only} alter index if exists public.snf_claim_lines_new_pkey
${logic.psql-only}    rename to snf_claim_lines_pkey;

-- hsql doesn't support rename on primary key; we'll drop and re-create primary key
${logic.hsql-only} alter table public.snf_claim_lines_new
${logic.hsql-only}     drop constraint public.snf_claim_lines_new_pkey;
${logic.hsql-only} alter table public.snf_claim_lines_new
${logic.hsql-only}     add constraint snf_claim_lines_pkey primary key (clm_id, clm_line_num);

-- ========================================================================
-- final fixup:
--   : drop view(s) named "<base_name>"
--   : rename "<base_name>_new" tables to "<base_name>"
-- ========================================================================

-- beneficiaries
drop view ${logic.psql-only} if exists 
    beneficiaries;

alter table ${logic.psql-only} if exists 
    public.beneficiaries_new rename to beneficiaries;

-- beneficiaries_history
drop view ${logic.psql-only} if exists 
    beneficiaries_history;

alter table ${logic.psql-only} if exists 
    public.beneficiaries_history_new rename to beneficiaries_history;

-- medicare_beneficiaryid_history
drop view ${logic.psql-only} if exists 
    medicare_beneficiaryid_history;

alter table ${logic.psql-only} if exists 
    public.medicare_beneficiaryid_history_new rename to medicare_beneficiaryid_history;

-- beneficiary_monthly
drop view ${logic.psql-only} if exists 
    beneficiary_monthly;

alter table ${logic.psql-only} if exists 
    public.beneficiary_monthly_new rename to beneficiary_monthly;

-- skipped_rif_records
drop view ${logic.psql-only} if exists 
    skipped_rif_records;

alter table ${logic.psql-only} if exists 
    public.skipped_rif_records_new rename to skipped_rif_records;

-- carrier_claims
drop view ${logic.psql-only} if exists 
    carrier_claims;

alter table ${logic.psql-only} if exists 
    public.carrier_claims_new rename to carrier_claims;

-- carrier_claim_lines
drop view ${logic.psql-only} if exists 
    carrier_claim_lines;

alter table ${logic.psql-only} if exists 
    public.carrier_claim_lines_new rename to carrier_claim_lines;

-- dme_claims
drop view ${logic.psql-only} if exists 
    dme_claims;

alter table ${logic.psql-only} if exists 
    public.dme_claims_new rename to dme_claims;

-- dme_claim_lines
drop view ${logic.psql-only} if exists 
    dme_claim_lines;

alter table ${logic.psql-only} if exists 
    public.dme_claim_lines_new rename to dme_claim_lines;

-- hha_claims
drop view ${logic.psql-only} if exists 
    hha_claims;

alter table ${logic.psql-only} if exists 
    public.hha_claims_new rename to hha_claims;

-- hha_claim_lines
drop view ${logic.psql-only} if exists 
    hha_claim_lines;

alter table ${logic.psql-only} if exists 
    public.hha_claim_lines_new rename to hha_claim_lines;

-- hospice_claims
drop view ${logic.psql-only} if exists 
    hospice_claims;

alter table ${logic.psql-only} if exists 
    public.hospice_claims_new rename to hospice_claims;

-- hospice_claim_lines
drop view ${logic.psql-only} if exists 
    hospice_claim_lines;

alter table ${logic.psql-only} if exists 
    public.hospice_claim_lines_new rename to hospice_claim_lines;

-- inpatient_claims
drop view ${logic.psql-only} if exists 
    inpatient_claims;

alter table ${logic.psql-only} if exists 
    public.inpatient_claims_new rename to inpatient_claims;

-- inpatient_claim_lines
drop view ${logic.psql-only} if exists 
    inpatient_claim_lines;

alter table ${logic.psql-only} if exists 
    public.inpatient_claim_lines_new rename to inpatient_claim_lines;

-- outpatient_claims
drop view ${logic.psql-only} if exists 
    outpatient_claims;

alter table ${logic.psql-only} if exists 
    public.outpatient_claims_new rename to outpatient_claims;

-- outpatient_claim_lines
drop view ${logic.psql-only} if exists 
    outpatient_claim_lines;

alter table ${logic.psql-only} if exists 
    public.outpatient_claim_lines_new rename to outpatient_claim_lines;

-- partd_events
drop view ${logic.psql-only} if exists 
    partd_events;

alter table ${logic.psql-only} if exists 
    public.partd_events_new rename to partd_events;

-- snf_claims
drop view ${logic.psql-only} if exists 
    snf_claims;

alter table ${logic.psql-only} if exists 
    public.snf_claims_new rename to snf_claims;

-- snf_claim_lines
drop view ${logic.psql-only} if exists 
    snf_claim_lines;

alter table ${logic.psql-only} if exists 
    public.snf_claim_lines_new rename to snf_claim_lines;
