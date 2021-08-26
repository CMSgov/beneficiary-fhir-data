${logic.hsql} CREATE TYPE json AS longvarchar;

drop table "pre_adj"."FissDiagnosisCodes";
drop table "pre_adj"."FissPayers";
drop table "pre_adj"."FissProcCodes";
drop table "pre_adj"."FissClaims";

drop table "pre_adj"."McsDetails";
drop table "pre_adj"."McsDiagnosisCodes";
drop table "pre_adj"."McsClaims";

/**
  Simple container table to hold FissClaim objects as JSON with only columns needed for
  querying outside of the json.
 */
create table "pre_adj"."FissClaimsJson" (
    "dcn"            varchar(23)   not null,
    "mbi"            varchar(13),
    "mbiHash"        varchar(64),
    "stmtCovToDate"  date,
    "sequenceNumber" bigint        not null,
    "lastUpdated"    timestamp with time zone,
    "claim"          ${type.jsonb} not null,
    constraint "FissClaimsJson_pkey" primary key ("dcn")
);

/**
  Simple container table to hold McsClaim objects as JSON with only columns needed for
  querying outside of the json.
 */
create table "pre_adj"."McsClaimsJson" (
    "idrClmHdIcn"       varchar(15)   not null,
    "idrClaimMbi"       varchar(13),
    "idrClaimMbiHash"   varchar(64),
    "idrHdrToDateOfSvc" date,
    "sequenceNumber"    bigint        not null,
    "lastUpdated"       timestamp with time zone,
    "claim"             ${type.jsonb} not null,
    constraint "McsClaimsJson_pkey" primary key ("idrClmHdIcn")
);

/* Index to allow quick determination of where to resume stream in calls to RDA API. */
create index "FissClaimsJson_sequenceNumber_idx" on "pre_adj"."FissClaimsJson"("sequenceNumber");
create index "McsClaimsJson_sequenceNumber_idx" on "pre_adj"."McsClaimsJson"("sequenceNumber");

/* Index to allow fast query by update time by BFD API. */
create index "FissClaimsJson_lastUpdated_idx" on "pre_adj"."FissClaimsJson"("lastUpdated");
create index "McsClaimsJson_lastUpdated_idx" on "pre_adj"."McsClaimsJson"("lastUpdated");

/* Index to allow fast query by MBI by BFD API. */
create index "FissClaimsJson_mbi_idx" on "pre_adj"."FissClaimsJson"("mbi");
create index "McsClaimsJson_idrClaimMbi_idx" on "pre_adj"."McsClaimsJson"("idrClaimMbi");

/* Index to allow fast query by MBI hash by BFD API. */
create index "FissClaimsJson_mbiHash_idx" on "pre_adj"."FissClaimsJson"("mbiHash");
create index "McsClaimsJson_idrClaimMbiHash_idx" on "pre_adj"."McsClaimsJson"("idrClaimMbiHash");

/* Index to allow fast query by service date by BFD API. */
create index "FissClaimsJson_stmtCovToDate_idx" on "pre_adj"."FissClaimsJson"("stmtCovToDate");
create index "McsClaimsJson_idrHdrToDateOfSvc_idx" on "pre_adj"."McsClaimsJson"("idrHdrToDateOfSvc");
