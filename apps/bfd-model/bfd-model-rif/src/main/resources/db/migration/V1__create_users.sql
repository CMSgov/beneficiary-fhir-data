select create_role_if_not_exists('bfduser');

select create_role_if_not_exists('svc_fhirdb_migrator');
select create_role_if_not_exists('svc_bfd_pipeline_0');
select create_role_if_not_exists('svc_bfd_pipeline_1');
select create_role_if_not_exists('svc_bfd_server_0');
select create_role_if_not_exists('svc_bfd_server_1');
select create_role_if_not_exists('api_migrator_svcs');

select create_role_if_not_exists('api_pipeline_svcs');
select create_role_if_not_exists('api_reader_svcs');

select create_role_if_not_exists('bfd_analyst_group');
select create_role_if_not_exists('bfd_data_admin_group');
select create_role_if_not_exists('bfd_migrator_role');
select create_role_if_not_exists('bfd_reader_role');
select create_role_if_not_exists('bfd_schema_admin_group');
select create_role_if_not_exists('bfd_writer_role');

select create_role_if_not_exists('paca_analyst_group');
select create_role_if_not_exists('paca_data_admin_group');
select create_role_if_not_exists('paca_migrator_role');
select create_role_if_not_exists('paca_reader_role');
select create_role_if_not_exists('paca_schema_admin_group');
select create_role_if_not_exists('paca_writer_role');
select create_role_if_not_exists('rdsadmin');

select set_fhirdb_owner('svc_fhirdb_migrator');