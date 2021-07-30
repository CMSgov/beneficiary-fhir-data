-- grants default perms on the pre_adj schema (if run on postgres)
${logic.perms} IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'read_only_bb') THEN
${logic.perms}   GRANT USAGE ON SCHEMA pre_adj TO read_only_bb;
${logic.perms}   ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT SELECT ON TABLES TO read_only_bb;
${logic.perms} END IF;
${logic.perms} IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_role') THEN
${logic.perms}   GRANT USAGE ON SCHEMA pre_adj TO svc_bfd_pipeline_role;
${logic.perms}   ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL ON TABLES TO svc_bfd_pipeline_role;
${logic.perms}   ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL ON SEQUENCES TO svc_bfd_pipeline_role;
${logic.perms} END IF;
