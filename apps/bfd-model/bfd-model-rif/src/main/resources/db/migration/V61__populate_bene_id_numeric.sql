-- POPULATE_BENE_ID_NUMERIC.SQL
-- This script will populate a column, bene_id_numeric, in the
-- beneficiaries table, by casting each record's current varchar
-- value of its bene_id column. Once the entire table has been updated,
-- then a unique index will be built referencing bene_id_numeric.

-- Try to get a lock on the beneficiaries table; while in theory, we
-- could depend on row-level locking of the UPDATE, we acquire a SHARED
-- UPDATE EXCLUSIVE which by definition assures us that no other update
-- operations can run while we are doing the updates in this script.

-- Wrap in a transaction; acquiring a lock can only be done inside a transaction.
${logic.psql-only} BEGIN;

-- Set a short lock timeout...no one should be updating table (yet);
-- lock requests are queued, so we don't want to hang around if some other
-- process currently has a lock. If we don't get the requested lock in less
-- than 100ms, then the script will exit/fail.
${logic.psql-only} set lock_timeout = 100;

-- try to grab a lock on the table
${logic.psql-only} LOCK TABLE ONLY public.beneficiaries IN SHARE UPDATE EXCLUSIVE MODE;

-- update will be fairly long running; our acquired lock does not impact readers (select)
UPDATE public.beneficiaries
${logic.hsql-only} SET bene_id_numeric = convert(bene_id, SQL_BIGINT)
${logic.psql-only} SET bene_id_numeric = bene_id::bigint
WHERE bene_id is not null;

-- building index on bene_id_numeric will also take some time; readers still OK
CREATE UNIQUE INDEX beneficiaries_bene_id_numeric_idx
    ON public.beneficiaries (bene_id_numeric);

-- release the lock
${logic.psql-only} COMMIT;