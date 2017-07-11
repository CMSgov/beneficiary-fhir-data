/*
 * This script will re-create all primary keys, foreign keys, and indices used 
 * in the database. It's intended to undo (when/as needed) its sister 
 * `Drop_all_constraints.sql` script.
 */

-- alter table "CarrierClaimLines" drop constraint ${logic.if-exists} "CarrierClaimLines_parentClaim_to_CarrierClaims";
-- alter table "DMEClaimLines" drop constraint ${logic.if-exists} "DMEClaimLines_parentClaim_to_DMEClaims";
-- alter table "HHAClaimLines" drop constraint ${logic.if-exists} "HHAClaimLines_parentClaim_to_HHAClaims";
-- alter table "HospiceClaimLines" drop constraint ${logic.if-exists} "HospiceClaimLines_parentClaim_to_HospiceClaims";
-- alter table "InpatientClaimLines" drop constraint ${logic.if-exists} "InpatientClaimLines_parentClaim_to_InpatientClaims";
-- alter table "OutpatientClaimLines" drop constraint ${logic.if-exists} "OutpatientClaimLines_parentClaim_to_OutpatientClaims";
-- alter table "SNFClaimLines" drop constraint ${logic.if-exists} "SNFClaimLines_parentClaim_to_SNFClaims";

alter table "Beneficiaries" add constraint "Beneficiaries_pkey" primary key ("beneficiaryId");
alter table "CarrierClaimLines" add constraint "CarrierClaimLines_pkey" primary key ("lineNumber", "parentClaim");
alter table "CarrierClaims" add constraint "CarrierClaims_pkey" primary key ("claimId");
-- alter table "DMEClaimLines" drop constraint ${logic.if-exists} "DMEClaimLines_pkey";
-- alter table "DMEClaims" drop constraint ${logic.if-exists} "DMEClaims_pkey";
-- alter table "HHAClaimLines" drop constraint ${logic.if-exists} "HHAClaimLines_pkey";
-- alter table "HHAClaims" drop constraint ${logic.if-exists} "HHAClaims_pkey";
-- alter table "HospiceClaimLines" drop constraint ${logic.if-exists} "HospiceClaimLines_pkey";
-- alter table "HospiceClaims" drop constraint ${logic.if-exists} "HospiceClaims_pkey";
-- alter table "InpatientClaimLines" drop constraint ${logic.if-exists} "InpatientClaimLines_pkey";
-- alter table "InpatientClaims" drop constraint ${logic.if-exists} "InpatientClaims_pkey";
-- alter table "OutpatientClaimLines" drop constraint ${logic.if-exists} "OutpatientClaimLines_pkey";
-- alter table "OutpatientClaims" drop constraint ${logic.if-exists} "OutpatientClaims_pkey";
-- alter table "PartDEvents" drop constraint ${logic.if-exists} "PartDEvents_pkey";
-- alter table "SNFClaimLines" drop constraint ${logic.if-exists} "SNFClaimLines_pkey";
-- alter table "SNFClaims" drop constraint ${logic.if-exists} "SNFClaims_pkey";
