---------------------------
-- 3. DROP EXISTING TABLES
---------------------------

--3a. GENERATE DROP SQL SCRIPT
------------------------------
-- Run SQL Coammand:
select 'DROP TABLE '||schemaname||'."'||tablename||'" CASCADE;' from pg_tables where schemaname='public';

/**
fhirdb=# select 'DROP TABLE '||schemaname||'."'||tablename||'" CASCADE;' from pg_tables where schemaname='public';
                         ?column?
-----------------------------------------------------------
 DROP TABLE public."DMEClaims" CASCADE;
 DROP TABLE public."SNFClaims" CASCADE;
 DROP TABLE public."InpatientClaimLines" CASCADE;
 DROP TABLE public."duplicate_record_ids" CASCADE;
 DROP TABLE public."HospiceClaims" CASCADE;
 DROP TABLE public."HospiceClaimLines" CASCADE;
 DROP TABLE public."PartDEvents" CASCADE;
 DROP TABLE public."HHAClaims" CASCADE;
 DROP TABLE public."Beneficiaries" CASCADE;
 DROP TABLE public."CarrierClaimLines" CASCADE;
 DROP TABLE public."DMEClaimLines" CASCADE;
 DROP TABLE public."HHAClaimLines" CASCADE;
 DROP TABLE public."synthetic_record_ids" CASCADE;
 DROP TABLE public."OutpatientClaims" CASCADE;
 DROP TABLE public."SNFClaimLines" CASCADE;
 DROP TABLE public."CarrierClaims" CASCADE;
 DROP TABLE public."InpatientClaims" CASCADE;
 DROP TABLE public."OutpatientClaimLines" CASCADE;
 DROP TABLE public."MedicareBeneficiaryIdHistory" CASCADE;
 DROP TABLE public."BeneficiariesHistory_old" CASCADE;
 DROP TABLE public."BeneficiariesHistory" CASCADE;
(21 rows)
**/
--- 3b. REVIEW AND RUN THE ABOVE SQL COMMANDS.