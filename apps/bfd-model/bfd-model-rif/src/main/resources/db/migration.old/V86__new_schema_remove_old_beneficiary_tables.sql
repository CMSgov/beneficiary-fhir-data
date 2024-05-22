--
-- Script to drop old beneficiary-related tables that have been replaced
-- by _new tables. Once all db changes are complete, the _new table names
-- will be renamed back to their original table names.
--
DROP TABLE IF EXISTS public.beneficiaries_history CASCADE;
DROP TABLE IF EXISTS public.medicare_beneficiaryid_history CASCADE;
DROP TABLE IF EXISTS public.beneficiary_monthly CASCADE;
DROP TABLE IF EXISTS public.skipped_rif_records CASCADE;
DROP TABLE IF EXISTS public.beneficiaries CASCADE;
