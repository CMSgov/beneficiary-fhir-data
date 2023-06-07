-- BFD-2629
-- Drop_Support_Medicare_BeneficiaryId_History
-- flyway migration to drop support for;
--   - medicare_beneficiaryid_history
--   - medicare_beneficiaryid_history_invalid_beneficiaries
--
-- These tables were created as a one-time CCW data drop containing
-- historical MBI data; historical MBI data (i.e., MBI_NUM) can be
-- used to derive a bene_id (beneficiary) using old(er) data.
-- 
-- Current BFD functionality for looking up bene_id using historical
-- MBI_NUM is now handled by the beneficiaries_history table.
--
drop table if exists medicare_beneficiaryid_history cascade;
drop table if exists medicare_beneficiaryid_history_invalid_beneficiaries cascade;
drop table if exists medicare_beneficiaryid_history_temp cascade;
