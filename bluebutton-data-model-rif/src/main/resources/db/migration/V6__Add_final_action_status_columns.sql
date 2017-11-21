/*
 * Alters the claim tables to add `finalAction` columns to each.
 * 
 * See http://issues.hhsdevcloud.us/browse/CBBD-301.
 */

alter table "CarrierClaims"
  add column "finalAction" char(1);

alter table "DMEClaims"
  add column "finalAction" char(1);

alter table "HHAClaims"
  add column "finalAction" char(1);

alter table "HospiceClaims"
  add column "finalAction" char(1);

alter table "InpatientClaims"
  add column "finalAction" char(1);

alter table "OutpatientClaims"
  add column "finalAction" char(1);

alter table "PartDEvents"
  add column "finalAction" char(1);

alter table "SNFClaims"
  add column "finalAction" char(1);
