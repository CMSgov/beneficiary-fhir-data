/*
 * This script will drop all primary keys, foreign keys, and indices used in 
 * the database. This is needed to speed up initial loads.
 */

alter table "CarrierClaimLines" drop constraint if exists "CarrierClaimLines_parentClaim_to_CarrierClaims";
alter table "DMEClaimLines" drop constraint if exists "DMEClaimLines_parentClaim_to_DMEClaims";
alter table "HHAClaimLines" drop constraint if exists "HHAClaimLines_parentClaim_to_HHAClaims";
alter table "HospiceClaimLines" drop constraint if exists "HospiceClaimLines_parentClaim_to_HospiceClaims";
alter table "InpatientClaimLines" drop constraint if exists "InpatientClaimLines_parentClaim_to_InpatientClaims";
alter table "OutpatientClaimLines" drop constraint if exists "OutpatientClaimLines_parentClaim_to_OutpatientClaims";
alter table "SNFClaimLines" drop constraint if exists "SNFClaimLines_parentClaim_to_SNFClaims";

alter table "Beneficiaries" drop constraint if exists "Beneficiaries_pkey";
alter table "CarrierClaimLines" drop constraint if exists "CarrierClaimLines_pkey";
alter table "CarrierClaims" drop constraint if exists "CarrierClaims_pkey";
alter table "DMEClaimLines" drop constraint if exists "DMEClaimLines_pkey";
alter table "DMEClaims" drop constraint if exists "DMEClaims_pkey";
alter table "HHAClaimLines" drop constraint if exists "HHAClaimLines_pkey";
alter table "HHAClaims" drop constraint if exists "HHAClaims_pkey";
alter table "HospiceClaimLines" drop constraint if exists "HospiceClaimLines_pkey";
alter table "HospiceClaims" drop constraint if exists "HospiceClaims_pkey";
alter table "InpatientClaimLines" drop constraint if exists "InpatientClaimLines_pkey";
alter table "InpatientClaims" drop constraint if exists "InpatientClaims_pkey";
alter table "OutpatientClaimLines" drop constraint if exists "OutpatientClaimLines_pkey";
alter table "OutpatientClaims" drop constraint if exists "OutpatientClaims_pkey";
alter table "PartDEvents" drop constraint if exists "PartDEvents_pkey";
alter table "SNFClaimLines" drop constraint if exists "SNFClaimLines_pkey";
alter table "SNFClaims" drop constraint if exists "SNFClaims_pkey";

drop index if exists "Beneficiaries_hicn_idx";
drop index if exists "BeneficiariesHistory_hicn_idx";