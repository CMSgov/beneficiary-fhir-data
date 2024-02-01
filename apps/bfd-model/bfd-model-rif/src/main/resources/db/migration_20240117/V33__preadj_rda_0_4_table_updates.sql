/*
 * Creates RDA FissPayers table for pre-adjudicated claim payers and adds
 * new columns to FissClaims for RDA API 0.4.
 * https://confluenceent.cms.gov/display/MPSM/MVP+0.4+Data+Dictionary+for+Replicated+Data+Access+API
 *
 * Where possible column names map directly to fields in the RDA API result message.
 */

/*
 * Add new FISS claim fields from RDA API MVP 0.2 release.
 */
alter table "pre_adj"."FissClaims"
    add "lobCd" varchar(1);
alter table "pre_adj"."FissClaims"
    add "servTypeCdMapping" varchar(20);
alter table "pre_adj"."FissClaims"
    add "servTypeCd" varchar(1);
alter table "pre_adj"."FissClaims"
    add "freqCd" varchar(1);
alter table "pre_adj"."FissClaims"
    add "billTypCd" varchar(3);

/*
 * Add new FISS Claim payers table from RDA API 0.4 release.
 * The priority column is used to sort the payer records and is populated using array index in RDA
 * API response array.
 */

create table "pre_adj"."FissPayers" (
    "dcn"              varchar(23) not null,
    "priority"         smallint    not null,
    "payerType"        varchar(20),
    "payersId"         varchar(1),
    "payersName"       varchar(32),
    "relInd"           varchar(1),
    "assignInd"        varchar(1),
    "providerNumber"   varchar(13),
    "adjDcnIcn"        varchar(23),
    "priorPmt"         decimal(11, 2),
    "estAmtDue"        decimal(11, 2),
    "beneRel"          varchar(2),
    "beneLastName"     varchar(15),
    "beneFirstName"    varchar(10),
    "beneMidInit"      varchar(1),
    "beneSsnHic"       varchar(19),
    "insuredRel"       varchar(2),
    "insuredName"      varchar(25),
    "insuredSsnHic"    varchar(19),
    "insuredGroupName" varchar(17),
    "insuredGroupNbr"  varchar(20),
    "beneDob"          date,
    "beneSex"          varchar(1),
    "treatAuthCd"      varchar(18),
    "insuredSex"       varchar(1),
    "insuredRelX12"    varchar(2),
    "insuredDob"       date,
    "insuredDobText"   varchar(9),
    "lastUpdated"      timestamp with time zone,
    constraint "FissPayers_pkey" primary key ("dcn", "priority"),
    constraint "FissPayers_claim" foreign key ("dcn") references "pre_adj"."FissClaims"("dcn")
);
