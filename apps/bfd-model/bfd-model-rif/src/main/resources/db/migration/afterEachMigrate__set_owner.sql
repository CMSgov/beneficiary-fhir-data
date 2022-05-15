${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM set_fhirdb_owner('fhir');
${logic.psql-only} END
${logic.psql-only} $$ LANGUAGE plpgsql;
