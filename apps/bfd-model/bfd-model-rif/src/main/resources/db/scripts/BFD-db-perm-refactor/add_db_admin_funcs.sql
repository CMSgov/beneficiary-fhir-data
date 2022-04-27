/*
  This callback runs one time, just after the database has been created, but before flyway starts creating schemas.
  
  It adds a set of functions to help manage role based access controls and designates a fhirdb owner role.

*/
DO $$
BEGIN
  --
  -- revoke_schema_privs
  --
  CREATE OR REPLACE FUNCTION revoke_schema_privs(schema_name TEXT, role_name TEXT) RETURNS void AS
  $func$
  BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
      EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON TABLES FROM %I;', schema_name, role_name);
      EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON SEQUENCES FROM %I;', schema_name, role_name);
      EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON FUNCTIONS FROM %I;', schema_name, role_name);
      EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM %I;', schema_name, role_name);
      EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM %I;', schema_name, role_name);
      EXECUTE format('REVOKE ALL ON ALL FUNCTIONS IN SCHEMA %I FROM %I;', schema_name, role_name);
      EXECUTE format('REVOKE ALL ON SCHEMA %I FROM %I;', schema_name, role_name);
    END IF;
  END $func$ LANGUAGE plpgsql;

  --
  -- create_role_if_not_exists does what's on the tin
  --
  CREATE OR REPLACE FUNCTION create_role_if_not_exists(role_name TEXT) RETURNS void AS
  $func$
  BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
      EXECUTE format('CREATE ROLE %I;', role_name);
    END IF;
  END $func$ LANGUAGE plpgsql;

  --
  -- revoke_db_privs runs revoke_schema_privs for each (non system) schema
  --
  CREATE OR REPLACE FUNCTION revoke_db_privs(db_name TEXT, role_name TEXT) RETURNS void AS
  $func$
  DECLARE
    t record;
  BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
      FOR t IN
        SELECT nspname FROM pg_catalog.pg_namespace WHERE nspname NOT LIKE 'pg_%' AND nspname != 'information_schema'
      LOOP
        PERFORM revoke_schema_privs(t.nspname, role_name);
      END LOOP;
      EXECUTE format('REVOKE ALL ON DATABASE %I FROM %I;', db_name, role_name);
    END IF;
  END $func$ LANGUAGE plpgsql;

  --
  -- add_reader_role_to_schema
  --
  CREATE OR REPLACE FUNCTION add_reader_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS
  $func$
  BEGIN
    PERFORM create_role_if_not_exists(role_name);
    PERFORM revoke_schema_privs(role_name, schema_name);
    EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('GRANT SELECT ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON TABLES TO %I;', schema_name, role_name);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT ON SEQUENCES TO %I;', schema_name, role_name);
  END $func$ LANGUAGE plpgsql;

  --
  -- add_writer_role_to_schema TODO: need write or references?
  --
  CREATE OR REPLACE FUNCTION add_writer_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS
  $func$
  BEGIN
    PERFORM create_role_if_not_exists(role_name);
    PERFORM revoke_schema_privs(role_name, schema_name);
    EXECUTE format('GRANT USAGE ON SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('GRANT USAGE ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO %I;', schema_name, role_name);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT USAGE ON SEQUENCES TO %I;', schema_name, role_name);
  END $func$ LANGUAGE plpgsql;

  --
  -- add_migrator_role_to_schema
  --
  CREATE OR REPLACE FUNCTION add_migrator_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS
  $func$
  BEGIN
    PERFORM create_role_if_not_exists(role_name);
    PERFORM revoke_schema_privs(role_name, schema_name);
    EXECUTE format('GRANT ALL ON SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('GRANT ALL ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('GRANT ALL ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval, setval
    EXECUTE format('GRANT ALL ON ALL ROUTINES IN SCHEMA %I TO %I;', schema_name, role_name);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO %I;', schema_name, role_name);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON SEQUENCES TO %I;', schema_name, role_name);
    EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON ROUTINES TO %I;', schema_name, role_name);
  END $func$ LANGUAGE plpgsql;

  --
  -- add_db_group_if_not_exists
  --
  CREATE OR REPLACE FUNCTION add_db_group_if_not_exists(db_name TEXT, group_name TEXT) RETURNS void AS
  $func$
  BEGIN
    PERFORM create_role_if_not_exists(group_name);
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I;', db_name, group_name);
    -- ignore the error if the group already exists
    EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
  END $func$ LANGUAGE plpgsql;

  --
  -- set_fhirdb_owner
  --
  CREATE OR REPLACE FUNCTION set_fhirdb_owner(role_name TEXT) RETURNS void AS
  $func$
  DECLARE
  t record;
  BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE fhirdb TO %I', role_name); -- TODO: does this role need this?
    IF role_name != CURRENT_USER THEN
      -- you must be a member of a role to alter its ownership
      EXECUTE format('GRANT %I TO %I;', role_name, CURRENT_USER);
    END IF;

    -- make role_name own all non system schemas
    FOR t IN
      SELECT nspname FROM pg_catalog.pg_namespace WHERE nspname NOT LIKE 'pg_%' AND nspname != 'information_schema'
    LOOP
      EXECUTE format('ALTER SCHEMA %I OWNER TO %I', t.nspname, role_name);
    END LOOP;

    -- make role_name own all tables/views in all schemas (excludes system schemas/tables)
    FOR t IN
      SELECT table_schema, table_name
      FROM information_schema.tables
      WHERE table_type = 'BASE TABLE'
          AND table_schema NOT LIKE 'pg_%'
          AND table_schema != 'information_schema'
          AND table_name NOT LIKE 'pg_%'
    LOOP
      EXECUTE format('ALTER TABLE %I.%I OWNER TO %I;', t.table_schema, t.table_name, role_name);
    END LOOP;

    -- sequences
    FOR t IN
      SELECT sequence_schema, sequence_name
      FROM information_schema.sequences
      WHERE sequence_name NOT LIKE 'pg_%'
          AND sequence_schema != 'information_schema'
          AND sequence_schema NOT LIKE 'pg_%'
    LOOP
      EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO %I;', t.sequence_schema, t.sequence_name, role_name);
    END LOOP;

    -- functions and stored procedures
    FOR t IN
      SELECT routine_schema, routine_name
      FROM information_schema.routines
      WHERE routine_schema NOT LIKE 'pg_%'
          AND routine_schema != 'information_schema'
          AND routine_name NOT LIKE 'pg_%'
    LOOP
      EXECUTE format('ALTER ROUTINE %I.%I OWNER TO %I;', t.routine_schema, t.routine_name, role_name);
    END LOOP;
  END $func$ LANGUAGE plpgsql;

  PERFORM create_role_if_not_exists('fhir');
END 
$$ LANGUAGE plpgsql;
