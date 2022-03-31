/*
 * The column doesn't have a default value to avoid updating the column on migration. The pipeline server
 * will populate the column as new beneficaries are added or existing beneficaries are updated. 
 */

alter table "Beneficiaries" add column "derivedMailingAddress1" varchar(40);
alter table "Beneficiaries" add column "derivedMailingAddress2" varchar(40);
alter table "Beneficiaries" add column "derivedMailingAddress3" varchar(40);
alter table "Beneficiaries" add column "derivedMailingAddress4" varchar(40);
alter table "Beneficiaries" add column "derivedMailingAddress5" varchar(40);
alter table "Beneficiaries" add column "derivedMailingAddress6" varchar(40);
alter table "Beneficiaries" add column "derivedCityName" varchar(100);
alter table "Beneficiaries" add column "derivedStateCode" varchar(2);
alter table "Beneficiaries" add column "derivedZipCode" varchar(9);
alter table "Beneficiaries" add column "mbiEffectiveDate" date;
alter table "Beneficiaries" add column "mbiObsoleteDate" date;
alter table "Beneficiaries" add column "beneLinkKey" numeric(38);

alter table "BeneficiariesHistory" add column "mbiEffectiveDate" date;
alter table "BeneficiariesHistory" add column "mbiObsoleteDate" date;

alter table "InpatientClaims" add column "claimUncompensatedCareAmount" numeric(38, 2);

alter table "CarrierClaims" add column "claimCarrierControlNumber" varchar(23);

alter table "DMEClaims" add column "claimCarrierControlNumber" varchar(23);

alter table "HHAClaims" add column "fiDocumentClaimControlNumber" varchar(23);
alter table "HHAClaims" add column "fiOriginalClaimControlNumber" varchar(23);

alter table "HospiceClaims" add column "fiDocumentClaimControlNumber" varchar(23);
alter table "HospiceClaims" add column "fiOriginalClaimControlNumber" varchar(23);

alter table "InpatientClaims" add column "fiDocumentClaimControlNumber" varchar(23);
alter table "InpatientClaims" add column "fiOriginalClaimControlNumber" varchar(23);

alter table "OutpatientClaims" add column "fiDocumentClaimControlNumber" varchar(23);
alter table "OutpatientClaims" add column "fiOriginalClaimControlNumber" varchar(23);

alter table "SNFClaims" add column "fiDocumentClaimControlNumber" varchar(23);
alter table "SNFClaims" add column "fiOriginalClaimControlNumber" varchar(23);
