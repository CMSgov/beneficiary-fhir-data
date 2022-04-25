-- Add a pre-update trigger to the beneficiaries table to populate
-- bene_id_numeric on a record update. Therei is currently a
-- pre-insert trigger, but we also need a pre-update trigger since
-- the Beneficiary.java ORM object will initialize the native data
-- type to 0 (zero) when the object is instantiated.

-- create a database trigger which will be used to imbue a bigint value
-- into the bene_id_numeric column during a table record UPDATE operation.
-- The trigger is defined as a pre-update trigger, which means that prior to
-- record being written, the database engine will execute the trigger, causing
-- the bene_id_numeric to be populated with a value based on a 'cast'
-- of the bene_id (varchar).

-- HSQL always gets created/initialized from scratch so no need to drop trigger
-- Unfortunately HSQL doesn't support 'insert or update' directive so we have to
-- have a separate trigger for the update.
${logic.hsql-only} CREATE TRIGGER beneficiaries_pre_update_trigger BEFORE UPDATE ON public.beneficiaries
${logic.hsql-only}    REFERENCING NEW as newrow FOR EACH ROW
${logic.hsql-only}       BEGIN ATOMIC
${logic.hsql-only}            SET newrow.bene_id_numeric = convert(newrow.bene_id, SQL_BIGINT);
${logic.hsql-only}       END;

-- for Postgres, we can combine the directive.
${logic.psql-only} DROP TRIGGER IF EXISTS beneficiaries_insert_trigger ON public.beneficiaries;
${logic.psql-only} CREATE TRIGGER beneficiaries_insert_trigger
${logic.psql-only}     BEFORE INSERT OR UPDATE ON public.beneficiaries
${logic.psql-only}     FOR EACH ROW
${logic.psql-only}       EXECUTE FUNCTION public.beneficiaries_pre_insert();