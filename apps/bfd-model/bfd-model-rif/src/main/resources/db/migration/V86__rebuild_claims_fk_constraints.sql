-- Set a short lock timeout...no one should be updating table (yet);
-- lock requests are queued, so we don't want to hang around if some other
-- process currently has a lock. If we don't get the requested lock in less
-- than 100ms, then the script will exit/fail.
${logic.psql-only} set lock_timeout = 100;

-- Wrap in a transaction; acquiring a lock can only be done inside a transaction.
${logic.psql-only} begin;

-- alter table creates an exclusive lock while it validates the data for the
-- table on which the constraint is being defined. We'll create the constraint
-- in 'not valid' mode which allows the constraint to created but not validated,
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
alter table if exists public.carrier_claims_new
    add constraint carrier_claims_bene_id_to_beneficiaries
    foreign key (bene_id)
${logic.hsql-only} references public.beneficiaries_new (bene_id);
${logic.psql-only} references public.beneficiaries_new not valid;

-- release the lock
${logic.psql-only} commit;

-- The remaining constraint defintion(s) follows the pattern from above; wrap
-- things in a transaction, add the constraint using 'not valid' mode, and then
-- committing the transaction. We'll validate all out 'invalid' constraints at
-- the end of this Flyway scropt.

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
-- so no need to have any further table locking.
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

${logic.psql-only} alter table if exists public.partd_Events_new
${logic.psql-only} validate constraint partd_events_bene_id_to_beneficiaries;

${logic.psql-only} alter table if exists public.snf_claims_new 
${logic.psql-only} validate constraint snf_claims_bene_id_to_beneficiaries;

${logic.psql-only} commit;