/*
 * Rebuilds the claim line tables' PK indexes to have the correct column order:
 * `parentClaim` columns should always be the first index column, allowing for
 * fast WHERE clauses using these searches.
 * 
 * See http://issues.hhsdevcloud.us/browse/CBBD-297.
 */

alter table "CarrierClaimLines"
  drop constraint "CarrierClaimLines_pkey";
alter table "CarrierClaimLines"
  add constraint "CarrierClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");

alter table "DMEClaimLines"
  drop constraint "DMEClaimLines_pkey";
alter table "DMEClaimLines"
  add constraint "DMEClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");

alter table "HHAClaimLines"
  drop constraint "HHAClaimLines_pkey";
alter table "HHAClaimLines"
  add constraint "HHAClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");

alter table "HospiceClaimLines"
  drop constraint "HospiceClaimLines_pkey";
alter table "HospiceClaimLines"
  add constraint "HospiceClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");

alter table "InpatientClaimLines"
  drop constraint "InpatientClaimLines_pkey";
alter table "InpatientClaimLines"
  add constraint "InpatientClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");

alter table "OutpatientClaimLines"
  drop constraint "OutpatientClaimLines_pkey";
alter table "OutpatientClaimLines"
  add constraint "OutpatientClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");

alter table "SNFClaimLines"
  drop constraint "SNFClaimLines_pkey";
alter table "SNFClaimLines"
  add constraint "SNFClaimLines_pkey"
  primary key ("parentClaim", "lineNumber");
