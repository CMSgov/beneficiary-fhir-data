/*
 * Creates the beneficiary ID history table required by the application. This
 * schema is derived from the JPA metadata, via the 
 * gov.hhs.cms.bluebutton.data.pipeline.rif.schema.HibernateSchemaPrinter 
 * utility.
 */

${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "BeneficiariesHistory" (
  "beneficiaryHistoryId" bigint not null,
  "beneficiaryId" varchar(15) not null,
  "birthDate" date not null,
  "hicn" varchar(64) not null,
  "sex" char(1) not null,
  constraint "BeneficiariesHistory_pkey" primary key ("beneficiaryHistoryId")
)
${logic.tablespaces-escape} tablespace "beneficiaries_ts"
;

/*
 * FIXME For consistency, sequence names should be mixed-case, but can't be, due
 * to https://hibernate.atlassian.net/browse/HHH-9431.
 */
create sequence beneficiaryHistory_beneficiaryHistoryId_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 50;

create index "BeneficiariesHistory_hicn_idx"
  on "BeneficiariesHistory" ("hicn");