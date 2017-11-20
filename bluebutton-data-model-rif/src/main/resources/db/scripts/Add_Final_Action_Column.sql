/*
 * This script will add the finalAction column to the below tables with a default of "F"
 */


-- Add finalAction column to below tables.

  
alter table "CarrierClaims" 
   add column "finalAction" char(1) not null default "F";

alter table "DMEClaims" 
   add column "finalAction" char(1) not null default "F";

alter table "HHAClaims" 
    add column "finalAction" char(1) not null default "F";

alter table "HospiceClaims" 
    add column "finalAction" char(1) not null default "F";

alter table "InpatientClaims" 
    add column "finalAction" char(1) not null default "F";

alter table "OutpatientClaims" 
    add column "finalAction" char(1) not null default "F";

alter table "PartDEvents" 
    add column "finalAction" char(1) not null default "F";

alter table "SNFClaims" 
	add column "finalAction" char(1) not null default "F";


