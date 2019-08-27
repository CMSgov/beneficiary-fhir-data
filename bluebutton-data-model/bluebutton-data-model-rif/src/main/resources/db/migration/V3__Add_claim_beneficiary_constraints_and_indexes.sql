/*
 * Adds the foreign keys and indexes required to manage the relationships
 * between the claim tables and the "Beneficiaries" table.
 */


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
