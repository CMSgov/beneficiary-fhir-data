--
-- Script to drop old claims tables that have been replaced by _new tables.
-- Once all db changes are complete, the _new table names will be renamed
-- back to their original table names.
--
DROP TABLE IF EXISTS public.partd_events CASCADE;
--
DROP TABLE IF EXISTS public.carrier_claim_lines CASCADE;
DROP TABLE IF EXISTS public.carrier_claims CASCADE;
--
DROP TABLE IF EXISTS public.dme_claim_lines CASCADE;
DROP TABLE IF EXISTS public.dme_claims CASCADE;
--
DROP TABLE IF EXISTS public.hha_claim_lines CASCADE;
DROP TABLE IF EXISTS public.hha_claims CASCADE;
--
DROP TABLE IF EXISTS public.hospice_claim_lines CASCADE;
DROP TABLE IF EXISTS public.hospice_claims CASCADE;
--
DROP TABLE IF EXISTS public.inpatient_claim_lines CASCADE;
DROP TABLE IF EXISTS public.inpatient_claims CASCADE;
--
DROP TABLE IF EXISTS public.outpatient_claim_lines CASCADE;
DROP TABLE IF EXISTS public.outpatient_claims CASCADE;
--
DROP TABLE IF EXISTS public.snf_claim_lines CASCADE;
DROP TABLE IF EXISTS public.snf_claims CASCADE;