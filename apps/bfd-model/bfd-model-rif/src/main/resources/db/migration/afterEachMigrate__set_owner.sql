/*
  This callback is executed after each pending flyway migration is executed.
  
  Ensuring any objects created in a migration gets owned by the designated role without requiring event triggers (yuk).
*/
${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM set_fhirdb_owner('fhir');
${logic.psql-only} END
${logic.psql-only} $$ LANGUAGE plpgsql;
