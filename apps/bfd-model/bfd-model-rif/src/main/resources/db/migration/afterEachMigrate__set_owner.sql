/*
  This callback is executed after each pending migration is executed, ensuring any objects created in the migration
  gets owned by the fhirdb owner role.
*/
${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fhir') THEN 
${logic.psql-only}     PERFORM set_fhirdb_owner('fhir');
${logic.psql-onky}   END IF;
${logic.psql-only} END
${logic.psql-only} $$ LANGUAGE plpgsql;
