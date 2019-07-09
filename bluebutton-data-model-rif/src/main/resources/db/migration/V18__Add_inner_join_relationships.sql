/*
 * Alters the BeneficiariesHistory and MedicareBeneficiaryIdHistory tables to add foreign
 * keys to Beneficiary.
 * 
 * See: https://jira.cms.gov/browse/BLUEBUTTON-865
 */

/*
 * Move all BeneificariesHistory entries that don't point to a valid Beneficiaries record to
 * a separate table (we could delete it, but people who delete data are bad people). Really,
 * we should not have ever received these BeneficiariesHistory entries from the CCW in the
 * first place: we can't use them, and they prevent us from having a foreign key (and JPA 
 * relationship) on this table.
 */
create table "BeneficiariesHistoryInvalidBeneficiaries" (
  "beneficiaryHistoryId" bigint not null,
  "beneficiaryId" varchar(15) not null,
  "birthDate" date not null,
  "hicn" varchar(64) not null,
  "sex" char(1) not null,
  "hicnUnhashed" varchar(11),
  "medicareBeneficiaryId" varchar(11),
  constraint "BeneficiariesHistoryInvalidBeneficiaries_pkey" primary key ("beneficiaryHistoryId")
)
${logic.tablespaces-escape} tablespace "beneficiaries_ts"
;
insert into "BeneficiariesHistoryInvalidBeneficiaries"
  select
    "beneficiaryHistoryId",
  	"beneficiaryId",
  	"birthDate",
  	"hicn",
  	"sex",
  	"hicnUnhashed",
  	"medicareBeneficiaryId"
    from "BeneficiariesHistory"
    left join "Beneficiaries"
      on "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
    where "Beneficiaries"."beneficiaryId" is NULL;
delete
  from "BeneficiariesHistory"
  where not exists (select "beneficiaryId"
  					from "Beneficiaries"
  					where "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId");

alter table "BeneficiariesHistory" 
  add constraint "BeneficiariesHistory_beneficiaryId_to_Beneficiary" 
  foreign key ("beneficiaryId") 
  references "Beneficiaries";

-- Move all MedicareBeneficiaryIdHistory entries that don't point to a valid Beneficiaries 
-- record to a separate table, just as we did with BeneficiariesHistory entries above.
create table "MedicareBeneficiaryIdHistoryInvalidBeneficiaries" (
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
  constraint "MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey" primary key ("medicareBeneficiaryIdKey")
)
${logic.tablespaces-escape} tablespace "beneficiaries_ts"
;
insert into "MedicareBeneficiaryIdHistoryInvalidBeneficiaries"
  select
    "medicareBeneficiaryIdKey",
  	"beneficiaryId",
  	"claimAccountNumber",
  	"beneficiaryIdCode",
  	"mbiSequenceNumber",
 	"medicareBeneficiaryId",
 	"mbiEffectiveDate",
 	"mbiEndDate",
 	"mbiEffectiveReasonCode",
 	"mbiEndReasonCode",
 	"mbiCardRequestDate",
	"mbiAddUser",
 	"mbiAddDate",
 	"mbiUpdateUser",
	"mbiUpdateDate",
 	"mbiCrntRecIndId"
    from "MedicareBeneficiaryIdHistory"
    left join "Beneficiaries"
      on "MedicareBeneficiaryIdHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
    where "Beneficiaries"."beneficiaryId" is NULL;
delete
  from "MedicareBeneficiaryIdHistory"
  where not exists (select "beneficiaryId"
  					from "Beneficiaries"
  					where "MedicareBeneficiaryIdHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId");

alter table "MedicareBeneficiaryIdHistory" 
   add constraint "MedicareBeneficiaryIdHistory_beneficiaryId_to_Beneficiary" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";
