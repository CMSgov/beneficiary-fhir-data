-- Add a pre-[insert|update] trigger to the beneficiaries table to populate
-- bene_id_numeric on a record update. We need both pre-insert and
-- pre-update trigger to fire since the Beneficiary.java ORM object will
-- initialize the native data type to 0 (zero) when the object is instantiated.

-- The database trigger will be used to imbue a bigint value into the bene_id_numeric
-- column during a table record INSERT or UPDATE operation what this means is that
-- prior to a record being written, the database engine will execute the trigger, causing
-- the bene_id_numeric to be populated with a value based on a 'cast' of the bene_id (varchar).

-- Unfortunately HSQL doesn't support 'insert or update' directive so we have to
-- have a separate trigger for the update.
${logic.hsql-only} CREATE TRIGGER beneficiaries_pre_update_trigger BEFORE UPDATE ON public.beneficiaries
${logic.hsql-only}    REFERENCING NEW as newrow FOR EACH ROW
${logic.hsql-only}       BEGIN ATOMIC
${logic.hsql-only}            SET newrow.bene_id_numeric = convert(newrow.bene_id, SQL_BIGINT);
${logic.hsql-only}       END;

-- for psql, completely clean out previous trigger and function created in V60 flyway script
${logic.psql-only} DROP TRIGGER IF EXISTS beneficiaries_insert_trigger ON public.beneficiaries CASCADE;
${logic.psql-only} DROP FUNCTION IF EXISTS public.beneficiaries_pre_insert() CASCADE;

-- create a trigger function that can be invoked to populate the bene_id_numeric
${logic.psql-only} CREATE OR REPLACE FUNCTION public.beneficiaries_populate_bene_id_numeric()
${logic.psql-only}     RETURNS trigger
${logic.psql-only}     LANGUAGE 'plpgsql'
${logic.psql-only}    VOLATILE NOT LEAKPROOF
${logic.psql-only} AS $BODY$
${logic.psql-only} BEGIN
${logic.psql-only}   NEW.bene_id_numeric = NEW.bene_id::bigint;
${logic.psql-only}   RETURN NEW;
${logic.psql-only} END;
${logic.psql-only} $BODY$;

-- for Postgres, we can combine the directive using "INSERT OR UPDATE"
${logic.psql-only} DROP TRIGGER IF EXISTS beneficiaries_pre_insert_update_trigger ON public.beneficiaries CASCADE;
${logic.psql-only} CREATE TRIGGER beneficiaries_pre_insert_update_trigger
${logic.psql-only}     BEFORE INSERT OR UPDATE ON public.beneficiaries
${logic.psql-only}     FOR EACH ROW
${logic.psql-only}       EXECUTE FUNCTION public.beneficiaries_populate_bene_id_numeric();