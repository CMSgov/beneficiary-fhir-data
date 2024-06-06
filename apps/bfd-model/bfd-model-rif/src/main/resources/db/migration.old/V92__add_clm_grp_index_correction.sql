-- ADD CLM_GRP_ID INDEX
--
-- This flyway script is intended to add an index on clm_grp_id for a handful
-- of tables that need their clm_grp_ids cleaned up for certain synthetic data.
-- This index is temporary and will be removed in a subsequent script.

-- This is done in psql only since this index is only required for a one-time
-- cleanup script against the actual database. This script corrects the previous
-- script which did not have unique names for each index.

-- =====================
-- inpatient_claims
-- =====================
${logic.psql-only} create index concurrently if not exists clm_grp_id_index_2 on public.inpatient_claims(clm_grp_id);

-- =====================
-- outpatient_claims
-- =====================
${logic.psql-only} create index concurrently if not exists clm_grp_id_index_3 on public.outpatient_claims(clm_grp_id);

-- =====================
-- partd_events
-- =====================
${logic.psql-only} create index concurrently if not exists clm_grp_id_index_4 on public.partd_events(clm_grp_id);