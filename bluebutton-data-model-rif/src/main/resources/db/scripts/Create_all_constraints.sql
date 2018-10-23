/*
 * This script will re-create all primary keys, foreign keys, and indices used 
 * in the database. It's intended to undo (when/as needed) its sister 
 * `Drop_all_constraints.sql` script.
 */


-- Create all of the primary keys.
alter table "Beneficiaries" 
    add constraint "Beneficiaries_pkey" 
    primary key ("beneficiaryId");

alter table "BeneficiariesHistory"
    add constraint "BeneficiariesHistory_pkey"
    primary key ("beneficiaryHistoryId");

alter table "CarrierClaimLines" 
    add constraint "CarrierClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "CarrierClaims" 
    add constraint "CarrierClaims_pkey" 
    primary key ("claimId");

alter table "DMEClaimLines" 
    add constraint "DMEClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "DMEClaims" 
    add constraint "DMEClaims_pkey" 
    primary key ("claimId");

alter table "HHAClaimLines" 
    add constraint "HHAClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "HHAClaims" 
    add constraint "HHAClaims_pkey" 
    primary key ("claimId");

alter table "HospiceClaimLines" 
    add constraint "HospiceClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "HospiceClaims" 
    add constraint "HospiceClaims_pkey" 
    primary key ("claimId");

alter table "InpatientClaimLines" 
    add constraint "InpatientClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "InpatientClaims" 
    add constraint "InpatientClaims_pkey" 
    primary key ("claimId");

alter table "OutpatientClaimLines" 
    add constraint "OutpatientClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "OutpatientClaims" 
    add constraint "OutpatientClaims_pkey" 
    primary key ("claimId");

alter table "PartDEvents" 
    add constraint "PartDEvents_pkey" 
    primary key ("eventId");

alter table "SNFClaimLines" 
    add constraint "SNFClaimLines_pkey" 
    primary key ("parentClaim", "lineNumber");

alter table "SNFClaims" 
    add constraint "SNFClaims_pkey" 
    primary key ("claimId");


-- Create all of the header-to-line table foreign keys.
alter table "CarrierClaimLines" 
   add constraint "CarrierClaimLines_parentClaim_to_CarrierClaims" 
   foreign key ("parentClaim") 
   references "CarrierClaims";

alter table "DMEClaimLines" 
   add constraint "DMEClaimLines_parentClaim_to_DMEClaims" 
   foreign key ("parentClaim") 
   references "DMEClaims";

alter table "HHAClaimLines" 
   add constraint "HHAClaimLines_parentClaim_to_HHAClaims" 
   foreign key ("parentClaim") 
   references "HHAClaims";

alter table "HospiceClaimLines" 
   add constraint "HospiceClaimLines_parentClaim_to_HospiceClaims" 
   foreign key ("parentClaim") 
   references "HospiceClaims";

alter table "InpatientClaimLines" 
   add constraint "InpatientClaimLines_parentClaim_to_InpatientClaims" 
   foreign key ("parentClaim") 
   references "InpatientClaims";

alter table "OutpatientClaimLines" 
   add constraint "OutpatientClaimLines_parentClaim_to_OutpatientClaims" 
   foreign key ("parentClaim") 
   references "OutpatientClaims";

alter table "SNFClaimLines" 
   add constraint "SNFClaimLines_parentClaim_to_SNFClaims" 
   foreign key ("parentClaim") 
   references "SNFClaims";


-- Create all of the claim-to-bene table foreign keys.
alter table "CarrierClaims" 
   add constraint "CarrierClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "DMEClaims" 
   add constraint "DMEClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "HHAClaims" 
   add constraint "HHAClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "HospiceClaims" 
   add constraint "HospiceClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "InpatientClaims" 
   add constraint "InpatientClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "OutpatientClaims" 
   add constraint "OutpatientClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "PartDEvents" 
   add constraint "PartDEvents_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";

alter table "SNFClaims" 
   add constraint "SNFClaims_beneficiaryId_to_Beneficiaries" 
   foreign key ("beneficiaryId") 
   references "Beneficiaries";


-- Create all of the "beneficiaryId" column indexes for claim tables.
create index "CarrierClaims_beneficiaryId_idx" 
    on "CarrierClaims" ("beneficiaryId");

create index "DMEClaims_beneficiaryId_idx" 
    on "DMEClaims" ("beneficiaryId");

create index "HHAClaims_beneficiaryId_idx" 
    on "HHAClaims" ("beneficiaryId");

create index "HospiceClaims_beneficiaryId_idx" 
    on "HospiceClaims" ("beneficiaryId");

create index "InpatientClaims_beneficiaryId_idx" 
    on "InpatientClaims" ("beneficiaryId");

create index "OutpatientClaims_beneficiaryId_idx" 
    on "OutpatientClaims" ("beneficiaryId");

create index "PartDEvents_beneficiaryId_idx" 
    on "PartDEvents" ("beneficiaryId");

create index "SNFClaims_beneficiaryId_idx" 
    on "SNFClaims" ("beneficiaryId");


-- Create all of the HICN indexes on beneficiary tables.
create index ${logic.index-create-concurrently} "Beneficiaries_hicn_idx"
    on "Beneficiaries" ("hicn");

create index "BeneficiariesHistory_hicn_idx"
  on "BeneficiariesHistory" ("hicn");