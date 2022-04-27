/*
  This callback runs one time, just after the database has been created, but before flyway starts creating schemas.
  
  It adds a set of functions to help manage role based access controls and designates a fhirdb owner role.

*/
${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only} --
${logic.psql-only} -- revoke_schema_privs
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION revoke_schema_privs(schema_name TEXT, role_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} BEGIN
${logic.psql-only}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
${logic.psql-only}     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON TABLES FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON SEQUENCES FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON FUNCTIONS FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('REVOKE ALL ON ALL FUNCTIONS IN SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('REVOKE ALL ON SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}   END IF;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- create_role_if_not_exists does what's on the tin
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION create_role_if_not_exists(role_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} BEGIN
${logic.psql-only}   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
${logic.psql-only}     EXECUTE format('CREATE ROLE %I;', role_name);
${logic.psql-only}   END IF;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- revoke_db_privs runs revoke_schema_privs for each (non system) schema
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION revoke_db_privs(db_name TEXT, role_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} DECLARE
${logic.psql-only}   t record;
${logic.psql-only} BEGIN
${logic.psql-only}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
${logic.psql-only}     FOR t IN
${logic.psql-only}       SELECT nspname FROM pg_catalog.pg_namespace WHERE nspname NOT LIKE 'pg_%' AND nspname != 'information_schema'
${logic.psql-only}     LOOP
${logic.psql-only}       PERFORM revoke_schema_privs(t.nspname, role_name);
${logic.psql-only}     END LOOP;
${logic.psql-only}     EXECUTE format('REVOKE ALL ON DATABASE %I FROM %I;', db_name, role_name);
${logic.psql-only}   END IF;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- add_reader_role_to_schema
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION add_reader_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(role_name, schema_name);
${logic.psql-only}   EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON TABLES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON SEQUENCES TO %I;', schema_name, role_name);
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- add_writer_role_to_schema TODO: need write or references?
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION add_writer_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(role_name, schema_name);
${logic.psql-only}   EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT USAGE ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT USAGE ON SEQUENCES TO %I;', schema_name, role_name);
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- add_migrator_role_to_schema
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION add_migrator_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(role_name, schema_name);
${logic.psql-only}   EXECUTE format('GRANT ALL ON SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT ALL ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('GRANT ALL ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval, setval
${logic.psql-only}   EXECUTE format('GRANT ALL ON ALL ROUTINES IN SCHEMA %I TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON SEQUENCES TO %I;', schema_name, role_name);
${logic.psql-only}   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON ROUTINES TO %I;', schema_name, role_name);
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- add_db_group_if_not_exists
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION add_db_group_if_not_exists(db_name TEXT, group_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(group_name);
${logic.psql-only}   EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I;', db_name, group_name);
${logic.psql-only}   -- ignore the error if the group already exists
${logic.psql-only}   EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only} --
${logic.psql-only} -- set_fhirdb_owner
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION set_fhirdb_owner(role_name TEXT) RETURNS void AS
${logic.psql-only} $func$
${logic.psql-only} DECLARE
${logic.psql-only}  t record;
${logic.psql-only} BEGIN
${logic.psql-only}   EXECUTE format('GRANT CONNECT ON DATABASE fhirdb TO %I', role_name); -- TODO: does this role need this?
${logic.psql-only}   IF role_name != CURRENT_USER THEN
${logic.psql-only}     -- you must be a member of a role to alter its ownership
${logic.psql-only}     EXECUTE format('GRANT %I TO %I;', role_name, CURRENT_USER);
${logic.psql-only}   END IF;
${logic.psql-only}
${logic.psql-only}   -- make role_name own all non system schemas
${logic.psql-only}   FOR t IN
${logic.psql-only}     SELECT nspname FROM pg_catalog.pg_namespace WHERE nspname NOT LIKE 'pg_%' AND nspname != 'information_schema'
${logic.psql-only}   LOOP
${logic.psql-only}     EXECUTE format('ALTER SCHEMA %I OWNER TO %I', t.nspname, role_name);
${logic.psql-only}   END LOOP;
${logic.psql-only}
${logic.psql-only}   -- make role_name own all tables/views in all schemas (excludes system schemas/tables)
${logic.psql-only}   FOR t IN
${logic.psql-only}     SELECT table_schema, table_name
${logic.psql-only}     FROM information_schema.tables
${logic.psql-only}     WHERE table_type = 'BASE TABLE'
${logic.psql-only}         AND table_schema NOT LIKE 'pg_%'
${logic.psql-only}         AND table_schema != 'information_schema'
${logic.psql-only}         AND table_name NOT LIKE 'pg_%'
${logic.psql-only}   LOOP
${logic.psql-only}     EXECUTE format('ALTER TABLE %I.%I OWNER TO %I;', t.table_schema, t.table_name, role_name);
${logic.psql-only}   END LOOP;
${logic.psql-only}
${logic.psql-only}   -- sequences
${logic.psql-only}   FOR t IN
${logic.psql-only}     SELECT sequence_schema, sequence_name
${logic.psql-only}     FROM information_schema.sequences
${logic.psql-only}     WHERE sequence_name NOT LIKE 'pg_%'
${logic.psql-only}         AND sequence_schema != 'information_schema'
${logic.psql-only}         AND sequence_schema NOT LIKE 'pg_%'
${logic.psql-only}   LOOP
${logic.psql-only}     EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO %I;', t.sequence_schema, t.sequence_name, role_name);
${logic.psql-only}   END LOOP;
${logic.psql-only}
${logic.psql-only}   -- functions and stored procedures
${logic.psql-only}   FOR t IN
${logic.psql-only}     SELECT routine_schema, routine_name
${logic.psql-only}     FROM information_schema.routines
${logic.psql-only}     WHERE routine_schema NOT LIKE 'pg_%'
${logic.psql-only}         AND routine_schema != 'information_schema'
${logic.psql-only}         AND routine_name NOT LIKE 'pg_%'
${logic.psql-only}   LOOP
${logic.psql-only}     EXECUTE format('ALTER ROUTINE %I.%I OWNER TO %I;', t.routine_schema, t.routine_name, role_name);
${logic.psql-only}   END LOOP;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only}
${logic.psql-only}
${logic.psql-only} -- set an initial fhirdb owner
${logic.psql-only} PERFORM create_role_if_not_exists('fhir');
${logic.psql-only} PERFORM set_fhirdb_owner('fhir');
${logic.psql-only} END $$ LANGUAGE plpgsql;
