-- add a column to beneficiaries table; an ALTER TABLE by definition
-- creates an exclusive lock on the table.
ALTER TABLE public.beneficiaries ADD bene_id_numeric bigint;

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