-- NEW_SCHEMA_FINAL-CLEANUP.SQL
--
-- This flyway script is intended to a perform final cleanup of BFD database
-- schema changes related to using BIGINT for bene_id, clm_id, and clm_grp_id.
--
-- In particular, this script:
--   1) fixes spelling errors in constraint names
--   2) removes some unused table indexes.
--   3) drop db triggers that are no longer used and in fact are invalid.
--   4) drops some tables that were temporarily used during various data analysis efforts
--   5) drop unused procedures that were used from db trigger(s)
--
-- fix spelling error 'benficiaries' in constraint names
--
${logic.psql-only} alter table if exists public.medicare_beneficiaryid_history
${logic.psql-only}  rename constraint medicare_beneficiaryid_history_to_benficiaries
${logic.psql-only}    to medicare_beneficiaryid_history_to_beneficiaries;

${logic.psql-only} alter table if exists public.beneficiary_monthly
${logic.psql-only}  rename constraint beneficiary_monthly_to_benficiaries
${logic.psql-only}    to beneficiary_monthly_to_beneficiaries;

-- remove unused indexes from beneficiaries table
drop index if exists  beneficiaries_ptd_cntrct_number_jan_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_feb_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_mar_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_apr_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_may_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_jun_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_jul_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_aug_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_sept_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_oct_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_nov_id_idx;
drop index if exists  beneficiaries_ptd_cntrct_number_dec_id_idx;

-- drop unused / invalid db triggers.
${logic.psql-only} drop trigger if exists
${logic.psql-only}   beneficiaries_populate_bene_id_numeric
${logic.psql-only}     on beneficiary_monthly cascade;

${logic.psql-only} drop trigger if exists
${logic.psql-only}   track_bene_monthly_change
${logic.psql-only}     on beneficiary_monthly cascade;

-- drop tables that were used (at some point in time) for
-- various data analysis or trouble-shooting efforts.
${logic.psql-only} drop table if exists bene_hist_temp;
${logic.psql-only} drop table if exists beneficiary_monthly_audit;
${logic.psql-only} drop table if exists ccw_load_temp;
${logic.psql-only} drop table if exists public."TempBene";

-- cleanup old stored procedures
${logic.psql-only} drop procedure if exists load_from_ccw;
${logic.psql-only} drop procedure if exists update_bene_monthly;