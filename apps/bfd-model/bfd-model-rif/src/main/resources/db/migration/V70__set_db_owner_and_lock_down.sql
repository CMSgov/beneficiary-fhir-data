/*
  This migration:
    1. Creates and makes 'fhir' the designated fhirdb owner role
    2. Locks down the public schema and fhirdb to prevent users from gaining access via inheritence
    3. Revokes superuser privileges that may have been granted to the current user in order to run the
*/

${logic.psql-only} DO $$
${logic.psql-only} DECLARE
${logic.psql-only} 	t record;
${logic.psql-only} BEGIN
${logic.psql-only}   -- designate a fhirdb owner
${logic.psql-only}   PERFORM create_role_if_not_exists('fhir');
${logic.psql-only}   ALTER ROLE fhir WITH NOINHERIT NOCREATEDB NOCREATEROLE NOLOGIN;
${logic.psql-only}   PERFORM set_fhirdb_owner('fhir');
${logic.psql-only}
${logic.psql-only}   -- "PUBLIC" is a built-in implicit role/keyword similar to CURRENT_USER. By default, PUBLIC grants
${logic.psql-only}   -- any user with INHERIT (the default) create/usage privileges Yuk.
${logic.psql-only}   REVOKE ALL ON SCHEMA public FROM PUBLIC;   -- don't allow schema access by default
${logic.psql-only}   REVOKE ALL ON DATABASE fhirdb FROM PUBLIC; -- don't allow database access by default
${logic.psql-only}
${logic.psql-only}   -- revoke elevated permissions we may have temporarily granted to run these migrations
${logic.psql-only}   IF pg_has_role('rds_superuser', 'member') THEN
${logic.psql-only}     REVOKE rds_superuser FROM CURRENT_USER;
${logic.psql-only}   END IF;
${logic.psql-only} END 
${logic.psql-only} $$ LANGUAGE plpgsql;
