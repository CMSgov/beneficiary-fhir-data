-- lock the beneficiaries table for UPDATE EXCLUSIVE; we need
-- to wrap in a transaction since lock can only be acquired in a transaction.
${logic.psql-only} BEGIN;
-- set a short lock timeout...no one should be updating table (yet)
${logic.psql-only} set lock_timeout = 100;
-- try to grab a lock on the table
${logic.psql-only} LOCK TABLE ONLY public.beneficiaries IN SHARE UPDATE EXCLUSIVE MODE;
-- once we have the lock, turn off the lock timeout since the rest of
-- the DDL will take some time; read (SELECT) will still have access 
${logic.psql-only} set lock_timeout = 0;

-- update will be fairly long running; no on effect readers (select)
UPDATE public.beneficiaries
${logic.hsql-only} SET bene_id_numeric = convert(bene_id, SQL_BIGINT)
${logic.psql-only} SET bene_id_numeric = bene_id::bigint
WHERE bene_id is not null;

-- building index will take some time as well, but readers still OK
CREATE UNIQUE INDEX beneficiaries_bene_id_numeric_idx
    ON public.beneficiaries (bene_id_numeric);

-- release the lock
${logic.psql-only} COMMIT;