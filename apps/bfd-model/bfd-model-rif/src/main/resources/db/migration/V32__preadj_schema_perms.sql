/*
GRANTS DEFAULT PERMS ON THE PRE_ADJ SCHEMA (IF RUN ON POSTGRES)
NOTES:
- DO runs an anonymous block using the default language (PL/pgSQL).
- $$ is to make the block readable. See https://www.postgresqltutorial.com/plpgsql-block-structure/
- PL/pgSQL's BEGIN/END are only for grouping, they do not start or end a transaction.
- The logic.perms placeholder will be replaced by '--' (commented out) if hsql, removed if postgres.
- So these perms will only be applied if run on postgres. See DatabaseSchemaManager.java for placeholders.
*/

${logic.perms} DO $$
${logic.perms} BEGIN
${logic.perms}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'read_only_bb') THEN
${logic.perms}     GRANT USAGE ON SCHEMA pre_adj TO read_only_bb;
${logic.perms}     ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT SELECT ON TABLES TO read_only_bb;
${logic.perms}     GRANT SELECT ON TABLE pre_adj."FissClaims" TO read_only_bb;
${logic.perms}     GRANT SELECT ON TABLE pre_adj."FissDiagnosisCodes" TO read_only_bb;
${logic.perms}     GRANT SELECT ON TABLE pre_adj."FissProcCodes" TO read_only_bb;
${logic.perms}     GRANT SELECT ON TABLE pre_adj."McsClaims" TO read_only_bb;
${logic.perms}     GRANT SELECT ON TABLE pre_adj."McsDetails" TO read_only_bb;
${logic.perms}     GRANT SELECT ON TABLE pre_adj."McsDiagnosisCodes" TO read_only_bb;
${logic.perms}   END IF;
${logic.perms}   IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'svc_bfd_pipeline_role') THEN
${logic.perms}     GRANT USAGE ON SCHEMA pre_adj TO svc_bfd_pipeline_role;
${logic.perms}     ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL ON TABLES TO svc_bfd_pipeline_role;
${logic.perms}     ALTER DEFAULT PRIVILEGES IN SCHEMA pre_adj GRANT ALL ON SEQUENCES TO svc_bfd_pipeline_role;
${logic.perms}     GRANT ALL ON TABLE pre_adj."FissClaims" TO svc_bfd_pipeline_role;
${logic.perms}     GRANT ALL ON TABLE pre_adj."FissDiagnosisCodes" TO svc_bfd_pipeline_role;
${logic.perms}     GRANT ALL ON TABLE pre_adj."FissProcCodes" TO svc_bfd_pipeline_role;
${logic.perms}     GRANT ALL ON TABLE pre_adj."McsClaims" TO svc_bfd_pipeline_role;
${logic.perms}     GRANT ALL ON TABLE pre_adj."McsDetails" TO svc_bfd_pipeline_role;
${logic.perms}     GRANT ALL ON TABLE pre_adj."McsDiagnosisCodes" TO svc_bfd_pipeline_role;
${logic.perms}   END IF;
${logic.perms} END $$;
