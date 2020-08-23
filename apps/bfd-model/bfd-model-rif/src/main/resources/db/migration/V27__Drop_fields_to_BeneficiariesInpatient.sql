/*
 * The column doesn't have a default value to avoid updating the column on migration. The pipeline server
 * will populate the column as new beneficaries are added or existing beneficaries are updated. 
 */

alter table "Beneficiaries" drop column "derivedMailingAddress1";
alter table "Beneficiaries" drop column "derivedMailingAddress2";
alter table "Beneficiaries" drop column "derivedMailingAddress3";
alter table "Beneficiaries" drop column "derivedMailingAddress4";
alter table "Beneficiaries" drop column "derivedMailingAddress5";
alter table "Beneficiaries" drop column "derivedMailingAddress6";
alter table "Beneficiaries" drop column "derivedCityName";
alter table "Beneficiaries" drop column "derivedStateCode";
alter table "Beneficiaries" drop column "derivedZipCode";
alter table "Beneficiaries" drop column "mbiEffectiveDate";
alter table "Beneficiaries" drop column "mbiObsoleteDate";
alter table "Beneficiaries" drop column "beneLinkKey";

alter table "BeneficiariesHistory" drop column "mbiEffectiveDate";
alter table "BeneficiariesHistory" drop column "mbiObsoleteDate";

alter table "InpatientClaimLines" drop column "clmUncompensatedCareAmount";
