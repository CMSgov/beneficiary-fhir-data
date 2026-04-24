DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rds_iam') THEN
      GRANT SELECT ON ALL TABLES IN SCHEMA idr TO rds_iam;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'svc_bfd_server_1') THEN
      GRANT SELECT ON ALL TABLES IN SCHEMA idr TO svc_bfd_server_1;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'svc_bfd_pipeline_1') THEN
      GRANT SELECT ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
      GRANT INSERT ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
      GRANT UPDATE ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
      GRANT DELETE ON ALL TABLES IN SCHEMA idr TO svc_bfd_pipeline_1;
  END IF;
END
$$;
