/*
 * This script will drop all primary keys, foreign keys, and indices used in 
 * the database. This is needed to speed up initial loads.
 */

alter table "CarrierClaimLines" drop constraint ${logic.if-exists} "CarrierClaimLines_parentClaim_to_CarrierClaims";
alter table "DMEClaimLines" drop constraint ${logic.if-exists} "DMEClaimLines_parentClaim_to_DMEClaims";
alter table "HHAClaimLines" drop constraint ${logic.if-exists} "HHAClaimLines_parentClaim_to_HHAClaims";
alter table "HospiceClaimLines" drop constraint ${logic.if-exists} "HospiceClaimLines_parentClaim_to_HospiceClaims";
alter table "InpatientClaimLines" drop constraint ${logic.if-exists} "InpatientClaimLines_parentClaim_to_InpatientClaims";
alter table "OutpatientClaimLines" drop constraint ${logic.if-exists} "OutpatientClaimLines_parentClaim_to_OutpatientClaims";
alter table "SNFClaimLines" drop constraint ${logic.if-exists} "SNFClaimLines_parentClaim_to_SNFClaims";

alter table "Beneficiaries" drop constraint ${logic.if-exists} "Beneficiaries_pkey";
alter table "CarrierClaimLines" drop constraint ${logic.if-exists} "CarrierClaimLines_pkey";
alter table "CarrierClaims" drop constraint ${logic.if-exists} "CarrierClaims_pkey";
alter table "DMEClaimLines" drop constraint ${logic.if-exists} "DMEClaimLines_pkey";
alter table "DMEClaims" drop constraint ${logic.if-exists} "DMEClaims_pkey";
alter table "HHAClaimLines" drop constraint ${logic.if-exists} "HHAClaimLines_pkey";
alter table "HHAClaims" drop constraint ${logic.if-exists} "HHAClaims_pkey";
alter table "HospiceClaimLines" drop constraint ${logic.if-exists} "HospiceClaimLines_pkey";
alter table "HospiceClaims" drop constraint ${logic.if-exists} "HospiceClaims_pkey";
alter table "InpatientClaimLines" drop constraint ${logic.if-exists} "InpatientClaimLines_pkey";
alter table "OutpatientClaimLines" drop constraint ${logic.if-exists} "OutpatientClaimLines_pkey";
alter table "OutpatientClaims" drop constraint ${logic.if-exists} "OutpatientClaims_pkey";
alter table "PartDEvents" drop constraint ${logic.if-exists} "PartDEvents_pkey";
alter table "SNFClaimLines" drop constraint ${logic.if-exists} "SNFClaimLines_pkey";
alter table "SNFClaims" drop constraint ${logic.if-exists} "SNFClaims_pkey";
