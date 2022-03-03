/*
    This migration is the first step in a larger initiative that aims to harden our database permissions and to implement
    least-priviledged access across all schemas.

    This migration:
        1. Adds a new set of explicitly defined reader, writer, and migrator roles for the pre_adj schema.
        2. Adds new paca groups to help us manage individual user access:
            - paca_analyst_group (read only)
            - paca_data_admins_group (read+write but no alter/drop)
            - paca_db_admins_group (read+write+alter/drop)
        3. Sets default permissions so the above gets applied to newly created objects.
*/

${logic.perms} DO $$
${logic.perms}  -- new paca roles on pre_adj schema: paca_reader_role, paca_writer_role, paca_migrator_role (read+write+alter)
${logic.perms}  
${logic.perms}  -- reader role
${logic.perms}  DROP ROLE IF EXISTS paca_reader_role;
${logic.perms}  CREATE ROLE paca_reader_role;
${logic.perms}  GRANT USAGE ON SCHEMA pre_adj TO paca_reader_role;
${logic.perms}  GRANT SELECT ON ALL TABLES IN SCHEMA pre_adj TO paca_reader_role;
${logic.perms}  GRANT SELECT ON ALL SEQUENCES IN SCHEMA pre_adj TO paca_reader_role; --curval
${logic.perms}  ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT SELECT ON TABLES TO paca_reader_role;
${logic.perms}  ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT SELECT ON SEQUENCES TO paca_reader_role;
${logic.perms}  
${logic.perms}  -- writer role
${logic.perms}  DROP ROLE IF EXISTS paca_writer_role;
${logic.perms}  CREATE ROLE paca_writer_role;
${logic.perms}  GRANT USAGE ON SCHEMA pre_adj TO paca_writer_role;
${logic.perms}  GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA pre_adj TO paca_writer_role;
${logic.perms}  GRANT USAGE ON ALL SEQUENCES IN SCHEMA pre_adj TO paca_writer_role; -- curval, nextval
${logic.perms}  ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO paca_writer_role;
${logic.perms}  ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT USAGE ON SEQUENCES TO paca_writer_role;
${logic.perms}  
${logic.perms}  -- migrator role
${logic.perms}  DROP ROLE IF EXISTS paca_migrator_role;
${logic.perms}  CREATE ROLE paca_migrator_role;
${logic.perms}  GRANT ALL PRIVILEGES ON SCHEMA pre_adj TO paca_migrator_role;
${logic.perms}  GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA pre_adj TO paca_migrator_role;
${logic.perms}  GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA pre_adj TO paca_migrator_role; -- curval, nextval, setval
${logic.perms}  ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL PRIVILEGES ON TABLES TO paca_migrator_role;
${logic.perms}  ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL PRIVILEGES ON SEQUENCES TO paca_migrator_role;
${logic.perms}  
${logic.perms}
${logic.perms}  -- new paca groups: paca_analyst_group, paca_data_admins_group, paca_db_admins_group (read+write+alter)
${logic.perms}  
${logic.perms}  -- analyst group
${logic.perms}  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'paca_analyst_group') THEN
${logic.perms}      CREATE GROUP paca_analyst_group WITH
${logic.perms}          NOSUPERUSER
${logic.perms}          NOCREATEDB
${logic.perms}          NOCREATEROLE
${logic.perms}          NOINHERIT
${logic.perms}          NOLOGIN
${logic.perms}          NOREPLICATION
${logic.perms}          NOBYPASSRLS;
${logic.perms}  END IF;
${logic.perms}  GRANT CONNECT ON DATABASE fhirdb TO paca_analyst_group;
${logic.perms}  GRANT paca_reader_role TO paca_analyst_group;
${logic.perms}  
${logic.perms}  -- data admins group
${logic.perms}  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'paca_data_admins_group') THEN
${logic.perms}      CREATE GROUP paca_data_admins_group WITH
${logic.perms}          NOSUPERUSER
${logic.perms}          NOCREATEDB
${logic.perms}          NOCREATEROLE
${logic.perms}          NOINHERIT
${logic.perms}          NOLOGIN
${logic.perms}          NOREPLICATION
${logic.perms}          NOBYPASSRLS;
${logic.perms}  END IF;
${logic.perms}  GRANT CONNECT ON DATABASE fhirdb TO paca_data_admins_group;
${logic.perms}  GRANT paca_writer_role TO paca_data_admins_group;
${logic.perms}  
${logic.perms}  -- db admins group
${logic.perms}  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'paca_db_admins_group') THEN
${logic.perms}      CREATE GROUP paca_db_admins_group WITH
${logic.perms}          NOSUPERUSER
${logic.perms}          NOCREATEDB
${logic.perms}          NOCREATEROLE
${logic.perms}          NOINHERIT
${logic.perms}          NOLOGIN
${logic.perms}          NOREPLICATION
${logic.perms}          NOBYPASSRLS;
${logic.perms}  END IF;
${logic.perms}  GRANT CONNECT ON DATABASE fhirdb TO paca_db_admins_group;
${logic.perms}  GRANT paca_migrator_role TO paca_db_admins_group;
${logic.perms}  
${logic.perms}
${logic.perms}  -- Ensure current api/pipeline roles have the necessary permissions for zero
${logic.perms}  -- downtime deployments. This is temporary until we finish refactoring our
${logic.perms}  -- database permissions. Once completed, a future migration will drop the various 
${logic.perms}  -- read_only_bb, svc_bfd_* roles making the rest of this migration a noop.
${logic.perms}
${logic.perms}  -- grant reader to existing reader roles
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'read_only_bb') THEN
${logic.perms}      GRANT paca_reader_role TO read_only_bb;
${logic.perms}  END IF;
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_server_0') THEN
${logic.perms}      GRANT paca_reader_role TO svc_bfd_server_0;
${logic.perms}  END IF;
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_server_1') THEN
${logic.perms}      GRANT paca_reader_role TO svc_bfd_server_1;
${logic.perms}  END IF;
${logic.perms}  
${logic.perms}  -- grant writer to existing pipeline roles
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_role') THEN
${logic.perms}      GRANT paca_writer_role TO svc_bfd_pipeline_role;
${logic.perms}  END IF;
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_0') THEN
${logic.perms}      GRANT paca_writer_role TO svc_bfd_pipeline_0;
${logic.perms}  END IF;
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_1') THEN
${logic.perms}      GRANT paca_writer_role TO svc_bfd_pipeline_1;
${logic.perms}  END IF;
${logic.perms}  
${logic.perms}  -- and grant migrator to existing pipeline roles
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_role') THEN
${logic.perms}      GRANT paca_migrator_role TO svc_bfd_pipeline_role;
${logic.perms}  END IF;
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_0') THEN
${logic.perms}      GRANT paca_migrator_role TO svc_bfd_pipeline_0;
${logic.perms}  END IF;
${logic.perms}  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_1') THEN
${logic.perms}      GRANT paca_migrator_role TO svc_bfd_pipeline_1;
${logic.perms}  END IF;
${logic.perms}
${logic.perms} END $$;
