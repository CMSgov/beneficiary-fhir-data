-- REMOVE CLM_GRP_ID INDEX
--
-- This flyway script is intended to remove an index on clm_grp_id for a handful
-- of tables that needed their clm_grp_ids cleaned up for certain synthetic data.
-- This index was temporary and can now be removed since the data cleanup is complete.

-- This is done in psql only since this was only added in the real databases.

-- =====================
-- carrier_claims
-- =====================
${logic.psql-only} drop index concurrently if exists clm_grp_id_index;

-- =====================
-- inpatient_claims
-- =====================
${logic.psql-only} drop index concurrently if exists clm_grp_id_index_2;

-- =====================
-- outpatient_claims
-- =====================
${logic.psql-only} drop index concurrently if exists clm_grp_id_index_3;

-- =====================
-- partd_events
-- =====================
${logic.psql-only} drop index concurrently if exists clm_grp_id_index_4;