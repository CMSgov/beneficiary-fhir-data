-- This view tracks the current version of a beneficiary tied to each xref group.
-- This process is inexact due to the nature of the data, but we make our best guess based on the information we have available.

-- Our best guess is stored in a field called xref_rank which is defined as follows:
-- If we have an MBI and the xref_sw is set to 'N' (meaning this record is NOT merged into another), assign a rank of 1 (1st place)
-- If we have an MBI and the xref_sw is set to 'Y' (meaning this record IS merged into another), assign a rank of 2 (2nd place)
-- Otherwise, assign a rank of 3 (last place). This case should only be hit if the MBI is null
-- because each bene with an xref id will have an xref_sw value of 'Y' or 'N'

-- For each xref group, we take the record with the lowest rank
-- If there's a tie, we take the record with the highest bene_id

CREATE MATERIALIZED VIEW IF NOT EXISTS ccw.current_beneficiaries AS (
    WITH ranked_benes AS (
        SELECT
            bene_id,
            xref_grp_id,
            CASE
                WHEN mbi_num IS NOT NULL
                    AND xref_sw = 'N'
                THEN 1
                WHEN mbi_num IS NOT NULL
                    AND xref_sw = 'Y'
                THEN 2
                ELSE 3
        END AS xref_rank
        FROM ccw.beneficiaries
        WHERE xref_grp_id IS NOT NULL
    )
    SELECT DISTINCT ON (xref_grp_id)
        bene_id,
        xref_grp_id
    FROM ranked_benes
    ORDER BY xref_grp_id,
        xref_rank,
        bene_id DESC
);

CREATE UNIQUE INDEX IF NOT EXISTS current_beneficiaries_bene_id_idx ON ccw.current_beneficiaries(bene_id);

-- Care must be taken to create "security definer" functions safely
-- see https://www.postgresql.org/docs/current/sql-createfunction.html
CREATE OR REPLACE FUNCTION ccw.refresh_current_beneficiaries()
RETURNS VOID
LANGUAGE sql
-- Only the owner of the view may refresh it, we need to set "security definer" so the function
-- can execute in the context of the creator
SECURITY DEFINER
-- Using "concurrently" will make the refresh slower, but it will not block any reads
-- on the view while the refresh is in progress
AS 'REFRESH MATERIALIZED VIEW CONCURRENTLY ccw.current_beneficiaries';
-- search_path is the order in which schemas are searched when a name is referenced with no schema specified
-- Postgres recommends setting this on functions marked as "security definer" to prevent malicious users from
-- creating an object that shadows an existing one on a globally writable schema
ALTER FUNCTION ccw.refresh_current_beneficiaries() SET search_path = ccw;
-- Execute privilege is granted to PUBLIC by default
REVOKE ALL ON FUNCTION ccw.refresh_current_beneficiaries() FROM PUBLIC;
-- This only needs to be executed by the pipeline
GRANT EXECUTE ON FUNCTION ccw.refresh_current_beneficiaries() TO api_pipeline_svcs;
