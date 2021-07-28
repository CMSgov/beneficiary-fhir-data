-- Creates the RDA FISS pre-adjudicated claims schema required by the application.
-- https://confluenceent.cms.gov/pages/viewpage.action?spaceKey=MPSM&title=MVP+0.1+Data+Dictionary+for+Replicated+Data+Access+API
CREATE SCHEMA IF NOT EXISTS "pre_adj";

DO
$do$
BEGIN
  -- FHIR API
  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE  rolname = 'read_only_bb') THEN
    GRANT USAGE ON SCHEMA pre_adj TO read_only_bb;
    ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT SELECT ON TABLES TO read_only_bb;
  END IF;
  -- ETL PIPELINE
  IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE  rolname = 'svc_bfd_pipeline_role') THEN
    GRANT USAGE ON SCHEMA pre_adj TO svc_bfd_pipeline_role;
    ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL ON TABLES TO svc_bfd_pipeline_role;
    ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL ON SEQUENCES TO svc_bfd_pipeline_role;
  END IF;
END
$do$;
