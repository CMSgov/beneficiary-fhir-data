/*
 * Creates the RDA FISS pre-adjudicated claims tables required by the application.
 * https://confluenceent.cms.gov/pages/viewpage.action?spaceKey=MPSM&title=MVP+0.1+Data+Dictionary+for+Replicated+Data+Access+API
 */

/*
 TODO - Determine whether we want to store tables in public schema or create a new one.
 For now the table names are prefixed with PreAdj (pre-adjudicated).
 */

/*
TODO - what tablespace should we use as a default?
*/
${logic.tablespaces-escape} SET default_tablespace = fhirdb_ts2;

create table "PreAdjFissClaims"
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
    constraint "PreAdjFissClaims_pkey" primary key ("dcn")
)
/*
TODO - should we have a tablespace for this table or just use the default?
${logic.tablespaces-escape} tablespace "preadjfissclaims_ts"
*/
;

/*
 * Index the MedicareBeneficiaryIdentifier (MBI) hash column for mbi searches.
 * The search is used by partners to find claims for specific beneficiaries.
 * Since this is a frequent operation, the hash is indexed.
 */

create index "PreAdjFissClaims_mbi_hash_idx"
    on "PreAdjFissClaims" ("mbiHash");

/*
 * Columns with leading underscore in the name are unique to our database.
 * Other columns map directly to fields in the RDA API result message.
 *
 * The _priority column is a 0 based index indicating the order in which the codes
 * appear in the RDA API result message for the claim.
 */
create table "PreAdjFissProcCodes"
(
    "dcn"         varchar(23) not null,
    "_priority"   smallint    not null,
    "procCode"    varchar(10) not null,
    "procFlag"    varchar(4),
    "procDate"    date,
    "lastUpdated" timestamp with time zone,
    constraint "PreAdjFissProcCodes_pkey" primary key ("dcn", "_priority"),
    constraint "PreAdjFissProcCodes_claim" foreign key ("dcn") references "PreAdjFissClaims" ("dcn")
)
/*
TODO - should we have a tablespace for this table or just use the default?
${logic.tablespaces-escape} tablespace "preadjfissproccodes_ts"
*/
;
