/*
 * Creates the RDA FISS pre-adjudicated claims tables required by the application.
 * https://confluenceent.cms.gov/pages/viewpage.action?spaceKey=MPSM&title=MVP+0.1+Data+Dictionary+for+Replicated+Data+Access+API
 */

CREATE SCHEMA IF NOT EXISTS "pre_adj";

/*
 * Where possible column names map directly to fields in the RDA API result message.
 */
create table "pre_adj"."FissClaims"
(
    "dcn"               varchar(23) not null,
    "hicNo"             varchar(12) not null,
    "currStatus"        char(1)     not null,
    "currLoc1"          char(1)     not null,
    "currLoc2"          varchar(5)  not null,
    "medaProvId"        varchar(13),
    "totalChargeAmount" decimal(11, 2),
    "receivedDate"      date,
    "currTranDate"      date,
    "admitDiagCode"     varchar(7),
    "principleDiag"     varchar(7),
    "npiNumber"         varchar(10),
    "mbi"               varchar(13),
    "mbiHash"           varchar(64),
    "fedTaxNumber"      varchar(10),
    "lastUpdated"       timestamp with time zone,
    constraint "FissClaims_pkey" primary key ("dcn")
);

/*
 * Index the MedicareBeneficiaryIdentifier (MBI) hash column for mbi searches.
 * The search is used by partners to find claims for specific beneficiaries.
 * Since this is a frequent operation, the hash is indexed.
 */

create index "FissClaims_mbi_hash_idx"
    on "pre_adj"."FissClaims" ("mbiHash");

/*
 * Where possible column names map directly to fields in the RDA API result message.
 *
 * The priority column is a 0 based index indicating the order in which the codes
 * appear in the RDA API result message for the claim.
 */
create table "pre_adj"."FissProcCodes"
(
    "dcn"         varchar(23) not null,
    "priority"    smallint    not null,
    "procCode"    varchar(10) not null,
    "procFlag"    varchar(4),
    "procDate"    date,
    "lastUpdated" timestamp with time zone,
    constraint "FissProcCodes_pkey" primary key ("dcn", "priority"),
    constraint "FissProcCodes_claim" foreign key ("dcn") references "pre_adj"."FissClaims" ("dcn")
);
