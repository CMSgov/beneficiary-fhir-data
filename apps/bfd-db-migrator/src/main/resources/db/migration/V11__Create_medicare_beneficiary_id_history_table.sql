/*
 * Creates the MedicareBeneficiaryIdHistory table required by the application. This
 * schema is derived from the JPA metadata, via the 
 * gov.hhs.cms.bluebutton.data.pipeline.rif.schema.HibernateSchemaPrinter 
 * utility.
 */

${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "MedicareBeneficiaryIdHistory" (
  "medicareBeneficiaryIdKey" numeric not null,
  "beneficiaryId" numeric,
  "claimAccountNumber" varchar(9),
  "beneficiaryIdCode" varchar(2),
  "mbiSequenceNumber" numeric,
  "medicareBeneficiaryId" varchar(11),
  "mbiEffectiveDate" date,
  "mbiEndDate" date,
  "mbiEffectiveReasonCode" varchar(5),
  "mbiEndReasonCode" varchar(5),
  "mbiCardRequestDate" date,
  "mbiAddUser" varchar(30),
  "mbiAddDate" timestamp,
  "mbiUpdateUser" varchar(30),
  "mbiUpdateDate" timestamp,
  "mbiCrntRecIndId"  numeric,
   
    constraint "MedicareBeneficiaryIdHistory_pkey" primary key ("medicareBeneficiaryIdKey")
)
${logic.tablespaces-escape} tablespace "medicarebeneficiaryidhistory_ts"
;

create index "MedicareBeneficiaryIdHistory_beneficiaryId_idx"
  on "MedicareBeneficiaryIdHistory" ("beneficiaryId");
  
  





