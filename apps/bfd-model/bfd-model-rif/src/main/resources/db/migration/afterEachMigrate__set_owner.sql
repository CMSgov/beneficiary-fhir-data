/*
  This callback is executed after each pending migration is executed, ensuring any objects created in the migration
  gets owned by the fhirdb owner role.
*/
${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM set_fhirdb_owner('fhir');
${logic.psql-only} END
${logic.psql-only} $$ LANGUAGE plpgsql;
