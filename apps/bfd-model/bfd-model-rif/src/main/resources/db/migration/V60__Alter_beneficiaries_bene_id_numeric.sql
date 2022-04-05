-- add a column to beneficiaries table; an ALTER TABLE by definition
<<<<<<< HEAD
-- creates an exclusive lock on the table.
ALTER TABLE public.beneficiaries ADD bene_id_numeric bigint;

=======
-- creates an 'access exclusive' lock on the table.
ALTER TABLE public.beneficiaries ADD bene_id_numeric bigint;

-- create a database trigger which will be used to imbue a bigint value
-- into the bene_id_numeric column  during a table record INSERT operation.
-- The trigger is defined as a pre-insert trigger, which means that prior to
-- new record being written, the database engine will execute the trigger,
-- which in turn, fills the bene_id_numeric with a value based on a 'cast'
-- of the bene_id (varchar).

>>>>>>> master
-- HSQL always gets created/initialized from scratch so no need to drop trigger
${logic.hsql-only} CREATE TRIGGER beneficiaries_insert_trigger BEFORE INSERT ON public.beneficiaries
${logic.hsql-only}    REFERENCING NEW as newrow FOR EACH ROW
${logic.hsql-only}       BEGIN ATOMIC
${logic.hsql-only}            SET newrow.bene_id_numeric = convert(newrow.bene_id, SQL_BIGINT);
${logic.hsql-only}       END;

${logic.psql-only} CREATE OR REPLACE FUNCTION public.beneficiaries_pre_insert()
${logic.psql-only}     RETURNS trigger
${logic.psql-only}     LANGUAGE 'plpgsql'
${logic.psql-only}    VOLATILE NOT LEAKPROOF
${logic.psql-only} AS $BODY$
${logic.psql-only} BEGIN
${logic.psql-only}   NEW.bene_id_numeric = NEW.bene_id::bigint;
${logic.psql-only}   RETURN NEW;
${logic.psql-only} END;
${logic.psql-only} $BODY$;

${logic.psql-only} DROP TRIGGER IF EXISTS beneficiaries_insert_trigger ON public.beneficiaries;
${logic.psql-only} CREATE TRIGGER beneficiaries_insert_trigger
${logic.psql-only}     BEFORE INSERT ON public.beneficiaries
${logic.psql-only}     FOR EACH ROW
${logic.psql-only}       EXECUTE FUNCTION public.beneficiaries_pre_insert();