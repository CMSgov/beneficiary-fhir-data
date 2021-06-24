/*
 * Creates RDA MCS pre-adjudicated claims tables required by the application.
 * https://confluenceent.cms.gov/display/MPSM/2021-04-22+Tech+Spec%3A+Specific+Schema+for+MVP+v0.2
 *
 * Where possible column names map directly to fields in the RDA API result message.
 */

/*
 * Add new FISS claim fields from RDA API MVP 0.2 release.
 */
alter table "pre_adj"."FissClaims"
    add "pracLocAddr1" ${type.text};
alter table "pre_adj"."FissClaims"
    add "pracLocAddr2" ${type.text};
alter table "pre_adj"."FissClaims"
    add "pracLocCity" ${type.text};
alter table "pre_adj"."FissClaims"
    add "pracLocState" varchar(2);
alter table "pre_adj"."FissClaims"
    add "pracLocZip" varchar(15);

/*
 * Add new FISS Claim diagnosis code table from RDA API 0.2 release.
 * The priority column is used to sort the detail records and is populated using array index in RDA
 * API response array.
 */

create table "pre_adj"."FissDiagnosisCodes" (
    "dcn"         varchar(23) not null,
    "priority"    smallint    not null,
    "diagCd2"     varchar(7)  not null,
    "diagPoaInd"  varchar(1)  not null,
    "bitFlags"    varchar(4),
    "lastUpdated" timestamp with time zone,
    constraint "FissDiagnosisCodes_pkey" primary key ("dcn", "priority"),
    constraint "FissDiagnosisCodes_claim" foreign key ("dcn") references "pre_adj"."FissClaims"("dcn")
);

/*
 * Add table for MSC claims.
 */
create table "pre_adj"."McsClaims" (
    "idrClmHdIcn"          varchar(15) not null,
    "idrContrId"           varchar(5)  not null,
    "idrHic"               varchar(12) not null,
    "idrClaimType"         varchar(1)  not null,
    "idrDtlCnt"            smallint,
    "idrBeneLast_1_6"      varchar(6),
    "idrBeneFirstInit"     varchar(1),
    "idrBeneMidInit"       varchar(1),
    "idrBeneSex"           varchar(1),
    "idrStatusCode"        varchar(1),
    "idrStatusDate"        date,
    "idrBillProvNpi"       varchar(10),
    "idrBillProvNum"       varchar(10),
    "idrBillProvEin"       varchar(10),
    "idrBillProvType"      varchar(2),
    "idrBillProvSpec"      varchar(2),
    "idrBillProvGroupInd"  varchar(1),
    "idrBillProvPriceSpec" varchar(2),
    "idrBillProvCounty"    varchar(2),
    "idrBillProvLoc"       varchar(2),
    "idrTotAllowed"        decimal(7, 2),
    "idrCoinsurance"       decimal(7, 2),
    "idrDeductible"        decimal(7, 2),
    "idrBillProvStatusCd"  varchar(1),
    "idrTotBilledAmt"      decimal(7, 2),
    "idrClaimReceiptDate"  date,
    "idrClaimMbi"          varchar(13),
    "idrClaimMbiHash"      varchar(64),
    "lastUpdated"          timestamp with time zone,
    constraint "McsClaims_pkey" primary key ("idrClmHdIcn")
);

/*
 * Index the MedicareBeneficiaryIdentifier (MBI) hash column for mbi searches.
 * The search is used by partners to find claims for specific beneficiaries.
 * Since this is a frequent operation, the hash is indexed.
 */

create index "McsClaims_mbi_hash_idx"
    on "pre_adj"."McsClaims" ("idrClaimMbiHash");

/*
 * The priority column is used to sort the detail records and is populated using array index in RDA
 * API response array.
 */
create table "pre_adj"."McsDiagnosisCodes" (
    "idrClmHdIcn" varchar(15) not null,
    "priority"    smallint    not null,
    "diagIcdType" varchar(1),
    "diagCode"    varchar(7),
    "lastUpdated" timestamp with time zone,
    constraint "McsDiagnosisCodes_pkey" primary key ("idrClmHdIcn", "priority"),
    constraint "McsDiagnosisCodes_claim" foreign key ("idrClmHdIcn") references "pre_adj"."McsClaims"("idrClmHdIcn")
);

/*
 * The priority column is used to sort the detail records and is populated using array index in RDA
 * API response array.
 */
create table "pre_adj"."McsDetails" (
    "idrClmHdIcn"           varchar(15) not null,
    "priority"              smallint    not null,
    "idrDtlStatus"          varchar(1),
    "idrDtlFromDate"        date,
    "idrDtlToDate"          date,
    "idrProcCode"           varchar(5),
    "idrModOne"             varchar(2),
    "idrModTwo"             varchar(2),
    "idrModThree"           varchar(2),
    "idrModFour"            varchar(2),
    "idrDtlDiagIcdType"     varchar(1),
    "idrDtlPrimaryDiagCode" varchar(7),
    "idrKPosLnameOrg"       varchar(60),
    "idrKPosFname"          varchar(35),
    "idrKPosMname"          varchar(25),
    "idrKPosAddr1"          varchar(55),
    "idrKPosAddr2_1st"      varchar(30),
    "idrKPosAddr2_2nd"      varchar(25),
    "idrKPosCity"           varchar(30),
    "idrKPosState"          varchar(2),
    "idrKPosZip"            varchar(15),
    "lastUpdated"           timestamp with time zone,
    constraint "McsDetails_pkey" primary key ("idrClmHdIcn", "priority"),
    constraint "McsDetails_claim" foreign key ("idrClmHdIcn") references "pre_adj"."McsClaims"("idrClmHdIcn")
);
