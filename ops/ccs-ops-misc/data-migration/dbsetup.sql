-- Create database
CREATE DATABASE fhirdb;

-- Restore procedure will grant owner on restored tables to svc_fhir_etl
CREATE USER svc_fhir_etl WITH PASSWORD '(REPLACE)';
GRANT ALL PRIVILEGES ON DATABASE fhirdb TO svc_fhir_etl;

-- Restore procedure will grant select on restored tables to read_only_bb
CREATE USER svc_fhir_bb WITH PASSWORD '(REPLACE)';
CREATE ROLE read_only_bb;
GRANT CONNECT ON DATABASE fhirdb TO read_only_bb;
GRANT USAGE ON SCHEMA public TO read_only_bb;
GRANT read_only_bb TO svc_fhir_bb;

-- Allow our admin user to restore tables owned by svc_fhir_etl
GRANT svc_fhir_etl TO bfduser;

-- Create tablespaces for compatibility, even though they are all on the same volume within RDS
CREATE TABLESPACE fhirdb_ts OWNER bfduser LOCATION '/u01/tbs/pg_tblspc';
CREATE TABLESPACE fhirdb_ts2 OWNER bfduser LOCATION '/u01/pg_tblspc2';
CREATE TABLESPACE beneficiaries_ts OWNER bfduser LOCATION '/u01/pg_Beneficiaries_ts';
CREATE TABLESPACE carrierclaimlines_ts OWNER bfduser LOCATION '/u01/pg_CarrierClaimLines_ts';
CREATE TABLESPACE carrierclaims_ts OWNER bfduser LOCATION '/u01/pg_CarrierClaims_ts';
CREATE TABLESPACE dmeclaims_ts OWNER bfduser LOCATION '/u01/pg_DMEClaims_ts';
CREATE TABLESPACE hhaclaimlines_ts OWNER bfduser LOCATION '/u01/pg_HHAClaimLines_ts';
CREATE TABLESPACE hhaclaims_ts OWNER bfduser LOCATION '/u01/pg_HHAClaims_ts';
CREATE TABLESPACE inpatientclaimlines_ts OWNER bfduser LOCATION '/u01/pg_InpatientClaimLines_ts';
CREATE TABLESPACE inpatientclaims_ts OWNER bfduser LOCATION '/u01/pg_InpatientClaims_ts';
CREATE TABLESPACE outpatientclaimlines_ts OWNER bfduser LOCATION '/u01/pg_OutpatientClaimLines_ts';
CREATE TABLESPACE outpatientclaims_ts OWNER bfduser LOCATION '/u01/pg_OutpatientClaims_ts';
CREATE TABLESPACE partdevents_ts OWNER bfduser LOCATION '/u01/pg_PartDEvents_ts';
CREATE TABLESPACE snfclaims_ts OWNER bfduser LOCATION '/u01/pg_SNFClaims_ts';
CREATE TABLESPACE dmeclaimlines_ts OWNER bfduser LOCATION '/u01/pg_DMEClaimLines_ts';
CREATE TABLESPACE hospiceclaims_ts OWNER bfduser LOCATION '/u01/pg_HospiceClaims_ts';
CREATE TABLESPACE snfclaimlines_ts OWNER bfduser LOCATION '/u01/pg_SNFClaimLines_ts';
CREATE TABLESPACE hospiceclaimlines_ts OWNER bfduser LOCATION '/u01/pg_HospiceClaimLines_ts';
CREATE TABLESPACE dba_util_ts OWNER bfduser LOCATION '/u01/pg_dba_util_ts';
CREATE TABLESPACE medicarebeneficiaryidhistory_ts OWNER bfduser LOCATION '/u01/pg_MedicareBeneficiaryIdHistory_ts';
