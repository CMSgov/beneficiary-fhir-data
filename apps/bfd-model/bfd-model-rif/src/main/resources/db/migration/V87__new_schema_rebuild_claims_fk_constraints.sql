-- REBUILD_CLAIMS_FK_CONSTRAINTS.SQL
--
-- This flyway script (re-) implements foreign key (FK) constraints between
-- parent claims tables (i.e., carrier_claims) and the beneficiaries table
-- on the column bene_id.
--
-- The script does this operation 'in-place' meaning that the constraints
-- are created on active database(s) (i.e., prod, prod-sbx, test). Normally
-- an ALTER TABLE, ADD CONSTRAINT requires an EXCLUSIVE LOCK on a table
-- because the db engine has to validate the integrity of the column value(s)
-- used in the constraint. While there is no getting around the necessity for
-- an EXCLUSIVE LOCK, we can minimize the duration of the lock by using a
-- 2-step pattern:
--
--   1) creating the constraint as NOT VALID; this allows a constraint to be
--      created (a meta-data operation) without verifying the column data. This
--      limits the exclusive lock duration to be milli-seconds.
--
--   2) then the 2nd step performs the actual validation of the table column
--      data; however, that operation only requires a SHARED LOCK on a table,
--      meaning that things like READ (select) operations can function normally.
--
-- Set a short lock timeout...no one should be updating a table (yet);
-- lock requests are queued, so we don't want to hang around if some other
-- process currently has a lock. If we don't get the requested lock in less
-- than 100ms, then the script will exit/fail.
${logic.psql-only} set lock_timeout = 100;

-- alter table creates an exclusive lock while it validates the data for the
-- table on which the constraint is being defined. We'll create the constraint
-- in 'NOT VALID' mode which allows the constraint to created but not validated,
-- resulting in a very fast meta-data only change.
--
-- A side note for reviewers; the BFD standard naming convention calls for
-- including the table name(s) as part of constraint name. We deviate slightly
-- from this for the '_new' tables since:
--   a) we are not currently using the original constraint name(s)
--   b) by naming the constraints to use its original name, we eliminate
--      the necessity to alter the constraint names once we rename all the
--      _new tables back to their original name(s).
--
-- Wrap in a transaction; acquiring a lock can only be done inside a transaction.
-- We are explicitly starting a transaction to minimize exclusive lock duration;
-- doing an ALTER TABLE implicitly acquires an EXCLUSIVE LOCK on the table and in
-- theory, the lock would last only as long as the ALTER TABLE operation(s) were
-- in progress. However, the flyway .conf file for this script turns off flyway
-- transaction processing to allow this script to be in complete control of the
-- transaction and its associated commit (which releases the lock).

${logic.psql-only} begin;
alter table if exists public.carrier_claims_new
    add constraint carrier_claims_bene_id_to_beneficiaries
    foreign key (bene_id)
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
-- release the lock by either a commit or rollback of the transaction.
${logic.psql-only} commit;

-- The remaining constraint defintion(s) follows the pattern from above; wrap
-- things in a transaction, add the constraint using 'not valid' mode, and then
-- committing the transaction. We'll validate all our 'invalid' constraints in
-- the next section of this Flyway script.

${logic.psql-only} begin;
alter table if exists public.dme_claims_new 
   add constraint dme_claims_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

${logic.psql-only} begin;
alter table if exists public.hha_claims_new 
   add constraint hha_claims_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

${logic.psql-only} begin;
alter table if exists public.hospice_claims_new 
   add constraint hospice_claims_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

${logic.psql-only} begin;
alter table if exists public.inpatient_claims_new 
   add constraint inpatient_claims_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

${logic.psql-only} begin;
alter table if exists public.outpatient_claims_new 
   add constraint outpatient_claims_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

${logic.psql-only} begin;
alter table if exists public.partd_events_new
   add constraint partd_events_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

${logic.psql-only} begin;
alter table if exists public.snf_claims_new 
   add constraint snf_claims_bene_id_to_beneficiaries
   foreign key (bene_id) 
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;
${logic.psql-only} commit;

-- the rest of the processing will be wrapped in a transaction; a validation
-- of a constraint operates in shared access mode (i.e. allows read/select),
-- so no need to have any further table locking. However, since we've turned
-- off flyway transaction processing, we are now responsible for committing
-- the changes.
${logic.psql-only} begin;

${logic.psql-only} alter table public.carrier_claims_new
${logic.psql-only} validate constraint carrier_claims_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.dme_claims_new 
${logic.psql-only} validate constraint dme_claims_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.hha_claims_new 
${logic.psql-only} validate constraint hha_claims_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.hospice_claims_new 
${logic.psql-only} validate constraint hospice_claims_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.inpatient_claims_new 
${logic.psql-only} validate constraint inpatient_claims_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.outpatient_claims_new 
${logic.psql-only} validate constraint outpatient_claims_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.partd_events_new
${logic.psql-only} validate constraint partd_events_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.snf_claims_new 
${logic.psql-only} validate constraint snf_claims_bene_id_to_beneficiaries;

${logic.psql-only} commit;