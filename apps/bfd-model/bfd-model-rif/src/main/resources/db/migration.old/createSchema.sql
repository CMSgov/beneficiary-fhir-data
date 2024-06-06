/*
  This callback (https://flywaydb.org/documentation/concepts/callbacks) is executed once, on new db installations, before Flyway
  starts creating schemas.

  It adds a set of database admin functions to help us manage role-based access and ownership, while keeping versioned migrations DRY.
  
  Functions (all functions are idempotent):
    1. revoke_schema_privs(schema_name TEXT, role_name TEXT) - Revokes schema privileges from a role (tables, sequences, routines).
    2. create_role_if_not_exists(role_name TEXT) - Adds a new role, skipping if the role already exists.
    3. revoke_db_privs(db_name TEXT, role_name TEXT) - Revokes db privs (includes running revoke_schema_privs on all schemas in a db).
    4. add_reader_role_to_schema(role_name TEXT, schema_name TEXT) - Adds a read only role applied to the schema
    5. add_writer_role_to_schema(role_name TEXT, schema_name TEXT) - Read+Write
    6. add_migrator_role_to_schema(role_name TEXT, schema_name TEXT) - Read+Write+DDL
    7. add_db_group_if_not_exists(db_name TEXT, group_name TEXT) - Adds a new group, skipping if the group already exists.
    8. set_fhirdb_owner(role_name TEXT) - Makes the designated role own all existing and future tables, views, sequences, and routines in all schemas.

*/

${logic.psql-only} --
${logic.psql-only} -- revoke_schema_privs
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION revoke_schema_privs(schema_name TEXT, role_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
${logic.psql-only}     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON TABLES FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON SEQUENCES FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON ROUTINES FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}     EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}     BEGIN
${logic.psql-only}       -- we will get an error in aws if we try to modify perms on routines installed via system extensions
${logic.psql-only}       -- (pg_stat_statements, etc).. just log and ignore the error
${logic.psql-only}       EXECUTE format('REVOKE ALL ON ALL ROUTINES IN SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}       EXCEPTION WHEN SQLSTATE '42501' THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
${logic.psql-only}     END;
${logic.psql-only}     EXECUTE format('REVOKE ALL ON SCHEMA %I FROM %I;', schema_name, role_name);
${logic.psql-only}   ELSE
${logic.psql-only}     RAISE NOTICE 'role "%" not found, skipping', role_name;
${logic.psql-only}   END IF;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only} 
${logic.psql-only} --
${logic.psql-only} -- create_role_if_not_exists
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION create_role_if_not_exists(role_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   EXECUTE format('CREATE ROLE %I;', role_name);
${logic.psql-only}   EXCEPTION WHEN SQLSTATE '42710' THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only} 
${logic.psql-only} --
${logic.psql-only} -- revoke_db_privs
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION revoke_db_privs(db_name TEXT, role_name TEXT) RETURNS void AS $func$
${logic.psql-only} DECLARE
${logic.psql-only}   t record;
${logic.psql-only} BEGIN
${logic.psql-only}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
${logic.psql-only}     FOR t IN
${logic.psql-only}       SELECT nspname FROM pg_catalog.pg_namespace
${logic.psql-only}       WHERE nspname NOT LIKE 'pg_%'
${logic.psql-only}       AND nspname != 'information_schema'
${logic.psql-only}       AND nspname != 'public'
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
${logic.psql-only} CREATE OR REPLACE FUNCTION add_reader_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
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
${logic.psql-only} -- add_writer_role_to_schema
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION add_writer_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
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
${logic.psql-only} CREATE OR REPLACE FUNCTION add_migrator_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(role_name);
${logic.psql-only}   PERFORM revoke_schema_privs(role_name, schema_name);
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
${logic.psql-only} 
${logic.psql-only} --
${logic.psql-only} -- add_db_group_if_not_exists
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION add_db_group_if_not_exists(db_name TEXT, group_name TEXT) RETURNS void AS $func$
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM create_role_if_not_exists(group_name);
${logic.psql-only}   EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I;', db_name, group_name);
${logic.psql-only}   -- ignore dup errors
${logic.psql-only}   EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
${logic.psql-only} 
${logic.psql-only} --
${logic.psql-only} -- set_fhirdb_owner
${logic.psql-only} --
${logic.psql-only} CREATE OR REPLACE FUNCTION set_fhirdb_owner(role_name TEXT) RETURNS void AS $func$
${logic.psql-only} DECLARE
${logic.psql-only}  t record;
${logic.psql-only} BEGIN
${logic.psql-only}   -- make the fhirdb owner role own all schemas except public/system schemas
${logic.psql-only}   FOR t IN
${logic.psql-only}     SELECT nspname, nspowner FROM pg_catalog.pg_namespace 
${logic.psql-only}     WHERE nspname != 'public' AND nspname != 'information_schema' AND nspname NOT LIKE 'pg_%'
${logic.psql-only}   LOOP
${logic.psql-only}     EXECUTE format('GRANT %I TO %I;', role_name, CURRENT_USER);
${logic.psql-only}     EXECUTE format('ALTER SCHEMA %I OWNER TO %I', t.nspname, role_name);
${logic.psql-only}   END LOOP;
${logic.psql-only} 
${logic.psql-only}   -- make role_name own all tables/views in all non system schemas/tables
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
${logic.psql-only}       AND sequence_schema != 'information_schema'
${logic.psql-only}       AND sequence_schema NOT LIKE 'pg_%'
${logic.psql-only}   LOOP
${logic.psql-only}     EXECUTE format('ALTER SEQUENCE %I.%I OWNER TO %I;', t.sequence_schema, t.sequence_name, role_name);
${logic.psql-only}   END LOOP;
${logic.psql-only} 
${logic.psql-only}   -- functions and procedures (routines)
${logic.psql-only}   FOR t IN
${logic.psql-only}     SELECT routine_schema, routine_name
${logic.psql-only}     FROM information_schema.routines
${logic.psql-only}     WHERE routine_schema != 'pg_catalog' AND routine_schema != 'information_schema'
${logic.psql-only}   LOOP
${logic.psql-only}     BEGIN
${logic.psql-only}       -- in aws, we will get an error if we try to modify perms on routines installed via system extensions
${logic.psql-only}       -- (pg_stat_statements, etc).. just log and ignore the error
${logic.psql-only}       EXECUTE format('ALTER ROUTINE %I.%I OWNER TO %I;', t.routine_schema, t.routine_name, role_name);
${logic.psql-only}       EXCEPTION WHEN SQLSTATE '42501' THEN RAISE NOTICE 'set_fhir_db_owner: %, skipping', SQLERRM USING ERRCODE = SQLSTATE;
${logic.psql-only}     END;
${logic.psql-only}   END LOOP;
${logic.psql-only} END $func$ LANGUAGE plpgsql;
