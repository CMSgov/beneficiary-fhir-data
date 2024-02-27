/*
V1__Users_and_Roles.sql

Flyway script to initialize db USER(s) and ROLE(s).

To be clear, in Postgres a USER and a ROLE are often used inter-changeably.
The difference is simply, a USER will have a password which allows them to
log into the db; a ROLE has privileges that allow them to do something within
the database; a ROLE without a password, precludes logging into a db.
A USER can be associated with a ROLE by granting them all privileges specific to
that ROLE.

Both a ROLE and a USER are associated with a database server, which itself
can contain multiple databases; each database may contain multiple schemas.

For BFD we have 6 users who are not part of AWS RDS, and have the ability to
log into the FHIRDB; these users are:

Not Read Only (RO)                Read Only
------------------                -----------------
bfduser                           svc_bfd_server_0
svc_bfd_pipeline_0                svc_bfd_server_1
svc_bfd_pipeline_1
svc_fhirdb_migrator

Over time, our PROD, PROD-SBX- and TEST databases have additional users defined
who can log into the db; many of these users are transient and may have been
defined out-of-band (i.e., not done via flyway).

For the purposes of supporting Postgres in our CI/CD end-to-end testing, we will
only ensure that the above 6 users are defined.
*/
DO $$ BEGIN
PERFORM public.create_role_if_not_exists('bfduser');

-- Common BFD, RDA USERs (non AWS RDS)
PERFORM public.create_role_if_not_exists('svc_bfd_pipeline_0');
PERFORM public.create_role_if_not_exists('svc_bfd_pipeline_1');
PERFORM public.create_role_if_not_exists('svc_bfd_server_0');
PERFORM public.create_role_if_not_exists('svc_bfd_server_1');
PERFORM public.create_role_if_not_exists('svc_fhirdb_migrator');

-- ROLEs used within BFD and RDA for db privs
PERFORM public.create_role_if_not_exists('api_migrator_svcs');
PERFORM public.create_role_if_not_exists('api_pipeline_svcs');
PERFORM public.create_role_if_not_exists('api_reader_svcs');

PERFORM public.create_role_if_not_exists('bfd_analyst_group');
PERFORM public.create_role_if_not_exists('bfd_data_admin_group');
PERFORM public.create_role_if_not_exists('bfd_migrator_role');
PERFORM public.create_role_if_not_exists('bfd_reader_role');
PERFORM public.create_role_if_not_exists('bfd_writer_role');
PERFORM public.create_role_if_not_exists('bfd_schema_admin_group');

PERFORM public.create_role_if_not_exists('paca_analyst_group');
PERFORM public.create_role_if_not_exists('paca_data_admin_group');
PERFORM public.create_role_if_not_exists('paca_migrator_role');
PERFORM public.create_role_if_not_exists('paca_reader_role');
PERFORM public.create_role_if_not_exists('paca_writer_role');
PERFORM public.create_role_if_not_exists('paca_schema_admin_group');
END $$;

-- schema creation and ownership
--
CREATE SCHEMA IF NOT EXISTS ccw AUTHORIZATION svc_fhirdb_migrator;
CREATE SCHEMA IF NOT EXISTS rda AUTHORIZATION svc_fhirdb_migrator;

-- USAGE grants
--
GRANT ALL ON SCHEMA public TO PUBLIC;
GRANT ALL ON SCHEMA public TO bfduser;
GRANT ALL ON SCHEMA ccw    TO bfduser;
GRANT ALL ON SCHEMA rda    TO bfduser;
GRANT ALL ON SCHEMA ccw    TO bfd_migrator_role;
GRANT ALL ON SCHEMA rda    TO paca_migrator_role;

-- migrator has access to .public functions
--
GRANT EXECUTE ON FUNCTION public.add_db_group_if_not_exists(db_name text, group_name text)      TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.add_migrator_role_to_schema(role_name text, schema_name text)  TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.add_reader_role_to_schema(role_name text, schema_name text)    TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.add_writer_role_to_schema(role_name text, schema_name text)    TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.create_role_if_not_exists(role_name text)                      TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.revoke_schema_privs(schema_name text, role_name text)          TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.revoke_db_privs(db_name text, role_name text)                  TO bfd_migrator_role;
GRANT EXECUTE ON FUNCTION public.set_fhirdb_owner(role_name TEXT)                               TO bfd_migrator_role;

/*
-- SSL extension not installed by default; therefore not needed for BFD CI/CD.
-- Left here as code example for interested parties.
--
GRANT ALL ON FUNCTION public.ssl_extension_info(OUT name text, OUT value text, OUT critical boolean) TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_cipher()                                                   TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_client_cert_present()                                      TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_client_dn()                                                TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_client_dn_field(text)                                      TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_client_serial()                                            TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_is_used()                                                  TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_issuer_dn()                                                TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_issuer_field(text)                                         TO bfd_migrator_role;
GRANT ALL ON FUNCTION public.ssl_version() 
*/


