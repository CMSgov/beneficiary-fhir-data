/*
 * Alters the claim line tables' `parentClaim` columns to have the correct 
 * width: 15, instead of 255.
 * 
 * See http://issues.hhsdevcloud.us/browse/CBBD-306.
 */

alter table "CarrierClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);

alter table "DMEClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);

alter table "HHAClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);

alter table "HospiceClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);

alter table "InpatientClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);

alter table "OutpatientClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);

alter table "SNFClaimLines"
  alter column "parentClaim" ${logic.alter-column-type} varchar(15);
