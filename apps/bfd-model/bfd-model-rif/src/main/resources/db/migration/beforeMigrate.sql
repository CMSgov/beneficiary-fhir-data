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

 --
 -- revoke_schema_privs
 --
 CREATE OR REPLACE FUNCTION revoke_schema_privs(schema_name TEXT, role_name TEXT) RETURNS void AS $func$
 BEGIN
   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON TABLES FROM %I;', schema_name, role_name);
     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON SEQUENCES FROM %I;', schema_name, role_name);
     EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I REVOKE ALL ON ROUTINES FROM %I;', schema_name, role_name);
     EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA %I FROM %I;', schema_name, role_name);
     EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA %I FROM %I;', schema_name, role_name);
     BEGIN
       -- we will get an error in aws if we try to modify perms on routines installed via system extensions
       -- (pg_stat_statements, etc).. just log and ignore the error
       EXECUTE format('REVOKE ALL ON ALL ROUTINES IN SCHEMA %I FROM %I;', schema_name, role_name);
       EXCEPTION WHEN SQLSTATE '42501' THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
     END;
     EXECUTE format('REVOKE ALL ON SCHEMA %I FROM %I;', schema_name, role_name);
   ELSE
     RAISE NOTICE 'role "%" not found, skipping', role_name;
   END IF;
 END $func$ LANGUAGE plpgsql;
 
 --
 -- create_role_if_not_exists
 --
 CREATE OR REPLACE FUNCTION create_role_if_not_exists(role_name TEXT) RETURNS void AS $func$
 BEGIN
   EXECUTE format('CREATE ROLE %I;', role_name);
   EXCEPTION WHEN SQLSTATE '42710' THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
 END $func$ LANGUAGE plpgsql;
 
 --
 -- revoke_db_privs
 --
 CREATE OR REPLACE FUNCTION revoke_db_privs(db_name TEXT, role_name TEXT) RETURNS void AS $func$
 DECLARE
   t record;
 BEGIN
   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = role_name) THEN
     FOR t IN
       SELECT nspname FROM pg_catalog.pg_namespace
       WHERE nspname NOT LIKE 'pg_%'
       AND nspname != 'information_schema'
       AND nspname != 'public'
     LOOP
       PERFORM revoke_schema_privs(t.nspname, role_name);
     END LOOP;
     EXECUTE format('REVOKE ALL ON DATABASE %I FROM %I;', db_name, role_name);
   END IF;
 END $func$ LANGUAGE plpgsql;
 
 --
 -- add_reader_role_to_schema
 --
 CREATE OR REPLACE FUNCTION add_reader_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
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
 -- add_writer_role_to_schema
 --
 CREATE OR REPLACE FUNCTION add_writer_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
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
 CREATE OR REPLACE FUNCTION add_migrator_role_to_schema(role_name TEXT, schema_name TEXT) RETURNS void AS $func$
 BEGIN
   PERFORM create_role_if_not_exists(role_name);
   PERFORM revoke_schema_privs(role_name, schema_name);
   EXECUTE format('GRANT ALL ON SCHEMA %I TO %I;', schema_name, role_name); -- uwsage + create
   EXECUTE format('GRANT ALL ON ALL TABLES IN SCHEMA %I TO %I;', schema_name, role_name); -- tables/views
   EXECUTE format('GRANT ALL ON ALL SEQUENCES IN SCHEMA %I TO %I;', schema_name, role_name); --curval, nextval, setval
   BEGIN
     -- in aurora, we will get an error if we try to modify perms on routines installed via system extensions
     -- like pg_stat_statements, so just log the error and skip it
     EXECUTE format('GRANT ALL ON ALL ROUTINES IN SCHEMA %I TO %I;', schema_name, role_name);
     EXCEPTION WHEN SQLSTATE '42501' THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
   END;
   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO %I;', schema_name, role_name);
   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON SEQUENCES TO %I;', schema_name, role_name);
   EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON ROUTINES TO %I;', schema_name, role_name);
 END $func$ LANGUAGE plpgsql;
 
 --
 -- add_db_group_if_not_exists
 --
 CREATE OR REPLACE FUNCTION add_db_group_if_not_exists(db_name TEXT, group_name TEXT) RETURNS void AS $func$
 BEGIN
   PERFORM create_role_if_not_exists(group_name);
   EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I;', db_name, group_name);
   -- ignore dup errors
   EXCEPTION WHEN duplicate_object THEN RAISE NOTICE '%, skipping', SQLERRM USING ERRCODE = SQLSTATE;
 END $func$ LANGUAGE plpgsql;
 
 --
 -- set_fhirdb_owner
 --
 CREATE OR REPLACE FUNCTION set_fhirdb_owner(role_name TEXT) RETURNS void AS $func$
 DECLARE
  t record;
 BEGIN
   -- make the fhirdb owner role own all schemas except public/system schemas
   FOR t IN
     SELECT nspname, nspowner FROM pg_catalog.pg_namespace 
     WHERE nspname != 'public' AND nspname != 'information_schema' AND nspname NOT LIKE 'pg_%'
   LOOP
     EXECUTE format('GRANT %I TO %I;', role_name, CURRENT_USER);
     EXECUTE format('ALTER SCHEMA %I OWNER TO %I', t.nspname, role_name);
   END LOOP;
 
   -- make role_name own all tables/views in all non system schemas/tables
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
 
   -- functions and procedures (routines)
   FOR t IN
     SELECT routine_schema, routine_name
     FROM information_schema.routines
     WHERE routine_schema != 'pg_catalog' AND routine_schema != 'information_schema'
   LOOP
     BEGIN
       -- in aws, we will get an error if we try to modify perms on routines installed via system extensions
       -- (pg_stat_statements, etc).. just log and ignore the error
       EXECUTE format('ALTER ROUTINE %I.%I OWNER TO %I;', t.routine_schema, t.routine_name, role_name);
       EXCEPTION WHEN SQLSTATE '42501' THEN RAISE NOTICE 'set_fhir_db_owner: %, skipping', SQLERRM USING ERRCODE = SQLSTATE;
     END;
   END LOOP;
 END $func$ LANGUAGE plpgsql;



