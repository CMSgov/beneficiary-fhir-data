/*
 * Alters the claim tables to add `finalAction` columns to each, backfilling
 * existing rows with the value 'F'.
 * 
 * See:
 * * http://issues.hhsdevcloud.us/browse/CBBD-301
 * * http://issues.hhsdevcloud.us/browse/CBBD-361
 */

alter table "CarrierClaims" add column "finalAction" char(1);
update "CarrierClaims" set "finalAction" = 'F';
alter table "CarrierClaims" alter column "finalAction" set not null;

alter table "DMEClaims" add column "finalAction" char(1);
update "DMEClaims" set "finalAction" = 'F';
alter table "DMEClaims" alter column "finalAction" set not null;

alter table "HHAClaims" add column "finalAction" char(1);
update "HHAClaims" set "finalAction" = 'F';
alter table "HHAClaims" alter column "finalAction" set not null;

alter table "HospiceClaims" add column "finalAction" char(1);
update "HospiceClaims" set "finalAction" = 'F';
alter table "HospiceClaims" alter column "finalAction" set not null;

alter table "InpatientClaims" add column "finalAction" char(1);
update "InpatientClaims" set "finalAction" = 'F';
alter table "InpatientClaims" alter column "finalAction" set not null;

alter table "OutpatientClaims" add column "finalAction" char(1);
update "OutpatientClaims" set "finalAction" = 'F';
alter table "OutpatientClaims" alter column "finalAction" set not null;

alter table "PartDEvents" add column "finalAction" char(1);
update "PartDEvents" set "finalAction" = 'F';
alter table "PartDEvents" alter column "finalAction" set not null;

alter table "SNFClaims" add column "finalAction" char(1);
update "SNFClaims" set "finalAction" = 'F';
alter table "SNFClaims" alter column "finalAction" set not null;
