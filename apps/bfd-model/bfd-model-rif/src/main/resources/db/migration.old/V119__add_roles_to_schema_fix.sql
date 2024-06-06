-- there is a bug in add_reader_role_to_schema(), add_migrator_role() and add_writer_role_to_schema() functions
-- where the parameters for the revoke_schema_privs() function call were reversed. This fixes it.

${logic.psql-only} CREATE OR REPLACE FUNCTION add_reader_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON TABLES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON SEQUENCES TO %I;', schema_name, role_name);
${logic.psql-only} END $func$ LANGUAGE plpgsql;

${logic.psql-only} CREATE OR REPLACE FUNCTION add_writer_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT USAGE ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT USAGE ON SEQUENCES TO %I;', schema_name, role_name);
${logic.psql-only} END $func$ LANGUAGE plpgsql;

${logic.psql-only} CREATE OR REPLACE FUNCTION add_migrator_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT ALL ON SCHEMA %I TO %I;', schema_name, role_name); -- uwsage + create
${logic.psql-only}   EXECUTE format('GRANT ALL ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name); -- tables/views
${logic.psql-only}   EXECUTE format('GRANT ALL ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval, setval
${logic.psql-only}   BEGIN
${logic.psql-only}     -- in aurora, we will get an error if we try to modify perms on routines installed via system extensions
${logic.psql-only}     -- like pg_stat_statements, so just log the error and skip it
${logic.psql-only}     EXECUTE format('GRANT ALL ON ALL ROUTINES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}     EXCEPTION WHEN SQLSTATE '42501' THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
${logic.psql-only}   END;
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON SEQUENCES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON ROUTINES TO %I;', schema_name, role_name);
${logic.psql-only} END $func$ LANGUAGE plpgsql;
